// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R

// D32: el usuario elige el destino de la fusión y sobrevive su nombre. Aquí solo se hace la elección;
// el conteo de afectados lo muestra la confirmación de D40.
@Composable
fun <T> MergeDestinationDialog(
    title: String,
    hint: String,
    candidates: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint)
                candidates.forEach { candidate ->
                    TextButton(onClick = { onSelect(candidate) }) { Text(label(candidate)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
