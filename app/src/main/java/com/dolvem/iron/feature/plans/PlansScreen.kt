// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.parsePlanDays
import com.dolvem.iron.ui.components.ConfirmCountDialog
import com.dolvem.iron.ui.components.ScreenPlaceholder
import com.dolvem.iron.ui.components.formatDayNames
import com.dolvem.iron.ui.components.outcomeMessage
import kotlinx.coroutines.launch

// D40: confirmación con conteo de elementos afectados, sin undo en entidades grandes. El conteo lo
// aporta la preconsulta separada del borrado (D5).
private data class Confirmation(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val onConfirm: () -> Unit
)

// Pantalla Planes: lista ordenada por sequence, con los nulos al final, y el plan de hoy resaltado
// (views.md). El CRUD del plan está en las filas de la lista, como en Ejercicios (D69); el detalle es solo
// configuración, y la acción de crear es pequeña, no un FAB, por el mismo motivo.
@Composable
fun PlansScreen(onOpenDetail: (Int) -> Unit) {
    // LocalResources en lugar de LocalContext para las cadenas: LocalContext no es configuration-aware.
    val resources = LocalResources.current
    val app = LocalContext.current.applicationContext as IronApp
    val viewModel: PlansViewModel = viewModel(factory = PlansViewModel.factory(app.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var createOpen by rememberSaveable { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun report(outcome: Outcome<*>) {
        if (outcome is Outcome.Error) notify(outcomeMessage(resources, outcome))
    }

    // D33: la confirmación avisa que las sesiones quedan como sesiones libres, con el conteo de esas
    // sesiones, porque es lo que el usuario pierde. El borrado sigue bloqueado mientras haya una sesión
    // abierta, así que la confirmación solo aplica a sesiones cerradas.
    fun confirmPlanDelete(plan: Plan) {
        scope.launch {
            val sessions = viewModel.affectedSessions(plan)
            confirmation = Confirmation(
                resources.getString(R.string.plan_delete_title),
                resources.getQuantityString(R.plurals.confirm_delete_plan, sessions, sessions),
                destructive = true,
                onConfirm = {
                    confirmation = null
                    scope.launch { report(viewModel.deletePlan(plan)) }
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Los insets de la ventana los aplica el Scaffold de IronNavHost; aquí no se reaplican para no
        // duplicar el hueco superior.
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.nav_plans), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                // D35: el target táctico de IconButton es de 48dp y el icono lleva contentDescription.
                IconButton(onClick = { createOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.plans_create_action)
                    )
                }
            }

            if (state.plans.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    ScreenPlaceholder(stringResource(R.string.plans_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Reglas de orden: los planes por sequence, con los nulos al final. LazyColumn con key
                    // en todos los listados.
                    items(state.plans, key = { it.plan.id }) { row ->
                        PlanRowView(
                            row = row,
                            onOpen = { onOpenDetail(row.plan.id) },
                            onDelete = { confirmPlanDelete(row.plan) }
                        )
                    }
                }
            }
        }
    }

    if (createOpen) {
        PlanFormDialog(
            title = stringResource(R.string.plan_create_title),
            actionLabel = stringResource(R.string.action_create),
            initialTitle = "",
            initialDays = emptyList(),
            initialSequence = null,
            onSubmit = { title, days, sequence ->
                createOpen = false
                scope.launch { report(viewModel.createPlan(title, days, sequence)) }
            },
            onDismiss = { createOpen = false }
        )
    }

    val pending = confirmation
    if (pending != null) {
        ConfirmCountDialog(
            title = pending.title,
            message = pending.message,
            destructive = pending.destructive,
            onConfirm = pending.onConfirm,
            onDismiss = { confirmation = null }
        )
    }
}

@Composable
private fun PlanRowView(row: PlanRow, onOpen: () -> Unit, onDelete: () -> Unit) {
    val plan = row.plan
    // El resaltado del plan de hoy es el fondo del contenedor. Con colores dinámicos el pairing
    // garantizado por la paleta es secondaryContainer con onSecondaryContainer, así que el color de
    // contenido se fija al emparejado, no a onSurface (D35). La fila completa es el acceso al detalle y
    // supera los 48dp.
    val highlight = if (row.matchesToday) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor =
        if (row.matchesToday) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlight)
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plan.title ?: stringResource(R.string.plan_title_default),
                    style = MaterialTheme.typography.titleMedium
                )
                PlanLine(plan)
            }

            if (row.matchesToday) {
                Text(stringResource(R.string.plan_today_label), style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_row_actions),
                    contentDescription = stringResource(
                        R.string.exercises_row_actions,
                        plan.title ?: stringResource(R.string.plan_title_default)
                    )
                )
            }
        }
    }
}

// Línea de la fila: los días con el nombre en el idioma activo (D72) y el orden cuando está definido.
// Cadena vacía en days significa "nunca" (D62), y un plan sin sequence no muestra orden. Compartida por
// la fila de la lista y por la identidad del detalle.
@Composable
internal fun PlanLine(plan: Plan) {
    val locale = LocalLocale.current.platformLocale
    val days = parsePlanDays(plan.days)
    val parts = mutableListOf<String>()

    if (days.isEmpty()) {
        parts.add(stringResource(R.string.plan_days_none))
    } else {
        parts.add(formatDayNames(plan.days, locale))
    }

    plan.sequence?.let { parts.add("${stringResource(R.string.plan_sequence_label)} $it") }

    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
}
