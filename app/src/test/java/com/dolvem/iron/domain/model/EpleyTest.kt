// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// D10: Epley, solo reps >= 1, series sin reps omitidas, redondeo a 0,5 kg al mostrar.
class EpleyTest {

    @Test
    fun epleyFormula() {
        assertEquals(75.0, epley1rmKg(100.0, 10)!!, 1e-9)
        assertEquals(42.857142857142854, epley1rmKg(60.0, 12)!!, 1e-9)
    }

    @Test
    fun rejectsRepsBelowOne() {
        assertNull(epley1rmKg(100.0, 0))
        assertNull(epley1rmKg(100.0, -1))
    }

    @Test
    fun omitsSetsWithoutReps() {
        val set = ExerciseSet(id = 1, exerciseId = 1, workoutId = 1, timestamp = 0L, reps = null, loadKg = 100.0)
        assertNull(epley1rmKg(set))
    }

    @Test
    fun omitsSetsWithoutLoad() {
        val set = ExerciseSet(id = 1, exerciseId = 1, workoutId = 1, timestamp = 0L, reps = 10, loadKg = null)
        assertNull(epley1rmKg(set))
    }

    @Test
    fun roundsToHalfKgWhenDisplaying() {
        val set = ExerciseSet(id = 1, exerciseId = 1, workoutId = 1, timestamp = 0L, reps = 1, loadKg = 100.0)
        assertEquals(97.0, epley1rmKg(set)!!, 1e-9)
    }
}
