<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 1 — Scaffolding (cerrada)

Bloque de `docs/state/todo.md`. Decisiones del bloque: D21, D22, D23, D24, D25, D27, D36, D38, D47 a D61, D65 y D66.

- [x] Proyecto mono-módulo `app`, Kotlin DSL, catálogo `libs.versions.toml` con versiones fijadas (D20).
- [x] Material You (dynamic color), tema claro/oscuro/sistema con `ThemeMode` y `ThemeSource` fijo (D65).
- [x] Bottom nav con los 4 destinos (Hoy, Planes, Ejercicios, Historial) y Ajustes como ruta desde la cabecera de Hoy.
- [x] `git init` y `.gitignore` (D22, D48).
- [x] `LICENSE` GPL-3.0-or-later con la carpeta `LICENSES/` de REUSE y cabecera SPDX en cada fichero (D21, D57).
- [x] `README.md` de la raíz enlazando a `docs/README.md`.
- [x] `.editorconfig` con `max_line_length = 120` y ktlint-gradle con `code_style = android_studio` (D27, D50).
- [x] Android Lint con warnings como error, `lint-baseline.xml` vacío y `lint.xml` (D51).
- [x] Icono de aplicación: adaptive icon con fondo `#1B1B1D`, foreground vectorial y capa monochrome (D25, D60).
- [x] Orientación bloqueada en vertical, regla `LockedOrientationActivity` desactivada (D38).
- [x] Idioma base en inglés: `values/` en inglés y `values-es` en español (D36).
- [x] Versionado `iron.version` en `gradle.properties` con `versionCode` semver (D23).
- [x] CI: job único `check` en `ubuntu-latest` con ktlint, Lint, test y build (D49).

## Notas del bloque

Sin notas. El bloque se cerró con las cuatro tareas de CI.
