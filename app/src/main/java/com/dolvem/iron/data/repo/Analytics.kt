// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.data.db.SetDao
import com.dolvem.iron.domain.model.IsoWeek
import com.dolvem.iron.domain.model.bestWeightKg
import com.dolvem.iron.domain.model.currentIsoWeek
import com.dolvem.iron.domain.model.isoWeekOf
import com.dolvem.iron.domain.model.localToday
import com.dolvem.iron.domain.model.round2
import com.dolvem.iron.domain.model.setsInWeek
import com.dolvem.iron.domain.model.setsWithMetric
import com.dolvem.iron.domain.model.totalReps
import com.dolvem.iron.domain.model.volumeKg
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// D9: cuatro métricas por ejercicio y ni una más: PR, volumen, repeticiones totales y series por
// periodo. El cálculo lo hacen las funciones de Metrics.kt, que ya excluyen el calentamiento; aquí
// solo se agrupa por ejercicio y se redondea.
data class ExerciseMetrics(
    val exerciseId: Int,
    val bestWeightKg: Double?,
    val volumeKg: Double,
    val totalReps: Int,
    val setsCount: Int
)

// D7: ventana ISO con su valor. value null es un hueco en la serie temporal, no un cero.
data class WindowValue(val week: IsoWeek, val value: Double?, val fallback: Boolean)

// D7: en el conteo de series por periodo un bucket sin datos es cero, no hueco.
data class PeriodCount(val week: IsoWeek, val sets: Int, val fallback: Boolean)

// D8: ventana móvil desde hoy, 30 días naturales incluyendo hoy.
data class RollingWindow(val start: LocalDate, val end: LocalDate, val value: Double?)

// D8: una sesión libre aporta volumen global y conteo de series, pero no a adherencia ni frecuencia
// de plan, que se marca con countsForAdherence.
data class SessionMetrics(
    val workoutId: Int,
    val isFreeSession: Boolean,
    val volumeKg: Double,
    val setsCount: Int,
    val countsForAdherence: Boolean
)

// D17: Clock y ZoneId entran por constructor, así las ventanas son deterministas en los tests.
class Analytics(private val dao: SetDao, private val clock: Clock, private val zone: ZoneId) {
    // D5: las lecturas son Flow directo, sin Outcome.
    fun metricsForExercise(exerciseId: Int): Flow<ExerciseMetrics> =
        dao.observeForExercise(exerciseId).map { sets -> exerciseMetrics(exerciseId, sets) }

    // D7: ventana actual, la semana ISO en curso. Si está vacía, se usa la última semana con datos
    // y la ventana queda etiquetada con fallback.
    fun windowVolume(exerciseId: Int): Flow<WindowValue> = dao.observeForExercise(exerciseId).map { sets ->
        val week = windowWithData(sets)
        WindowValue(
            week = week,
            value = round2(volumeKg(setsInWeek(sets, week, zone))),
            fallback = week != currentIsoWeek(clock, zone)
        )
    }

    // D7: el valor actual del detalle es el mejor valor de la ventana actual, aquí en la métrica de
    // mejor peso. La ventana vacía usa la última semana con datos y queda etiquetada, la misma regla
    // que windowVolume.
    fun windowBestWeight(exerciseId: Int): Flow<WindowValue> = dao.observeForExercise(exerciseId).map { sets ->
        val week = windowWithData(sets)
        WindowValue(
            week = week,
            value = bestWeightKg(setsInWeek(sets, week, zone))?.let { round2(it) },
            fallback = week != currentIsoWeek(clock, zone)
        )
    }

    // D7: serie temporal por semanas ISO, de la más antigua a la más reciente. Un bucket sin datos
    // es un hueco (value null).
    fun weeklyVolumeSeries(exerciseId: Int, weeks: Int): Flow<List<WindowValue>> =
        dao.observeForExercise(exerciseId).map { sets ->
            seriesWeeks(weeks).map { week ->
                val inWeek = setsInWeek(sets, week, zone)
                WindowValue(week, if (inWeek.isEmpty()) null else round2(volumeKg(inWeek)), false)
            }
        }

    // D7: conteo de series por periodo, de la más antigua a la más reciente. Bucket sin datos, cero.
    fun weeklySetCounts(exerciseId: Int, weeks: Int): Flow<List<PeriodCount>> =
        dao.observeForExercise(exerciseId).map { sets ->
            seriesWeeks(weeks).map { week ->
                PeriodCount(week, setsWithMetric(setsInWeek(sets, week, zone)), false)
            }
        }

    // D8: volumen de la ventana móvil de 30 días naturales incluyendo hoy.
    fun last30DaysVolume(exerciseId: Int): Flow<RollingWindow> = dao.observeForExercise(exerciseId).map { sets ->
        val today = localToday(clock, zone)
        val start = today.minusDays(29)
        RollingWindow(start, today, round2(volumeKg(sets.filter { localDateOf(it, zone) >= start })))
    }

    // D8: el conteo de series de los últimos 30 días incluye las sesiones libres.
    fun last30DaysSetCount(exerciseId: Int): Flow<Int> = dao.observeForExercise(exerciseId).map { sets ->
        val start = localToday(clock, zone).minusDays(29)
        setsWithMetric(sets.filter { localDateOf(it, zone) >= start })
    }

    // D8: métricas de una sesión. Las sesiones libres cuentan en volumen y conteo de series, pero no
    // en adherencia ni frecuencia de plan.
    fun sessionMetrics(workoutId: Int, isFreeSession: Boolean): Flow<SessionMetrics> =
        dao.observeForWorkout(workoutId).map { sets ->
            SessionMetrics(
                workoutId = workoutId,
                isFreeSession = isFreeSession,
                volumeKg = round2(volumeKg(sets)),
                setsCount = setsWithMetric(sets),
                countsForAdherence = !isFreeSession
            )
        }

    // D10: redondeo de 2 decimales medio arriba. roundHalfKg se reserva para el 1RM, que no es una
    // de las cuatro métricas (D9), así que aquí todo sale con round2.
    private fun exerciseMetrics(exerciseId: Int, sets: List<ExerciseSet>): ExerciseMetrics = ExerciseMetrics(
        exerciseId = exerciseId,
        bestWeightKg = bestWeightKg(sets)?.let { round2(it) },
        volumeKg = round2(volumeKg(sets)),
        totalReps = totalReps(sets),
        setsCount = setsWithMetric(sets)
    )

    // D7: las últimas N semanas ISO, de la más antigua a la más reciente.
    private fun seriesWeeks(weeks: Int): List<IsoWeek> {
        val today = localToday(clock, zone)
        return (weeks - 1 downTo 0).map { offset -> isoWeekOf(today.minusDays(7L * offset)) }
    }

    // D7: ventana con datos: la semana en curso, o la última semana con datos si está vacía.
    private fun windowWithData(sets: List<ExerciseSet>): IsoWeek {
        val current = currentIsoWeek(clock, zone)
        if (setsInWeek(sets, current, zone).isNotEmpty()) return current
        val lastDate = sets.map { localDateOf(it, zone) }.maxOrNull()
        return lastDate?.let { isoWeekOf(it) } ?: current
    }

    // D1: la frontera de día es medianoche local, no UTC.
    private fun localDateOf(set: ExerciseSet, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(set.timestamp).atZone(zone).toLocalDate()
}
