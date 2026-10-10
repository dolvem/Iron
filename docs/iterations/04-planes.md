<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 4 — Planes (en curso)

Bloque de `docs/state/todo.md`. Decisiones del bloque: D8, D33 y D67.

- [ ] CRUD de planes y de `PlanExercise`: orden, `maxSets`, `warmupSets`, `targetRpe`, `enabled`.
- [ ] Frecuencia en el detalle: última sesión y sesiones en los últimos 30 días (ventana móvil desde hoy, 30 días naturales incluyendo hoy).
- [ ] Las sesiones libres no cuentan en la frecuencia ni en la adherencia de ningún plan (D8).
- [ ] Pantalla Planes: lista ordenada por `sequence` (nulos al final), con el plan de hoy resaltado.
- [ ] Pantalla PlanDetalle: solo configuración, bloqueada mientras hay una sesión abierta.
- [ ] `Plan.title` nulo se muestra como `Plan`.
- [ ] Borrado de plan: confirmación que avisa que las sesiones quedan como sesiones libres, con conteo de esas sesiones; bloqueado mientras haya una sesión abierta (D33).
- [ ] Quick log FAB desde PlanDetalle: preselecciona el ejercicio del plan.

## Notas del bloque

- `PlanFrequencyTest.frequencyReemitsWhenASessionIsInserted` fallaba de forma intermitente. Room invalida las dos consultas del `combine` por separado, así que el orden de la reemisión no está garantizado y el `combine` puede emitir primero el estado intermedio `(null, 1)` antes de reejecutar la consulta de última sesión. El test ahora acepta el intermedio y afirma el estado final. Dos pasadas con `--rerun-tasks` limpias.
- Firmas de lectura: `PlanDao.observeAll()`, `PlanExerciseDao.observeForPlan()` y `PlanExerciseDao.observeForPlanIncludingDisabled()` pasan a `Flow<List<T>>` (D67), la firma que ya usan `CategoryDao`, `ExerciseDao.search` y `SetDao`. `ExerciseDao.observeAll()` y `ExerciseDao.observeAllIncludingArchived()` quedan pendientes de alinear en la pasada de Historial, para no dejar las dos firmas conviviendo. Los tests que hoy pasan con `Flow<T>` lo hacen porque provocan la invalidación con una escritura; con la lista se afirma el resultado completo en la emisión inicial.
