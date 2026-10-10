// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Exercise

// Formulario de una fila de la plantilla: orden, series máx, calentamiento, RPE objetivo y habilitado,
// los campos editables de PlanExercise. El ejercicio solo se elige al crear; al editar la fila es fija,
// porque es la relación entre el plan y el ejercicio.
// Los campos numéricos vacíos se guardan como null: el default de maxSets y warmupSets es "sin asignar"
// (D37). La UI no valida el rango orientativo de targetRpe (D37, D6): el error siempre viene del
// repositorio, y aquí no hay error de negocio esperado salvo unique(planId, exerciseId) al crear.
@Composable
fun PlanExerciseFormDialog(
    title: String,
    actionLabel: String,
    exercises: List<Exercise>,
    initialExercise: Exercise?,
    initialSequence: Int?,
    initialMaxSets: Int?,
    initialWarmupSets: Int?,
    initialTargetRpe: Int?,
    initialEnabled: Boolean,
    onSubmit: (PlanExerciseDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseId by remember { mutableStateOf(initialExercise?.id) }
    var sequence by remember { mutableStateOf(initialSequence?.toString().orEmpty()) }
    var maxSets by remember { mutableStateOf(initialMaxSets?.toString().orEmpty()) }
    var warmupSets by remember { mutableStateOf(initialWarmupSets?.toString().orEmpty()) }
    var targetRpe by remember { mutableStateOf(initialTargetRpe?.toString().orEmpty()) }
    var enabled by remember { mutableStateOf(initialEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialExercise == null) {
                    Text(stringResource(R.string.plan_exercise_label), style = MaterialTheme.typography.bodySmall)
                    // El selector es una lista con nombre accesible propio (D71). Con un catálogo grande
                    // el diálogo se queda corto, así que la lista es desplazable.
                    Column(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        exercises.forEach { exercise ->
                            ExerciseOption(exercise.name, exercise.id, exercise.id == exerciseId) {
                                exerciseId = it
                            }
                        }
                    }
                } else {
                    Text(initialExercise.name, style = MaterialTheme.typography.titleMedium)
                }

                NumberField(stringResource(R.string.plan_sequence_label), sequence) { sequence = it }
                NumberField(stringResource(R.string.plan_max_sets_label), maxSets) { maxSets = it }
                NumberField(stringResource(R.string.plan_warmup_sets_label), warmupSets) { warmupSets = it }
                NumberField(stringResource(R.string.plan_target_rpe_label), targetRpe) { targetRpe = it }

                // D71: el control recibe nombre accesible con la fila fusionada, porque por sí solo no lo
                // tiene; el target táctico del contenedor clicable llega a 48dp (D35).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(stringResource(R.string.plan_enabled_label))
                }
            }
        },
        confirmButton = {
            val chosen = exerciseId
            val order = sequence.toIntOrNull()
            TextButton(
                onClick = {
                    if (chosen != null && order != null) {
                        onSubmit(
                            PlanExerciseDraft(
                                chosen,
                                order,
                                maxSets.toIntOrNull(),
                                warmupSets.toIntOrNull(),
                                targetRpe.toIntOrNull(),
                                enabled
                            )
                        )
                    }
                },
                enabled = chosen != null && order != null
            ) { Text(actionLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// Opción del selector de ejercicio, la misma forma que CategoryOption: RadioButton en fila clicable.
@Composable
private fun ExerciseOption(label: String, value: Int, selected: Boolean, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onSelect(value) })
            // D71: nombre accesible propio para el control dentro de la fila clicable.
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = { onSelect(value) })
        Text(label)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
