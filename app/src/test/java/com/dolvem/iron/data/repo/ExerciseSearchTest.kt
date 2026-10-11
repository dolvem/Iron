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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D31: la búsqueda es case-insensitive sobre la columna normalizada nameKey, la misma que usa la
// unicidad de D4, porque LIKE en SQLite solo pliega ASCII. La normalización del patrón la hace el
// repositorio con la única función de D4.
@RunWith(RobolectricTestRunner::class)
class ExerciseSearchTest {

    private lateinit var db: IronDatabase
    private lateinit var repo: ExerciseRepo

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        repo = ExerciseRepo(db, db.exerciseDao(), db.planExerciseDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    // D31 y D4: pliega mayúsculas y acentos, porque el patrón entra por nameKey.
    @Test
    fun searchFoldsCaseAndAccentsThroughNameKey() = runBlocking {
        repo.createExercise("Péculo", null)
        repo.createExercise("Sentadilla", null)

        val found = repo.search("péCULo", null, false).first()
        assertEquals(1, found.size)
        assertEquals("Péculo", found.first().name)
    }

    // D31: la caja de búsqueda es de substring, no de nombre exacto. El acento se conserva, porque la
    // normalización de D4 es NFC y lowercasing, no eliminación de acentos.
    @Test
    fun searchMatchesSubstring() = runBlocking {
        repo.createExercise("Sentadilla búlgara", null)
        repo.createExercise("Peso muerto", null)

        val found = repo.search("BÚLGAR", null, false).first()
        assertEquals(1, found.size)
        assertEquals("Sentadilla búlgara", found.first().name)
    }

    // Reglas de orden y D81: archived queda excluido por defecto, y el filtro es excluyente, así que
    // activado muestra solo archivados. No hay un estado "todos incluidos", y por eso con el filtro
    // activado el ejercicio activo no aparece.
    @Test
    fun archivedFilterIsExclusive() = runBlocking {
        (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val archived = (repo.createExercise("Peso muerto", null) as Outcome.Ok<Exercise>).value
        repo.setArchived(archived, true)

        val hidden = repo.search("", null, false).first()
        assertEquals(listOf("Sentadilla"), hidden.map { it.name })

        val onlyArchived = repo.search("", null, true).first()
        assertEquals(listOf("Peso muerto"), onlyArchived.map { it.name })
    }

    // D32: los candidatos de fusión se leen con la lectura que ignora el filtro de archivado, porque si
    // el destino archivado no fuera alcanzable la regla del menos restrictivo no se aplicaría nunca. Se
    // lee con query vacía: el filtro de la lista no limita el destino que el usuario elige. Con D81 la
    // búsqueda de la pantalla es excluyente, así que esta lectura es aparte.
    @Test
    fun mergeCandidatesIgnoreTheQueryAndIncludeArchived() = runBlocking {
        repo.createExercise("Sentadilla", null)
        val archived = (repo.createExercise("Peso muerto", null) as Outcome.Ok<Exercise>).value
        repo.setArchived(archived, true)

        assertEquals(1, repo.search("sentadilla", null, false).first().size)
        assertEquals(2, repo.searchIncludingArchived("", null).first().size)
    }

    // Reglas de orden: los ejercicios por nombre, que es nameKey. Con la colación BINARY de SQLite,
    // ORDER BY name pondría "Zapada" antes que "gemelo".
    @Test
    fun orderUsesNameKeySoItIsCaseInsensitive() = runBlocking {
        repo.createExercise("Zapada", null)
        repo.createExercise("gemelo", null)

        val all = repo.search("", null, false).first()
        assertEquals(listOf("gemelo", "Zapada"), all.map { it.name })
    }

    // El filtro de categoría admite null, que es "todas las categorías", y el orden es por nombre.
    @Test
    fun categoryFilterAndOrderByName() = runBlocking {
        val leg = db.categoryDao().insert(Category(0, "Pierna", "pierna")).toInt()
        repo.createExercise("Gemelo", leg)
        repo.createExercise("Sentadilla", null)

        val all = repo.search("", null, false).first()
        assertEquals(2, all.size)
        assertEquals("Gemelo", all.first().name)

        val filtered = repo.search("", leg, false).first()
        assertEquals(1, filtered.size)
        assertEquals("Gemelo", filtered.first().name)
    }
}
