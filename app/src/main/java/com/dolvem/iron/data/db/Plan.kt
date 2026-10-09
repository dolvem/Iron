// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// D26: camelCase en la BD, sin @ColumnInfo.
// D62: days se queda como TEXT CSV con números ISO 1-7, ordenado y sin espacios. El parseo se
// hace en el modelo de dominio, no en el conversor. Cadena vacía significa "nunca".
@Entity
data class Plan(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String? = null,
    val days: String,
    val sequence: Int? = null
)
