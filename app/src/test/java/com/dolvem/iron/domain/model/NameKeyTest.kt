// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

// D4: nameKey con NFC y Locale.ROOT, la única función de normalización, usada por el insert,
// el renombrar y la búsqueda.
class NameKeyTest {

    @Test
    fun foldsCase() {
        assertEquals("sentadilla", nameKey("Sentadilla"))
        assertEquals(nameKey("Sentadilla"), nameKey("sentadilla"))
    }

    @Test
    fun foldsAccentsWithNfc() {
        // Péculo con é precompuesto (U+00E9) frente a péculo con e + acento combinante (U+0301).
        val precomposed = "Péculo"
        val decomposed = "Pe\u0301culo"
        assertEquals(nameKey(precomposed), nameKey(decomposed))
        assertEquals("péculo", nameKey(decomposed))
    }

    // Locale.ROOT fija el pliegue: con el idioma turco del dispositivo la I mayúscula caería en
    // la i sin punto, y la base de datos no coincidiría con el repositorio.
    @Test
    fun doesNotDependOnDeviceLocale() {
        assertEquals("istanbul", nameKey("Istanbul"))
    }
}
