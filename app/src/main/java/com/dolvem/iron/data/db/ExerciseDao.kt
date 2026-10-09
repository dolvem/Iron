// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

// D5: ABORT en el insert y el update, porque con IGNORE la carrera entre la comprobación y el insert
// se traga en silencio y no hay excepción que mapear en el repositorio. El índice único está en
// nameKey (D4).
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: Exercise): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(exercise: Exercise): Int

    @Delete
    suspend fun delete(exercise: Exercise)

    // D5: las lecturas devuelven Flow directo, sin Outcome. Reglas de orden: los ejercicios por
    // nombre. archived queda excluido por defecto, con la variante para el filtro que lo muestra.
    @Query("SELECT * FROM Exercise WHERE archived = 0 ORDER BY name")
    fun observeAll(): Flow<Exercise>

    @Query("SELECT * FROM Exercise ORDER BY name")
    fun observeAllIncludingArchived(): Flow<Exercise>

    @Query("SELECT * FROM Exercise WHERE id = :exerciseId")
    fun observeById(exerciseId: Int): Flow<Exercise?>

    // D4: la lectura por nameKey es la columna normalizada, la misma que usa la búsqueda de D31,
    // porque LIKE en SQLite solo es case-insensitive en ASCII.
    @Query("SELECT * FROM Exercise WHERE nameKey = :nameKey")
    fun observeByNameKey(nameKey: String): Flow<Exercise?>

    // D5: la preconsulta de conteo es un método separado del borrado, y es lo que permite la
    // confirmación con conteo de afectados. Se cuenta antes del CASCADE.
    @Query("SELECT COUNT(*) FROM ExerciseSet WHERE exerciseId = :exerciseId")
    suspend fun countSets(exerciseId: Int): Int
}
