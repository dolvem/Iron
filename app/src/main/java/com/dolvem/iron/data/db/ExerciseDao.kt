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
    // nombre, que es nameKey: la colación por defecto de SQLite es BINARY, y ORDER BY name pondría
    // "Zapada" antes que "gemelo". archived queda excluido por defecto, con la variante para el filtro
    // que lo muestra.
    @Query("SELECT * FROM Exercise WHERE archived = 0 ORDER BY nameKey")
    fun observeAll(): Flow<Exercise>

    @Query("SELECT * FROM Exercise ORDER BY nameKey")
    fun observeAllIncludingArchived(): Flow<Exercise>

    @Query("SELECT * FROM Exercise WHERE id = :exerciseId")
    fun observeById(exerciseId: Int): Flow<Exercise?>

    // D4: la lectura por nameKey es la columna normalizada, la misma que usa la búsqueda de D31,
    // porque LIKE en SQLite solo es case-insensitive en ASCII.
    @Query("SELECT * FROM Exercise WHERE nameKey = :nameKey")
    fun observeByNameKey(nameKey: String): Flow<Exercise?>

    // D31: la búsqueda es case-insensitive sobre nameKey, la columna normalizada de D4, porque LIKE en
    // SQLite solo pliega ASCII. El patrón llega ya normalizado desde el repositorio. archived queda
    // excluido por defecto con la variante que lo muestra (reglas de orden), y el filtro de categoría
    // admite null, que es "todas las categorías". Devuelve la lista completa porque la pantalla pinta
    // todos los resultados, a diferencia de las lecturas por fila de la iteración 2. El orden es por
    // nameKey, la misma columna de D4, por la colación BINARY de SQLite.
    @Query(
        "SELECT * FROM Exercise " +
            "WHERE nameKey LIKE :pattern " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND (:showArchived OR archived = 0) " +
            "ORDER BY nameKey"
    )
    fun search(pattern: String, categoryId: Int?, showArchived: Boolean): Flow<List<Exercise>>

    // D5: la preconsulta de conteo es un método separado del borrado, y es lo que permite la
    // confirmación con conteo de afectados. Se cuenta antes del CASCADE.
    @Query("SELECT COUNT(*) FROM ExerciseSet WHERE exerciseId = :exerciseId")
    suspend fun countSets(exerciseId: Int): Int

    // D32: la fusión remapea las series al destino antes del CASCADE del origen, así que el origen se
    // puede borrar sin perder los registros. Se hace dentro de la transacción del repositorio.
    @Query("UPDATE ExerciseSet SET exerciseId = :destinationId WHERE exerciseId = :sourceId")
    suspend fun remapSets(sourceId: Int, destinationId: Int): Int
}
