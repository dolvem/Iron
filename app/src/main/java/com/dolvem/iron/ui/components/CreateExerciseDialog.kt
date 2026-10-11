// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category

// D76: el selector de categoría en la creación es un campo dropdown outlined, solo estado cerrado. El
// valor por defecto es la categoría del filtro activo: con el chip de categoría de D78 la intención es
// clara, así que el filtro influye en el valor propuesto, no solo en lo que se muestra.
// D73: título `headlineSmall`, text buttons, input `bodyLarge` y label flotante `bodySmall`.
// El campo es el ancla del menú con `PrimaryNotEditable`, porque `Modifier.clickable` no funciona sobre
// un text field: el campo consume el puntero en el Main pass antes de que el clickable lo reciba, y el
// menú no se abría. El ancla observa el toque en el Initial pass y aporta la semántica del dropdown
// (`Role.DropdownList` y la acción de abrir), que es lo que hace operable el campo con TalkBack
// (D35, D71). El nombre accesible lo da el label.
// D85: el contenedor del menú es `large` (16dp) con `surfaceContainerLow`, pasado explícito.
// D86: los items son opciones de selección, no acciones, así que no llevan leading icon. El glifo
// propio se rota con el estado, que es el comportamiento del componente sin la librería de iconos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDialog(
    title: String,
    nameLabel: String,
    categoryLabel: String,
    actionLabel: String,
    categories: List<Category>,
    defaultCategoryId: Int?,
    onCreate: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(defaultCategoryId) }
    var menuOpen by remember { mutableStateOf(false) }

    val noCategory = stringResource(R.string.exercise_category_none)
    val selectedName = categories.find { it.id == categoryId }?.name ?: noCategory

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel, style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Solo estado cerrado: el campo muestra el valor y el menú se abre al pulsar.
                ExposedDropdownMenuBox(
                    expanded = menuOpen,
                    onExpandedChange = { menuOpen = it }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        label = { Text(categoryLabel, style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_drop_down),
                                contentDescription = null,
                                modifier = Modifier.rotate(if (menuOpen) 180f else 0f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        DropdownMenuItem(
                            text = { Text(noCategory) },
                            onClick = {
                                menuOpen = false
                                categoryId = null
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    menuOpen = false
                                    categoryId = category.id
                                }
                            )
                        }
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
