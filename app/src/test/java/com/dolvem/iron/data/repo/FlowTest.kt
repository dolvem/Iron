// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.domain.model.Outcome
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D18, fila "Repositorios con Flow": Turbine cubre la emisión inicial, la reemisión cuando cambia una
// fila y que al cancelar la colección el Flow deja de emitir. D5: las lecturas de los DAO son Flow
// directo, sin Outcome, y Room reemite por el invalidation tracker en cada escritura. D18: Robolectric
// solo por el contexto que necesita Room en memoria, sin driver.
@RunWith(RobolectricTestRunner::class)
class FlowTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("Europe/Madrid"))
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 14)
    private val now = clock.instant().toEpochMilli()

    private lateinit var db: IronDatabase
    private lateinit var exercises: ExerciseRepo
    private lateinit var workouts: WorkoutRepo
    private lateinit var analytics: Analytics

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        exercises = ExerciseRepo(db, db.exerciseDao(), db.planExerciseDao())
        workouts = WorkoutRepo(db, db.workoutDao(), clock, zone)
        analytics = Analytics(db.setDao(), clock, zone)
    }

    @After
    fun closeDb() {
        db.close()
    }

    // D5: la primera lectura de la colección emite el estado actual de la tabla.
    @Test
    fun initialEmissionOnInsert() = runBlocking {
        val created = (exercises.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value

        exercises.observeAll().test {
            assertEquals(created, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // D5: una escritura en la tabla reemite la fila cambiada, no el estado completo.
    @Test
    fun emissionWhenARowChanges() = runBlocking {
        val created = (exercises.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value

        exercises.observeAll().test {
            assertEquals(created, awaitItem())

            val renamed = exercises.renameExercise(created, "Sentadilla búlgara") as Outcome.Ok<Exercise>
            assertEquals(renamed.value, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    // D5 y D3: la sesión abierta se emite al crearse y vuelve a emitirse al cerrarla.
    @Test
    fun emissionWhenASessionRowChanges() = runBlocking {
        val open = workouts.openSession(null)

        workouts.observeAll().test {
            assertEquals(open, awaitItem())

            val closed = workouts.closeSession(open) as Outcome.Ok<Workout>
            assertEquals(closed.value, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    // D5: el map de Analytics conserva la naturaleza del Flow, una serie nueva vuelve a emitir.
    @Test
    fun mappedFlowReemitsWhenSetsChange() = runBlocking {
        val exerciseId = db.exerciseDao().insert(Exercise(0, "Sentadilla", "sentadilla")).toInt()
        val workoutId = workouts.openSession(null).id

        db.setDao().insert(ExerciseSet(0, exerciseId, workoutId, now, 10, 100.0))

        analytics.metricsForExercise(exerciseId).test {
            val first = awaitItem()
            assertEquals(1, first.setsCount)
            assertEquals(1000.0, first.volumeKg, 1e-9)

            db.setDao().insert(ExerciseSet(0, exerciseId, workoutId, now + 1000, 8, 100.0))

            val second = awaitItem()
            assertEquals(2, second.setsCount)
            assertEquals(1800.0, second.volumeKg, 1e-9)

            cancelAndConsumeRemainingEvents()
        }
    }

    // D18: al cancelar la colección el colector se cierra y el Flow deja de emitir. Lo que se inserta
    // después entra en la base de datos, pero no llega a la suscripción cancelada.
    // La fila existe antes de empezar a coleccionar, así la primera emisión es determinista. Sin eso, el
    // colector de Turbine arranca antes del insert y la primera consulta encuentra la tabla vacía, que
    // con un Flow de fila no nula lanza IllegalStateException.
    @Test
    fun cancellingTheCollectorStopsEmissions() = runBlocking {
        val received = mutableListOf<Exercise>()
        var collecting = false
        val tracked = exercises
            .observeAll()
            .onStart { collecting = true }
            .onEach { received.add(it) }
            .onCompletion { collecting = false }

        val first = (exercises.createExercise("Sentadilla", null) as Outcome.Ok<Exercise>).value

        tracked.test {
            assertEquals(first, awaitItem())
            assertTrue(collecting)

            cancelAndConsumeRemainingEvents()
        }

        assertFalse(collecting)

        val before = received.size
        db.exerciseDao().insert(Exercise(0, "Remo", "remo"))
        assertEquals(before, received.size)
        assertEquals("Remo", db.exerciseDao().observeByNameKey("remo").first()!!.name)
    }
}
