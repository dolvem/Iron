// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import com.dolvem.iron.data.settings.ShortDateFormat
import com.dolvem.iron.data.settings.StrengthUnit
import com.dolvem.iron.domain.model.kgToLb
import com.dolvem.iron.domain.model.parsePlanDays
import com.dolvem.iron.domain.model.round2
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// D28: se aceptan coma y punto y se normalizan a punto, porque en español el teclado produce coma. Es
// parsing de entrada, no presentación. El redondeo ocurre al guardar, el mismo criterio de D10.
fun parseDecimal(input: String): Double? = input.replace(',', '.').toDoubleOrNull()?.let { round2(it) }

// D10: 2 decimales al mostrar, medio arriba. kg es la unidad canónica; lb es solo de display, y la
// unidad la decide Ajustes.
fun formatLoad(loadKg: Double, unit: StrengthUnit, locale: Locale = Locale.getDefault()): String {
    val value = if (unit == StrengthUnit.LB) kgToLb(loadKg) else loadKg
    return String.format(locale, "%.2f", value)
}

// D63: el patrón es el valor resuelto en Ajustes por idioma efectivo. Aquí solo se aplica.
fun formatDate(date: LocalDate, format: ShortDateFormat, locale: Locale = Locale.getDefault()): String {
    val pattern = when (format) {
        ShortDateFormat.DD_MM_SLASH -> "dd/MM/yyyy"
        ShortDateFormat.DD_MM_DASH -> "dd-MM-yyyy"
        ShortDateFormat.MM_DD_SLASH -> "MM/dd/yyyy"
    }
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

// Nombres de los días de Plan.days en el idioma activo. El parseo es del modelo de dominio (D62), que es
// el único sitio donde se interpreta la cadena; aquí solo se presenta, con el locale de Compose (D72),
// porque Locale.getDefault() no respeta localeOverride de D64. Cadena vacía devuelve cadena vacía, que la
// pantalla muestra como "sin días".
fun formatDayNames(days: String, locale: Locale): String =
    parsePlanDays(days).joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) }
