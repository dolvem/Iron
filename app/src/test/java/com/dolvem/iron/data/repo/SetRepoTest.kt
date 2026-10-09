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
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.domain.model.Outcome
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D1: para distinguir el momento del cierre del timestamp de la serie hace falta un reloj que avance
// entre las lecturas de la transacción. Con el Clock.fixed de D17 los dos instantes coinciden y el test
// no puede separarlos, así que este reloj avanza un paso en cada lectura y sigue siendo determinista.
private class AdvancingClock(private val base: Clock, private val step: Duration) : Clock() {
    private var current: Instant = base.instant()

    override fun getZone(): ZoneId = base.getZone()

    override fun withZone(zone: ZoneId): Clock = AdvancingClock(base.withZone(zone), step)

    override fun instant(): Instant {
        val read = current
        current = current.plus(step)
        return read
    }
}

// D17: Clock y ZoneId por constructor. El instante fijo es 22:30Z, que en Europe/Madrid ya es el día
// siguiente, así que la zona cambia el sessionDate sin cambiar el reloj (D1).
@RunWith(RobolectricTestRunner::class)
class SetRepoTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-09T22:30:00Z"), ZoneId.of("Europe/Madrid"))
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 10)
    private val yesterday = LocalDate.of(2026, 10, 9)
    private val now = clock.instant().toEpochMilli()

    private lateinit var db: IronDatabase
    private lateinit var repo: SetRepo
    private var exerciseId = 0
    private var planId = 0

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        repo = SetRepo(db, db.setDao(), db.workoutDao(), clock, zone)
        runBlocking {
            // Las FK de las entidades exigen la fila padre antes de insertar la serie.
            exerciseId = db.exerciseDao().insert(Exercise(0, "Sentadilla", "sentadilla")).toInt()
            planId = db.planDao().insert(Plan(0, null, "")).toInt()
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    // D5: las lecturas del DAO son Flow, así que la lectura puntual de una sesión usa first().
    private suspend fun readWorkout(id: Int): Workout? = db.workoutDao().observeById(id).first()

    // D3: apertura perezosa, la primera serie crea la sesión con startedAt y sessionDate en el momento
    // de apertura, en la zona local.
    @Test
    fun firstSetOpensTheSessionAtTheMomentOfOpeningInLocalZone() = runBlocking {
        val set = (repo.addSet(null, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val session = readWorkout(set.workoutId)!!

        // En Madrid el instante fijo ya es el día 10: la frontera de día es la medianoche local (D1).
        assertEquals(today, session.sessionDate)
        assertEquals(now, session.startedAt)
        assertNull(session.endedAt)
        assertEquals(set.workoutId, session.id)
        assertEquals(now, set.timestamp)
    }

    // D1: la serie después de medianoche cierra la sesión de la víspera y abre la del día en la misma
    // transacción. endedAt es el momento del cierre, no el timestamp de la serie.
    @Test
    fun dayBoundaryClosesTheEveAndOpensTheDayInTheSameTransaction() = runBlocking {
        val startedAt = Instant.parse("2026-10-09T20:00:00Z").toEpochMilli()
        val eve = db.workoutDao().insert(Workout(0, startedAt, yesterday, null, ClosedBy.EXPLICIT, planId)).toInt()

        val step = Duration.ofMinutes(7)
        val advancingRepo = SetRepo(db, db.setDao(), db.workoutDao(), AdvancingClock(clock, step), zone)
        val set = (advancingRepo.addSet(planId, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value

        // El reloj se lee tres veces en la transacción: la fecha local, el timestamp de la serie y el
        // momento del cierre, por eso los dos instantes son distintos.
        val setTimestamp = clock.instant().plus(step).toEpochMilli()
        val closedAt = clock.instant().plus(step).plus(step).toEpochMilli()

        val session = readWorkout(set.workoutId)!!
        assertEquals(today, session.sessionDate)
        assertEquals(closedAt, session.startedAt)
        assertEquals(setTimestamp, set.timestamp)
        assertNotEquals(setTimestamp, closedAt)

        val closedEve = readWorkout(eve)!!
        assertEquals(closedAt, closedEve.endedAt!!)
        assertEquals(ClosedBy.AUTO, closedEve.closedBy)
    }

    // D1 y D17: la frontera de día es la medianoche local, no UTC. Con la misma Clock, el mismo instante
    // es el día 10 en Europe/Madrid y el día 9 en UTC.
    @Test
    fun sameClockInUtcProducesADifferentSessionDate() = runBlocking {
        val utcRepo = SetRepo(db, db.setDao(), db.workoutDao(), clock, ZoneId.of("UTC"))

        val madridSet = (repo.addSet(null, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val utcSet = (utcRepo.addSet(null, exerciseId, 8, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value

        assertEquals(today, readWorkout(madridSet.workoutId)!!.sessionDate)
        assertEquals(yesterday, readWorkout(utcSet.workoutId)!!.sessionDate)
        // El instante de apertura no cambia: lo que cambia es la fecha local derivada de la zona.
        assertEquals(madridSet.timestamp, utcSet.timestamp)
    }

    // D1: una sesión activa por plan, la segunda serie va a la misma sesión abierta.
    @Test
    fun secondSetReusesTheOpenSessionOfThePlan() = runBlocking {
        val first = (repo.addSet(planId, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val second = (repo.addSet(planId, exerciseId, 8, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value

        assertEquals(first.workoutId, second.workoutId)
        assertEquals(1, db.workoutDao().countOpenForPlan(planId))
        assertEquals(today, readWorkout(first.workoutId)!!.sessionDate)
    }

    // La frontera (planId, día) se extiende a planId IS NULL: una sesión libre abierta por día.
    @Test
    fun secondFreeSetReusesTheOpenFreeSessionOfTheDay() = runBlocking {
        val first = (repo.addSet(null, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val second = (repo.addSet(null, exerciseId, 8, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value

        assertEquals(first.workoutId, second.workoutId)
        assertEquals(1, db.workoutDao().countFreeForDate(today))
        assertNull(readWorkout(first.workoutId)!!.planId)
    }

    // D44: el historial distingue el cierre automático del explícito. D62: una sesión abierta se inserta
    // con EXPLICIT, que es el cierre previsto, y pasa a AUTO solo cuando lo cierra el cambio de día.
    @Test
    fun automaticCloseIsDistinguishedFromExplicitClose() = runBlocking {
        // Cierre explícito dentro del mismo día.
        val explicitSet = (repo.addSet(planId, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val explicit = readWorkout(explicitSet.workoutId)!!
        assertEquals(ClosedBy.EXPLICIT, explicit.closedBy)
        db.workoutDao().update(explicit.copy(endedAt = now, closedBy = ClosedBy.EXPLICIT))

        // Cierre automático por cambio de día.
        val startedAt = Instant.parse("2026-10-09T19:00:00Z").toEpochMilli()
        val eve = db.workoutDao().insert(Workout(0, startedAt, yesterday, null, ClosedBy.EXPLICIT, planId)).toInt()
        val set = (repo.addSet(planId, exerciseId, 8, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value

        assertEquals(ClosedBy.AUTO, readWorkout(eve)!!.closedBy)
        assertEquals(now, readWorkout(eve)!!.endedAt!!)
        assertEquals(ClosedBy.EXPLICIT, readWorkout(explicit.id)!!.closedBy)
        assertEquals(now, readWorkout(explicit.id)!!.endedAt!!)
        assertEquals(ClosedBy.EXPLICIT, readWorkout(set.workoutId)!!.closedBy)
    }

    // D2: isWarmup se fija por posición en el insert y no se recalcula al borrar.
    @Test
    fun isWarmupIsFixedByPositionAndNotRecalculatedOnDelete() = runBlocking {
        val first = (repo.addSet(planId, exerciseId, 10, 100.0, null, 1) as Outcome.Ok<ExerciseSet>).value
        val second = (repo.addSet(planId, exerciseId, 8, 100.0, null, 1) as Outcome.Ok<ExerciseSet>).value
        assertTrue(first.isWarmup)
        assertFalse(second.isWarmup)

        repo.deleteSet(first)
        val third = (repo.addSet(planId, exerciseId, 6, 100.0, null, 1) as Outcome.Ok<ExerciseSet>).value
        assertFalse(third.isWarmup)
        assertEquals(2, db.setDao().countForWorkout(first.workoutId))

        // D2: sin warmupSets no hay calentamiento, porque el flag depende de la configuración del plan.
        val noWarmup = (repo.addSet(planId, exerciseId, 5, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        assertFalse(noWarmup.isWarmup)
    }

    // D6: reps >= 1, loadKg > 0 y al menos una métrica, validado antes del insert, así que nunca llega
    // una serie inválida a la base de datos y tampoco se abre sesión.
    @Test
    fun invalidMetricsAreRejectedBeforeTheInsert() = runBlocking {
        assertEquals(Outcome.InvalidReps(0), repo.addSet(planId, exerciseId, 0, 100.0, null, null))
        assertEquals(Outcome.InvalidLoad(0), repo.addSet(planId, exerciseId, 10, 0.0, null, null))
        assertEquals(Outcome.SetWithoutMetric(0), repo.addSet(planId, exerciseId, null, null, null, null))
        assertEquals(Outcome.InvalidReps(0), repo.quickLog(exerciseId, 0, 100.0, null))

        assertEquals(0, db.setDao().countForExercise(exerciseId))
        assertEquals(0, db.workoutDao().countOpenForPlan(planId))
        assertEquals(0, db.workoutDao().countFreeForDate(today))
    }

    // D39: al borrar la última serie de una sesión abierta la sesión se elimina; una sesión cerrada vacía
    // se conserva. D40: DeletedSet lleva la sesión eliminada con su id y startedAt originales.
    @Test
    fun lastSetDeletesTheOpenSessionButPreservesAClosedEmptySession() = runBlocking {
        val openSet = (repo.addSet(planId, exerciseId, 10, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val openSession = readWorkout(openSet.workoutId)!!

        val deleted = repo.deleteSet(openSet) as Outcome.Ok<DeletedSet>
        assertEquals(openSet, deleted.value.set)
        assertEquals(openSession.id, deleted.value.workout!!.id)
        assertEquals(openSession.startedAt, deleted.value.workout!!.startedAt)
        assertNull(readWorkout(openSession.id))

        val closedSet = (repo.addSet(planId, exerciseId, 8, 100.0, null, null) as Outcome.Ok<ExerciseSet>).value
        val closedSession = readWorkout(closedSet.workoutId)!!
        db.workoutDao().update(closedSession.copy(endedAt = now, closedBy = ClosedBy.EXPLICIT))

        val kept = repo.deleteSet(closedSet) as Outcome.Ok<DeletedSet>
        assertNull(kept.value.workout)
        assertEquals(0, db.setDao().countForWorkout(closedSession.id))
        assertEquals(closedSession.id, readWorkout(closedSession.id)!!.id)
    }

    // Quick log: reutiliza la sesión libre abierta del día, si está cerrada crea una nueva, y siempre es
    // serie de trabajo, nunca calentamiento (D2).
    @Test
    fun quickLogReusesTheOpenFreeSessionAndAlwaysWritesWorkSets() = runBlocking {
        val first = (repo.quickLog(exerciseId, 10, 100.0, null) as Outcome.Ok<ExerciseSet>).value
        assertFalse(first.isWarmup)

        val open = readWorkout(first.workoutId)!!
        assertNull(open.planId)
        assertEquals(today, open.sessionDate)

        val second = (repo.quickLog(exerciseId, 8, 100.0, null) as Outcome.Ok<ExerciseSet>).value
        assertEquals(first.workoutId, second.workoutId)

        db.workoutDao().update(open.copy(endedAt = now, closedBy = ClosedBy.EXPLICIT))
        val third = (repo.quickLog(exerciseId, 12, 100.0, null) as Outcome.Ok<ExerciseSet>).value
        assertFalse(third.isWarmup)
        assertNotEquals(first.workoutId, third.workoutId)
        assertEquals(2, db.workoutDao().countFreeForDate(today))
    }
}
