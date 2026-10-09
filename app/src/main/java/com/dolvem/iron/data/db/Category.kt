// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// D26: camelCase en la BD, sin @ColumnInfo.
// D4: el índice único está en nameKey, la columna normalizada derivada. name conserva su índice
// único; la normalización la hace una única función en domain/model, no SQLite.
@Entity(
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["nameKey"], unique = true)
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val nameKey: String
)
