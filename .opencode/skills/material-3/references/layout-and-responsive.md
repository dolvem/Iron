<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# MD3 Layout and Responsive Design

Reference for Material Design 3's layout system: breakpoints, canonical layouts, spacing, and
insets.

**Iron scope (D38)**: compact window class only (portrait phone). Breakpoints and multi-pane
layouts are kept as reference for future work; the operative guidance below is the compact
column.

## Jetpack Compose

- Use **`calculateWindowSizeClass`**
  (`androidx.compose.material3:material3-window-size-class`) and
  **`androidx.compose.material3.adaptive`** (`currentWindowAdaptiveInfo`, list-detail
  scaffolds) instead of hand-rolling raw `BoxWithConstraints` width checks. In v1 the window
  is always compact (D38).
- **Edge-to-edge**: `enableEdgeToEdge()` on the Activity when drawing behind the system bars.
  Apply `WindowInsets` (`Modifier.statusBarsPadding()`, `navigationBarsPadding()`,
  `imePadding()`, `displayCutoutPadding()`) and `Scaffold` `contentWindowInsets` so content
  and IME behave correctly.
- **Foldables**: `WindowInfoTracker` / `FoldingFeature` (Jetpack WindowManager) — out of v1
  scope (D38), kept as reference.

## Google I/O 2026: expressive layout

- **Expressive layout scaffold**: design screens to adapt across form factors; start from
  adaptive scaffolds/window size classes instead of fixed phone-first layouts.
- **8dp spacing system**: treat spacing as tokens. Use an 8dp scale for margins, padding,
  gaps, and component spacing so density and device-class changes apply consistently.

### Spacing token pattern

Define spacing once, then map it to context:

```kotlin
object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}
```

Use spacing tokens for: screen margins, pane gaps, list item spacing, card internal padding,
component groups and toolbars. Do not scatter raw `Dp` literals throughout reusable UI; keep
one-off values local only when truly component-specific.

## Window size classes

| Class | Width range | Typical devices | Columns | Iron (v1) |
|-------|-----------|----------------|---------|-----------|
| Compact | < 600dp | Phone portrait | 4 | ✅ always |
| Medium | 600–839dp | Tablet portrait, foldable | 8 | out of scope (D38) |
| Expanded | 840–1199dp | Tablet landscape, small desktop | 12 | out of scope (D38) |
| Large | 1200–1599dp | Desktop | 12 | out of scope (D38) |
| Extra-large | 1600dp+ | Ultra-wide | 12 | out of scope (D38) |

## Layout anatomy

- **Window**: the visible area of the app
- **Pane**: a layout container within the window (fixed, flexible, floating, semi-permanent)
- **Column**: a vertical content block within a pane
- **Margin**: space between screen edge and content
- **Gutter**: space between columns
- **Spacer**: space between panes (multi-pane layouts)

### Margin and gutter values

| Window size | Margins | Gutters | Iron (v1) |
|-------------|---------|---------|-----------|
| Compact | 16dp | 8dp | ✅ |
| Medium | 24dp | 16dp | reference |
| Expanded | 24dp | 16dp | reference |
| Large | 24dp | 24dp | reference |
| Extra-large | 24dp | 24dp | reference |

## Canonical layouts

MD3 defines 3 canonical layouts as starting points. Always begin from one of these rather
than from a raw grid.

### Feed layout

**Use when**: a large collection of browsable items (list of sets, history, exercise list).

```text
Compact (Iron):  single column of cards
Medium:          2-column grid          ← reference
Expanded:        3-column grid          ← reference
Large:           4-column grid + side panel ← reference
```

### List-detail layout

**Use when**: browsing a list where each item has detailed content (exercise list + exercise
detail with charts).

```text
Compact (Iron):  list view OR detail view, navigate between them (route-based)
Medium:          side-by-side list (1/3) + detail (2/3)   ← reference
Expanded:        side-by-side with wider detail pane      ← reference
```

In Compose, when multi-pane becomes relevant, use `NavigableListDetailPaneScaffold` from
`androidx.compose.material3.adaptive` rather than manual two-column branches.

### Supporting pane layout

**Use when**: primary content needs supplementary information.

```text
Compact (Iron):  stacked — primary on top, supporting below (or bottom sheet)
Medium:          side-by-side (2/3 primary + 1/3 supporting) ← reference
```

## Adaptive component behavior

| Component | Compact (Iron) | Medium+ (reference) |
|-----------|----------------|---------------------|
| Navigation | Bottom bar | Rail → drawer |
| App bar | Small (64dp) | Small or medium (112dp) |
| Dialog | Full-screen | Centered dialog (max 560dp wide) |
| Bottom sheet | Full height | Partial height → side sheet |
| Search | Full-screen search view | Persistent search bar |
| Cards | Full-width single column | Multi-column grid (max 4 cols) |
| Content panes | Single pane | Two or three panes |
| Input method | Touch only | Touch + stylus → touch + mouse + keyboard |

## Large screens (reference, out of v1)

- Do not stretch content to fill ultra-wide screens; lines longer than ~80 characters are
  hard to scan. Constrain body content to a max width (840–1040dp) and center it.
- Use the extra space for multi-pane layouts, not wider single columns.
- Hover states for pointer devices; touch targets remain 48dp minimum.
- Place no interactive content across the fold/hinge on foldables.

## Spacing system

MD3 uses a 4dp base grid for spacing:

| Use | Values |
|-----|--------|
| Component internal padding | 4, 8, 12, 16, 24dp |
| Between components | 8, 12, 16, 24dp |
| Section spacing | 24, 32, 48dp |
| Layout margins (compact) | 16dp |
| Grid gutters (compact) | 8dp |

Always use multiples of 4dp for consistent spatial rhythm; prefer the 8dp token scale for
margins, padding and gaps.

## Audit checklist for layout

- [ ] Window size class handled via `calculateWindowSizeClass` / adaptive APIs (when
  multi-pane is in scope)
- [ ] Compact margins 16dp, gutters 8dp
- [ ] Spacing comes from tokens, not scattered raw `Dp` literals
- [ ] Edge-to-edge with correct `WindowInsets` handling (status bars, nav bars, IME)
- [ ] Touch targets remain 48dp minimum
- [ ] Dialogs full-screen on compact (D73 padding rules)
- [ ] Content max-width constraint when large screens come into scope
