// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dolvem.iron.AppContainer
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.formatPlanDays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Fila de la lista de Planes: el plan y si coincide con hoy, que es lo que se resalta (views.md).
// D34 deja el recálculo a medianoche en la iteración 6, así que aquí la coincidencia se resuelve en
// cada emisión, con el Clock y la ZoneId inyectados (D17).
data class PlanRow(val plan: Plan, val matchesToday: Boolean)

data class PlansState(val plans: List<PlanRow> = emptyList())

class PlansViewModel(private val container: AppContainer) : ViewModel() {

    // Reglas de orden: los planes por sequence, con los nulos al final. D67: Flow<List<Plan>>, así que
    // la emisión inicial trae el resultado completo y la lista es alcanzable desde la pantalla.
    val state: StateFlow<PlansState> = container.planRepo.observeAll()
        .map { plans -> PlansState(plans.map { PlanRow(it, container.planRepo.matchesToday(it)) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansState())

    // D5: en Plan no hay índice único, así que el crear no tiene error de negocio esperado. El formato
    // canónico de days (D62) se escribe aquí con formatPlanDays, la única función de escritura del dominio.
    suspend fun createPlan(title: String?, days: List<Int>, sequence: Int?): Outcome<Plan> =
        container.planRepo.createPlan(title, formatPlanDays(days), sequence)

    suspend fun editPlan(plan: Plan, title: String?, days: List<Int>, sequence: Int?): Outcome<Plan> =
        container.planRepo.editPlan(plan, title, formatPlanDays(days), sequence)

    // D33: la confirmación cuenta las sesiones que quedan como sesiones libres, porque es lo que el
    // usuario pierde. D5: la preconsulta es un método separado del borrado, y es una lectura.
    suspend fun affectedSessions(plan: Plan): Int = container.planRepo.affectedSessions(plan.id)

    suspend fun deletePlan(plan: Plan): Outcome<Unit> = container.planRepo.deletePlan(plan)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { PlansViewModel(container) }
        }
    }
}
