<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# MD3 Typography, Shape, Elevation, and Motion

Reference for Material Design 3's visual token systems beyond color. In Compose, these map to
`MaterialTheme.typography` and `MaterialTheme.shapes`.

## Typography

### Type scale

MD3 uses 15 baseline styles + 15 emphasized styles organized in 5 categories (Display,
Headline, Title, Body, Label) with 3 sizes each (Large, Medium, Small).

#### Baseline type scale (default values)

| Style | Font | Weight | Size (sp) | Line height | Tracking |
|-------|------|--------|-----------|-------------|----------|
| Display Large | Roboto | 400 | 57 | 64sp | -0.25sp |
| Display Medium | Roboto | 400 | 45 | 52sp | 0 |
| Display Small | Roboto | 400 | 36 | 44sp | 0 |
| Headline Large | Roboto | 400 | 32 | 40sp | 0 |
| Headline Medium | Roboto | 400 | 28 | 36sp | 0 |
| Headline Small | Roboto | 400 | 24 | 32sp | 0 |
| Title Large | Roboto | 400 | 22 | 28sp | 0 |
| Title Medium | Roboto | 500 | 16 | 24sp | 0.15sp |
| Title Small | Roboto | 500 | 14 | 20sp | 0.1sp |
| Body Large | Roboto | 400 | 16 | 24sp | 0.5sp |
| Body Medium | Roboto | 400 | 14 | 20sp | 0.25sp |
| Body Small | Roboto | 400 | 12 | 16sp | 0.4sp |
| Label Large | Roboto | 500 | 14 | 20sp | 0.1sp |
| Label Medium | Roboto | 500 | 12 | 16sp | 0.5sp |
| Label Small | Roboto | 500 | 11 | 16sp | 0.5sp |

In Compose, use `MaterialTheme.typography.displayLarge`, `.titleMedium`, etc. The default
`Typography()` matches this scale.

#### Emphasized type styles (Expressive update)

The 15 emphasized styles mirror the baseline scale but with **higher weight** and minor
adjustments. Use for:

- Selected/active states in components
- Primary action buttons
- Headlines needing emphasis
- Unread/important content

To use: swap the baseline style for the emphasized version (e.g.
`md.sys.typescale.display-large` → `md.sys.typescale.emphasized.display-large`; in Compose,
per BOM, expressive typography is exposed through the expressive type schemes).

### Typeface customization

MD3 uses two typeface roles:

- **Brand**: used for Display and Headline styles (expression-focused)
- **Plain**: used for Title, Body and Label styles (readability-focused)

Both default to Roboto. Roboto is the correct default typeface on Android; replace only when
intentionally customizing the type scale. Roboto Flex is a variable font supporting weight
(100–1000), width (25–151) and optical size (8–144) axes.

### Font size units

Android uses **sp**, 1:1 with the scale values. Respect the system font scale: never hardcode
font sizes outside the type scale, and verify layouts do not clip at large font scales (D35).

### Component type usage

| Component | Type style |
|-----------|-----------|
| Button label | Label Large |
| Card title | Title Medium |
| Card body | Body Medium |
| Top app bar title | Title Large |
| Navigation label | Label Medium |
| Dialog headline | Headline Small |
| Dialog body | Body Medium |
| Chip label | Label Large |
| Text field input | Body Large |
| Text field label | Body Small (floating) / Body Large (resting) |
| List headline | Body Large |
| List supporting text | Body Medium |
| Snackbar text | Body Medium |
| Tooltip text | Body Small |
| Tab label | Title Small |
| Badge count | Label Small |

## Shape

### Corner radius scale

| Token | Value (dp) | Default components |
|-------|-----------|--------------------|
| `none` | 0 | — |
| `extra-small` | 4 | Snackbars |
| `small` | 8 | Text fields, menus, chips |
| `medium` | 12 | Cards |
| `large` | 16 | FAB, extended FAB, nav drawer |
| `large-increased` | 20 | (Expressive) |
| `extra-large` | 28 | Dialogs, bottom sheets, side sheets |
| `extra-large-increased` | 32 | (Expressive) |
| `extra-extra-large` | 48 | (Expressive) |
| `full` | pill | Buttons, badges, pills, sliders |

In Compose, use `MaterialTheme.shapes.small` / `.medium` / `.large` or the expressive shape
scheme per BOM. Do not hardcode `RoundedCornerShape(12.dp)` for token-mapped components.

### Component shape mapping

| Component | Default shape token |
|-----------|-------------------|
| Buttons (all types) | `full` |
| FAB | `large` |
| Extended FAB | `large` |
| Icon button | `full` |
| Chips | `small` |
| Cards | `medium` |
| Dialogs | `extra-large` |
| Text fields | `small` (top corners) |
| Menus | `small` |
| Navigation drawer | `large` (end corners) |
| Bottom sheets | `extra-large` (top corners) |
| Snackbars | `extra-small` |
| Badges | `full` |
| Sliders (handle) | `full` |
| Switch (track) | `full` |
| Tabs (indicator) | `full` (top corners) |
| Search bar | `full` |

### Shape morphing (Expressive)

In the M3 Expressive update, components can morph between shapes on interaction: button
shapes morph when pressed, selected states can change shape, loading indicators use shape
morphing to show progress. Jetpack Compose is where Google documents expressive
shape/motion behavior for Android (per BOM, `ExperimentalMaterial3ExpressiveApi`).

## Elevation

### Elevation levels

| Level | DP height | Use |
|-------|-----------|-----|
| 0 | 0dp | Most resting components |
| 1 | 1dp | Elevated variants (cards, sheets) |
| 2 | 3dp | Menus, nav bar, scrolled app bar |
| 3 | 6dp | FAB, dialogs, search, date/time pickers |
| 4 | 8dp | Hover/focus increase only |
| 5 | 12dp | Hover/focus increase only |

### Tonal elevation (not shadows)

MD3 uses **tonal surface color** to communicate elevation, not shadows. Higher elevation =
lighter surface tone. The surface container roles map to this concept:

- Level 0: `surface` (flattest)
- Level 1: `surface-container-low`
- Level 2: `surface-container`
- Level 3: `surface-container-high`
- Level 4–5: `surface-container-highest`

### When to use shadows

Shadows are only appropriate when:

- A component floats over content that may be visually busy (e.g. FAB over images)
- Additional depth cue is needed beyond color (e.g. overlapping elements)
- The platform convention expects shadows (some Android components)

### Component elevation mapping

| Level | Components at rest |
|-------|--------------------|
| 0 | App bar (flat), filled/tonal/outlined buttons, button groups, filled/outlined cards, carousel, chips, full-screen dialogs, icon buttons, lists, nav rail, segmented buttons, sliders, split buttons, tabs |
| 1 | Banners, modal bottom sheet, elevated button, elevated card, elevated chips, modal nav drawer, modal side sheet |
| 2 | App bar (scrolled), menus, nav bar, rich tooltips, toolbar |
| 3 | Date pickers, modal dialogs, extended FAB, FAB, FAB menu close button, search, time pickers |

**Hover/focus**: most interactive components increase by 1 level on hover/focus (e.g. FAB goes
from level 3 to level 4).

## Motion

### Spring-based motion (Expressive update)

MD3 Expressive (May 2025) introduced spring-based motion physics for component animations.
Springs create more natural, responsive motion:

- Springs have no fixed duration — they respond dynamically to input
- Two schemes: **standard** (utilitarian) and **expressive** (bouncy)
- Jetpack Compose exposes motion schemes / spring-oriented APIs in current Material3
  (`MotionScheme`, per BOM)

### Easing and duration (transitions)

The easing/duration system is used for **transitions** (entering, exiting, shared-axis):

#### Easing sets

**Emphasized** (recommended for most transitions — captures the MD3 style):

| Type | Cubic-bezier | Use |
|------|--------------|-----|
| Emphasized | `cubic-bezier(0.2, 0, 0, 1)` | Begin and end on screen |
| Emphasized decelerate | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Enter the screen |
| Emphasized accelerate | `cubic-bezier(0.3, 0, 0.8, 0.15)` | Exit the screen |

**Standard** (for utility transitions):

| Type | Cubic-bezier | Use |
|------|--------------|-----|
| Standard | `cubic-bezier(0.2, 0, 0, 1)` | Begin and end on screen |
| Standard decelerate | `cubic-bezier(0, 0, 0, 1)` | Enter the screen |
| Standard accelerate | `cubic-bezier(0.3, 0, 1, 1)` | Exit the screen |

In Compose, these are available as `Easing` / `EasingFunctions` constants per BOM.

#### Duration scale

| Token | Value | Use |
|-------|-------|-----|
| Short 1 | 50ms | Micro-interactions |
| Short 2 | 100ms | Small transitions |
| Short 3 | 150ms | Small transitions |
| Short 4 | 200ms | Exit transitions |
| Medium 1 | 250ms | Medium transitions |
| Medium 2 | 300ms | Standard transitions |
| Medium 3 | 350ms | Medium transitions |
| Medium 4 | 400ms | Enter transitions |
| Long 1 | 450ms | Large transitions |
| Long 2 | 500ms | Large, emphasized transitions |
| Long 3 | 550ms | Complex transitions |
| Long 4 | 600ms | Complex transitions |
| Extra long 1 | 700ms | Page transitions |
| Extra long 2 | 800ms | Page transitions |
| Extra long 3 | 900ms | Complex page transitions |
| Extra long 4 | 1000ms | Complex page transitions |

#### Suggested pairings

| Transition | Easing | Duration |
|-----------|--------|----------|
| Element stays on screen | Emphasized | 500ms |
| Element enters screen | Emphasized decelerate | 400ms |
| Element exits permanently | Emphasized accelerate | 200ms |
| Element exits temporarily | Emphasized | 300ms |
| Small utility transition | Standard | 300ms |

Respect the system "reduce motion" setting: honor animation scale = 0 from accessibility
settings.
