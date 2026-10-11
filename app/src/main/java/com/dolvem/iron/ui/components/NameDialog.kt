// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R

// Formulario de nombre compartido por el CRUD de ejercicios y categorías. El estado local de pantalla
// usa rememberSaveable, como fija la UX operativa.
// D73: título `headlineSmall` y text buttons. Tipografías de la skill: input `bodyLarge` y label
// flotante `bodySmall`.
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    actionLabel: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(value) }, enabled = value.isNotBlank()) { Text(actionLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
