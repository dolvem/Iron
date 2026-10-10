// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import com.dolvem.iron.data.db.Category
import com.dolvem.iron.data.db.CategoryDao
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.nameKey
import kotlinx.coroutines.flow.Flow

// D32: resumen de la fusión de categorías. Las categorías no tienen notes ni archived, así que de las
// reglas de D32 solo se aplica la que interesa aquí: sobrevive el nombre del destino y los ejercicios
// se remapean.
data class CategoryMergeSummary(val destination: Category, val remappedExercises: Int)

// D5: la violación de único sobre nameKey se captura aquí y se mapea a DuplicateName. Las lecturas
// devuelven Flow directo, sin Outcome, y la preconsulta de conteo es un método separado del borrado.
// Recibe la base de datos porque D32 exige que el remapeo de los ejercicios y el borrado del origen
// estén en la misma transacción.
class CategoryRepo(private val db: IronDatabase, private val dao: CategoryDao) {

    // Reglas de orden: las categorías por nombre.
    fun observeAll(): Flow<List<Category>> = dao.observeAll()

    fun observeById(categoryId: Int): Flow<Category?> = dao.observeById(categoryId)

    suspend fun createCategory(name: String): Outcome<Category> {
        val category = Category(id = 0, name = name, nameKey = nameKey(name))
        try {
            return Outcome.Ok(category.copy(id = dao.insert(category).toInt()))
        } catch (e: SQLiteException) {
            val error = mapConstraintViolation(name, e.message)
            if (error == null) throw e
            return error
        }
    }

    // D4: el renombrar usa la misma función de normalización que el insert.
    suspend fun renameCategory(category: Category, name: String): Outcome<Category> {
        val renamed = category.copy(name = name, nameKey = nameKey(name))
        try {
            dao.update(renamed)
            return Outcome.Ok(renamed)
        } catch (e: SQLiteException) {
            val error = mapConstraintViolation(name, e.message)
            if (error == null) throw e
            return error
        }
    }

    // D5: preconsulta de conteo separada del borrado. En categoría lo que se cuenta son los ejercicios
    // que quedan sin categoría por el SET NULL, que es lo que el usuario pierde.
    suspend fun affectedExercises(categoryId: Int): Int = dao.countExercises(categoryId)

    // Aquí no hay error de negocio esperado: el borrado de categoría solo puede fallar por I/O o por
    // base de datos, que son los casos que D5 deja en las excepciones. Los ejercicios quedan sin
    // categoría por el SET NULL de la FK.
    suspend fun deleteCategory(category: Category): Outcome<Unit> {
        dao.delete(category)
        return Outcome.Ok(Unit)
    }

    // D32: el usuario elige el destino y sobrevive su nombre. Los ejercicios se remapean antes del
    // borrado del origen, en la misma transacción, porque borrar el origen sin remapear dejaría los
    // ejercicios sin categoría.
    suspend fun mergeCategory(source: Category, destination: Category): Outcome<CategoryMergeSummary> =
        db.withWriteTransaction {
            val remappedExercises = dao.remapExercises(source.id, destination.id)
            dao.delete(source)
            Outcome.Ok(CategoryMergeSummary(destination, remappedExercises))
        }
}
