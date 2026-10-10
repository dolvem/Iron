// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.quicklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.ui.components.outcomeMessage
import com.dolvem.iron.ui.components.parseDecimal
import kotlinx.coroutines.launch

// El quick log es un FAB contextual, no un bloque de Hoy (views_plan.md). El ejercicio que se abre
// preseleccionado es el contexto de la pantalla que lo lanza.
// D30: el editor de serie completo, que se mantiene abierto tras registrar y pasa el foco al siguiente
// campo, es el componente compartido de la iteración 5. Aquí está el formulario mínimo sobre el mismo
// camino de quick log, sin variantes por pantalla.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(exercise: Exercise, viewModel: QuickLogViewModel, onDismiss: () -> Unit) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var load by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }

    // D6: el editor muestra el error que devuelve el repositorio, no valida por su cuenta. El error se
    // muestra dentro del sheet, porque el SnackbarHost de la pantalla queda tapado mientras el sheet
    // está abierto, y el editor se mantiene abierto tras el rechazo (D30).
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.quick_log_title), style = MaterialTheme.typography.titleMedium)
            Text(exercise.name, style = MaterialTheme.typography.bodyMedium)

            // D28: el campo acepta lo que el usuario escribe, con coma o punto, y el redondeo ocurre al
            // guardar. La UI no valida: el error de D6 lo devuelve el repositorio.
            OutlinedTextField(
                value = load,
                onValueChange = { load = it },
                label = { Text(stringResource(R.string.quick_log_load)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text(stringResource(R.string.quick_log_reps)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(onClick = {
                scope.launch {
                    val outcome = viewModel.save(exercise.id, reps.toIntOrNull(), parseDecimal(load))
                    when (outcome) {
                        // D30: el editor se mantiene abierto tras registrar, así que solo se limpian los
                        // campos. El foco al siguiente campo llega con el editor compartido de la iteración 5.
                        is Outcome.Ok -> {
                            load = ""
                            reps = ""
                            errorMessage = null
                        }
                        is Outcome.Error -> errorMessage = outcomeMessage(resources, outcome)
                    }
                }
            }) { Text(stringResource(R.string.action_save)) }

            val message = errorMessage
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    // D35: el mensaje se anuncia al aparecer, no solo cuando se lee. Role no tiene
                    // variante Error en Compose 1.12, así que se usa liveRegion.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
