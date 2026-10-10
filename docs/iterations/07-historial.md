<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 7 — Historial

Bloque de `docs/state/todo.md`. Decisiones del bloque: D32, D43, D44, D45 y D67.

- [ ] Vista principal: sesiones agrupadas por `sessionDate`, fecha descendente, cargadas de 30 sesiones por página.
- [ ] Detalle de sesión: sus series, editables y eliminables, con el editor de serie compartido.
- [ ] Modo lista plana de registros como filtro, no como vista principal.
- [ ] Filtros: ejercicio, plan, categoría, rango de fechas y sesiones libres (D45).
- [ ] Sesiones libres con título fijo y filtro propio.
- [ ] Cierre de sesión desde el detalle del historial; una sesión cerrada no se reabre (D43).
- [ ] El cierre automático se distingue del explícito en el historial (D44).
- [ ] Ejercicios archivados incluidos en el historial (regla de archivados, D32).
- [ ] Sesión cerrada con 0 series aparece con un estado vacío en el detalle (D39).
- [ ] Alinear a `Flow<List<T>>` (D67) las firmas que hoy devuelven `Flow<T>`: `WorkoutDao.observeAll()`, `ExerciseDao.observeAll()` y `ExerciseDao.observeAllIncludingArchived()`.

## Notas del bloque

Sin notas.
