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
import com.dolvem.iron.domain.model.IsoWeek
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D17: Clock y ZoneId por constructor, así las ventanas son deterministas. El instante fijo es el
// miércoles 14 de octubre de 2026, miércoles de la semana ISO 42 (D7), que empieza el lunes 12.
// D18: Robolectric solo por el contexto que necesita Room en memoria, sin driver.
@RunWith(RobolectricTestRunner::class)
class AnalyticsTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("Europe/Madrid"))
    private val zone = ZoneId.of("Europe/Madrid")

    private val week40 = IsoWeek(2026, 40, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4))
    private val week41 = IsoWeek(2026, 41, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))
    private val week42 = IsoWeek(2026, 42, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18))

    private lateinit var db: IronDatabase
    private lateinit var analytics: Analytics
    private var exerciseId = 0

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        analytics = Analytics(db.setDao(), clock, zone)
        runBlocking {
            // Las FK de las entidades exigen la fila padre antes de insertar la serie.
            exerciseId = db.exerciseDao().insert(Exercise(0, "Sentadilla", "sentadilla")).toInt()
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun insertWorkout(planId: Int?): Int {
        val startedAt = clock.instant().toEpochMilli()
        val draft = Workout(0, startedAt, LocalDate.of(2026, 10, 14), null, ClosedBy.EXPLICIT, planId)
        return db.workoutDao().insert(draft).toInt()
    }

    // D1: el timestamp se guarda como instante, pero la ventana lo lee como fecha local.
    private suspend fun insertSet(workoutId: Int, day: LocalDate, reps: Int?, loadKg: Double?, warmup: Boolean) {
        val timestamp = day.atStartOfDay(zone).toInstant().toEpochMilli()
        db.setDao().insert(ExerciseSet(0, exerciseId, workoutId, timestamp, reps, loadKg, null, warmup))
    }

    // D7: la ventana actual con datos no es fallback, es la semana ISO en curso.
    @Test
    fun currentWindowWithDataIsNotFallback() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 10, 100.0, false)

        val window = analytics.windowVolume(exerciseId).first()
        assertEquals(week42, window.week)
        assertFalse(window.fallback)
        assertEquals(1000.0, window.value!!, 1e-9)
    }

    // D7: ventana actual vacía, se usa la última semana con datos y la ventana queda etiquetada.
    @Test
    fun emptyCurrentWindowFallsBackToTheLastWeekWithData() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 7), 12, 60.0, false)

        val window = analytics.windowVolume(exerciseId).first()
        assertTrue(window.fallback)
        assertEquals(week41, window.week)
        assertEquals(720.0, window.value!!, 1e-9)
    }

    // D7: el valor actual del detalle es el mejor valor de la ventana actual en la métrica de mejor
    // peso, con la misma regla de ventana vacía.
    @Test
    fun currentValueUsesTheBestWeightOfTheWindow() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 10, 100.0, false)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 5, 60.0, true)

        val window = analytics.windowBestWeight(exerciseId).first()
        assertFalse(window.fallback)
        // El calentamiento se excluye de la métrica (D9), así que el valor es el de la serie de trabajo.
        assertEquals(100.0, window.value!!, 1e-9)
    }

    // D7: en la serie temporal un bucket sin datos es hueco (value null); en el conteo por periodo es
    // cero, no hueco.
    @Test
    fun emptyBucketIsAGapInSeriesButZeroInSetCounts() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 7), 12, 60.0, false)

        val series = analytics.weeklyVolumeSeries(exerciseId, 3).first()
        assertEquals(listOf(week40, week41, week42), series.map { it.week })
        assertNull(series[0].value)
        assertEquals(720.0, series[1].value!!, 1e-9)
        assertNull(series[2].value)

        val counts = analytics.weeklySetCounts(exerciseId, 3).first()
        assertEquals(listOf(0, 1, 0), counts.map { it.sets })
        assertFalse(counts.any { it.fallback })
    }

    // D9: el calentamiento se excluye de las cuatro métricas.
    @Test
    fun warmupSetsAreExcludedFromMetrics() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 5, 60.0, true)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 10, 100.0, false)

        val metrics = analytics.metricsForExercise(exerciseId).first()
        assertEquals(100.0, metrics.bestWeightKg!!, 1e-9)
        assertEquals(1000.0, metrics.volumeKg, 1e-9)
        assertEquals(10, metrics.totalReps)
        assertEquals(1, metrics.setsCount)
    }

    // D9: una serie con reps nulo no aporta volumen, pero sí cuenta en el conteo por periodo (D6).
    @Test
    fun setsWithNullRepsAreExcludedFromVolume() = runBlocking {
        val workoutId = insertWorkout(null)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), null, 80.0, false)
        insertSet(workoutId, LocalDate.of(2026, 10, 14), 10, 100.0, false)

        val metrics = analytics.metricsForExercise(exerciseId).first()
        assertEquals(1000.0, metrics.volumeKg, 1e-9)
        assertEquals(10, metrics.totalReps)
        assertEquals(2, metrics.setsCount)
    }

    // D8: la sesión libre aporta volumen global y conteo de series de los 30 días, pero no a adherencia.
    @Test
    fun freeSessionContributesVolumeAndSetCountButNotAdherence() = runBlocking {
        val free = insertWorkout(null)
        insertSet(free, LocalDate.of(2026, 10, 14), 10, 100.0, false)

        val freeMetrics = analytics.sessionMetrics(free, true).first()
        assertEquals(1000.0, freeMetrics.volumeKg, 1e-9)
        assertEquals(1, freeMetrics.setsCount)
        assertFalse(freeMetrics.countsForAdherence)

        val window = analytics.last30DaysVolume(exerciseId).first()
        assertEquals(LocalDate.of(2026, 9, 15), window.start)
        assertEquals(LocalDate.of(2026, 10, 14), window.end)
        assertEquals(1000.0, window.value!!, 1e-9)
        assertEquals(1, analytics.last30DaysSetCount(exerciseId).first())

        // Una sesión de plan sí cuenta para adherencia, con el mismo volumen y conteo.
        val planId = db.planDao().insert(Plan(0, null, "")).toInt()
        val planned = insertWorkout(planId)
        insertSet(planned, LocalDate.of(2026, 10, 14), 10, 100.0, false)

        val planMetrics = analytics.sessionMetrics(planned, false).first()
        assertTrue(planMetrics.countsForAdherence)
    }
}
