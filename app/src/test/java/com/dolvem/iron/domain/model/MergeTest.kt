// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// D32: las dos reglas de fusión que se pueden comprobar sin base de datos. El remapeo de las FKs y el
// conflicto de unique(planId, exerciseId) se comprueban sobre Room en memoria en ExerciseMergeTest.
class MergeTest {

    // D32: gana el estado menos restrictivo, que es el activo. Fusionar un ejercicio activo en uno
    // archivado no puede archivar el resultado, porque eso lo saca de la lista por defecto.
    @Test
    fun activeWinsOverArchived() {
        assertFalse(mergedArchived(true, false))
        assertFalse(mergedArchived(false, true))
        assertFalse(mergedArchived(false, false))
    }

    @Test
    fun archivedSurvivesOnlyWhenBothAreArchived() {
        assertTrue(mergedArchived(true, true))
    }

    // D32: las notas concatenadas, con el destino primero porque es el que sobrevive.
    @Test
    fun notesConcatenateWithDestinationFirst() {
        assertEquals("destino\norigen", mergedNotes("origen", "destino"))
    }

    // Un lado vacío no aporta separador, y las dos vacías dan null, no una cadena vacía.
    @Test
    fun blankNotesDoNotRepeatTheSeparator() {
        assertEquals("destino", mergedNotes(null, "destino"))
        assertEquals("origen", mergedNotes("origen", null))
        assertEquals("destino", mergedNotes("   ", "destino"))
        assertEquals("origen", mergedNotes("origen", "   "))
        assertNull(mergedNotes(null, null))
    }
}
