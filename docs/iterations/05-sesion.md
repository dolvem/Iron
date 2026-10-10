<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 5 — Sesión

Bloque de `docs/state/todo.md`. Decisiones del bloque: D28, D29, D30, D39, D40, D41, D42 y D70.

- [ ] Pantalla SesiónDetalle: checklist de solo lectura derivado de las series frente a `maxSets`, progreso por ejercicio, volumen de la sesión, botón de cierre.
- [ ] Editor de serie compartido en `ui/components/`: se mantiene abierto tras registrar, foco al siguiente campo (D30).
- [ ] Entrada numérica: peso con teclado decimal con autofocus, `reps` entero con stepper de 1 a 100 y estado vacío (D30).
- [ ] Separador decimal: coma y punto normalizados a punto, solo en modo teclado; el redondeo ocurre al guardar (D28).
- [ ] Botones de placas como accesos rápidos de suma: kg 1,25 / 2,5 / 5 / 10 / 20 / 25; lb 2,5 / 5 / 10 / 25 / 35 / 45 (D29).
- [ ] El editor muestra el error que devuelve el repositorio por `Outcome`, no valida por su cuenta (D6), y lo muestra dentro del sheet, no en el `SnackbarHost` de la pantalla (D70).
- [ ] Campos editables de una serie: métricas, notas, `isWarmup` y `timestamp`; el ejercicio no (D42).
- [ ] Cierre explícito dentro del día; el botón de cierre se oculta con 0 series (D39).
- [ ] Deshacer en snackbar, ventana de 6 s, solo en series: recrea la sesión eliminada con el `id` y el `startedAt` originales, y revierte el cierre automático si la serie lo provocó (D40).
- [ ] `targetRpe` mostrado como referencia orientativa junto al ejercicio, sin marcarse por serie.
- [ ] Quick log: exige `reps` o `loadKg`, escribe en la sesión abierta o crea una sesión libre para hoy, siempre serie de trabajo.

## Notas del bloque

Sin notas.
