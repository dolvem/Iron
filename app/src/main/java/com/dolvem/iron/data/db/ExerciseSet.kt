// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// D26: camelCase en la BD, sin @ColumnInfo.
// Métricas en kg, la unidad canónica. workoutId es obligatorio (D40).
// D2: isWarmup se fija en el insert por posición y no se recalcula al borrar o editar.
// D6: la validación de métricas la hace el repositorio antes del CASCADE.
@Entity(
    indices = [Index("exerciseId"), Index("workoutId")],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val exerciseId: Int,
    val workoutId: Int,
    val timestamp: Long,
    val reps: Int? = null,
    val loadKg: Double? = null,
    val notes: String? = null,
    val isWarmup: Boolean = false
)
