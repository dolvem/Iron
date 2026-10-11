// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R

// D40: en el borrado de entidades grandes (ejercicio, plan, categoría) se pide confirmación con conteo
// de elementos afectados, sin undo. El conteo lo aporta la preconsulta separada del borrado (D5), y el
// mensaje usa plurals.xml, con reglas distintas en cada idioma (D36).
// D73: los actions son text buttons; el label del confirming usa `primary`, y el de una acción
// destructiva usa `error`. El título es `headlineSmall` (Compose trae `titleLarge` por defecto, así que
// se fuerza).
@Composable
fun ConfirmCountDialog(
    title: String,
    message: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.action_confirm),
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
