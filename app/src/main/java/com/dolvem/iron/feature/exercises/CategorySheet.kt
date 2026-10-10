// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category

// Las categorías se gestionan desde la barra de filtros de la lista, como sheet, no como ruta
// (views_plan.md). El sheet solo presenta y pide; los diálogos de confirmación con conteo (D40) y de
// destino de fusión (D32) los tiene la pantalla.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    state: ExercisesState,
    onSelect: (Int?) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Category) -> Unit,
    onMerge: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleMedium)

            // El filtro de categoría admite null, que es "todas las categorías".
            FilterChip(
                selected = state.filter.categoryId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.exercises_filter_all_categories)) }
            )

            if (state.categories.isEmpty()) {
                Text(stringResource(R.string.categories_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    // Reglas de orden: las categorías por nombre. LazyColumn con key en todos los listados.
                    items(state.categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            selected = state.filter.categoryId == category.id,
                            onSelect = { onSelect(category.id) },
                            onRename = { onRename(category) },
                            onMerge = { onMerge(category) },
                            onDelete = { onDelete(category) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_create_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if (name.isNotBlank()) {
                    onCreate(name)
                    name = ""
                }
            }) { Text(stringResource(R.string.action_create)) }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selected,
            onClick = onSelect,
            label = { Text(category.name) },
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { menuOpen = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_row_actions),
                contentDescription = stringResource(R.string.exercises_row_actions, category.name)
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = {
                    menuOpen = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_merge)) },
                onClick = {
                    menuOpen = false
                    onMerge()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                }
            )
        }
    }
}
