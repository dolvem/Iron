// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dolvem.iron.AppContainer
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.data.db.PlanExercise
import com.dolvem.iron.data.repo.PlanFrequency
import com.dolvem.iron.data.settings.Settings
import com.dolvem.iron.data.settings.SettingsDefaults
import com.dolvem.iron.data.settings.effectiveLanguage
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.formatPlanDays
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// Salida del formulario de una fila de la plantilla. sequence es obligatorio porque la columna no es
// nullable; los demás campos admiten null, que es "sin asignar" (D37). La UI no valida el rango de
// targetRpe: el error siempre viene del repositorio (D6), y D37 fija el rango como orientativo.
data class PlanExerciseDraft(
    val exerciseId: Int,
    val sequence: Int,
    val maxSets: Int?,
    val warmupSets: Int?,
    val targetRpe: Int?,
    val enabled: Boolean
)

// Fila de la plantilla con el ejercicio resuelto, porque la pantalla pinta el nombre y el quick log
// necesita la entidad. El nombre se resuelve con la lectura de todos los ejercicios, incluida la
// variante con archived, porque una fila de la plantilla puede apuntar a un ejercicio archivado.
data class PlanExerciseRow(val planExercise: PlanExercise, val exercise: Exercise)

data class PlanRowsState(
    val showDisabled: Boolean = false,
    val rows: List<PlanExerciseRow> = emptyList(),
    val exercises: List<Exercise> = emptyList()
)

data class PlanDetailState(
    val plan: Plan? = null,
    val rows: PlanRowsState = PlanRowsState(),
    val frequency: PlanFrequency = PlanFrequency(null, 0),
    val openSession: Boolean = false,
    val settings: Settings = SettingsDefaults.resolved(effectiveLanguage(null, Locale.getDefault()))
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanDetailViewModel(private val container: AppContainer, private val planId: Int) : ViewModel() {

    // Reglas de orden: enabled = false excluido por defecto, con el filtro para mostrarlo.
    private val showDisabled = MutableStateFlow(false)

    // Los candidatos del selector se leen con query vacía y con archived, igual que los candidatos de
    // fusión de la iteración 3: el filtro de la pantalla no debe limitar lo que el usuario puede elegir.
    private val rowsFlow = combine(
        showDisabled,
        showDisabled.flatMapLatest { show ->
            if (show) {
                container.planRepo.observeExercisesIncludingDisabled(planId)
            } else {
                container.planRepo.observeExercises(planId)
            }
        },
        container.exerciseRepo.search("", null, true)
    ) { show, rows, exercises ->
        PlanRowsState(
            show,
            rows.mapNotNull { row -> exercises.find { it.id == row.exerciseId }?.let { PlanExerciseRow(row, it) } },
            exercises
        )
    }

    // D5: las lecturas devuelven Flow directo, sin Outcome. openSession es la lectura que decide el
    // bloqueo de la pantalla, y la frecuencia es la de D8.
    val state: StateFlow<PlanDetailState> = combine(
        container.planRepo.observeById(planId),
        rowsFlow,
        container.planRepo.observeFrequency(planId),
        container.workoutRepo.observeOpenForPlan(planId),
        container.settings.settings
    ) { plan, rows, frequency, openSession, settings ->
        PlanDetailState(plan, rows, frequency, openSession != null, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanDetailState())

    fun setShowDisabled(show: Boolean) {
        showDisabled.update { show }
    }

    // Reglas de borrados: el plan con sesión abierta bloquea edición y borrado, y la plantilla se
    // bloquea por la misma razón. El error lo devuelve el repositorio (D5).
    suspend fun editPlan(plan: Plan, title: String?, days: List<Int>, sequence: Int?): Outcome<Plan> =
        container.planRepo.editPlan(plan, title, formatPlanDays(days), sequence)

    // D5: la violación de unique(planId, exerciseId) se mapea a DuplicateName con el nombre del ejercicio.
    suspend fun addExercise(draft: PlanExerciseDraft): Outcome<PlanExercise> = container.planRepo.addExercise(
        planId,
        draft.exerciseId,
        draft.sequence,
        draft.maxSets,
        draft.warmupSets,
        draft.targetRpe,
        draft.enabled
    )

    suspend fun editExercise(row: PlanExerciseRow, draft: PlanExerciseDraft): Outcome<PlanExercise> =
        container.planRepo.editExercise(
            row.planExercise,
            draft.sequence,
            draft.maxSets,
            draft.warmupSets,
            draft.targetRpe,
            draft.enabled
        )

    // Borrar una fila de la plantilla no toca las series ya registradas, porque las series apuntan al
    // ejercicio y a la sesión, no a la plantilla. No hay elementos afectados que contar, así que no hay
    // confirmación con conteo (D40): el borrado es directo.
    suspend fun deleteExercise(row: PlanExerciseRow): Outcome<Unit> =
        container.planRepo.deleteExercise(row.planExercise)

    companion object {
        fun factory(container: AppContainer, planId: Int) = viewModelFactory {
            initializer { PlanDetailViewModel(container, planId) }
        }
    }
}
