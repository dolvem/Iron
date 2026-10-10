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

// D5: ABORT en el insert y el update. En Plan no hay índice único, pero se mantiene ABORT para que
// cualquier fallo de la base de datos llegue como excepción a la reserva de D5.
@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: Plan): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(plan: Plan): Int

    @Delete
    suspend fun delete(plan: Plan)

    // D5: las lecturas devuelven Flow directo, sin Outcome. Reglas de orden: los planes por
    // sequence, con los nulos al final. SQLite pone los nulos al principio, así que la clave
    // IS NULL va delante.
    @Query("SELECT * FROM Plan ORDER BY sequence IS NULL, sequence")
    fun observeAll(): Flow<List<Plan>>

    @Query("SELECT * FROM Plan WHERE id = :planId")
    fun observeById(planId: Int): Flow<Plan?>

    // D5: la preconsulta de conteo es un método separado del borrado, para la confirmación con
    // conteo de afectados. Aquí se cuentan las filas de la plantilla que el CASCADE se lleva; el
    // conteo de sesiones que quedan como sesiones libres (D33) está en WorkoutDao.
    @Query("SELECT COUNT(*) FROM PlanExercise WHERE planId = :planId")
    suspend fun countPlanExercises(planId: Int): Int
}
