<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# MD3 Component Catalog

Complete reference for Material Design 3 components, mapped to Jetpack Compose
(`androidx.compose.material3`). Some expressive APIs require
`@OptIn(ExperimentalMaterial3Api::class)` or
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` depending on the active BOM.

## Google I/O 2026 component updates

Material's [I/O 2026 update](https://m3.material.io/blog/whats-new-at-io26) highlights
refreshed expressive guidance for **lists**, **menus**, **search**, and **search app bars**,
with Jetpack Compose as the primary implementation path:

- Prefer current `androidx.compose.material3` components and verify expressive APIs against
  the active Material3 BOM.
- Expect expressive variants to include richer visual style, motion, and flexible
  configuration.

## Actions

### Buttons

MD3 has 5 button types ordered by emphasis: Filled > Filled Tonal > Elevated > Outlined > Text.

Shared attributes: disabled state, icon leading/trailing, form role. Minimum touch target
48dp; icon-only buttons always need an accessible name.

| Type | Compose API | Use when |
|------|-------------|----------|
| Filled | `Button` | Primary action, highest emphasis |
| Filled tonal | `FilledTonalButton` | Medium emphasis, softer than filled; secondary actions alongside a filled button |
| Elevated | `ElevatedButton` | Medium emphasis with shadow; on colored backgrounds where tonal blends in |
| Outlined | `OutlinedButton` | Medium emphasis, neutral; secondary actions |
| Text | `TextButton` | Lowest emphasis; inline actions; dialog actions (D73: dialog actions are text buttons) |

```kotlin
Button(onClick = { /* primary action */ }) {
    Icon(Icons.Default.Check, contentDescription = null) // decorative next to label
    Text("Get started")
}

OutlinedButton(onClick = { /* secondary */ }) { Text("Cancel") }
TextButton(onClick = { /* inline */ }) { Text("Learn more") }
```

**Button sizes (Expressive)**: 5 sizes — extra-small, small (default), medium, large,
extra-large — via `ButtonDefaults.buttonColors`/size APIs per BOM. Toggle buttons exist in
the expressive set.

### Button group

Grouping related actions with connected visual treatment. Compose: `ButtonGroup` /
`ButtonGroupDefaults` (Expressive, per BOM).

```kotlin
// Expressive, per BOM
ButtonGroup(selectedIndex = 0, options = listOf("Day", "Week", "Month")) { index ->
    ButtonGroupItem(onClick = { select(index) }) { Text(options[index]) }
}
```

### FAB (Floating Action Button)

The single most important action on a screen. Always provide `contentDescription` (icon-only).

| Size | Compose API |
|------|-------------|
| Small | `SmallFloatingActionButton` |
| Medium (default) | `FloatingActionButton` |
| Large | `LargeFloatingActionButton` |

Variants: surface, primary, secondary, tertiary (via `FabDefaults` colors per BOM).

```kotlin
FloatingActionButton(
    onClick = { /* quick log */ },
    contentDescription = "Quick log",
) {
    Icon(Icons.Default.Add, contentDescription = null)
}
```

### Extended FAB

Primary action with explanatory text: `ExtendedFloatingActionButton`.

```kotlin
ExtendedFloatingActionButton(
    onClick = { /* open session */ },
    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
    text = { Text("New session") },
)
```

### FAB menu

Expandable FAB with menu items (Expressive, per BOM).

### Icon button

4 variants:

| Variant | Compose API |
|---------|-------------|
| Standard | `IconButton` |
| Filled | `FilledIconButton` |
| Filled tonal | `FilledTonalIconButton` |
| Outlined | `OutlinedIconButton` |

Toggle behavior available (Expressive, per BOM). Always provide `contentDescription`; for
toggle buttons, descriptive labels for both states.

```kotlin
IconButton(onClick = { /* settings */ }) {
    Icon(Icons.Default.Settings, contentDescription = "Settings")
}
```

### Segmented buttons

Single-select or multi-select group. Compose: `SegmentedButton` (ExperimentalMaterial3Api).

```kotlin
SegmentedButton(selected = listOf(activeIndex), onSelectionChanged = { select(it) }) {
    Segment(0) { Text("List") }
    Segment(1) { Text("Grid") }
}
```

### Split button

Primary action + attached menu (Expressive, per BOM).

### Badge

Small (dot) or large (count). Compose: `Badge` and `BadgedBox`.

```kotlin
BadgedBox(badge = { Badge { Text("3") } }) {
    Icon(Icons.Default.Notifications, contentDescription = "Notifications, 3 unread")
}
```

### Progress indicator

Linear and circular, determinate/indeterminate. Compose: `LinearProgressIndicator`,
`CircularProgressIndicator`. Add an accessible label for context (e.g. "Loading").

```kotlin
LinearProgressIndicator(progress = { 0.6f })          // determinate
CircularProgressIndicator()                          // indeterminate
```

### Snackbar

Single-line, two-line, with action. Compose: `Snackbar` inside `SnackbarHost` (wired through
`Scaffold(snackbarHost = ...)`). Iron: undo snackbar for set deletion, 6-second window (D40).

```kotlin
SnackbarHostView(host = snackbarHost)

snackbarHost.show(
    Snackbar(
        duration = SnackbarDuration.Short,
        action = { TextButton(onClick = undo) { Text("Undo") } },
    ) { Text("Set deleted") }
)
```

Colors: `inverse-surface` / `inverse-on-surface` / `inverse-primary` (action).

### Tooltip

Plain (short label on hover/focus, for icon buttons and truncated text) and rich (multi-line
with optional actions, for complex explanations). Compose: `PlainTooltip`, `RichTooltip`,
`TooltipBox`.

## Containment

### Card

Three variants:

| Variant | Appearance | Elevation | Compose API |
|---------|-----------|-----------|-------------|
| Filled | `surface-container-highest` fill, no border | Level 0 | `Card` |
| Outlined | `surface` fill, `outline-variant` border | Level 0 | `OutlinedCard` |
| Elevated | `surface-container-low` fill, shadow | Level 1 | `ElevatedCard` |

Shape: `medium` (12dp).

```kotlin
OutlinedCard(onClick = { open(id) }) {
    Column(Modifier.padding(16.dp)) {
        Text("Card title", style = MaterialTheme.typography.titleMedium)
        Text(
            "Supporting text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### Carousel

Multi-browse, uncontained, hero. Compose: `HorizontalCarousel` (ExperimentalMaterial3Api).

### Dialog

Basic and full-screen. Compose: `AlertDialog`, `FullScreenDialog` (Expressive, per BOM).

**Iron rules (D73, strict M3 reading)**: actions are text buttons, no filled container inside
the dialog; aligned to the trailing edge, confirming closest to the edge, dismissive to its
left; padding 24dp all sides; 24dp between content and actions; 8dp gap between buttons;
text button height 36dp; confirming label uses `primary`, destructive action uses `error`.

```kotlin
AlertDialog(
    onDismiss = { dismiss() },
    title = { Text("Confirm action") },
    text = { Text("Are you sure you want to proceed?") },
    confirmButton = {
        TextButton(onClick = confirm) {
            Text("Confirm", color = MaterialTheme.colorScheme.primary)
        }
    },
    dismissButton = {
        TextButton(onClick = dismiss) { Text("Cancel") }
    },
)
```

Dialog headline: `headlineSmall`; body: `bodyMedium`. Shape: `extra-large`.

### Bottom sheet

Standard (persistent, coexists with content) and modal (blocks interaction, scrim). Compose:
`ModalBottomSheet`. Shape: `extra-large` top corners; elevation level 1.

### Side sheet

Standard (docked alongside content) and modal (overlay with scrim). No dedicated Compose
component; implement with a `Row` + `Surface` using `surface-container` and shape tokens.
Out of v1 scope (D38, compact only).

### Divider

Full-width and inset. Compose: `HorizontalDivider`, `VerticalDivider`. Color:
`outline-variant` (never `outline`).

```kotlin
HorizontalDivider()
HorizontalDivider(Modifier.padding(horizontal = 16.dp)) // inset
```

### Checkbox

States: checked, unchecked, indeterminate, disabled. Compose: `Checkbox`. Wrap with a label
or `Semantics`; built-in checkbox role.

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = isWarmup, onCheckedChange = { toggle(it) })
    Text("Warm-up")
}
```

### Chips

| Variant | Compose API | Use |
|---------|-------------|-----|
| Assist | `AssistChip` | Smart suggestions, shortcuts |
| Filter | `FilterChip` | Filtering content, multi-select |
| Input | `InputChip` | User input tokens (email recipients) |
| Suggestion | `SuggestionChip` | Suggested responses, queries |

Shape: `small` (8dp). Iron (D74): no chip row for category filter; the filter entry is an
icon inside the search bar.

```kotlin
FilterChip(selected = active, onClick = { toggle() }, label = { Text("Strength") })
```

### Date picker

Docked (inline calendar), modal (full dialog), range. Compose: `DatePicker`,
`DatePickerDialog`, `DatePickerDefaults`, `rememberDatePickerState`. Elevation level 3.

### Menu

Expressive menus have updated guidance (I/O 2026): more flexible, vibrant configurations.
Compose: `DropdownMenu`, `DropdownMenuItem` (ExperimentalMaterial3Api). Shape: `small`;
elevation level 2.

```kotlin
DropdownMenu(expanded = open, onDismissRequest = { close() }) {
    DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = edit)
    DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = delete)
}
```

### Radio button

Compose: `RadioButton`. Group in a container with an accessible group name
(`mergeDescendants = true`, D71).

### Slider

Continuous, discrete (step), range. Compose: `Slider`, `RangeSlider`. Handle shape: `full`.

```kotlin
Slider(value = progress, onValueChange = { update(it) }, valueRange = 1f..100f, steps = 99)
```

### Switch

With or without icons. Compose: `Switch`. Track shape: `full`.

```kotlin
Switch(checked = enabled, onCheckedChange = { toggle(it) })
```

### Text field

Filled and outlined. Compose: `TextField` (filled), `OutlinedTextField`. Prefer
state-based APIs (`TextFieldState`, `rememberTextFieldState()`) on current Material3
releases. Shape: `small` top corners.

Attributes: label, value, keyboard type, placeholder, required, disabled, error state +
error text, supporting text, prefix/suffix, max length (counter), multiline rows.

```kotlin
val state = rememberTextFieldState("")

OutlinedTextField(
    state = state,
    label = { Text("Weight (kg)") },
    supportingText = { if (error != null) Text(error) },
    isError = error != null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = Modifier.fillMaxWidth(),
)
```

**Iron rules**: the editor shows the error returned by the repository, it does not validate
on its own (D5, D6). Decimal separator: accept comma and dot, normalize to dot on parsing
(D28). Shape: `small` top corners.

### Time picker

Docked (inline time input) and modal (clock dial in dialog). Compose: `TimeInput`,
`TimePicker`, `TimePickerDialog` (ExperimentalMaterial3Api). Elevation level 3.

## Navigation

### App bar (top)

Four variants:

| Variant | Height | Title position | Scroll behavior | Compose API |
|---------|--------|----------------|-----------------|-------------|
| Center-aligned | 64dp | Center | Elevates to level 2 on scroll | `CenterAlignedTopAppBar` |
| Small | 64dp | Start | Elevates on scroll | `TopAppBar` |
| Medium | 112dp | Bottom, start | Collapses to small on scroll | `MediumTopAppBar` |
| Large | 152dp | Bottom, start | Collapses to small on scroll | `LargeTopAppBar` |

Expressive variants (large flexible, search app bar) may require
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` per BOM. Title style: `titleLarge`.

```kotlin
TopAppBar(
    title = { Text("Today") },
    navigationIcon = { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") } },
    actions = { IconButton(onClick = search) { Icon(Icons.Default.Search, "Search") } },
)
```

### Navigation bar

3–5 primary destinations, compact screens, persistent. Compose: `NavigationBar` +
`NavigationBarItem`. Height 80dp, elevation level 2. Always show labels; filled icons for
active state. Iron: bottom nav with four destinations (D38, compact).

```kotlin
NavigationBar {
    destinations.forEach { dest ->
        NavigationBarItem(
            selected = currentRoute == dest.route,
            onClick = { navController.navigate(dest.route) },
            icon = { Icon(dest.icon, contentDescription = dest.label) },
            label = { Text(dest.label) },
        )
    }
}
```

### Navigation drawer

Many destinations or deep hierarchies. Compose: `ModalNavigationDrawer`,
`PermanentNavigationDrawer`, `DismissibleNavigationDrawer`, `NavigationDrawerItem`.
Standard drawer: `surface-container` background; modal: elevation level 1 + scrim. Active
item: `secondary-container`. Shape: `large` on end corners. Out of v1 scope (D38); kept as
reference.

### Navigation rail

3–7 destinations, medium screens. Compose: `NavigationRail` + `NavigationRailItem`. Width
80dp, optional FAB at top, elevation level 0. Out of v1 scope (D38); kept as reference.

### Search

Search bar (persistent field in app bar area) and search view (expandable overlay with
suggestions). Compose: `SearchBar`, `SearchBarRow`, `SearchRow`, expressive search per BOM.
I/O 2026: refreshed visual style, motion, flexible trailing icon behavior. Shape: `full`;
elevation level 3.

Iron (D74): the search box is case-insensitive in the filter bar of Exercises; the category
filter entry is an icon inside the search bar.

### Tabs

Primary tabs (top-level content switching) and secondary tabs (sub-sections). Compose:
`PrimaryTabRow`, `SecondaryTabRow`, `Tab`. Tab label: `titleSmall`. Connect tabs to panels
with semantics.

```kotlin
PrimaryTabRow(selectedTabIndex = active) {
    tabs.forEachIndexed { index, label ->
        Tab(selected = index == active, onClick = { select(index) }, text = { Text(label) })
    }
}
```

### Toolbar

Frequently used actions relevant to current page context, typically below the top app bar.
No dedicated Compose component; implement with a `Row` of icon buttons using `surface`
background and elevation level 2.

## Data display

### List

One-line, two-line, three-line items. Compose: `ListItem` for single items; for long lists,
`LazyColumn` with `key` on every item (project rule). Headline: `bodyLarge`; supporting
text: `bodyMedium`; trailing metadata: `bodySmall`.

```kotlin
LazyColumn {
    items(sets, key = { it.id }) { set ->
        ListItem(
            headlineContent = { Text(set.exerciseName) },
            supportingContent = { Text("${set.reps} x ${set.loadKg} kg") },
            trailingContent = { Text(set.timestamp) },
            leadingContent = { Icon(Icons.Default.Fitness, contentDescription = null) },
        )
    }
}
```

Clickable rows: controls inside a clickable row receive an accessible name via
`Modifier.semantics(mergeDescendants = true)` (D71).

### Accessibility checks (all components, D35)

- Touch targets 48dp minimum on every interactive element
- `contentDescription` on all icons; icon-only buttons always have an accessible name
- Respect the system font scale
- Contrast verified with dynamic color active: 4.5:1 normal text, 3:1 large text/borders
- Charts: textual alternative with bucket label and selected metric value, as an
  always-available collapsible section (Vico is not accessible by default)
- Verified on emulator (D19 keeps UI out of tests)
