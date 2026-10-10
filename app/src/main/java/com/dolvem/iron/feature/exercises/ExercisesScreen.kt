// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dolvem.iron.IronApp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.ui.components.ConfirmCountDialog
import com.dolvem.iron.ui.components.MergeDestinationDialog
import com.dolvem.iron.ui.components.NameDialog
import com.dolvem.iron.ui.components.ScreenPlaceholder
import com.dolvem.iron.ui.components.outcomeMessage
import kotlinx.coroutines.launch

// D40: confirmación con conteo de elementos afectados, sin undo en entidades grandes. El conteo lo
// aporta la preconsulta separada del borrado (D5).
private data class Confirmation(val title: String, val message: String, val onConfirm: () -> Unit)

// La lista está optimizada para filtrar y acceder, no para gestionar (views_plan.md). Las categorías se
// gestionan desde la barra de filtros, como sheet, no como ruta.
@Composable
fun ExercisesScreen(onOpenDetail: (Int) -> Unit) {
    // LocalResources en lugar de LocalContext para las cadenas: LocalContext no es configuration-aware.
    val resources = LocalResources.current
    val app = LocalContext.current.applicationContext as IronApp
    val viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.factory(app.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var exerciseRename by remember { mutableStateOf<Exercise?>(null) }
    var categoryRename by remember { mutableStateOf<Category?>(null) }
    var mergeTarget by remember { mutableStateOf<Exercise?>(null) }
    var categoryMerge by remember { mutableStateOf<Category?>(null) }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }

    fun quantity(res: Int, count: Int): String = resources.getQuantityString(res, count, count)

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun report(outcome: Outcome<*>) {
        if (outcome is Outcome.Error) notify(outcomeMessage(resources, outcome))
    }

    fun confirmExerciseDelete(exercise: Exercise) {
        scope.launch {
            val message = quantity(R.plurals.confirm_delete_exercise, viewModel.affectedSets(exercise))
            confirmation = Confirmation(
                resources.getString(R.string.exercise_delete_title),
                message,
                onConfirm = {
                    confirmation = null
                    scope.launch { report(viewModel.deleteExercise(exercise)) }
                }
            )
        }
    }

    // D32: el usuario elige el destino, y la confirmación cuenta lo que se remapea.
    fun confirmExerciseMerge(source: Exercise, destination: Exercise) {
        scope.launch {
            val message = quantity(R.plurals.confirm_merge_exercise, viewModel.affectedSets(source)) +
                "\n" +
                quantity(R.plurals.confirm_merge_plan_rows, viewModel.affectedPlanExercises(source))
            confirmation = Confirmation(
                resources.getString(R.string.exercise_merge_title),
                message,
                onConfirm = {
                    confirmation = null
                    scope.launch {
                        val outcome = viewModel.mergeExercise(source, destination)
                        report(outcome)
                        // D32: los conflictos de unique(planId, exerciseId) se registran en el resumen.
                        if (outcome is Outcome.Ok && outcome.value.conflicts > 0) {
                            notify(quantity(R.plurals.merge_conflicts, outcome.value.conflicts))
                        }
                    }
                }
            )
        }
    }

    fun confirmCategoryDelete(category: Category) {
        scope.launch {
            val message = quantity(R.plurals.confirm_delete_category, viewModel.affectedExercises(category))
            confirmation = Confirmation(
                resources.getString(R.string.category_delete_title),
                message,
                onConfirm = {
                    confirmation = null
                    scope.launch { report(viewModel.deleteCategory(category)) }
                }
            )
        }
    }

    fun confirmCategoryMerge(source: Category, destination: Category) {
        scope.launch {
            val message = quantity(R.plurals.confirm_merge_category, viewModel.affectedExercises(source))
            confirmation = Confirmation(
                resources.getString(R.string.category_merge_title),
                message,
                onConfirm = {
                    confirmation = null
                    scope.launch { report(viewModel.mergeCategory(source, destination)) }
                }
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FilterBar(
                state = state,
                onQueryChange = { query ->
                    viewModel.setFilter(query, state.filter.categoryId, state.filter.showArchived)
                },
                onOpenCategories = { sheetOpen = true },
                onToggleArchived = { show ->
                    viewModel.setFilter(state.filter.query, state.filter.categoryId, show)
                },
                // El crear entra por la barra: la lista es de filtrar y acceder, y el CRUD se usa sobre
                // todo en el setup inicial (views_plan.md). El nombre se envía con la categoría del filtro.
                onCreate = { createOpen = true }
            )

            if (state.exercises.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    val text = if (state.filter.query.isEmpty() && state.filter.categoryId == null) {
                        stringResource(R.string.exercises_empty)
                    } else {
                        stringResource(R.string.exercises_no_results)
                    }
                    ScreenPlaceholder(text)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Reglas de orden: los ejercicios por nombre. LazyColumn con key en todos los
                    // listados.
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            categoryName = state.categories.find { it.id == exercise.categoryId }?.name,
                            onOpen = { onOpenDetail(exercise.id) },
                            onRename = { exerciseRename = exercise },
                            onMerge = { mergeTarget = exercise },
                            onDelete = { confirmExerciseDelete(exercise) },
                            onArchive = { scope.launch { viewModel.setArchived(exercise, !exercise.archived) } }
                        )
                    }
                }
            }
        }
    }

    if (createOpen) {
        NameDialog(
            title = stringResource(R.string.exercise_create_title),
            label = stringResource(R.string.exercise_name_label),
            initial = "",
            actionLabel = stringResource(R.string.action_create),
            onSubmit = { name ->
                createOpen = false
                scope.launch { report(viewModel.createExercise(name, state.filter.categoryId)) }
            },
            onDismiss = { createOpen = false }
        )
    }

    val exercise = exerciseRename
    if (exercise != null) {
        NameDialog(
            title = stringResource(R.string.exercise_rename_title),
            label = stringResource(R.string.exercise_name_label),
            initial = exercise.name,
            actionLabel = stringResource(R.string.action_rename),
            onSubmit = { name ->
                exerciseRename = null
                scope.launch { report(viewModel.renameExercise(exercise, name)) }
            },
            onDismiss = { exerciseRename = null }
        )
    }

    val category = categoryRename
    if (category != null) {
        NameDialog(
            title = stringResource(R.string.category_rename_title),
            label = stringResource(R.string.exercise_name_label),
            initial = category.name,
            actionLabel = stringResource(R.string.action_rename),
            onSubmit = { name ->
                categoryRename = null
                scope.launch { report(viewModel.renameCategory(category, name)) }
            },
            onDismiss = { categoryRename = null }
        )
    }

    val source = mergeTarget
    if (source != null) {
        // D32: el destino lo elige el usuario y sobrevive su nombre.
        MergeDestinationDialog(
            title = stringResource(R.string.exercise_merge_title),
            hint = stringResource(R.string.exercise_merge_hint),
            candidates = state.mergeCandidates.filter { it.id != source.id },
            label = { it.name },
            onSelect = { destination ->
                mergeTarget = null
                confirmExerciseMerge(source, destination)
            },
            onDismiss = { mergeTarget = null }
        )
    }

    val categorySource = categoryMerge
    if (categorySource != null) {
        MergeDestinationDialog(
            title = stringResource(R.string.category_merge_title),
            hint = stringResource(R.string.exercise_merge_hint),
            candidates = state.categories.filter { it.id != categorySource.id },
            label = { it.name },
            onSelect = { destination ->
                categoryMerge = null
                confirmCategoryMerge(categorySource, destination)
            },
            onDismiss = { categoryMerge = null }
        )
    }

    val pending = confirmation
    if (pending != null) {
        ConfirmCountDialog(
            title = pending.title,
            message = pending.message,
            onConfirm = pending.onConfirm,
            onDismiss = { confirmation = null }
        )
    }

    if (sheetOpen) {
        CategorySheet(
            state = state,
            onSelect = { categoryId ->
                viewModel.setFilter(state.filter.query, categoryId, state.filter.showArchived)
            },
            onCreate = { name -> scope.launch { report(viewModel.createCategory(name)) } },
            onRename = { category -> categoryRename = category },
            onMerge = { category -> categoryMerge = category },
            onDelete = { category -> confirmCategoryDelete(category) },
            onDismiss = { sheetOpen = false }
        )
    }
}

// D31: caja de búsqueda case-insensitive sobre nameKey, en la barra de filtros.
@Composable
private fun FilterBar(
    state: ExercisesState,
    onQueryChange: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onCreate: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.exercises_search_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            FilterChip(
                selected = state.filter.categoryId != null,
                onClick = onOpenCategories,
                label = { Text(stringResource(R.string.exercises_filter_category)) }
            )
            FilterChip(
                selected = state.filter.showArchived,
                onClick = { onToggleArchived(!state.filter.showArchived) },
                label = { Text(stringResource(R.string.exercises_filter_show_archived)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // D35: el target táctico de IconButton es de 48dp y el icono lleva contentDescription.
            IconButton(onClick = onCreate) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.exercises_create_action)
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    categoryName: String?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        // La fila es el acceso al detalle; el target táctico de la fila completa supera los 48dp (D35).
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                categoryName ?: stringResource(R.string.exercise_category_none),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (exercise.archived) {
            Text(stringResource(R.string.exercises_archived), style = MaterialTheme.typography.bodySmall)
        }

        // D35: el target táctico de IconButton es de 48dp y el icono lleva contentDescription.
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_row_actions),
                contentDescription = stringResource(R.string.exercises_row_actions, exercise.name)
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
                text = {
                    Text(
                        if (exercise.archived) {
                            stringResource(R.string.action_unarchive)
                        } else {
                            stringResource(R.string.action_archive)
                        }
                    )
                },
                onClick = {
                    menuOpen = false
                    onArchive()
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
