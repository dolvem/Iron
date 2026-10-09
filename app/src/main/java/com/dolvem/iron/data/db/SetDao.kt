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

// D5: ABORT, porque la violación de único tiene que llegar como excepción para mapearla en el
// repositorio. Aquí no hay índice único: la FK con CASCADE es la que hace que una serie no se
// inserte sin sesión.
@Dao
interface SetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(set: ExerciseSet): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(set: ExerciseSet): Int

    @Delete
    suspend fun delete(set: ExerciseSet)

    // D5: las lecturas devuelven Flow directo, sin Outcome. Se devuelve la lista completa porque
    // Analytics agrupa por periodo y necesita todas las series de la ventana.
    @Query("SELECT * FROM ExerciseSet WHERE workoutId = :workoutId ORDER BY timestamp")
    fun observeForWorkout(workoutId: Int): Flow<List<ExerciseSet>>

    @Query("SELECT * FROM ExerciseSet WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun observeForExercise(exerciseId: Int): Flow<List<ExerciseSet>>

    // D2: isWarmup se fija en el insert por posición, que es el número de series ya registradas
    // para esa actividad en esa sesión.
    @Query("SELECT COUNT(*) FROM ExerciseSet WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun countForWorkoutAndExercise(workoutId: Int, exerciseId: Int): Int

    @Query("SELECT COUNT(*) FROM ExerciseSet WHERE workoutId = :workoutId")
    suspend fun countForWorkout(workoutId: Int): Int

    // D5: la preconsulta de conteo es un método separado del borrado, para la confirmación con
    // conteo de afectados. Se cuenta antes del CASCADE.
    @Query("SELECT COUNT(*) FROM ExerciseSet WHERE exerciseId = :exerciseId")
    suspend fun countForExercise(exerciseId: Int): Int
}
