// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import com.dolvem.iron.domain.model.Outcome

// D5: la violación de único se captura en el repositorio y se mapea a error de negocio, porque la
// comprobación previa por nameKey no cubre la carrera entre la comprobación y el insert.
// La detección mira el mensaje, no el código de error. SQLITE_CONSTRAINT_UNIQUE (2067) solo está en
// el código extendido del driver de framework, y Robolectric puede no incluirlo en el mensaje; el
// mensaje es lo único que comparten el dispositivo y el entorno de tests.
private const val UNIQUE_MARKER = "UNIQUE constraint failed"

fun isUniqueViolation(message: String?): Boolean = message != null && message.contains(UNIQUE_MARKER)

// El repositorio captura la excepción de SQLite y llama a esta función. Cualquier otra excepción se
// propaga: las excepciones se reservan para fallos de I/O y de base de datos (D5).
// Devuelve null cuando la excepción no es una violación de único, para que el llamador decida
// propagarla.
fun mapConstraintViolation(name: String, message: String?): Outcome.Error? =
    if (isUniqueViolation(message)) Outcome.DuplicateName(name) else null
