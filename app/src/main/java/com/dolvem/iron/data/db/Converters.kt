// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.ColumnTypeConverter
import java.time.LocalDate
import java.util.Locale

// D62: los conversores de columnas de la BD. sessionDate en TEXT con fecha ISO, closedBy en TEXT
// con el enum ClosedBy. Plan.days no tiene conversor: se parsea en el modelo de dominio.
// El texto guardado es el nombre en minúsculas, que es lo que se lee en los JSON de schemas/ y en
// el backup, y coincide con el formato del fichero de D16.
object Converters {

    @ColumnTypeConverter
    fun sessionDateFromString(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @ColumnTypeConverter
    fun sessionDateToString(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun closedByFromString(value: String?): ClosedBy? = value?.let { ClosedBy.valueOf(it.uppercase(Locale.ROOT)) }

    @ColumnTypeConverter
    fun closedByToString(value: ClosedBy?): String? = value?.name?.lowercase(Locale.ROOT)
}
