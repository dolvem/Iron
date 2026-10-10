<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 11 — Pulido

Bloque de `docs/state/todo.md`. Decisiones del bloque: D6, D19, D24, D35, D55, D56, D66, D69 y D71.

- [ ] Estados vacíos en todos los listados y pantallas.
- [ ] Validaciones: el error siempre viene del `Outcome` del repositorio, la UI no valida (D6).
- [ ] Accesibilidad verificada en emulador: targets táctiles de 48dp, `contentDescription` en iconos, escala de fuente, contraste con colores dinámicos, alternativa textual de las gráficas (D35), y nombre accesible en los controles de selector con `semantics(mergeDescendants = true)` (D71).
- [ ] Filtros y selección de gráfica en `SavedStateHandle`; estado local de pantalla con `rememberSaveable`.
- [ ] `LazyColumn` con `key` en todos los listados.
- [ ] Menú de acciones en la identidad de EjercicioDetalle: renombrar, borrar y archivar (D69); el detalle pasa de solo lectura a editable.
- [ ] Tests unitarios pendientes de la matriz D18: undo que recrea una sesión eliminada al borrar su última serie (Room en memoria), import por fusión con colisión de nombre, colisión de ID con otro nombre y remapeo de FKs (Room en memoria).
- [ ] Build reproducible verificado con las seis reglas (D24, D56) y firma externa con `apksigner` de build-tools 34.0.0 (D55, D66).

## Notas del bloque

Sin notas.
