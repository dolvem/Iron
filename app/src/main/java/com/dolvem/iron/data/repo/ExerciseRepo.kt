// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import androidx.sqlite.SQLiteException
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.ExerciseDao
import com.dolvem.iron.data.db.PlanExerciseDao
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.nameKey
import kotlinx.coroutines.flow.Flow

// D5: la violación de único se captura aquí y se mapea a DuplicateName. Cualquier otra excepción se
// propaga, que es la reserva de excepciones para fallos de I/O y de base de datos.
// Las lecturas no devuelven Outcome (D5).
class ExerciseRepo(private val dao: ExerciseDao, private val planExerciseDao: PlanExerciseDao) {

    // D5: las lecturas devuelven Flow directo, sin Outcome. Los ejercicios por nombre (reglas de orden),
    // archived excluido por defecto con la variante para el filtro que lo muestra.
    fun observeAll(): Flow<Exercise> = dao.observeAll()

    fun observeAllIncludingArchived(): Flow<Exercise> = dao.observeAllIncludingArchived()

    fun observeById(exerciseId: Int): Flow<Exercise?> = dao.observeById(exerciseId)

    // D4: la lectura por nameKey es la columna normalizada, la misma que usa la búsqueda de D31, porque
    // LIKE en SQLite solo es case-insensitive en ASCII.
    fun observeByNameKey(nameKey: String): Flow<Exercise?> = dao.observeByNameKey(nameKey)

    suspend fun createExercise(name: String, categoryId: Int?): Outcome<Exercise> {
        val exercise = Exercise(id = 0, name = name, nameKey = nameKey(name), categoryId = categoryId)
        try {
            return Outcome.Ok(exercise.copy(id = dao.insert(exercise).toInt()))
        } catch (e: SQLiteException) {
            val error = mapConstraintViolation(name, e.message)
            if (error == null) throw e
            return error
        }
    }

    // D4: el renombrar usa la misma función de normalización que el insert, así que la base de datos
    // y el repositorio coinciden con los acentos.
    suspend fun renameExercise(exercise: Exercise, name: String): Outcome<Exercise> {
        val renamed = exercise.copy(name = name, nameKey = nameKey(name))
        try {
            dao.update(renamed)
            return Outcome.Ok(renamed)
        } catch (e: SQLiteException) {
            val error = mapConstraintViolation(name, e.message)
            if (error == null) throw e
            return error
        }
    }

    // D5: la preconsulta es un método separado del borrado. Es una lectura, así que no devuelve
    // Outcome: devuelve el conteo simple que la confirmación de la iteración 3 muestra.
    suspend fun affectedSets(exerciseId: Int): Int = dao.countSets(exerciseId)

    // D32: la fusión remapea las filas de la plantilla al ejercicio destino, así que hacen falta las
    // filas de PlanExercise afectadas antes del CASCADE del origen. Es una lectura, sin Outcome, igual
    // que affectedSets, y la apoya el CRUD de planes de la iteración 4.
    suspend fun affectedPlanExercises(exerciseId: Int): Int = planExerciseDao.countForExercise(exerciseId)

    // Aquí no hay error de negocio esperado: el borrado de ejercicio solo puede fallar por I/O o por
    // base de datos, que son los casos que D5 deja en las excepciones.
    suspend fun deleteExercise(exercise: Exercise): Outcome<Unit> {
        dao.delete(exercise)
        return Outcome.Ok(Unit)
    }
}
