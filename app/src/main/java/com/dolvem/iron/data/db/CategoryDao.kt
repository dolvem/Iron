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

// D5: ABORT en el insert y el update, porque el índice único de D4 está en nameKey y la violación
// tiene que llegar como excepción para mapearla en el repositorio.
@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(category: Category): Int

    @Delete
    suspend fun delete(category: Category)

    // D5: las lecturas devuelven Flow directo, sin Outcome. Las categorías se ordenan por nombre
    // (reglas de orden).
    @Query("SELECT * FROM Category ORDER BY name")
    fun observeAll(): Flow<Category>

    @Query("SELECT * FROM Category WHERE id = :categoryId")
    fun observeById(categoryId: Int): Flow<Category?>

    // D4: la lectura por nameKey es la columna normalizada, la misma que usa la búsqueda de D31,
    // porque LIKE en SQLite solo es case-insensitive en ASCII.
    @Query("SELECT * FROM Category WHERE nameKey = :nameKey")
    fun observeByNameKey(nameKey: String): Flow<Category?>

    // D5: la preconsulta de conteo es un método separado del borrado, para la confirmación con
    // conteo de afectados. El borrado de categoría es SET NULL, así que lo que se cuenta son los
    // ejercicios que quedan sin categoría.
    @Query("SELECT COUNT(*) FROM Exercise WHERE categoryId = :categoryId")
    suspend fun countExercises(categoryId: Int): Int
}
