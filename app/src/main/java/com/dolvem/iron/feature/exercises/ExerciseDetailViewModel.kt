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
import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.data.repo.ExerciseMetrics
import com.dolvem.iron.data.repo.WindowValue
import com.dolvem.iron.data.settings.Settings
import com.dolvem.iron.data.settings.SettingsDefaults
import com.dolvem.iron.data.settings.effectiveLanguage
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Los últimos registros que muestra el detalle (views_plan.md).
private const val LAST_RECORDS = 10

// Estadísticas del detalle: PR, valor actual y volumen semanal. Las tres salen de Analytics, que ya
// excluye el calentamiento (D9) y resuelve la ventana con fallback (D7).
data class ExerciseStats(val pr: ExerciseMetrics?, val currentValue: WindowValue?, val weeklyVolume: WindowValue?)

// D1: el timestamp se guarda como instante y la conversión a fecha local se hace solo al leer. D17: la
// zona es la inyectada en AppContainer, no una llamada inline, así el cálculo sigue siendo determinista.
data class Record(val date: LocalDate, val set: ExerciseSet)

data class ExerciseDetailState(
    val exercise: Exercise?,
    val category: Category?,
    val settings: Settings,
    val stats: ExerciseStats,
    val records: List<Record>
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(private val container: AppContainer, private val exerciseId: Int) : ViewModel() {

    private val exerciseFlow = container.exerciseRepo.observeById(exerciseId)

    // La categoría depende del ejercicio, así que se resuelve sobre la emisión de la fila.
    private val categoryFlow = exerciseFlow.flatMapLatest { exercise ->
        if (exercise?.categoryId == null) flowOf(null) else container.categoryRepo.observeById(exercise.categoryId)
    }

    private val statsFlow = combine(
        container.analytics.metricsForExercise(exerciseId),
        container.analytics.windowBestWeight(exerciseId),
        container.analytics.windowVolume(exerciseId)
    ) { pr, current, weekly ->
        ExerciseStats(pr, current, weekly)
    }

    private val recordsFlow = container.setRepo.observeLastSets(exerciseId, LAST_RECORDS).map { sets ->
        sets.map { Record(Instant.ofEpochMilli(it.timestamp).atZone(container.zone).toLocalDate(), it) }
    }

    // D5: las lecturas devuelven Flow directo, sin Outcome.
    val state: StateFlow<ExerciseDetailState> = combine(
        exerciseFlow,
        categoryFlow,
        container.settings.settings,
        statsFlow,
        recordsFlow
    ) { exercise, category, settings, stats, records ->
        ExerciseDetailState(exercise, category, settings, stats, records)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ExerciseDetailState(
            null,
            null,
            SettingsDefaults.resolved(effectiveLanguage(null, Locale.getDefault())),
            ExerciseStats(null, null, null),
            emptyList()
        )
    )

    companion object {
        fun factory(container: AppContainer, exerciseId: Int) = viewModelFactory {
            initializer { ExerciseDetailViewModel(container, exerciseId) }
        }
    }
}
