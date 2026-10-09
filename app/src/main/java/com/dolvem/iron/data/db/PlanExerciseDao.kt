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

// D5: ABORT en el insert y el update, porque unique(planId, exerciseId) es la regla que impide que
// un ejercicio aparezca dos veces en el mismo plan, y la violación tiene que llegar como excepción
// para mapearla en el repositorio.
@Dao
interface PlanExerciseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(planExercise: PlanExercise): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(planExercise: PlanExercise): Int

    @Delete
    suspend fun delete(planExercise: PlanExercise)

    // D5: las lecturas devuelven Flow directo, sin Outcome. Reglas de orden: los ejercicios del plan
    // por sequence. enabled = false queda excluido por defecto, con la variante para el filtro que
    // lo muestra.
    @Query("SELECT * FROM PlanExercise WHERE planId = :planId AND enabled = 1 ORDER BY sequence")
    fun observeForPlan(planId: Int): Flow<PlanExercise>

    @Query("SELECT * FROM PlanExercise WHERE planId = :planId ORDER BY sequence")
    fun observeForPlanIncludingDisabled(planId: Int): Flow<PlanExercise>

    @Query("SELECT * FROM PlanExercise WHERE id = :planExerciseId")
    fun observeById(planExerciseId: Int): Flow<PlanExercise?>

    // D5: la preconsulta de conteo es un método separado del borrado, para la confirmación con
    // conteo de afectados. D32: la fusión remapea estas filas al ejercicio destino, así que hay que
    // contarlas antes del CASCADE del origen.
    @Query("SELECT COUNT(*) FROM PlanExercise WHERE exerciseId = :exerciseId")
    suspend fun countForExercise(exerciseId: Int): Int
}
