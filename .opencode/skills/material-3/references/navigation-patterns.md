<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# MD3 Navigation Patterns

Guide for choosing and implementing Material Design 3 navigation in Jetpack Compose.

**Iron scope (D38)**: compact window class only, portrait phone. The bottom navigation bar is
the active pattern; rail and drawer are kept as reference for future work. Iron's graph:
bottom nav with four data destinations, Settings reached from the Today header.

## Compose APIs

Use **`androidx.compose.material3`**: `NavigationBar`, `NavigationRail`,
`NavigationDrawerItem`, `ModalNavigationDrawer`, `DismissibleNavigationDrawer`,
`PermanentNavigationDrawer`, `NavigationBarItem`, `NavigationRailItem`, top app bars
(`TopAppBar`, `CenterAlignedTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar`, expressive
variants per BOM), and **`Scaffold`** (`bottomBar`, `floatingActionButton`, `snackbarHost`).

Wire destinations with **Navigation Compose** (`NavHost`, `composable`,
`rememberNavController`). For adaptive UIs, use `calculateWindowSizeClass` and
`androidx.compose.material3.adaptive` (`currentWindowAdaptiveInfo`,
`NavigableListDetailPaneScaffold`) — out of v1 scope (D38), kept as reference.

I/O 2026 emphasis:

- Prefer expressive/adaptive scaffolds rather than scaling one phone navigation model upward.
- Expressive search and search app bars have refreshed visual style, motion, and more
  flexible trailing icon behavior; use current Compose Material3 APIs where available.
- Keep navigation spacing on the 8dp spacing system so gaps adapt by device class and density.

```kotlin
// Conceptual — adapt routes and selection to the app
Scaffold(
    bottomBar = {
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
    },
) { innerPadding ->
    NavHost(
        navController = navController,
        startDestination = "hoy",
        modifier = Modifier.padding(innerPadding),
    ) { /* composable routes */ }
}
```

## Decision tree

```text
How many primary destinations?
├── 2 destinations → Tabs (primary)
├── 3–5 destinations
│   ├── Compact screen (<600dp) → Navigation Bar (bottom)     ← Iron, D38
│   ├── Medium screen (600–839dp) → Navigation Rail (side)    ← reference, out of v1
│   └── Expanded+ screen (840dp+) → Navigation Drawer or Rail ← reference, out of v1
├── 6+ destinations
│   ├── Compact → Navigation Drawer (modal)                   ← reference, out of v1
│   └── Medium+ → Drawer (standard) or Rail + overflow menu   ← reference, out of v1
└── Hierarchical (nested sections)
    └── Navigation Drawer with sections                       ← reference, out of v1
```

Quick reference:

| Component | Destinations | Screen size | Persistence | Position |
|-----------|-------------|-------------|-------------|----------|
| Navigation bar | 3–5 | Compact | Persistent | Bottom |
| Navigation rail | 3–7 | Medium | Persistent | Side (start) |
| Navigation drawer | Unlimited | Expanded+ | Standard or modal | Side (start) |
| Tabs | 2+ related views | Any | Persistent | Top (below app bar) |
| Bottom app bar | contextual actions | Compact | Persistent | Bottom |

## Navigation bar

**Use when**: 3–5 primary destinations on compact (mobile) screens. Position: bottom, always
visible.

Anatomy:

- Fixed at bottom, full width
- 3–5 items with icon + label
- Active item shows filled icon + indicator pill
- Height: 80dp, elevation level 2

Guidelines:

- Always show labels (never icon-only)
- Filled icons for active state, outlined for inactive
- Do not use for fewer than 3 or more than 5 destinations
- Hide on scroll down in content-heavy screens (optional)

## Navigation rail

**Use when**: 3–7 destinations on medium screens (600–839dp). Width 80dp, optional FAB at
top, items vertically stacked, active item shows indicator pill, elevation level 0.
Reference for future work (D38).

## Navigation drawer

**Use when**: many destinations, expanded screens, or deep hierarchies. Standard drawer:
always visible, width 360dp, `surface-container` background. Modal drawer: overlay with
scrim, elevation level 1. Group destinations with dividers and section headers; active item
uses `secondary-container`; shape `large` on end corners. Reference for future work (D38).

## Top app bar

**Use when**: every screen needs a title and optional actions.

| Variant | Title | Height | Scroll behavior |
|---------|-------|--------|-----------------|
| Center-aligned | Center | 64dp | Elevates to level 2 on scroll |
| Small | Start-aligned | 64dp | Elevates to level 2 on scroll |
| Medium | Bottom, start-aligned | 112dp | Collapses to 64dp on scroll |
| Large | Bottom, start-aligned | 152dp | Collapses to 64dp on scroll |

Title style: `titleLarge`. Scrolled state background: `surface-container`. Search app bars
are part of the current expressive search guidance; check the Material3 BOM for expressive
app bar and search APIs before hand-rolling.

## Tabs

**Use when**: switching between related content at the same hierarchy level. Primary tabs
for top-level switching, secondary tabs for sub-sections. Tab label: `titleSmall`. Connect
tabs to panels with semantics so screen readers announce the relationship.

## Responsive navigation (reference, out of v1)

The key MD3 pattern: navigation transforms across breakpoints.

```text
Compact (<600dp):   Navigation bar (bottom)   ← Iron (D38)
Medium (600–839dp): Navigation rail (side)
Expanded (840dp+):  Navigation drawer (side, standard)
```

In Compose, drive this with `calculateWindowSizeClass` / `currentWindowAdaptiveInfo` rather
than raw width checks. Predictive back: accepted default behavior with target API 36.
