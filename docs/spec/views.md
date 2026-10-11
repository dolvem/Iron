<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Plan: Vistas de Iron

Rediseño de la navegación desde cero. Sustituye la estructura de pestañas heredada de Flexify.

Parte de `docs/README.md`. El comportamiento de las pantallas está en `docs/spec/ux.md`, las reglas de negocio en `docs/spec/rules.md`, y el estado de las decisiones (D1–D76) en `docs/state/decisions.md`. Este documento solo tiene estructura: destinos, grafo y pantallas.

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
│     ├── filtro por categoría (chip con menú, no ruta)
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

El detalle contiene: identidad (nombre, categoría, notas), gráficas, últimos registros, estadísticas (PR, valor actual, volumen semanal), y el CRUD, que llega por el menú de acciones de la identidad: renombrar, borrar y archivar. Las categorías se gestionan desde el menú del chip de categoría de la lista (D82), no como vista propia.

El CRUD se escribe primero en las filas de la lista, que es donde se usa en el setup inicial. El menú de acciones de la identidad llega en la iteración 11, así que el detalle es solo lectura hasta entonces (D69).

**Fila de lista (D75, cerrada):** el nombre es el texto dominante y la categoría se muestra como píldora debajo. La píldora se oculta cuando esa categoría es el filtro activo, porque el chip de categoría de D78 ya lo enuncia. El badge Archivado convive en la misma línea de pills.

**Píldora de sin categoría (D83, cerrada):** la píldora se lee como badge, que es lo que justifica el shape `full`. Las pills de categoría y el badge Archivado usan la pareja `secondary_container` + `on_secondary_container` (D75); la de sin categoría usa la pareja neutra `surface_container` + `on_surface_variant`, la lectura M3 de un badge de baja énfasis, para diferenciarla.

**Crear en la lista (D80, cerrada):** el acceso a crear es un FAB menu con dos items, Crear ejercicio y Crear categoría, que revisa D77. El FAB sigue siendo el contenedor estándar de la acción principal en M3, y el menú agrupa las dos acciones emparentadas de la pantalla. La creación de categoría deja de ser un item del sheet, que desaparece con D78.

### Historial

Vista principal: **sesiones agrupadas**, con filtros por ejercicio, plan, categoría y rango de fechas. El detalle de cada sesión muestra sus series, editables y eliminables.

El modo "todos los registros" (lista plana) existe como filtro, no como la vista principal. Una lista plana de miles de series no es utilizable.

El editor de serie es el mismo componente que se usa durante la sesión. Dos formularios distintos se desincronizan.

### SesiónDetalle

La sesión abierta y sus series. Checklist de actividades del plan, series registradas, progreso por ejercicio frente a `maxSets` (por ejemplo 12/15, solo cuando `maxSets` esté definido), volumen de la sesión, botón de cierre.

Es la pantalla donde se registra; también se llega a ella desde el historial para revisar una sesión cerrada.

## Reglas de sesión

No viven aquí: están en `docs/spec/rules.md` (Sesiones). Lo único propio de este documento es que `session/:id` es ruta de primer nivel y se llega desde Hoy, desde el detalle del plan y desde el historial.

## Solapamientos

EjercicioDetalle e Historial muestran registros, pero responden a preguntas distintas y son complementarios:

- **EjercicioDetalle** es longitudinal: un ejercicio a través del tiempo.
- **Historial** es cronológico: todas las sesiones, con todos los ejercicios.

No hay que resolver el solapamiento, solo mantener cada pantalla en su rol.

## Cambios en el modelo derivados de estas vistas

No viven aquí: el esquema ya los refleja (`docs/spec/data.md`) y la justificación está en `docs/state/decisions.md`, en el bloque "Consecuencias del rediseño de vistas".

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

Las reglas de comportamiento del quick log están en `docs/spec/ux.md`. Lo único propio de este documento es el contexto de cada pantalla:

- Desde Hoy: ejercicio preseleccionado, el último usado.
- Desde EjercicioDetalle: ese ejercicio.
- Desde PlanDetalle: el ejercicio del plan.
- Historial no tiene FAB: la edición se hace en línea dentro de la sesión.

El FAB es un componente compartido entre pantallas, así que vive en un paquete común, no en `feature/today`.
