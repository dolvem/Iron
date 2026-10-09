// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

// Semana ISO 8601, lunes como primer día, anclada a la zona local (D7, D17).
class IsoWeekTest {

    @Test
    fun mondayIsFirstDay() {
        val week = isoWeekOf(LocalDate.of(2026, 1, 5))
        assertEquals(2, week.week)
        assertEquals(LocalDate.of(2026, 1, 5), week.start)
        assertEquals(LocalDate.of(2026, 1, 11), week.end)
    }

    @Test
    fun weeks52And53() {
        assertEquals(52, isoWeekOf(LocalDate.of(2026, 12, 24)).week)
        assertEquals(53, isoWeekOf(LocalDate.of(2026, 12, 31)).week)
    }

    @Test
    fun leapYear() {
        // 2020 es bisiesto y tiene 53 semanas ISO.
        assertEquals(53, isoWeekOf(LocalDate.of(2020, 12, 31)).week)

        // 2024-12-31 es martes, pero pertenece a la semana 1 de 2025.
        val week = isoWeekOf(LocalDate.of(2024, 12, 31))
        assertEquals(2025, week.weekBasedYear)
        assertEquals(1, week.week)
    }

    @Test
    fun anchoredToLocalZone() {
        val clock = Clock.fixed(Instant.parse("2025-12-29T02:00:00Z"), ZoneOffset.UTC)

        val madrid = currentIsoWeek(clock, ZoneId.of("Europe/Madrid"))
        assertEquals(2026, madrid.weekBasedYear)
        assertEquals(1, madrid.week)

        // En Nueva York el mismo instante todavía es domingo 28, de la semana 52 de 2025.
        val newYork = currentIsoWeek(clock, ZoneId.of("America/New_York"))
        assertEquals(2025, newYork.weekBasedYear)
        assertEquals(52, newYork.week)
    }

    @Test
    fun setsInWeekUseLocalDate() {
        val zone = ZoneId.of("Europe/Madrid")
        val week = isoWeekOf(LocalDate.of(2026, 1, 5))
        val inside = ExerciseSet(
            id = 1,
            exerciseId = 1,
            workoutId = 1,
            timestamp = Instant.parse("2026-01-04T23:30:00Z").toEpochMilli(),
            reps = 10,
            loadKg = 100.0
        )
        val outside = ExerciseSet(
            id = 2,
            exerciseId = 1,
            workoutId = 1,
            timestamp = Instant.parse("2026-01-12T00:30:00Z").toEpochMilli(),
            reps = 8,
            loadKg = 100.0
        )
        assertEquals(listOf(inside), setsInWeek(listOf(outside, inside), week, zone))
    }
}
