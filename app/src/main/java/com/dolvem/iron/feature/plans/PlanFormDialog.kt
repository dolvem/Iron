// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import java.time.DayOfWeek
import java.time.format.TextStyle

// Formulario de plan: título, días y orden, los tres campos de la entidad. El título vacío se guarda como
// null, y Plan.title nulo se muestra como "Plan" (rules.md). Los días se escriben con formatPlanDays en
// el ViewModel, el formato canónico de D62. El orden vacío se guarda como null, porque la columna es
// nullable y los nulos van al final del listado.
// El estado local de pantalla usa remember, como fija la UX operativa.
@Composable
fun PlanFormDialog(
    title: String,
    actionLabel: String,
    initialTitle: String,
    initialDays: List<Int>,
    initialSequence: Int?,
    onSubmit: (String?, List<Int>, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // D72: el locale es el de Compose, no Locale.getDefault().
    val locale = LocalLocale.current.platformLocale
    var name by remember { mutableStateOf(initialTitle) }
    var days by remember { mutableStateOf(initialDays) }
    var sequence by remember { mutableStateOf(initialSequence?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.plan_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.plan_days_label), style = MaterialTheme.typography.bodySmall)
                // Días ISO 1-7 en el mismo orden que el formato canónico de Plan.days (D62). El target
                // táctico del contenedor clicable del chip llega a 48dp (D35).
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..7).toList()) { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sequence,
                    onValueChange = { sequence = it },
                    label = { Text(stringResource(R.string.plan_sequence_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSubmit(name.trim().takeIf { it.isNotEmpty() }, days, sequence.toIntOrNull())
            }) { Text(actionLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
