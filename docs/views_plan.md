<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Plan: Vistas de Iron

Rediseño de la navegación desde cero. Sustituye la estructura de pestañas heredada de Flexify.

## Destinos

| Destino | Rol | Pregunta que responde |
|---|---|---|
| **Hoy** | Superficie de ejecución (landing) | ¿Qué entreno ahora mismo? |
| **Planes** | Plantillas y configuración | ¿Cómo está configurada esta plantilla? |
| **Ejercicios** | Catálogo y detalle con gráficas | ¿Cómo ha evolucionado este ejercicio? |
| **Historial** | Sesiones agrupadas, con modo lista plana | ¿Qué hice, sesión por sesión? |
| **Ajustes** | Ruta desde la cabecera de Hoy, no pestaña | Preferencias |

Bottom nav con cuatro destinos de datos. Ajustes se alcanza desde la cabecera de Hoy.

## Grafo de navegación

```
nav
├── hoy
│     ├── session/:id
│     └── progreso global
├── planes
│     └── plan/:id  ──[Abrir sesión]──▶ session/:id
├── ejercicios
│     ├── filtro por categoría (sheet, no ruta)
│     └── ejercicio/:id
├── historial
│     ├── sesión/:id
│     └── modo registros (lista plana + filtros)
└── ajustes
```

`session/:id` es ruta de primer nivel, no hija de `plan/:id`: se llega desde Hoy, desde el detalle del plan y desde el historial.

El quick log no es una ruta: es un componente (FAB) que abre el editor de serie con el contexto de la pantalla.

Ocho pantallas: Hoy, Planes, PlanDetalle, Ejercicios, EjercicioDetalle, Historial, SesiónDetalle, Ajustes.

## Pantallas

### Hoy

Dos bloques, sin más:

1. Plan de hoy (coincidencia con `plan.days`) y sesión abierta, con el aviso de hace cuántos días fue la última sesión.
2. Progreso global: una sola gráfica (volumen semanal o 1RM estimado).

El quick log no es un bloque de Hoy: es el FAB contextual descrito más abajo.

Hoy es la pantalla de "qué hago ahora". Todo lo que requiera navegar pertenece a otros destinos. Si Hoy se convierte en un cajón, pierde su función.

Con base de datos vacía, Hoy muestra un estado vacío guiado: crear ejercicio, crear plan.

### Planes y PlanDetalle

Lista de planes con el plan de hoy resaltado. El detalle es **solo configuración**: ejercicios, orden, series máx, calentamiento, RPE objetivo, habilitado, y dos datos de frecuencia: última sesión y sesiones en los últimos 30 días.

No permite editar la plantilla mientras hay una sesión abierta: eso mutaría la plantilla con la sesión en curso.

### Ejercicios y EjercicioDetalle

La lista está optimizada para **filtrar y acceder**, no para gestionar. El CRUD es relevante en el setup inicial y luego casi nunca.

El detalle contiene: identidad (nombre, categoría, notas), gráficas, últimos registros, estadísticas (PR, valor actual, volumen semanal), y el CRUD. Las categorías se gestionan desde la barra de filtros de la lista, no como vista propia.

### Historial

Vista principal: **sesiones agrupadas**, con filtros por ejercicio, plan, categoría y rango de fechas. El detalle de cada sesión muestra sus series, editables y eliminables.

El modo "todos los registros" (lista plana) existe como filtro, no como la vista principal. Una lista plana de miles de series no es utilizable.

El editor de serie es el mismo componente que se usa durante la sesión. Dos formularios distintos se desincronizan.

### SesiónDetalle

La sesión abierta y sus series. Checklist de actividades del plan, series registradas, progreso por ejercicio frente a `maxSets` (por ejemplo 12/15, solo cuando `maxSets` esté definido), volumen de la sesión, botón de cierre.

Es la pantalla donde se registra; también se llega a ella desde el historial para revisar una sesión cerrada.

## Reglas de sesión

- Sesión indexada por `(planId, día)`. Se abre con la primera serie registrada para una actividad del plan.
- Cierre explícito obligatorio. Sin cierre explícito, una sesión de mañana y otra de tarde del mismo día se fusionan.
- Series registradas sin plan van a una sesión libre.
- El `timestamp` es metadato de visualización: editarlo no reasigna la serie a otra sesión.
- Una sesión sin series se descarta. Una sesión cuyas series se borran todas se conserva como registro de que existió.
- Editar una serie de una sesión cerrada no reabre la sesión.

## Solapamientos

EjercicioDetalle e Historial muestran registros, pero responden a preguntas distintas y son complementarios:

- **EjercicioDetalle** es longitudinal: un ejercicio a través del tiempo.
- **Historial** es cronológico: todas las sesiones, con todos los ejercicios.

No hay que resolver el solapamiento, solo mantener cada pantalla en su rol.

## Cambios en el modelo derivados de estas vistas

1. **`ExerciseSet.workoutId` pasa a ser obligatorio.** Con historial agrupado por sesión, las series huérfanas rompen el agrupamiento. La sesión se crea de forma perezosa al registrar.
2. **Se eliminan `durationMs`, `distanceM` y las conversiones m/km/mi.** Solo fuerza: la unidad canónica es kg. La métrica RPM desaparece de las gráficas.
3. **Se elimina `Exercise.displayUnit`.** Con una sola dimensión de medida, la unidad global de Ajustes es suficiente.
4. **`graphMetric`, `graphPeriod` y `graphLimit` salen de la entidad Exercise** y pasan a Ajustes como defaults; el estado de la gráfica es local a la pantalla.
5. **Métricas de gráfica viables:** mejor peso, volumen (peso × reps), repeticiones totales, series por periodo. Cuatro, no más.
6. **Limitación aceptada para v1:** el histórico refleja la configuración actual del plan, no la de entonces. Un snapshot en `Workout` es la única forma de que el pasado sea fiel.

## Descartes respecto a la estructura heredada de Flexify

- **Pestaña Gráficas:** desaparece. Se reparte entre Hoy (progreso global) y EjercicioDetalle (gráfica por ejercicio).
- **Categorías como vista:** desaparece. Pasa a filtro dentro de Ejercicios.
- **Ajustes como pestaña del bottom nav:** desaparece.
- **"Iniciar plan" como paso ceremonial:** desaparece. La sesión se abre al registrar el primer dato.
- **Pestañas configurables:** desaparece. Los destinos son fijos.

## Adherencia

Repartida en tres niveles, cada uno en su pantalla:

| Qué se mide | Pantalla |
|---|---|
| Series registradas vs `maxSets` en la sesión actual | SesiónDetalle |
| Última sesión y sesiones en los últimos 30 días | PlanDetalle |
| Plan de hoy y hace cuántos días la última sesión | Hoy |

No hay porcentajes de cumplimiento ni rachas. El porcentaje no se puede calcular con fiabilidad porque `maxSets`, `warmupSets` y `enabled` son opcionales, y una racha obliga a definir qué cuenta como día fallado, qué pasa con los días de descanso y qué pasa al cambiar de plan a mitad de semana.

Las sesiones libres no cuentan para la adherencia de ningún plan.

## Quick log

Es un FAB contextual, no un bloque de Hoy:

- Desde Hoy: ejercicio preseleccionado, el último usado.
- Desde EjercicioDetalle: ese ejercicio.
- Desde PlanDetalle: el ejercicio del plan.
- Historial no tiene FAB: la edición se hace en línea dentro de la sesión.

Reglas:

- Escribe en la sesión abierta; si no hay ninguna, crea una sesión libre para hoy.
- Exige `reps` o `loadKg`. Un registro con ambos vacíos es ruido.
- No permite cambiar de plan.

Una sesión de plan puede contener series de ejercicios que no están en el plan, y eso es correcto: la sesión es lo que pasó, no lo que estaba previsto.

El FAB es un componente compartido entre pantallas, así que vive en un paquete común, no en `feature/today`.
