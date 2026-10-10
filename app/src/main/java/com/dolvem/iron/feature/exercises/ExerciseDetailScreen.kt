// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dolvem.iron.IronApp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.repo.WindowValue
import com.dolvem.iron.feature.quicklog.QuickLogSheet
import com.dolvem.iron.feature.quicklog.QuickLogViewModel
import com.dolvem.iron.ui.components.QuickLogFab
import com.dolvem.iron.ui.components.ScreenPlaceholder
import com.dolvem.iron.ui.components.formatDate
import com.dolvem.iron.ui.components.formatLoad

// El detalle es longitudinal: un ejercicio a través del tiempo (views_plan.md). Aquí están la identidad,
// las estadísticas y los últimos registros. La gráfica la escriben los componentes compartidos de
// feature/graph/ en la iteración 8, con la alternativa textual accesible de D35.
@Composable
fun ExerciseDetailScreen(exerciseId: Int) {
    val app = LocalContext.current.applicationContext as IronApp
    val viewModel: ExerciseDetailViewModel =
        viewModel(factory = ExerciseDetailViewModel.factory(app.container, exerciseId))
    val quickLog: QuickLogViewModel = viewModel(factory = QuickLogViewModel.factory(app.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var quickLogOpen by rememberSaveable { mutableStateOf(false) }

    val exercise = state.exercise
    if (exercise == null) {
        ScreenPlaceholder(stringResource(R.string.exercises_empty))
        return
    }

    Scaffold(
        floatingActionButton = {
            // Quick log FAB contextual: preselecciona este ejercicio (views_plan.md).
            QuickLogFab { quickLogOpen = true }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Identity(state, exercise)
            Stats(state)
            Records(state, Modifier.weight(1f))
        }
    }

    // El error de D6 se muestra dentro del sheet, no en un snackbar de la pantalla: el sheet tapa el
    // SnackbarHost y el editor se queda abierto tras el rechazo (D30).
    if (quickLogOpen) {
        QuickLogSheet(
            exercise = exercise,
            viewModel = quickLog,
            onDismiss = { quickLogOpen = false }
        )
    }
}

@Composable
private fun Identity(state: ExerciseDetailState, exercise: Exercise) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            state.category?.name ?: stringResource(R.string.exercise_category_none),
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.settings.showNotes && !exercise.notes.isNullOrBlank()) {
            Text(stringResource(R.string.exercise_notes), style = MaterialTheme.typography.titleSmall)
            Text(exercise.notes.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Estadísticas: PR, valor actual y volumen semanal. Las tres salen de Analytics, que ya excluye el
// calentamiento (D9) y resuelve la ventana con fallback (D7).
@Composable
private fun Stats(state: ExerciseDetailState) {
    val locale = LocalLocale.current.platformLocale
    val unit = state.settings.strengthUnit

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.exercise_stats), style = MaterialTheme.typography.titleMedium)

        val stats = state.stats
        StatRow(
            stringResource(R.string.exercise_pr),
            stats.pr?.bestWeightKg?.let {
                formatLoad(it, unit, locale)
            },
            null
        )
        StatRow(
            stringResource(R.string.exercise_current_value),
            stats.currentValue?.value?.let { formatLoad(it, unit, locale) },
            stats.currentValue
        )
        StatRow(
            stringResource(R.string.exercise_weekly_volume),
            stats.weeklyVolume?.value?.let { formatLoad(it, unit, locale) },
            stats.weeklyVolume
        )
    }
}

// D7: la ventana vacía se etiqueta, porque un hueco se lee como "no he entrenado" cuando la semana
// anterior sí tiene datos.
@Composable
private fun StatRow(label: String, value: String?, window: WindowValue?) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(value ?: stringResource(R.string.value_placeholder), style = MaterialTheme.typography.bodyMedium)
        if (window != null && window.fallback) {
            Text(stringResource(R.string.exercise_window_fallback), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Records(state: ExerciseDetailState, modifier: Modifier) {
    val locale = LocalLocale.current.platformLocale
    val unit = state.settings.strengthUnit

    Column(modifier = modifier.padding(16.dp)) {
        Text(stringResource(R.string.exercise_last_records), style = MaterialTheme.typography.titleMedium)

        if (state.records.isEmpty()) {
            Text(stringResource(R.string.exercise_no_records), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // LazyColumn con key en todos los listados.
                items(state.records, key = { it.set.id }) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            formatDate(record.date, state.settings.shortDateFormat, locale),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            record.set.loadKg?.let { formatLoad(it, unit, locale) }
                                ?: stringResource(R.string.value_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            record.set.reps?.toString() ?: stringResource(R.string.value_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (record.set.isWarmup) {
                            Text(stringResource(R.string.record_warmup), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
