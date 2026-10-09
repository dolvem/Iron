// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// D9: volumen, PR, repeticiones totales y series por periodo, con el calentamiento excluido.
class MetricsTest {

    private fun set(id: Int, loadKg: Double?, reps: Int?, warmup: Boolean = false): ExerciseSet =
        ExerciseSet(id, 1, 1, 0L, reps, loadKg, null, warmup)

    @Test
    fun volumeExcludesNullRepsAndWarmup() {
        val sets = listOf(
            set(1, 100.0, 10),
            set(2, 60.0, 5, warmup = true),
            set(3, 80.0, null)
        )
        assertEquals(1000.0, volumeKg(sets), 1e-9)
    }

    @Test
    fun prIsBestWeightExcludingWarmup() {
        val sets = listOf(set(1, 100.0, 10), set(2, 120.0, 1, warmup = true), set(3, 90.0, 8))
        assertEquals(100.0, bestWeightKg(sets)!!, 1e-9)
        assertNull(bestWeightKg(emptyList()))
    }

    @Test
    fun totalRepsExcludesWarmup() {
        val sets = listOf(set(1, 100.0, 10), set(2, 60.0, 5, warmup = true), set(3, 80.0, null))
        assertEquals(10, totalReps(sets))
    }

    @Test
    fun setsPerPeriodCountSetsWithAtLeastOneMetric() {
        val sets = listOf(
            set(1, 100.0, 10),
            set(2, null, 5),
            set(3, 60.0, null),
            set(4, null, null),
            set(5, 120.0, 1, warmup = true)
        )
        assertEquals(3, setsWithMetric(sets))
    }
}
