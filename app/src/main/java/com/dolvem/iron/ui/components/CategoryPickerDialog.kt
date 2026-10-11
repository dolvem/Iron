// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category

// Selector de categoría en forma de lista, con la opción "sin categoría". Se usa para "Mover a
// categoría" desde el menú de la fila: al pulsar una opción se aplica directamente.
@Composable
fun CategoryPickerDialog(
    title: String,
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                CategoryOption(stringResource(R.string.exercise_category_none), null, selectedId == null, onSelect)
                categories.forEach { category ->
                    CategoryOption(category.name, category.id, selectedId == category.id, onSelect)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// Opción de la lista. El RadioButton marca la selección y la fila es clicable; el contenedor clicable
// llega a 48dp (D35). Compartida con el diálogo de creación de ejercicio.
@Composable
internal fun CategoryOption(label: String, value: Int?, selected: Boolean, onSelect: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onSelect(value) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = { onSelect(value) })
        Text(label)
    }
}
