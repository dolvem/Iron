// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dolvem.iron.IronApp
import com.dolvem.iron.R
import com.dolvem.iron.data.db.Category
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.ui.components.CategoryPickerDialog
import com.dolvem.iron.ui.components.ConfirmCountDialog
import com.dolvem.iron.ui.components.CreateExerciseDialog
import com.dolvem.iron.ui.components.MergeDestinationDialog
import com.dolvem.iron.ui.components.NameDialog
import com.dolvem.iron.ui.components.Pill
import com.dolvem.iron.ui.components.ScreenPlaceholder
import com.dolvem.iron.ui.components.outcomeMessage
import kotlinx.coroutines.launch

// D40: confirmación con conteo de elementos afectados, sin undo en entidades grandes. El conteo lo
// aporta la preconsulta separada del borrado (D5).
private data class Confirmation(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val onConfirm: () -> Unit
)

// La lista está optimizada para filtrar y acceder, no para gestionar (views_plan.md). Las categorías se
// gestionan desde el menú del chip de categoría (D82), no como ruta.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(onOpenDetail: (Int) -> Unit) {
    // LocalResources en lugar de LocalContext para las cadenas: LocalContext no es configuration-aware.
    val resources = LocalResources.current
    val app = LocalContext.current.applicationContext as IronApp
    val viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.factory(app.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fabMenuOpen by rememberSaveable { mutableStateOf(false) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var categoryCreateOpen by rememberSaveable { mutableStateOf(false) }
    var exerciseRename by remember { mutableStateOf<Exercise?>(null) }
    var categoryRename by remember { mutableStateOf<Category?>(null) }
    var mergeTarget by remember { mutableStateOf<Exercise?>(null) }
    var categoryMerge by remember { mutableStateOf<Category?>(null) }
    var categoryMove by remember { mutableStateOf<Exercise?>(null) }
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
                destructive = true,
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
                destructive = false,
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
                destructive = true,
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
                destructive = false,
                onConfirm = {
                    confirmation = null
                    scope.launch { report(viewModel.mergeCategory(source, destination)) }
                }
            )
        }
    }

    // D31: la caja de búsqueda sincroniza el texto con el filtro del ViewModel. El SearchBar de
    // material3 1.4 trabaja con TextFieldState, así que la sincronización se hace por snapshotFlow.
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(state.filter.query)
    // D79: SearchBarState no expone un flag de expandido, sino el valor de la animación. Se lee el estado
    // actual y se colapsa con la animación, que es la API pública.
    val isExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { query ->
                if (query != state.filter.query) {
                    viewModel.setFilter(query, state.filter.category, state.filter.onlyArchived)
                }
            }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            // D79: el filtrado es en vivo, así que no hay submit. El action "Search" del teclado solo
            // colapsa la barra.
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(stringResource(R.string.exercises_search_label)) },
            leadingIcon = {
                // D79: en la search view expandida el leading icon pasa a ser la flecha de atrás, que es el
                // patrón del spec.
                if (isExpanded) {
                    IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.search_back_action)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.exercises_search_label)
                    )
                }
            },
            trailingIcon = {
                // D79: un solo trailing icon, la X con doble función: con texto borra el texto, con texto
                // vacío cancela y colapsa la barra. Con barra contraída y texto vacío no hay nada que
                // limpiar, así que el icono se oculta. D35: el target de IconButton es de 48dp.
                if (textFieldState.text.isNotEmpty() || isExpanded) {
                    IconButton(onClick = {
                        if (textFieldState.text.isNotEmpty()) {
                            textFieldState.edit { delete(0, length) }
                        } else {
                            scope.launch { searchBarState.animateToCollapsed() }
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.search_clear_action)
                        )
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }

    val results: @Composable ColumnScope.() -> Unit = {
        if (state.exercises.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                val text = if (state.filter.query.isEmpty() && state.filter.category == CategoryFilter.All) {
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
                    // D75: la píldora se oculta cuando esa categoría es el filtro activo, porque el chip de
                    // categoría de D78 ya lo enuncia.
                    val category = state.filter.category
                    val pillHidden = (category is CategoryFilter.ById && category.id == exercise.categoryId) ||
                        (category == CategoryFilter.None && exercise.categoryId == null)
                    ExerciseRow(
                        exercise = exercise,
                        categoryName = state.categories.find { it.id == exercise.categoryId }?.name,
                        pillHidden = pillHidden,
                        onOpen = { onOpenDetail(exercise.id) },
                        onRename = { exerciseRename = exercise },
                        onMoveToCategory = { categoryMove = exercise },
                        onMerge = { mergeTarget = exercise },
                        onDelete = { confirmExerciseDelete(exercise) },
                        onArchive = { scope.launch { viewModel.setArchived(exercise, !exercise.archived) } }
                    )
                }
            }
        }
    }

    // D78: la fila de chips se extrae a un composable compartido, como la lista de resultados, para que
    // aparezca igual en la barra contraída y en la search view expandida: buscar y filtrar se usan juntos.
    val chips: @Composable ColumnScope.() -> Unit = {
        FilterChips(
            state = state,
            onSelectCategory = { categoryFilter ->
                viewModel.setFilter(state.filter.query, categoryFilter, state.filter.onlyArchived)
            },
            onClearCategory = {
                viewModel.setFilter(state.filter.query, CategoryFilter.All, state.filter.onlyArchived)
            },
            onToggleArchived = { onlyArchived ->
                viewModel.setFilter(state.filter.query, state.filter.category, onlyArchived)
            },
            onRenameCategory = { category -> categoryRename = category },
            onMergeCategory = { category -> categoryMerge = category },
            onDeleteCategory = { category -> confirmCategoryDelete(category) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Los insets de la ventana los aplica el Scaffold de IronNavHost; aquí no se reaplican para no
        // duplicar el hueco superior.
        contentWindowInsets = WindowInsets(0.dp),
        // D80: el crear es un FAB menu con las dos acciones emparentadas de la pantalla. Se implementa con
        // FAB + DropdownMenu anclado, porque FloatingActionButtonMenu no existe en material3 1.4.0. El menú
        // se ancla sobre el FAB porque el FAB está en la esquina inferior y bajo él no cabría.
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.fab_menu_label)
                    )
                }
                // D85. D86: los dos items son acciones, así que llevan leading icon decorativo. Los dos
                // comparten el glifo de añadir, porque la acción es la misma: crear.
                DropdownMenu(
                    expanded = fabMenuOpen,
                    onDismissRequest = { fabMenuOpen = false },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.fab_menu_create_exercise)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) },
                        onClick = {
                            fabMenuOpen = false
                            createOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.fab_menu_create_category)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) },
                        onClick = {
                            fabMenuOpen = false
                            categoryCreateOpen = true
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FilterBar(searchBarState = searchBarState, inputField = inputField, chips = chips)
            results()
        }

        // El SearchBar se expande al enfocar; los resultados son la misma lista de la pantalla y los chips
        // siguen visibles, porque buscar y filtrar se usan juntos (D78).
        ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
            chips()
            results()
        }
    }

    if (createOpen) {
        // D76: el valor por defecto del selector es la categoría del filtro activo.
        CreateExerciseDialog(
            title = stringResource(R.string.exercise_create_title),
            nameLabel = stringResource(R.string.exercise_name_label),
            categoryLabel = stringResource(R.string.exercise_category_label),
            actionLabel = stringResource(R.string.action_create),
            categories = state.categories,
            defaultCategoryId = (state.filter.category as? CategoryFilter.ById)?.id,
            onCreate = { name, categoryId ->
                createOpen = false
                scope.launch { report(viewModel.createExercise(name, categoryId)) }
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

    val moveSource = categoryMove
    if (moveSource != null) {
        // La asignación no tiene error de negocio esperado (D5), así que no hay Outcome que reportar.
        CategoryPickerDialog(
            title = stringResource(R.string.action_move_to_category),
            categories = state.categories,
            selectedId = moveSource.categoryId,
            onSelect = { categoryId ->
                categoryMove = null
                scope.launch { viewModel.setCategory(moveSource, categoryId) }
            },
            onDismiss = { categoryMove = null }
        )
    }

    val pending = confirmation
    if (pending != null) {
        ConfirmCountDialog(
            title = pending.title,
            message = pending.message,
            destructive = pending.destructive,
            onConfirm = pending.onConfirm,
            onDismiss = { confirmation = null }
        )
    }

    if (categoryCreateOpen) {
        NameDialog(
            title = stringResource(R.string.category_create_title),
            label = stringResource(R.string.exercise_name_label),
            initial = "",
            actionLabel = stringResource(R.string.action_create),
            onSubmit = { name ->
                categoryCreateOpen = false
                scope.launch { report(viewModel.createCategory(name)) }
            },
            onDismiss = { categoryCreateOpen = false }
        )
    }
}

// D79: la barra es la search bar con el trailing X de doble función. D78: la fila de chips va debajo, y la
// línea de filtro activo de D74 se descarta porque el chip ya enuncia lo que está activo. La barra y los
// chips son el mismo grupo de acceso al filtro, así que el gap entre ellos es space100 (8dp), no la
// separación de sección de space300; el margen horizontal se queda en 16dp, el de la ventana compacta.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    chips: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchBar(
            state = searchBarState,
            inputField = inputField,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        chips()
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    categoryName: String?,
    pillHidden: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMoveToCategory: () -> Unit,
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
            // D75: el nombre es el texto dominante. Es la variante emphasized de bodyLarge, que la skill
            // permite para headlines que necesitan énfasis.
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            // D75: la píldora de categoría debajo del nombre, con el badge Archivado en la misma línea.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!pillHidden) {
                    // D83: la píldora de "sin categoría" usa la pareja neutra, para diferenciarla de las pills
                    // de categoría. El badge Archivado mantiene la pareja de D75.
                    if (categoryName == null) {
                        Pill(
                            stringResource(R.string.exercise_category_none),
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Pill(categoryName)
                    }
                }
                if (exercise.archived) {
                    Pill(stringResource(R.string.exercises_archived))
                }
            }
        }

        // El ancla del DropdownMenu es el padre de composición, así que el Box alrededor del botón es lo que
        // ancla el menú bajo el icono. Sin el Box el ancla sería la fila completa y el menú abriría en el
        // borde start, que es el borde izquierdo de la pantalla.
        Box {
            // D35: el target táctico de IconButton es de 48dp y el icono lleva contentDescription.
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_row_actions),
                    contentDescription = stringResource(R.string.exercises_row_actions, exercise.name)
                )
            }

            // D85: el contenedor del menú es `large` con `surfaceContainerLow`, la lectura del vertical
            // menu expressive. El default de Compose es el baseline de 4dp con `surfaceContainer`.
            // D86: los items de acción llevan leading icon de 24dp en `on_surface_variant`, que son los
            // valores que ya aplica Compose, así que solo se pasa el slot. El icono es decorativo: el
            // nombre accesible del item lo da el texto.
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_rename)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_to_category)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_category), null) },
                    onClick = {
                        menuOpen = false
                        onMoveToCategory()
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
                    // D86: el glifo cambia con el estado, como cambia el label del item.
                    leadingIcon = {
                        Icon(
                            painterResource(
                                if (exercise.archived) R.drawable.ic_unarchive else R.drawable.ic_archive
                            ),
                            null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onArchive()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_merge)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_merge), null) },
                    onClick = {
                        menuOpen = false
                        onMerge()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}
