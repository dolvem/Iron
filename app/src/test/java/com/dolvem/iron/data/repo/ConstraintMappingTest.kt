// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import com.dolvem.iron.domain.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// D5: la violación de único se mapea a DuplicateName, y el resto de excepciones se propagan, porque
// se reservan para fallos de I/O y de base de datos.
class ConstraintMappingTest {

    @Test
    fun mapsUniqueViolationWithCodeToDuplicateName() {
        val message = "UNIQUE constraint failed: exercise.nameKey (code 2067)"
        assertEquals(Outcome.DuplicateName("Péculo"), mapConstraintViolation("Péculo", message))
    }

    // Robolectric puede no incluir el código en el mensaje, así que la detección no depende de él.
    @Test
    fun mapsUniqueViolationWithoutCodeToDuplicateName() {
        val message = "UNIQUE constraint failed: exercise.nameKey"
        assertEquals(Outcome.DuplicateName("sentadilla"), mapConstraintViolation("sentadilla", message))
    }

    @Test
    fun doesNotMapOtherConstraints() {
        assertNull(mapConstraintViolation("Sentadilla", "FOREIGN KEY constraint failed"))
    }

    @Test
    fun doesNotMapNullMessage() {
        assertNull(mapConstraintViolation("Sentadilla", null))
    }
}
