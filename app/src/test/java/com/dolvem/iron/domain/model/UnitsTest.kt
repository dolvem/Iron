// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

// D10: conversión kg y lb, ida y vuelta, redondeo a 2 decimales medio arriba.
class UnitsTest {

    @Test
    fun roundTripKgLbKg() {
        assertEquals(220.46, kgToLb(100.0), 1e-9)
        assertEquals(100.0, lbToKg(220.46), 1e-9)
    }

    @Test
    fun roundsHalfUpAtTwoDecimals() {
        assertEquals(1.01, round2(1.005), 1e-9)
        assertEquals(2.68, round2(2.675), 1e-9)
        assertEquals(220.47, kgToLb(100.005), 1e-9)
    }

    // El valor guardado es la fuente de verdad: mostrar nunca redondea sobre un valor ya redondeado.
    @Test
    fun roundingIsIdempotent() {
        val stored = round2(62.505)
        assertEquals(stored, round2(stored), 1e-9)
    }

    @Test
    fun oneRepMaxUsesHalfKg() {
        assertEquals(97.0, roundHalfKg(epley1rmKg(100.0, 1)!!), 1e-9)
        assertEquals(75.0, roundHalfKg(epley1rmKg(100.0, 10)!!), 1e-9)
    }
}
