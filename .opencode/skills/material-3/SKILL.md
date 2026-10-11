---
name: Material Design 3
description: >
  Implement Material Design 3 (MD3) in Jetpack Compose: design tokens, MaterialTheme,
  Material3 components, theming, dynamic color, accessibility, and MD3 compliance audits.
  Use when: "material design", "MD3", "material you", "Jetpack Compose", "MaterialTheme",
  "material component", "md3 button", "compose material3".
---

<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# Material Design 3

Guidance for implementing Google's Material Design 3 (MD3) in Iron: a personal, adaptive,
expressive UI system built on dynamic color, tonal surfaces, rounded shapes, and
spring-based motion. Primary and only target: **Jetpack Compose** (`androidx.compose.material3`).

## Scope

- **Jetpack Compose only.** Flutter and web (`@material/web`) are not used in this project.
- **Compact window class only** (D38): Iron is locked to portrait phone. Tablet, foldable,
  watch and XR layouts are out of v1; breakpoints are kept as reference for future work.
- `docs/spec/` is the source of truth for product decisions. This skill cites decision IDs
  (e.g. D73, D35) instead of restating rules.

## Philosophy

MD3 is built on three principles:

- **Personal**: dynamic color adapts the UI to the user's wallpaper. Theming is individual,
  not one-size-fits-all.
- **Adaptive**: layouts transform across window size classes. Components resize, reposition,
  and change form factor responsively.
- **Expressive**: shape morphing, spring physics, and emphasized typography create moments of
  delight without sacrificing usability.

## Key differences from MD2

- Tonal surfaces replace elevation shadows as the primary depth cue
- Dynamic color generates full schemes from a single seed color
- Fully rounded corners by default (not slightly rounded)
- Spring-based motion physics replace fixed easing curves for components
- 3 levels of user-controlled contrast (standard/medium/high)

## Decision tree

**What are you building?**

```text
Full app scaffold        → "Common patterns" below + references/layout-and-responsive.md
Single component         → Component quick reference → references/component-catalog.md
Custom theme             → references/theming-and-dynamic-color.md
Form / input layout      → references/component-catalog.md § Input
Navigation structure     → references/navigation-patterns.md
Data display             → references/component-catalog.md § Data display
Accessibility check      → references/component-catalog.md § Accessibility + D35
MD3 compliance audit     → "Audit mode" below
```

## Design token system

All MD3 tokens use the `md.sys` namespace. In Compose, roles map to
`MaterialTheme.colorScheme`, `MaterialTheme.typography` and `MaterialTheme.shapes`
(same semantic roles as the spec).

### Color tokens

| Token | Purpose |
|-------|---------|
| `primary` | High-emphasis fills, text, icons against surface |
| `on-primary` | Text/icons on primary |
| `primary-container` | Standout fill for key components (FAB, etc.) |
| `on-primary-container` | Text/icons on primary-container |
| `secondary` / `on-secondary` | Less prominent accents |
| `secondary-container` / `on-secondary-container` | Recessive components (tonal buttons) |
| `tertiary` / `on-tertiary` | Contrasting accents |
| `tertiary-container` / `on-tertiary-container` | Complementary containers |
| `error` / `on-error` | Error states (static — does not change with dynamic color) |
| `error-container` / `on-error-container` | Error container fills |
| `surface` | Default background |
| `on-surface` | Text/icons on any surface |
| `on-surface-variant` | Lower-emphasis text/icons on surface |
| `surface-container-lowest` … `highest` | Container hierarchy for nesting depth |
| `surface-dim` / `surface-bright` | Maintain relative brightness across light/dark |
| `inverse-surface` / `inverse-on-surface` / `inverse-primary` | Contrasting elements (snackbars) |
| `outline` | Important boundaries (text field borders) |
| `outline-variant` | Decorative elements (dividers) |

Full details: `references/color-system.md`

### Typography tokens

| Scale | Sizes | Use |
|-------|-------|-----|
| Display | L / M / S | Hero text, large numbers |
| Headline | L / M / S | Section headers |
| Title | L / M / S | Smaller headers, card titles |
| Body | L / M / S | Paragraph text, descriptions |
| Label | L / M / S | Buttons, chips, captions |

Each style has font, weight, size, line-height and tracking tokens, plus 15 **emphasized**
variants (higher weight) for selected/active states. Full details:
`references/typography-and-shape.md`.

### Shape tokens

| Token | Value | Example components |
|-------|-------|-------------------|
| `none` | 0dp | — |
| `extra-small` | 4dp | Snackbars |
| `small` | 8dp | Text fields, menus, chips |
| `medium` | 12dp | Cards |
| `large` | 16dp | FABs, navigation drawer |
| `large-increased` | 20dp | (Expressive) |
| `extra-large` | 28dp | Dialogs, bottom sheets |
| `extra-large-increased` | 32dp | (Expressive) |
| `extra-extra-large` | 48dp | (Expressive) |
| `full` | pill | Buttons, chips, badges, sliders |

### Elevation levels

| Level | DP | Tonal offset | Use |
|-------|----|--------------|-----|
| 0 | 0dp | None | Flat surfaces, most components at rest |
| 1 | 1dp | +5% primary | Elevated cards, modal sheets |
| 2 | 3dp | +8% primary | Menus, nav bar, scrolled app bar |
| 3 | 6dp | +11% primary | FAB, dialogs, search, date/time pickers |
| 4 | 8dp | +12% primary | Hover/focus increase only |
| 5 | 12dp | +14% primary | Hover/focus increase only |

Elevation in MD3 is communicated through **tonal surface color**, not shadows.

### Motion

MD3 Expressive (May 2025) introduced **spring-based motion physics** for components
(`MotionScheme` in Compose, per BOM). The legacy easing/duration system is still used for
**transitions** (enter/exit/shared-axis):

| Easing | Duration | Transition type |
|--------|----------|-----------------|
| Emphasized | 500ms | Begin and end on screen |
| Emphasized decelerate | 400ms | Enter the screen |
| Emphasized accelerate | 200ms | Exit the screen |
| Standard | 300ms | Begin and end on screen (utility) |
| Standard decelerate | 250ms | Enter screen (utility) |
| Standard accelerate | 200ms | Exit screen (utility) |

Full details: `references/typography-and-shape.md` § Motion.

## Component quick reference

Compose mappings and examples live in `references/component-catalog.md`.

| Component | Compose API | Key variants | Category |
|-----------|-------------|--------------|----------|
| Button | `Button`, `FilledTonalButton`, `ElevatedButton`, `OutlinedButton`, `TextButton` | 5 emphasis levels; 5 sizes XS–XL (Expressive); toggle | Actions |
| Button group | `ButtonGroup` (Expressive, per BOM) | Standard, connected | Actions |
| FAB | `FloatingActionButton` | Small, Medium, Large | Actions |
| Extended FAB | `ExtendedFloatingActionButton` | Surface, Primary, Secondary, Tertiary | Actions |
| FAB menu | `FabMenu` (Expressive, per BOM) | — | Actions |
| Icon button | `IconButton`, `FilledIconButton`, `FilledTonalIconButton`, `OutlinedIconButton` | 4 variants; toggle | Actions |
| Segmented button | `SegmentedButton` | Single-select, multi-select | Actions |
| Split button | per BOM (Expressive) | — | Actions |
| Badge | `Badge`, `BadgedBox` | Small (dot), large (count) | Communication |
| Progress indicator | `LinearProgressIndicator`, `CircularProgressIndicator` | Determinate/indeterminate | Communication |
| Snackbar | `Snackbar` + `SnackbarHost` | Single-line, two-line, action | Communication |
| Tooltip | `PlainTooltip`, `RichTooltip`, `TooltipBox` | Plain, rich | Communication |
| Card | `Card`, `OutlinedCard`, `ElevatedCard` | Filled, outlined, elevated | Containment |
| Carousel | `HorizontalCarousel` | Multi-browse, uncontained, hero | Containment |
| Dialog | `AlertDialog`, full-screen dialog | Basic, full-screen | Containment |
| Bottom sheet | `ModalBottomSheet` | Standard, modal | Sheets |
| Side sheet | no dedicated component | Standard, modal | Sheets |
| Divider | `HorizontalDivider`, `VerticalDivider` | Full-width, inset | Containment |
| Checkbox | `Checkbox` | Indeterminate state | Input |
| Chips | `AssistChip`, `FilterChip`, `InputChip`, `SuggestionChip` | 4 variants | Input |
| Date picker | `DatePicker`, `DatePickerDialog` | Docked, modal, range | Input |
| Menu | `DropdownMenu`, `DropdownMenuItem` | Expressive per BOM | Input |
| Radio button | `RadioButton` | — | Input |
| Slider | `Slider`, `RangeSlider` | Continuous, discrete, range | Input |
| Switch | `Switch` | With/without icon | Input |
| Text field | `TextField`, `OutlinedTextField` | Filled, outlined | Input |
| Time picker | `TimeInput`, `TimePicker`, `TimePickerDialog` | Docked, modal | Input |
| App bar (top) | `TopAppBar`, `CenterAlignedTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar` | 4 variants | Navigation |
| Navigation bar | `NavigationBar`, `NavigationBarItem` | — | Navigation |
| Navigation drawer | `ModalNavigationDrawer`, `PermanentNavigationDrawer`, `DismissibleNavigationDrawer` | Standard, modal | Navigation |
| Navigation rail | `NavigationRail`, `NavigationRailItem` | — | Navigation |
| Search | `SearchBar`, `SearchRow`, expressive search per BOM | Search bar, search view | Navigation |
| Tabs | `PrimaryTabRow`, `SecondaryTabRow`, `Tab` | Primary, secondary | Navigation |
| Toolbar | no dedicated component | — | Navigation |
| List | `ListItem` + `LazyColumn` | One-line, two-line, three-line | Data display |

## Jetpack Compose

Use **`androidx.compose.material3`** with `MaterialTheme` and Material 3 composables
(`Scaffold`, `Button`, `NavigationBar`, top app bars, etc.).

- **Theming**: `MaterialTheme(colorScheme, typography, shapes)`. Dynamic color is available on
  every device Iron runs on (minSdk 31), so `dynamicLightColorScheme` / `dynamicDarkColorScheme`
  are used directly — no `Build.VERSION.SDK_INT` check (project rule). See
  `ui/theme/Theme.kt` and `references/theming-and-dynamic-color.md`.
- **Resources and locale**: `LocalResources.current` and `LocalLocale.current.platformLocale`,
  never `LocalContext` for strings nor `Locale.getDefault()` (D72). `LocalContext` is only used
  where a `Context` is genuinely required (dynamic color schemes, DI container).
- **Edge-to-edge & insets**: `enableEdgeToEdge()` on the Activity; lay out content with
  `WindowInsets` (`Modifier.statusBarsPadding()`, `navigationBarsPadding()`, `imePadding()`,
  `displayCutoutPadding()`) and `Scaffold` `contentWindowInsets` so bars and IME behave correctly.
- **Experimental APIs**: some Material 3 APIs require `@OptIn(ExperimentalMaterial3Api::class)`
  or `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`; match your BOM and compiler.
- **Lists**: `LazyColumn` with `key` on every item (project rule).

```kotlin
MaterialTheme(
    colorScheme = colorScheme, // dynamicLightColorScheme / dynamicDarkColorScheme (minSdk 31)
    typography = Typography(),
    shapes = Shapes(),
) {
    // M3 content — prefer references for Scaffold, navigation, text fields
}
```

## Accessibility (D35, D71)

- Touch targets: **48dp minimum** on every interactive element.
- `contentDescription` on all icons; icon-only buttons (FAB, icon buttons) always get an
  accessible name.
- Respect the system font scale; layouts must not clip at large scales.
- Contrast is verified with **dynamic color** active: 4.5:1 for normal text, 3:1 for large
  text and borders (WCAG 2.x). Tonal pairing rules in `references/color-system.md` keep this
  guarantee.
- Charts (Vico) are not accessible by default: provide a textual alternative with the bucket
  label and the selected metric value, as an always-available collapsible section (D35).
- Controls inside a clickable row receive an accessible name via
  `Modifier.semantics(mergeDescendants = true)` (D71).
- Verified on emulator, not in tests (D19 keeps UI out of tests).

## Anti-patterns

**Never do these when implementing MD3:**

- **Hardcode colors**: always use `MaterialTheme.colorScheme` roles, never raw
  `Color(0x…)` values. Hardcoded colors break dynamic theming, dark mode, and contrast.
- **Ignore tonal pairing**: only combine colors in their intended pairs (`primary` +
  `on-primary`, `surface-container` + `on-surface`, etc.). Arbitrary pairings break contrast
  in dynamic color and high contrast modes.
- **Use `outline` for dividers**: use `outline-variant` for dividers; `outline` is for
  important boundaries like text field borders.
- **Use `border-radius`/`RoundedCornerShape(12.dp)` directly**: use `MaterialTheme.shapes`
  tokens so shapes stay consistent with theming.
- **Use shadows for elevation by default**: MD3 communicates elevation through tonal surface
  color, not shadows. Shadows only when extra separation from busy backgrounds is needed.
- **Scatter raw `Dp` literals**: use the 8dp spacing system for margins, padding and gaps;
  keep one-off values local only when truly component-specific.
- **Stretch content to fill wide screens**: constrain content to a readable max width on
  large windows (840–1040dp) — reference for future, not applicable in v1 (D38).
- **Validate in the UI**: the UI does not validate; errors always come from the repository
  (D5, D6).
- **Use `java.util.Date`**: dates only with `java.time` (project rule); `Clock` and `ZoneId`
  are injected from `AppContainer`, never used inline in tested code (D17).

## Audit mode

When invoked with `audit` as the argument, or when asked to audit/review MD3 compliance,
analyze the target source tree and produce a compliance report.

### Procedure

1. **Identify the target**: file paths (read source) or a running app.
2. **Inspect these categories** and score each 0–10:

| Category | What to check |
|----------|--------------|
| **Color tokens** | `MaterialTheme.colorScheme` roles; no arbitrary `Color(0x…)` for surfaces without reason; proper tonal pairing (`onX` on `X`); dark theme; dynamic color |
| **Typography** | `MaterialTheme.typography` scale; correct roles (Display, Headline, Title, Body, Label) |
| **Shape** | `MaterialTheme.shapes` / component `Shape`; buttons full, cards medium; no magic numbers |
| **Elevation** | Tonal elevation (`Surface` tonal levels); shadows only where justified |
| **Components** | Material3 composables (`Button`, `Scaffold`, `NavigationBar`, etc.); correct variants |
| **Layout** | 8dp spacing system; readable max width; margins/gutters per size class (compact in v1) |
| **Navigation** | Bottom bar for compact (Iron, D38); predictive back accepted by default (target API 36) |
| **Motion** | `MotionScheme` / expressive APIs when used; transitions may still use easing/duration |
| **Accessibility** | Contrast with dynamic color (4.5:1 text, 3:1 UI); 48dp targets; `contentDescription`; font scale; textual alternative for charts (D35); `mergeDescendants` (D71) |
| **Theming** | `MaterialTheme` + light/dark/dynamic as designed |

3. **Generate the report**:

```text
# MD3 Compliance Audit Report

Target: [file path]
Date: [date]
Overall Score: [X/100]

## Scores by Category
| Category       | Score | Status |
|----------------|-------|--------|
| Color tokens   | X/10  | [pass/warn/fail] |
| Typography     | X/10  | [pass/warn/fail] |
| Shape          | X/10  | [pass/warn/fail] |
| Elevation      | X/10  | [pass/warn/fail] |
| Components     | X/10  | [pass/warn/fail] |
| Layout         | X/10  | [pass/warn/fail] |
| Navigation     | X/10  | [pass/warn/fail] |
| Motion         | X/10  | [pass/warn/fail] |
| Accessibility  | X/10  | [pass/warn/fail] |
| Theming        | X/10  | [pass/warn/fail] |

## Critical Issues
[Items scoring 0–3 with file:line references and fixes]

## Warnings
[Items scoring 4–6 with recommendations]

## Passing
[Items scoring 7–10 with notes on what is done well]

## Recommended Fixes (Priority Order)
1. [Most impactful fix first]
```

### Quick checks

```bash
# Raw Color(...) outside ui/theme/
grep -rn 'Color(0x' --include='*.kt' app/src/main | grep -v 'ui/theme/'

# Raw Dp literals outside spacing tokens
grep -rn '[0-9]\+\.dp' --include='*.kt' app/src/main

# Icons without contentDescription
grep -rn 'Icon(' --include='*.kt' app/src/main | grep -v 'contentDescription'

# Missing key in LazyColumn items
grep -rn 'items(' --include='*.kt' app/src/main | grep -v 'key ='
```

### Scoring guide

- **9–10**: fully MD3 compliant, correct tokens and patterns
- **7–8**: mostly compliant, minor issues (a few hardcoded values)
- **4–6**: partially compliant, some MD3 patterns but significant gaps
- **1–3**: major violations, mostly non-MD3 or MD2 patterns
- **0**: not applicable or completely absent

Status thresholds: **pass** (7+), **warn** (4–6), **fail** (0–3).

## Reference documents

- `references/color-system.md` — color roles, tonal palettes, dynamic color, pairing rules
- `references/typography-and-shape.md` — type scale, shape corners, elevation, motion
- `references/component-catalog.md` — all components with Compose mappings and examples
- `references/navigation-patterns.md` — navigation selection and Compose patterns
- `references/layout-and-responsive.md` — breakpoints, canonical layouts, insets, spacing
- `references/theming-and-dynamic-color.md` — theming, brand colors, dark mode, contrast levels
