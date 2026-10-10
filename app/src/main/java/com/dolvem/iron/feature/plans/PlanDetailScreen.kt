// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dolvem.iron.IronApp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.parsePlanDays
import com.dolvem.iron.feature.quicklog.QuickLogSheet
import com.dolvem.iron.feature.quicklog.QuickLogViewModel
import com.dolvem.iron.ui.components.QuickLogFab
import com.dolvem.iron.ui.components.ScreenPlaceholder
import com.dolvem.iron.ui.components.formatDate
import com.dolvem.iron.ui.components.outcomeMessage
import kotlinx.coroutines.launch

// Formulario abierto de una fila: exercise null es crear, row null es crear. Al editar, el ejercicio es
// fijo, porque la fila es la relación entre el plan y el ejercicio.
private data class RowForm(val exercise: Exercise?, val row: PlanExerciseRow?)

// PlanDetalle: solo configuración, bloqueada mientras hay una sesión abierta (views.md), porque editar la
// plantilla mutaría la plantilla con la sesión en curso. Aquí están la identidad, la frecuencia de D8 y la
// plantilla; el borrado del plan está en las filas de la lista (D69).
@Composable
fun PlanDetailScreen(planId: Int) {
    val resources = LocalResources.current
    val app = LocalContext.current.applicationContext as IronApp
    val viewModel: PlanDetailViewModel =
        viewModel(factory = PlanDetailViewModel.factory(app.container, planId))
    val quickLog: QuickLogViewModel = viewModel(factory = QuickLogViewModel.factory(app.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var planFormOpen by rememberSaveable { mutableStateOf(false) }
    var quickLogOpen by rememberSaveable { mutableStateOf(false) }
    var rowForm by remember { mutableStateOf<RowForm?>(null) }

    val plan = state.plan
    if (plan == null) {
        ScreenPlaceholder(stringResource(R.string.plans_empty))
        return
    }

    // Quick log FAB contextual: preselecciona el ejercicio del plan (views.md). Con varios ejercicios se
    // toma el primero en el orden de la plantilla, que es el que la pantalla muestra primero.
    val preselected = state.rows.rows.firstOrNull { it.planExercise.enabled }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun report(outcome: Outcome<*>) {
        if (outcome is Outcome.Error) notify(outcomeMessage(resources, outcome))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (preselected != null) QuickLogFab { quickLogOpen = true }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Identity(plan, blocked = state.openSession, onEdit = { planFormOpen = true })
            Frequency(state)

            if (state.openSession) {
                Text(
                    stringResource(R.string.plan_blocked_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            RowsBlock(
                state = state,
                modifier = Modifier.weight(1f),
                onAdd = { rowForm = RowForm(null, null) },
                onEdit = { rowForm = RowForm(it.exercise, it) },
                onDelete = { row -> scope.launch { report(viewModel.deleteExercise(row)) } },
                onToggleDisabled = { show -> viewModel.setShowDisabled(show) }
            )
        }
    }

    if (planFormOpen) {
        PlanFormDialog(
            title = stringResource(R.string.plan_edit_title),
            actionLabel = stringResource(R.string.action_save),
            initialTitle = plan.title.orEmpty(),
            initialDays = parsePlanDays(plan.days),
            initialSequence = plan.sequence,
            onSubmit = { title, days, sequence ->
                planFormOpen = false
                scope.launch { report(viewModel.editPlan(plan, title, days, sequence)) }
            },
            onDismiss = { planFormOpen = false }
        )
    }

    val form = rowForm
    if (form != null) {
        val row = form.row
        val create = row == null
        PlanExerciseFormDialog(
            title = stringResource(
                if (create) R.string.plan_exercise_create_title else R.string.plan_exercise_edit_title
            ),
            actionLabel = stringResource(if (create) R.string.action_create else R.string.action_save),
            exercises = state.rows.exercises,
            initialExercise = form.exercise,
            // Los campos sin asignar se prerellenan con el default de Ajustes (D37), que también puede
            // estar sin asignar. sequence es obligatorio, así que al crear se propone la siguiente posición.
            initialSequence = row?.planExercise?.sequence ?: state.rows.rows.size + 1,
            initialMaxSets = row?.planExercise?.maxSets ?: state.settings.maxSets,
            initialWarmupSets = row?.planExercise?.warmupSets ?: state.settings.warmupSets,
            initialTargetRpe = row?.planExercise?.targetRpe,
            initialEnabled = row?.planExercise?.enabled ?: true,
            onSubmit = { draft ->
                rowForm = null
                scope.launch {
                    if (row == null) {
                        report(viewModel.addExercise(draft))
                    } else {
                        report(viewModel.editExercise(row, draft))
                    }
                }
            },
            onDismiss = { rowForm = null }
        )
    }

    // El error de D6 se muestra dentro del sheet, no en el snackbar de la pantalla (D70).
    if (quickLogOpen && preselected != null) {
        QuickLogSheet(preselected.exercise, quickLog, onDismiss = { quickLogOpen = false })
    }
}

@Composable
private fun Identity(plan: Plan, blocked: Boolean, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                plan.title ?: stringResource(R.string.plan_title_default),
                style = MaterialTheme.typography.headlineSmall
            )
            PlanLine(plan)
        }

        // D35: el target táctico de IconButton es de 48dp y el icono lleva contentDescription.
        // El plan con sesión abierta bloquea la edición, así que el action está deshabilitado.
        IconButton(onClick = onEdit, enabled = !blocked) {
            Icon(
                painter = painterResource(R.drawable.ic_row_actions),
                contentDescription = stringResource(R.string.plan_edit_action)
            )
        }
    }
}

// D8: los dos datos de frecuencia, la última sesión y las sesiones de la ventana móvil de 30 días
// naturales incluyendo hoy. La ventana vacía muestra el placeholder, no cero como si hubiera datos.
@Composable
private fun Frequency(state: PlanDetailState) {
    val locale = LocalLocale.current.platformLocale

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.plan_frequency_title), style = MaterialTheme.typography.titleMedium)

        Text(stringResource(R.string.plan_last_session), style = MaterialTheme.typography.titleSmall)
        Text(
            state.frequency.lastSessionDate?.let { formatDate(it, state.settings.shortDateFormat, locale) }
                ?: stringResource(R.string.value_placeholder),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(stringResource(R.string.plan_sessions_last_30_days), style = MaterialTheme.typography.titleSmall)
        Text(state.frequency.sessionsLast30Days.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RowsBlock(
    state: PlanDetailState,
    modifier: Modifier,
    onAdd: () -> Unit,
    onEdit: (PlanExerciseRow) -> Unit,
    onDelete: (PlanExerciseRow) -> Unit,
    onToggleDisabled: (Boolean) -> Unit
) {
    val blocked = state.openSession

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.plan_exercises_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))

            // Reglas de orden: enabled = false excluido por defecto, con filtro para mostrarlo.
            FilterChip(
                selected = state.rows.showDisabled,
                onClick = { onToggleDisabled(!state.rows.showDisabled) },
                label = { Text(stringResource(R.string.plan_exercises_show_disabled)) }
            )

            IconButton(onClick = onAdd, enabled = !blocked) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.plan_exercise_add_action)
                )
            }
        }

        if (state.rows.rows.isEmpty()) {
            Text(
                stringResource(R.string.plan_exercises_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // Reglas de orden: los ejercicios del plan por sequence. LazyColumn con key en todos los
                // listados.
                items(state.rows.rows, key = { it.planExercise.id }) { row ->
                    PlanExerciseRowView(
                        row = row,
                        blocked = blocked,
                        onEdit = { onEdit(row) },
                        onDelete = { onDelete(row) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanExerciseRowView(row: PlanExerciseRow, blocked: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        // La fila muestra la configuración de la plantilla; el CRUD está en el menú de acciones, que es el
        // target táctico de 48dp (D35).
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
            FieldsLine(row)
        }

        IconButton(onClick = { menuOpen = true }, enabled = !blocked) {
            Icon(
                painter = painterResource(R.drawable.ic_row_actions),
                contentDescription = stringResource(R.string.exercises_row_actions, row.exercise.name)
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
                enabled = !blocked
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
                enabled = !blocked
            )
        }
    }
}

// Campos editables de la fila: orden, series máx, calentamiento, RPE objetivo y habilitado. Los null se
// omiten, porque son "sin asignar" (D37); enabled = false se muestra como etiqueta, porque la regla de
// exclusión por defecto solo tiene sentido cuando el filtro lo muestra.
@Composable
private fun FieldsLine(row: PlanExerciseRow) {
    val parts = mutableListOf("${stringResource(R.string.plan_sequence_label)} ${row.planExercise.sequence}")

    row.planExercise.maxSets?.let { parts.add("${stringResource(R.string.plan_max_sets_label)} $it") }
    row.planExercise.warmupSets?.let { parts.add("${stringResource(R.string.plan_warmup_sets_label)} $it") }
    row.planExercise.targetRpe?.let { parts.add("${stringResource(R.string.plan_target_rpe_label)} $it") }
    if (!row.planExercise.enabled) parts.add(stringResource(R.string.plan_disabled_label))

    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
}
