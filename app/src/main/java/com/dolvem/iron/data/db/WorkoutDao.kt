// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// D5: ABORT en el insert y el update, porque con IGNORE la carrera entre la comprobación y el insert
// se traga en silencio y no hay excepción que mapear en el repositorio.
@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workout: Workout): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(workout: Workout): Int

    @Delete
    suspend fun delete(workout: Workout)

    // D5: las lecturas devuelven Flow directo, sin Outcome. El historial ordena por sessionDate
    // descendente (reglas de orden).
    @Query("SELECT * FROM Workout ORDER BY sessionDate DESC, startedAt DESC")
    fun observeAll(): Flow<Workout>

    @Query("SELECT * FROM Workout WHERE id = :workoutId")
    fun observeById(workoutId: Int): Flow<Workout?>

    // Una sesión activa por plan (endedAt IS NULL). El índice compuesto (planId, endedAt) de D1
    // cubre esta lectura.
    @Query("SELECT * FROM Workout WHERE planId = :planId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenForPlan(planId: Int): Flow<Workout?>

    @Query("SELECT * FROM Workout WHERE planId = :planId AND endedAt IS NULL LIMIT 1")
    suspend fun openForPlan(planId: Int): Workout?

    // La frontera (planId, día) se extiende a planId IS NULL: una sesión libre abierta por día.
    @Query("SELECT * FROM Workout WHERE planId IS NULL AND endedAt IS NULL AND sessionDate = :sessionDate LIMIT 1")
    suspend fun openFreeForDate(sessionDate: LocalDate): Workout?

    @Query("SELECT COUNT(*) FROM Workout WHERE planId = :planId AND endedAt IS NULL")
    suspend fun countOpenForPlan(planId: Int): Int

    @Query("SELECT COUNT(*) FROM Workout WHERE planId IS NULL AND sessionDate = :sessionDate")
    suspend fun countFreeForDate(sessionDate: LocalDate): Int

    // D33: la confirmación de borrado de plan cuenta las sesiones que quedan como sesiones libres.
    @Query("SELECT COUNT(*) FROM Workout WHERE planId = :planId")
    suspend fun countSessions(planId: Int): Int

    // D8: frecuencia del detalle del plan, la última sesión por sessionDate descendente (reglas de
    // orden). Aquí no vale el índice (planId, endedAt) de D1, porque la frecuencia mira también las
    // sesiones cerradas. El desempate es startedAt descendente, para que con dos sesiones del mismo día
    // la última sea la más reciente.
    @Query("SELECT * FROM Workout WHERE planId = :planId ORDER BY sessionDate DESC, startedAt DESC LIMIT 1")
    fun observeLastSessionForPlan(planId: Int): Flow<Workout?>

    // D8: sesiones del plan en la ventana móvil de 30 días naturales incluyendo hoy, así que el
    // parámetro start es hoy menos 29. Las sesiones libres no cuentan porque el filtro es por planId, y
    // la sesión abierta sí cuenta: el conteo no mira endedAt.
    // D62: sessionDate es TEXT con fecha ISO, así que la comparación lexicográfica de >= es la
    // comparación cronológica.
    @Query("SELECT COUNT(*) FROM Workout WHERE planId = :planId AND sessionDate >= :start")
    fun observeSessionCountFrom(planId: Int, start: LocalDate): Flow<Int>
}
