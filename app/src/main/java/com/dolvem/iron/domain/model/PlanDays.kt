// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import java.time.LocalDate
import java.time.temporal.WeekFields

// D62: Plan.days se queda como TEXT CSV y el parseo se hace aquí, en el modelo de dominio, porque
// el conversor solo mueve el valor. Formato canónico: números ISO 1-7 ordenados y sin espacios.
// Cadena vacía significa "nunca". Se descarta el bitmask en un Int: es ilegible para depurar.
fun parsePlanDays(days: String): List<Int> {
    val values = days.split(',').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
    return values.filter { it in 1..7 }.distinct().sorted()
}

// Escritura del formato canónico, para que lo guardado sea siempre ordenado y legible en el JSON.
fun formatPlanDays(days: List<Int>): String = days.filter { it in 1..7 }.distinct().sorted().joinToString(",")

// Coincidencia con un día, que es lo que necesita Hoy (iteración 6). Se parsea una vez al leer.
fun planMatchesDay(days: String, date: LocalDate): Boolean =
    parsePlanDays(days).contains(date.get(WeekFields.ISO.dayOfWeek()))
