// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.dolvem.iron.data.db.ClosedBy
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.domain.model.Outcome
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D4 y D5: la violación de único sobre nameKey se captura en el repositorio y se mapea a
// DuplicateName, y la preconsulta de conteo es un método separado del borrado.
@RunWith(RobolectricTestRunner::class)
class ExerciseRepoTest {

    private lateinit var db: IronDatabase
    private lateinit var repo: ExerciseRepo

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        repo = ExerciseRepo(db.exerciseDao(), db.planExerciseDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    // D4: el índice único está en nameKey, así que la comprobación pliega mayúsculas y acentos.
    @Test
    fun duplicateNameOnNormalisedKeyFoldsCaseAndAccents() = runBlocking {
        val created = repo.createExercise("Péculo", null)
        assertTrue(created is Outcome.Ok)
        assertEquals(Outcome.DuplicateName("péculo"), repo.createExercise("péculo", null))
    }

    @Test
    fun renameToExistingNameReturnsDuplicateName() = runBlocking {
        repo.createExercise("Sentadilla", null)
        val second = (repo.createExercise("Peso muerto", null) as Outcome.Ok<Exercise>).value
        assertEquals(Outcome.DuplicateName("sentadilla"), repo.renameExercise(second, "sentadilla"))
    }

    @Test
    fun renameToNewNameReturnsOk() = runBlocking {
        val exercise = (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val renamed = repo.renameExercise(exercise, "Sentadilla búlgara") as Outcome.Ok<Exercise>
        assertEquals("Sentadilla búlgara", renamed.value.name)
        assertEquals("sentadilla búlgara", renamed.value.nameKey)
    }

    // D5: el conteo se hace antes del CASCADE, por eso es un método separado del borrado.
    @Test
    fun precountCountsAffectedSetsBeforeDelete() = runBlocking {
        val exercise = (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val workoutId = db
            .workoutDao()
            .insert(Workout(0, 0L, LocalDate.of(2026, 10, 9), null, ClosedBy.EXPLICIT, null))
            .toInt()
        db.setDao().insert(ExerciseSet(0, exercise.id, workoutId, 0L, 10, 100.0))
        db.setDao().insert(ExerciseSet(0, exercise.id, workoutId, 1L, 8, 100.0))

        assertEquals(2, repo.affectedSets(exercise.id))

        assertEquals(Outcome.Ok(Unit), repo.deleteExercise(exercise))
        assertEquals(0, repo.affectedSets(exercise.id))
    }
}
