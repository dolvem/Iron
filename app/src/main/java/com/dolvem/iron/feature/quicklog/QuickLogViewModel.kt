// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.quicklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dolvem.iron.AppContainer
import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.domain.model.Outcome

// Quick log: escribe en la sesión abierta del día; si no hay ninguna, crea una sesión libre para hoy,
// siempre serie de trabajo porque no hay contexto de plan (D2). Exige reps o loadKg, y el rechazo lo
// produce el repositorio, no el editor (D6).
class QuickLogViewModel(private val container: AppContainer) : ViewModel() {

    suspend fun save(exerciseId: Int, reps: Int?, loadKg: Double?): Outcome<ExerciseSet> =
        container.setRepo.quickLog(exerciseId, reps, loadKg, null)

    companion object {
        // Los ViewModel se construyen con viewModelFactory { initializer { } } leyendo el contenedor
        // desde la Application (IronApp).
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { QuickLogViewModel(container) }
        }
    }
}
