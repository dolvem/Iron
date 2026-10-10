<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 6 — Hoy

Bloque de `docs/state/todo.md`. Decisiones del bloque: D34.

- [ ] Bloque 1: plan de hoy (todos los planes que coinciden con `plan.days`), sesión abierta y aviso de hace cuántos días fue la última sesión.
- [ ] Bloque 2: progreso global, una sola gráfica (volumen semanal o 1RM estimado), con `defaultGraphMetric`.
- [ ] Ticker con `delayUntilMidnight` que recalcula la coincidencia al cambiar el día y provoca el cierre automático de D1; es una sola pieza (D34). Determinista en tests con `Clock` y `ZoneId`.
- [ ] Con la app muerta en segundo plano, se recalcula al relanzar.
- [ ] Estado vacío guiado en el primer arranque: crear ejercicio, crear plan.
- [ ] Quick log FAB contextual: desde Hoy preselecciona el último ejercicio usado.
- [ ] Semana ISO en curso para el volumen semanal, con hueco si no hay datos.

## Notas del bloque

Sin notas.
