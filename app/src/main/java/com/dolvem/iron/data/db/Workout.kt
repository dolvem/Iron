// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.time.LocalDate

// D26: camelCase en la BD, sin @ColumnInfo.
// D3: sessionDate es inmutable, se fija en la apertura perezosa en la zona local de ese momento.
// D62: sessionDate en TEXT con fecha ISO, closedBy en TEXT con enum ClosedBy.
// closedBy no es nulo, así que el repositorio lo fija en el insert y lo cambia al cerrar.
// D1: la restricción de una sesión activa por plan se implementa con este índice compuesto
// (Room no soporta índices parciales) más la validación transaccional en el repositorio.
@Entity(
    indices = [Index(value = ["planId", "endedAt"])],
    foreignKeys = [
        ForeignKey(
            entity = Plan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val startedAt: Long,
    val sessionDate: LocalDate,
    val endedAt: Long? = null,
    val closedBy: ClosedBy,
    val planId: Int? = null
)
