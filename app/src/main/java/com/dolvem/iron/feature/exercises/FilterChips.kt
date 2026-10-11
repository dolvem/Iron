// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category

// D78: la fila de chips bajo la search bar es el acceso al filtro, porque el chip es el componente que M3
// define para filtrar contenido. El chip de categoría abre el menú con las opciones de filtrado y el CRUD
// de la categoría seleccionada (D82); el de archivado es el toggle de los dos estados de D81. La línea de
// filtro activo de D74 se descarta: el chip ya enuncia lo que está activo. D84: el menú no lista "Todas",
// porque el chip es el filtro, no una opción de filtrado, y el estado sin filtro se enuncia con el label
// "Categoría".
// El chip con menú es un híbrido que el spec no define, FilterChip + DropdownMenu es la lectura estándar.
// D35: el visual del chip es de 32dp y Compose expande el target clicable hasta 48dp por fuera de los
// límites del composable, el mismo mecanismo de los chips de Material Components.
// El gap vertical con la search bar lo aplica `FilterBar` (8dp, space100); aquí no se repite el padding
// vertical, porque el target de 48dp ya aporta el aire visual alrededor del chip.
@Composable
fun FilterChips(
    state: ExercisesState,
    onSelectCategory: (CategoryFilter) -> Unit,
    onClearCategory: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onRenameCategory: (Category) -> Unit,
    onMergeCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val filter = state.filter.category
    val selected = (filter as? CategoryFilter.ById)?.id?.let { id -> state.categories.find { it.id == id } }
    val label = when {
        selected != null -> selected.name
        filter == CategoryFilter.None -> stringResource(R.string.exercise_category_none)
        else -> stringResource(R.string.filter_chip_category)
    }
    val archivedLabel = stringResource(R.string.filter_chip_archived)
    // D35: el nombre accesible de los chips incluye el estado, porque el check de M3 es solo visual.
    val chipName = accessibleName(label, filter != CategoryFilter.All)
    val archivedName = accessibleName(archivedLabel, state.filter.onlyArchived)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            FilterChip(
                selected = filter != CategoryFilter.All,
                onClick = { menuOpen = true },
                // El label se trunca con ellipsis: con maxLines = 1 el default de Compose es Clip, que corta
                // el nombre sin indicar que hay más texto.
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = if (filter != CategoryFilter.All) {
                    { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                trailingIcon = {
                    if (filter != CategoryFilter.All) {
                        // D35: el X es un peer clicable independiente dentro del chip. Para TalkBack el chip
                        // es un solo elemento, así que el nombre accesible incluye el estado y la acción, y
                        // quitar el filtro también está alcanzable con el item de limpiar del menú (D84).
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.filter_clear_action),
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable(onClick = onClearCategory)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                },
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .semantics { contentDescription = chipName }
            )

            // D85
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                state.categories.forEach { category ->
                    CountItem(category.name, state.categoryCounts[category.id] ?: 0) {
                        onSelectCategory(CategoryFilter.ById(category.id))
                    }
                }
                CountItem(stringResource(R.string.exercise_category_none), state.categoryCounts[null] ?: 0) {
                    onSelectCategory(CategoryFilter.None)
                }

                // D84: el item "Todas" se descarta, porque el chip es el filtro y no una opción de
                // filtrado: el estado sin filtro lo enuncia el label del chip. Limpiar es la X, y el item
                // la replica, porque el X es un clicable anidado que TalkBack fusiona dentro del chip y la
                // acción necesita un peer alcanzable (D71, D35).
                if (filter != CategoryFilter.All) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // D86: el leading icon decorativo va en los items de acción. Los items de selección de
                    // categoría no lo llevan, porque no representan una acción y romperían la alineación
                    // con el conteo.
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_clear_action)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_close), null) },
                        onClick = {
                            menuOpen = false
                            onClearCategory()
                        }
                    )

                    // D82: el CRUD solo aparece para la categoría seleccionada, porque el chip es el contexto.
                    if (selected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
                            onClick = {
                                menuOpen = false
                                onRenameCategory(selected)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_merge)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_merge), null) },
                            onClick = {
                                menuOpen = false
                                onMergeCategory(selected)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null) },
                            onClick = {
                                menuOpen = false
                                onDeleteCategory(selected)
                            }
                        )
                    }
                }
            }
        }

        FilterChip(
            selected = state.filter.onlyArchived,
            onClick = { onToggleArchived(!state.filter.onlyArchived) },
            label = { Text(archivedLabel) },
            leadingIcon = if (state.filter.onlyArchived) {
                { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
            } else {
                null
            },
            modifier = Modifier.semantics { contentDescription = archivedName }
        )
    }
}

// Item del menú del chip: label y conteo de resultados mostrados. El target táctico del item es de 48dp
// (D35), por encima de los 44dp del spec, que es la desviación consciente de la regla de accesibilidad.
// El contenedor del menú es `large` con `surfaceContainerLow` (D85), con elevación nivel 2.
@Composable
private fun CountItem(label: String, count: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { Text(count.toString(), style = MaterialTheme.typography.labelMedium) },
        onClick = onClick
    )
}

// D35: nombre accesible del chip con el estado, porque el check de M3 es solo visual y Compose no lo
// expone en la semántica del FilterChip.
@Composable
private fun accessibleName(label: String, selected: Boolean): String {
    val stateLabel = stringResource(
        if (selected) R.string.filter_chip_state_selected else R.string.filter_chip_state_unselected
    )
    return stringResource(R.string.filter_chip_name, label, stateLabel)
}
