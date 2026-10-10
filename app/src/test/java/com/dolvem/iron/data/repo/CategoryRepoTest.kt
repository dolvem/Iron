// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.dolvem.iron.data.db.Category
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.domain.model.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D4 y D5 en categorías: la violación de único sobre nameKey se mapea a DuplicateName, la preconsulta de
// conteo es un método separado del borrado, y el borrado es SET NULL.
// D32: en la fusión sobrevive el nombre del destino y los ejercicios se remapean.
@RunWith(RobolectricTestRunner::class)
class CategoryRepoTest {

    private lateinit var db: IronDatabase
    private lateinit var categories: CategoryRepo
    private lateinit var exercises: ExerciseRepo

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        categories = CategoryRepo(db, db.categoryDao())
        exercises = ExerciseRepo(db, db.exerciseDao(), db.planExerciseDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    // D4: el índice único está en nameKey, así que la comprobación pliega mayúsculas y acentos.
    @Test
    fun duplicateNameOnNormalisedKeyFoldsCaseAndAccents() = runBlocking {
        val created = categories.createCategory("Péculo")
        assertTrue(created is Outcome.Ok)
        assertEquals(Outcome.DuplicateName("péculo"), categories.createCategory("péculo"))
    }

    // Regla de borrado: SET NULL. El ejercicio no se pierde, queda sin categoría, y eso es lo que la
    // confirmación cuenta.
    @Test
    fun deleteCategoryLeavesExercisesWithoutCategory() = runBlocking {
        val category = (categories.createCategory("Pierna") as Outcome.Ok<Category>).value
        val exercise = (exercises.createExercise("Sentadilla", category.id) as Outcome.Ok<Exercise>).value

        assertEquals(1, categories.affectedExercises(category.id))

        assertEquals(Outcome.Ok(Unit), categories.deleteCategory(category))
        assertEquals(0, categories.affectedExercises(category.id))

        val after = db.exerciseDao().observeById(exercise.id).first()
        assertNull(after?.categoryId)
    }

    // D32: sobrevive el nombre del destino y los ejercicios se remapean antes del borrado del origen.
    @Test
    fun mergeKeepsDestinationNameAndRemapsExercises() = runBlocking {
        val origin = (categories.createCategory("Pierna") as Outcome.Ok<Category>).value
        val destination = (categories.createCategory("Gemelos") as Outcome.Ok<Category>).value
        val exercise = (exercises.createExercise("Pantorrilla", origin.id) as Outcome.Ok<Exercise>).value

        val summary = categories.mergeCategory(origin, destination) as Outcome.Ok<CategoryMergeSummary>
        assertEquals(1, summary.value.remappedExercises)
        assertEquals("Gemelos", summary.value.destination.name)

        val after = db.exerciseDao().observeById(exercise.id).first()
        assertEquals(destination.id, after?.categoryId)
        assertNull(db.categoryDao().observeById(origin.id).first())
    }

    // Reglas de orden: las categorías por nombre, que es nameKey por la colación BINARY de SQLite.
    @Test
    fun orderUsesNameKeySoItIsCaseInsensitive() = runBlocking {
        categories.createCategory("Pierna")
        categories.createCategory("gemelos")

        val all = categories.observeAll().first()
        assertEquals(listOf("gemelos", "Pierna"), all.map { it.name })
    }
}
