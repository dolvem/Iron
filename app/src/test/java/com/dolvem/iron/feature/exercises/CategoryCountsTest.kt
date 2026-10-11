// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import com.dolvem.iron.data.db.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

// Iteración 4.5 (D78): la lógica nueva del bloque es de presentación (D18, D19), pero el conteo por
// grupo del menú del chip se extrajo a una función pura, así que se testea. La clave null es "sin
// categoría", el grupo que el menú muestra aparte de las categorías con nombre. El conteo se calcula con
// el estado de archivado aplicado (D81), pero la función pura no lo conoce: recibe la lista ya filtrada.
class CategoryCountsTest {

    private fun exercise(categoryId: Int?) = Exercise(id = 0, name = "x", nameKey = "x", categoryId = categoryId)

    @Test
    fun countsGroupByCategoryWithNullAsNoCategory() {
        val exercises = listOf(exercise(1), exercise(1), exercise(2), exercise(null))
        assertEquals(mapOf(1 to 2, 2 to 1, null to 1), categoryCounts(exercises))
    }

    @Test
    fun emptyListHasNoCounts() {
        assertEquals(emptyMap<Int?, Int>(), categoryCounts(emptyList()))
    }
}
