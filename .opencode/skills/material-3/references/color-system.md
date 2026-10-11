<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Content distilled from hamen/material-3-skill (MIT license), adapted to Jetpack Compose only -->

# MD3 Color System

Reference for Material Design 3's color system: roles, tonal palettes, dynamic color, and
scheme generation. In Compose, every role maps to `MaterialTheme.colorScheme` (e.g. `primary`,
`onPrimary`).

## Color roles

MD3 defines 29+ color roles organized into groups.

### Accent colors

Three accent groups (primary, secondary, tertiary) each have 4 roles:

| Role | Purpose |
|------|---------|
| `primary` | High-emphasis fills, text, icons against surface |
| `on-primary` | Text and icons on primary |
| `primary-container` | Standout fill for key components (FAB, etc.) |
| `on-primary-container` | Text and icons on primary-container |
| `secondary` | Less prominent fills, text, icons |
| `on-secondary` | Text and icons on secondary |
| `secondary-container` | Recessive components (tonal buttons) |
| `on-secondary-container` | Text and icons on secondary-container |
| `tertiary` | Complementary fills, text, icons |
| `on-tertiary` | Text and icons on tertiary |
| `tertiary-container` | Complementary container fill |
| `on-tertiary-container` | Text and icons on tertiary-container |

**Usage guidance:**

- **Primary**: most prominent components — FABs, high-emphasis buttons, active states
- **Secondary**: less prominent components — filter chips, tonal buttons, selection states
- **Tertiary**: contrasting accents that balance primary/secondary — input fields, badges

### Error colors

Static colors that do not change with dynamic color schemes:

| Role | Purpose |
|------|---------|
| `error` | Attention-grabbing color for urgent elements |
| `on-error` | Text and icons on error |
| `error-container` | Error container fill |
| `on-error-container` | Text and icons on error-container |

### Surface colors

| Role | Purpose |
|------|---------|
| `surface` | Default background color |
| `on-surface` | Text and icons on any surface |
| `on-surface-variant` | Lower-emphasis text/icons on surface |
| `surface-container-lowest` | Lowest-emphasis container |
| `surface-container-low` | Low-emphasis container |
| `surface-container` | Default container (navigation areas) |
| `surface-container-high` | High-emphasis container |
| `surface-container-highest` | Highest-emphasis container |
| `surface-dim` | Dimmest surface in both themes |
| `surface-bright` | Brightest surface in both themes |

**Surface container hierarchy**: use `surface` for background, `surface-container` for
navigation. The 5 container levels create visual hierarchy and nesting depth.

**Surface dim/bright**: unlike regular surface (which flips from light to dark), dim and
bright maintain their relative brightness in both themes. Use bright for areas that should
always be the lightest, dim for areas that should always be the dimmest.

### Inverse colors

For elements that contrast against the surrounding UI (e.g. snackbars):

| Role | Purpose |
|------|---------|
| `inverse-surface` | Background for contrasting elements |
| `inverse-on-surface` | Text on inverse-surface |
| `inverse-primary` | Actionable text on inverse-surface |

### Outline colors

| Role | Purpose |
|------|---------|
| `outline` | Important boundaries (text field borders, 3:1 contrast) |
| `outline-variant` | Decorative elements (dividers, card borders) |

**Important**: do not use `outline` for dividers — use `outline-variant`. Do not use
`outline-variant` for interactive boundaries that need 3:1 contrast — use `outline`.

### Fixed accent colors (add-on)

These maintain the same color in both light and dark themes (unlike regular container colors
which change tone): `primary-fixed`, `primary-fixed-dim`, `on-primary-fixed`,
`on-primary-fixed-variant`, and the same pattern for secondary and tertiary.

**Caution**: fixed colors do not adapt to theme, so they may cause contrast issues. Use
regular accent roles for elements where contrast is critical.

## Tonal palette system

MD3 generates colors from a **seed color**:

1. A seed color is chosen
2. The seed generates **5 tonal palettes**: Primary, Secondary, Tertiary, Neutral,
   Neutral-Variant
3. Each palette uses tonal stops along **0–100**; the spec key tones are 0, 10, 20, 25, 30,
   40, 50, 60, 70, 80, 90, 95, 99, 100. Container roles use non-key tones (e.g. 94, 96, 92,
   90, 98, 6)
4. Color roles are mapped to specific tonal values depending on light or dark scheme

### Tonal value mapping (light scheme)

| Role | Tonal palette | Tone |
|------|--------------|------|
| Primary | Primary | 40 |
| On Primary | Primary | 100 |
| Primary Container | Primary | 90 |
| On Primary Container | Primary | 10 |
| Surface | Neutral | 98 |
| On Surface | Neutral | 10 |
| Surface Container | Neutral | 94 |
| Surface Container Low | Neutral | 96 |
| Surface Container Lowest | Neutral | 100 |
| Surface Container High | Neutral | 92 |
| Surface Container Highest | Neutral | 90 |
| Outline | Neutral-Variant | 50 |
| Outline Variant | Neutral-Variant | 80 |

### Tonal value mapping (dark scheme)

| Role | Tonal palette | Tone |
|------|--------------|------|
| Primary | Primary | 80 |
| On Primary | Primary | 20 |
| Primary Container | Primary | 30 |
| On Primary Container | Primary | 90 |
| Surface | Neutral | 6 |
| On Surface | Neutral | 90 |
| Surface Container | Neutral | 12 |
| Surface Container Low | Neutral | 10 |
| Surface Container Lowest | Neutral | 4 |
| Surface Container High | Neutral | 17 |
| Surface Container Highest | Neutral | 22 |
| Outline | Neutral-Variant | 60 |
| Outline Variant | Neutral-Variant | 30 |

## Color pairing rules

Colors must only be used in their intended pairs to ensure accessible contrast:

| Container/fill | Text/icon color |
|---------------|----------------|
| `primary` | `on-primary` |
| `primary-container` | `on-primary-container` |
| `secondary` | `on-secondary` |
| `secondary-container` | `on-secondary-container` |
| `tertiary` | `on-tertiary` |
| `tertiary-container` | `on-tertiary-container` |
| `error` | `on-error` |
| `error-container` | `on-error-container` |
| `surface` | `on-surface` or `on-surface-variant` |
| `surface-container-*` | `on-surface` or `on-surface-variant` |
| `inverse-surface` | `inverse-on-surface` or `inverse-primary` |

**Never pair colors outside their intended pairs** — this breaks contrast guarantees,
especially under dynamic color and high contrast modes.

## Dynamic color

Dynamic color creates personalized color schemes from external sources:

- **User-generated (wallpaper)**: the OS extracts a seed color from the user's wallpaper and
  generates a scheme. In Compose: `dynamicLightColorScheme(context)` /
  `dynamicDarkColorScheme(context)`. Iron runs on Android 12+ (minSdk 31), so dynamic color is
  always available — no `Build.VERSION.SDK_INT` check (project rule). See `ui/theme/Theme.kt`.
- **Content-based**: a seed color is extracted from in-app content (album art, book cover) to
  create a contextual scheme.

## Color harmonization

When integrating custom brand colors that do not come from the seed, use harmonization to
blend them into the tonal system: shift the custom color's hue slightly toward the scheme's
primary, making it feel cohesive without losing its identity. In Kotlin, use
`material-color-utilities` (e.g. `Blend.harmonize(customColorArgb, primaryColorArgb)`) or
generate the scheme with [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/)
and commit the generated `Theme.kt`.

## User-controlled contrast (May 2025)

MD3 supports 3 contrast levels:

- **Standard** (0.0): default contrast
- **Medium** (0.5): increased tonal distance between roles
- **High** (1.0): maximum tonal distance for vision accessibility

The contrast parameter adjusts the tonal distance between paired roles, increasing legibility
without changing the overall color feel. On Android this is handled by the system (user
accessibility settings); verify contrast with dynamic color active (D35).

**Expressive / vibrant schemes** (promoted in 1.5.0-beta01, not in the pinned BOM):
`materialExpressTheme` and `expressiveLightColorScheme` provide the expressive color reading;
component defaults expose vibrant variants (`MenuDefaults.itemVibrantColors()`,
`iconButtonVibrantColors()`).

## Baseline color scheme (default values)

The static baseline scheme for products not using dynamic color. Use it only as reference or
fallback seed values; Iron uses dynamic color (D35 note in `docs/spec/ux.md`).

### Light theme

| Role | Hex |
|------|-----|
| primary | `#6750A4` |
| on-primary | `#FFFFFF` |
| primary-container | `#EADDFF` |
| on-primary-container | `#21005D` |
| secondary | `#625B71` |
| on-secondary | `#FFFFFF` |
| secondary-container | `#E8DEF8` |
| on-secondary-container | `#1D192B` |
| tertiary | `#7D5260` |
| on-tertiary | `#FFFFFF` |
| tertiary-container | `#FFD8E4` |
| on-tertiary-container | `#31111D` |
| error | `#B3261E` |
| on-error | `#FFFFFF` |
| error-container | `#F9DEDC` |
| on-error-container | `#410E0B` |
| surface | `#FEF7FF` |
| on-surface | `#1D1B20` |
| on-surface-variant | `#49454F` |
| surface-container-lowest | `#FFFFFF` |
| surface-container-low | `#F7F2FA` |
| surface-container | `#F3EDF7` |
| surface-container-high | `#ECE6F0` |
| surface-container-highest | `#E6E0E9` |
| surface-dim | `#DED8E1` |
| surface-bright | `#FEF7FF` |
| outline | `#79747E` |
| outline-variant | `#CAC4D0` |
| inverse-surface | `#322F35` |
| inverse-on-surface | `#F5EFF7` |
| inverse-primary | `#D0BCFF` |

### Dark theme

| Role | Hex |
|------|-----|
| primary | `#D0BCFF` |
| on-primary | `#381E72` |
| primary-container | `#4F378B` |
| on-primary-container | `#EADDFF` |
| secondary | `#CCC2DC` |
| on-secondary | `#332D41` |
| secondary-container | `#4A4458` |
| on-secondary-container | `#E8DEF8` |
| tertiary | `#EFB8C8` |
| on-tertiary | `#492532` |
| tertiary-container | `#633B48` |
| on-tertiary-container | `#FFD8E4` |
| error | `#F2B8B5` |
| on-error | `#601410` |
| error-container | `#8C1D18` |
| on-error-container | `#F9DEDC` |
| surface | `#141218` |
| on-surface | `#E6E0E9` |
| on-surface-variant | `#CAC4D0` |
| surface-container-lowest | `#0F0D13` |
| surface-container-low | `#1D1B20` |
| surface-container | `#211F26` |
| surface-container-high | `#2B2930` |
| surface-container-highest | `#36343B` |
| surface-dim | `#141218` |
| surface-bright | `#3B383E` |
| outline | `#938F99` |
| outline-variant | `#49454F` |
| inverse-surface | `#E6E0E9` |
| inverse-on-surface | `#322F35` |
| inverse-primary | `#6750A4` |
