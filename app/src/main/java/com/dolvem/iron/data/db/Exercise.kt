// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// D26: camelCase en la BD, sin @ColumnInfo.
// D4: el índice único está en nameKey, la columna normalizada derivada.
// El borrado de la categoría deja los ejercicios sin categoría (SET NULL).
@Entity(
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["nameKey"], unique = true),
        Index("categoryId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val nameKey: String,
    val notes: String? = null,
    val archived: Boolean = false,
    val categoryId: Int? = null
)
