<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 10 — Ajustes completos

Bloque de `docs/state/todo.md`. Decisiones del bloque: D37, D63, D64, D65 y D72.

- [ ] Pantalla de Ajustes: unidad de fuerza, formatos de fecha, defaults de gráfica, estimación 1RM/reps, idioma.
- [ ] Selector de tema en la UI, conectado a `ThemeSource` (D65).
- [ ] Selector de idioma: `sistema` (default), `es` y `en`, aplicado de forma explícita en Compose con `LocalLocale.current.platformLocale` (D64, D72), con fallback a inglés si el idioma del sistema no está soportado.
- [ ] Defaults de formatos de fecha por idioma efectivo, tras `localeOverride` (D63).
- [ ] Los `plurals.xml` con reglas distintas en cada idioma.

## Notas del bloque

Sin notas.
