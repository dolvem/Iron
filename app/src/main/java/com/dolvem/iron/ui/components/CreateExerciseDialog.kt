// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.foundation.layout.Column
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
import com.dolvem.iron.data.db.Category

// Diálogo de creación de ejercicio: nombre y selector de categoría. La creación no hereda la categoría
// del filtro activo, porque el filtro cambia lo que se ve, no lo que se asigna.
@Composable
fun CreateExerciseDialog(
    title: String,
    nameLabel: String,
    categoryLabel: String,
    actionLabel: String,
    categories: List<Category>,
    onCreate: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true
                )
                Text(categoryLabel, style = MaterialTheme.typography.bodySmall)
                CategoryOption(stringResource(R.string.exercise_category_none), null, categoryId == null) {
                    categoryId = it
                }
                categories.forEach { category ->
                    CategoryOption(category.name, category.id, categoryId == category.id) {
                        categoryId = it
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, categoryId) }, enabled = name.isNotBlank()) { Text(actionLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
