// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dolvem.iron.AppContainer
import com.dolvem.iron.data.db.Category
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.repo.CategoryMergeSummary
import com.dolvem.iron.data.repo.ExerciseMergeSummary
import com.dolvem.iron.domain.model.Outcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// Estado de pantalla: el filtro de la barra (D31) y lo que emite el repositorio. La UI no valida, el
// error siempre viene del repositorio (D6), así que aquí no hay validación de nombres.
data class ExercisesFilter(val query: String = "", val categoryId: Int? = null, val showArchived: Boolean = false)

data class ExercisesState(
    val filter: ExercisesFilter = ExercisesFilter(),
    val categories: List<Category> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    // D32: los candidatos de fusión no siguen el filtro de la lista. Si el destino archivado no fuera
    // alcanzable, la regla del menos restrictivo en archived no se podría aplicar nunca.
    val mergeCandidates: List<Exercise> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel(private val container: AppContainer) : ViewModel() {

    private val filter = MutableStateFlow(ExercisesFilter())

    // D5: las lecturas devuelven Flow directo, sin Outcome. El filtro re-suscribe la consulta, así que
    // la búsqueda sobre nameKey se recalcula con la normalización de D4 en cada cambio.
    val state: StateFlow<ExercisesState> = combine(
        filter,
        filter.flatMapLatest { f -> container.exerciseRepo.search(f.query, f.categoryId, f.showArchived) },
        container.categoryRepo.observeAll(),
        // D32: los candidatos de fusión no siguen el filtro de la lista. Si el destino archivado no
        // fuera alcanzable, la regla del menos restrictivo en archived no se podría aplicar nunca.
        container.exerciseRepo.search("", null, true)
    ) { f, exercises, categories, candidates ->
        ExercisesState(f, categories, exercises, candidates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesState())

    fun setFilter(query: String, categoryId: Int?, showArchived: Boolean) {
        filter.update { it.copy(query = query, categoryId = categoryId, showArchived = showArchived) }
    }

    // CRUD de ejercicios. Las operaciones con error de negocio esperado devuelven Outcome (D5); el
    // archivado no lo tiene, así que devuelve la entidad.
    suspend fun createExercise(name: String, categoryId: Int?): Outcome<Exercise> =
        container.exerciseRepo.createExercise(name, categoryId)

    suspend fun renameExercise(exercise: Exercise, name: String): Outcome<Exercise> =
        container.exerciseRepo.renameExercise(exercise, name)

    suspend fun setArchived(exercise: Exercise, archived: Boolean): Exercise =
        container.exerciseRepo.setArchived(exercise, archived)

    // D5: la asignación de categoría no tiene error de negocio esperado, así que no devuelve Outcome,
    // como el archivado. El valor null es "sin categoría".
    suspend fun setCategory(exercise: Exercise, categoryId: Int?): Exercise =
        container.exerciseRepo.setCategory(exercise, categoryId)

    // D5: la preconsulta de conteo es un método separado del borrado, y es lo que permite la
    // confirmación con conteo de afectados de D40.
    suspend fun affectedSets(exercise: Exercise): Int = container.exerciseRepo.affectedSets(exercise.id)

    suspend fun deleteExercise(exercise: Exercise): Outcome<Unit> = container.exerciseRepo.deleteExercise(exercise)

    // D32: la fusión remapea las FKs y registra los conflictos de unique(planId, exerciseId).
    suspend fun mergeExercise(source: Exercise, destination: Exercise): Outcome<ExerciseMergeSummary> =
        container.exerciseRepo.mergeExercise(source, destination)

    // D5: la preconsulta de las filas de plantilla que la fusión remapea, separada del borrado del origen.
    suspend fun affectedPlanExercises(exercise: Exercise): Int =
        container.exerciseRepo.affectedPlanExercises(exercise.id)

    // CRUD de categorías, gestionadas desde la barra de filtros como sheet, no como ruta.
    suspend fun createCategory(name: String): Outcome<Category> = container.categoryRepo.createCategory(name)

    suspend fun renameCategory(category: Category, name: String): Outcome<Category> =
        container.categoryRepo.renameCategory(category, name)

    suspend fun affectedExercises(category: Category): Int = container.categoryRepo.affectedExercises(category.id)

    suspend fun deleteCategory(category: Category): Outcome<Unit> = container.categoryRepo.deleteCategory(category)

    suspend fun mergeCategory(source: Category, destination: Category): Outcome<CategoryMergeSummary> =
        container.categoryRepo.mergeCategory(source, destination)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ExercisesViewModel(container) }
        }
    }
}
