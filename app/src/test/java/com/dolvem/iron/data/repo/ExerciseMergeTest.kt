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
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.data.db.PlanExercise
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.domain.model.Outcome
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D32: el remapeo de las FKs y el conflicto de unique(planId, exerciseId) se comprueban sobre Room en
// memoria. Las reglas puras (archived y notes) están en MergeTest.
@RunWith(RobolectricTestRunner::class)
class ExerciseMergeTest {

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

    // D32: las series se remapean al destino antes del CASCADE del origen, así que el borrado del origen
    // no pierde los registros.
    @Test
    fun mergeRemapsSetsAndDeletesOrigin() = runBlocking {
        val origin = (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val destination = (repo.createExercise("Sentadilla búlgara", null) as Outcome.Ok<Exercise>).value
        val workoutId = db
            .workoutDao()
            .insert(Workout(0, 0L, LocalDate.of(2026, 10, 9), null, ClosedBy.EXPLICIT, null))
            .toInt()
        db.setDao().insert(ExerciseSet(0, origin.id, workoutId, 0L, 10, 100.0))

        val summary = repo.mergeExercise(origin, destination) as Outcome.Ok<ExerciseMergeSummary>
        assertEquals(1, summary.value.remappedSets)

        val sets = db.setDao().observeForExercise(destination.id).first()
        assertEquals(1, sets.size)
        assertEquals(destination.id, sets.first().exerciseId)

        assertEquals(0, repo.affectedSets(origin.id))
        assertNull(db.exerciseDao().observeById(origin.id).first())
    }

    // D32: en unique(planId, exerciseId) gana la fila existente, la misma regla que D11, y el conflicto
    // se registra en el resumen.
    @Test
    fun uniqueConflictKeepsExistingRowAndRecordsIt() = runBlocking {
        val origin = (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val destination = (repo.createExercise("Sentadilla búlgara", null) as Outcome.Ok<Exercise>).value
        val planId = db.planDao().insert(Plan(0, null, "", null)).toInt()
        db.planExerciseDao().insert(PlanExercise(0, planId, origin.id, 1))
        db.planExerciseDao().insert(PlanExercise(0, planId, destination.id, 2))

        val summary = repo.mergeExercise(origin, destination) as Outcome.Ok<ExerciseMergeSummary>
        assertEquals(1, summary.value.conflicts)
        assertEquals(0, summary.value.remappedPlanExercises)

        val rows = db.planExerciseDao().observeForPlan(planId).first()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(destination.id, row.exerciseId)
        // Gana la fila existente, con su configuración, no la del origen.
        assertEquals(2, row.sequence)
    }

    // D32: el destino conserva su nombre, en archived gana el menos restrictivo y las notas se concatenan
    // con el destino primero.
    @Test
    fun mergeKeepsDestinationNameAndAppliesLeastRestrictiveRules() = runBlocking {
        val origin = (repo.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value
        val destination = (repo.createExercise("Sentadilla búlgara", null) as Outcome.Ok<Exercise>).value

        // Las filas se escriben con el repositorio, así nameKey lo calcula la función de D4, y luego se
        // pone el estado que la fusión tiene que resolver.
        val archivedOrigin = repo.setArchived(origin.copy(notes = "rodillas"), true)
        val activeDestination = repo.setArchived(destination.copy(notes = "glúteo"), false)

        val summary = repo.mergeExercise(archivedOrigin, activeDestination) as Outcome.Ok<ExerciseMergeSummary>

        assertEquals("Sentadilla búlgara", summary.value.destination.name)
        assertEquals("glúteo\nrodillas", summary.value.destination.notes)
        // El activo gana al archivado, porque archivar el resultado lo sacaría de la lista por defecto.
        assertFalse(summary.value.destination.archived)
    }
}
