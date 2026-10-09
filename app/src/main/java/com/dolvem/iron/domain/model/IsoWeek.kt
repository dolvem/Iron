// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

// Semana ISO 8601, lunes como primer día, anclada a la zona local (D7).
// D17: Clock y ZoneId entran como parámetros, así que la semana es determinista en los tests.
data class IsoWeek(val weekBasedYear: Int, val week: Int, val start: LocalDate, val end: LocalDate)

// D1: la frontera de día es medianoche local en la zona del dispositivo, no en UTC.
fun localToday(clock: Clock, zone: ZoneId): LocalDate = Instant.now(clock).atZone(zone).toLocalDate()

fun isoWeekOf(date: LocalDate): IsoWeek {
    val fields = WeekFields.ISO
    val start = date.with(fields.dayOfWeek(), 1)
    return IsoWeek(
        weekBasedYear = date.get(fields.weekBasedYear()),
        week = date.get(fields.weekOfWeekBasedYear()),
        start = start,
        end = start.plusDays(6)
    )
}

// Ventana actual: la semana ISO en curso (D7).
fun currentIsoWeek(clock: Clock, zone: ZoneId): IsoWeek = isoWeekOf(localToday(clock, zone))

// Series de una semana, con el timestamp convertido a fecha local solo al leer (D1).
fun setsInWeek(sets: List<ExerciseSet>, week: IsoWeek, zone: ZoneId): List<ExerciseSet> =
    sets.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() in week.start..week.end }
