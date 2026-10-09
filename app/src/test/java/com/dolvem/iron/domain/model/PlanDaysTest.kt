// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import java.time.LocalDate
import java.time.temporal.WeekFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// D62: parseo de Plan.days, CSV ISO 1-7 ordenado. Cadena vacía significa "nunca".
class PlanDaysTest {

    @Test
    fun parsesCanonicalCsv() {
        assertEquals(listOf(1, 3, 5), parsePlanDays("1,3,5"))
    }

    @Test
    fun emptyMeansNever() {
        assertEquals(emptyList<Int>(), parsePlanDays(""))
    }

    @Test
    fun sortsAndTrims() {
        assertEquals(listOf(1, 3, 5), parsePlanDays(" 5 ,1,3 "))
        assertEquals(listOf(2, 4), parsePlanDays("4,2"))
    }

    @Test
    fun ignoresValuesOutsideOneToSeven() {
        assertEquals(listOf(7), parsePlanDays("7,8,0,x"))
    }

    @Test
    fun writesCanonicalFormat() {
        assertEquals("1,5", formatPlanDays(listOf(5, 1, 1)))
        assertEquals("", formatPlanDays(emptyList()))
    }

    @Test
    fun matchesDay() {
        val thursday = LocalDate.of(2026, 1, 1)
        assertEquals(4, thursday.get(WeekFields.ISO.dayOfWeek()))
        assertTrue(planMatchesDay("1,3,4", thursday))
        assertFalse(planMatchesDay("1,3,5", thursday))
        assertFalse(planMatchesDay("", thursday))
    }
}
