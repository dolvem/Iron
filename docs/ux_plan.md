<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# UX operativa

Parte de `docs/README.md`. El estado de las decisiones D28–D46 está en `docs/decisions_plan.md`. Las cerradas en la primera revisión del 08/10/2026 son D31, D38 y D41 a D45; en la quinta revisión se cierran D37 y D46; en la sexta revisión se cierran las demás: D28 a D30, D32 a D35, D39 y D40. No queda ninguna pendiente en este documento.

## Entrada de datos

- **Entrada numérica:** peso con teclado decimal, mostrado con 2 decimales, autofocus en el campo de peso. `reps` es entero con stepper entre 1 y 100, con estado vacío: `reps` puede quedar nulo y el rango solo aplica cuando hay valor, así que se puede registrar una serie con solo peso (D30, cerrada). Botones rápidos de placas, como accesos rápidos de suma y no como inventario de placas físicas: kg 1,25 / 2,5 / 5 / 10 / 20 / 25; lb 2,5 / 5 / 10 / 25 / 35 / 45 (D29, cerrada). Es el gesto más repetido de la app, así que se trata como una decisión de diseño, no como un detalle de formulario.
- **Separador decimal:** se aceptan coma y punto y se normaliza a punto, porque en español el teclado produce coma. La regla solo aplica en modo teclado, porque con `inputStyle = stepper` no se escribe un separador. El campo acepta lo que el usuario escribe y el redondeo a 2 decimales, medio arriba, ocurre al guardar, el mismo criterio de D10. El separador mostrado depende del idioma activo: la normalización es parsing de entrada, no presentación (D28, cerrada).
- **Validación en el editor:** el editor de serie muestra el error que devuelve el repositorio, no valida por su cuenta. `reps = 0` y `loadKg = 0` se rechazan antes de insertar.
- **Editor de serie:** se mantiene abierto tras registrar y el foco pasa al siguiente campo. Es el mismo componente en sesión, quick log e historial.
- **Campos editables de una serie:** métricas, notas, `isWarmup` y `timestamp`. El ejercicio no se edita, porque cambiarlo rompería la identidad longitudinal del ejercicio.
- **Quick log:** exige `reps` o `loadKg`. Escribe en la sesión abierta; si no hay ninguna, crea una sesión libre para hoy. El editor de serie y el FAB son componentes compartidos en `ui/components/`, no variantes por pantalla.

## Listados y filtros

- **Checklist de sesión:** solo lectura, derivado de las series registradas frente a `maxSets`. Con 0 series el checklist está vacío y el botón de cerrar se oculta, porque no hay nada que cerrar (D39, cerrada).
- **Sesión vacía en el historial:** una sesión cerrada con 0 series se conserva y aparece en el listado con un estado vacío en el detalle, porque el historial es el registro de lo que ocurrió (D39, cerrada).
- **Búsqueda:** caja de búsqueda case-insensitive en la barra de filtros de Ejercicios. Con un catálogo propio de cientos de ejercicios, filtrar sin búsqueda no es usable. La búsqueda usa la misma columna normalizada `nameKey` que la unicidad de D4, porque `LIKE` en SQLite solo es case-insensitive en ASCII.
- **Sesiones libres:** título fijo y filtro propio en el historial. El cierre automático se distingue del explícito.

## Confirmaciones y deshacer

- **Deshacer:** snackbar con undo en el borrado de series, ventana de 6 segundos, que es uno de los valores oficiales de Material 3, y solo en series. No reabre una sesión cerrada, salvo cuando la serie borrada provocó un cierre automático. Cuando el borrado de la última serie elimina la sesión, el undo la recrea con el `id` y el `startedAt` originales, porque deshacer devuelve el estado anterior y no crea uno nuevo. Cuando la serie provocó un cierre automático y además era la última, el undo primero recrea la sesión y después revierte el cierre. En el borrado de entidades grandes (ejercicio, plan, categoría) se pide confirmación con conteo de elementos afectados, sin undo (D40, cerrada).
- **Fusión:** el usuario elige el destino, y sobrevive su nombre. Las series y las referencias de plan se remapean y el origen se borra, con confirmación y conteo. En `unique(planId, exerciseId)` gana la fila existente y el conflicto se registra, la misma regla que D11. En `archived` y `notes` gana el estado menos restrictivo, con las notas concatenadas, porque fusionar un ejercicio activo en uno archivado lo saca de la lista por defecto. En categorías sobrevive el nombre destino y los ejercicios se remapean (D32, cerrada).
- **Borrado de plan:** la confirmación avisa que las sesiones quedan como sesiones libres, porque `Workout.planId` es SET NULL, y el conteo cuenta esas sesiones, porque es lo que el usuario pierde. El borrado sigue bloqueado mientras haya una sesión abierta, así que la confirmación solo aplica a sesiones cerradas (D33, cerrada).

## Comportamiento de pantalla

- **Hoy a medianoche:** la coincidencia con `plan.days` se recalcula con un Flow que emite al cambiar el día, mediante un ticker con `delayUntilMidnight`. No hace falta cron ni WorkManager. El mismo ticker es el que provoca el cierre automático de D1, así que hay una sola pieza. Con la app muerta en segundo plano se recalcula al relanzar. El cambio de zona horaria no reagrupa sesiones pasadas, porque `sessionDate` es inmutable (D34, cerrada).
- **Muerte del proceso:** Android mata la app en segundo plano. Los filtros y la selección de gráfica se guardan en `SavedStateHandle`; el estado local de pantalla usa `rememberSaveable`.
- **Back predictivo:** se acepta el comportamiento por defecto con target API 36.
- **Dynamic color:** con `minSdk` 31 está disponible en todos los dispositivos donde corre la app. No hay paleta de respaldo que escribir ni mantener.
- **Accesibilidad:** targets táctiles de 48dp, `contentDescription` en iconos, respeto a la escala de fuente, contraste verificado con colores dinámicos, y alternativa textual con la etiqueta del bucket y el valor de la métrica seleccionada en las gráficas, presentada como una sección plegable siempre disponible, porque Vico no es accesible por defecto y porque comprobar el estado de los servicios de accesibilidad para cambiar el layout es frágil. Se verifica en emulador, porque D19 deja la UI fuera de los tests (D35, cerrada).
- **Listas largas:** `LazyColumn` con `key` en todos los listados.
- **Alcance de pantalla:** solo móvil en vertical, bloqueado en el manifest con `android:screenOrientation="portrait"` en `MainActivity` (D38, cerrada). La regla `LockedOrientationActivity` se desactiva en `lint.xml`. Horizontal, tablet y plegables quedan fuera de v1.
- **Permisos:** ninguno. La app es offline-first y el acceso a ficheros se hace por SAF.
