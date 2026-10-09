// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// D26: camelCase en la BD, sin @ColumnInfo.
// unique(planId, exerciseId): un ejercicio no puede aparecer dos veces en el mismo plan.
// targetRpe es orientativo del plan (1..10), no se marca por serie.
@Entity(
    indices = [
        Index(value = ["planId", "exerciseId"], unique = true),
        Index("exerciseId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Plan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val planId: Int,
    val exerciseId: Int,
    val sequence: Int,
    val maxSets: Int? = null,
    val warmupSets: Int? = null,
    val targetRpe: Int? = null,
    val enabled: Boolean = true
)
