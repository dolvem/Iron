<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Reglas de negocio y definiciones de cálculo

Parte de `docs/README.md`. El esquema está en `docs/data_plan.md` y el estado de cada decisión (D1–D62) en `docs/decisions_plan.md`.

Estas reglas son las que convierten el esquema en comportamiento. Se cierran antes de escribir los DAOs, porque cambiarlas después obliga a migraciones. Las tres que estaban abiertas (frontera de sesión, calentamiento y día de sesión) están cerradas en la revisión del 08/10/2026 y ya aparecen reflejadas aquí. En la quinta revisión se cierran D6, D7, D8 y D10. En la sexta revisión se cierran D11 a D15 y D39, las pendientes que quedaban en este documento.

## Tiempo y frontera de día

- Los `timestamp` se guardan como epoch millis UTC. La conversión a fecha local se hace solo al leer.
- La frontera de día es medianoche local en la zona horaria actual del dispositivo (`ZoneId.systemDefault()`).
- Semana para analítica: ISO 8601, lunes como primer día, anclada a la zona local.
- El día de la sesión se guarda en `Workout.sessionDate` y no se recalcula. Cambiar la zona horaria afecta a las sesiones futuras, no a las pasadas, así que la limitación de reagrupación desaparece.
- Una serie registrada después de medianoche pertenece a la sesión de su día, no a la sesión abierta de la víspera.

## Formato canónico de `Plan.days`

- Cadena CSV de números ISO 1–7, ordenada y sin espacios: `"1,3,5"`. Cadena vacía significa "nunca".
- Se parsea una vez al leer. Es legible en el JSON de backup y fácil de testear.
- Se descarta el bitmask en un `Int`: es más eficiente pero peor para depurar.

## Plan de hoy

- Hoy muestra **todos** los planes que coinciden con hoy, ordenados por `sequence` (nulos al final). En la práctica casi siempre será uno.
- No existe un "plan activo" global. Cada plan coincidente muestra su sesión abierta, si la tiene.
- `Plan.title` nulo se muestra como `Plan`.

## Sesiones

- La frontera `(planId, día)` se extiende a `planId IS NULL`: **una sesión libre abierta por día**.
- Una serie escribe en la sesión abierta de su propio día. Si la sesión abierta pertenece a otro día, se cierra y se abre una nueva en la misma transacción del insert.
- El quick log reutiliza la sesión libre abierta del día; si está cerrada, crea una nueva.
- El cierre es explícito dentro del mismo día y automático al cambiar de día. `Workout.closedBy` distingue los dos casos, y el historial lo muestra. El trigger del cierre automático es el mismo ticker que recalcula Hoy (D34, cerrada), así que hay una sola pieza.
- `endedAt` del cierre automático se fija en el momento del cierre, no en el timestamp de la serie que lo provoca.
- Se puede cerrar una sesión desde su detalle en el historial. Una sesión cerrada no se reabre.
- No se pueden **añadir** series a una sesión cerrada. Sí editar y eliminar las existentes.
- `Workout.startedAt` se fija en el momento de la apertura perezosa, y `Workout.sessionDate` en ese mismo momento, en la zona local de apertura.
- Deshacer una serie que provocó un cierre automático revierte también el cierre. Es la única excepción a la regla de que el deshacer no reabre sesiones.
- Una sesión que nunca tuvo series no existe, porque la apertura es perezosa, y por tanto no se puede cerrar. Una sesión abierta se elimina al borrar su última serie, porque conservarla abierta bloquearía la restricción de una sesión activa por plan al día siguiente. Una sesión cerrada a la que se le borran todas las series se conserva y aparece en el historial con un estado vacío (D39, cerrada).
- El undo recrea la sesión cuando el borrado de la última serie la eliminó, con el `id` y el `startedAt` originales. Cuando la serie provocó un cierre automático y además era la última, el undo primero recrea la sesión y después revierte el cierre (D40, cerrada).
- `workoutId` es obligatorio. Editar el `timestamp` de una serie no la reasigna a otra sesión.

## Borrados

- **Ejercicio con series:** el borrado exige confirmación explícita con el número de series afectadas. El `CASCADE` es correcto, pero sin aviso es destructivo. Archivar sigue siendo la opción recomendada.
- **Plan con sesión abierta:** bloquear edición y borrado hasta cerrar la sesión.
- **Categoría:** `SET NULL`, sin cambios.

## Nombres únicos

- Regla cerrada (D4, cuarta revisión): la comparación case-insensitive se hace sobre la columna normalizada `nameKey`, calculada en Kotlin con NFC y `Locale.ROOT`, y el índice único está en esa columna. Se descarta `COLLATE NOCASE` porque SQLite solo pliega ASCII, así que con acentos la base de datos y el repositorio no coincidirían. La normalización se aplica en el insert y en el renombrar desde una única función en `domain/model`, y la comprobación en el repositorio queda solo para generar el mensaje de error.

## Progreso y métricas

- `x/maxSets` cuenta **solo series de trabajo**. El calentamiento se muestra aparte como `y/warmupSets` cuando esté definido.
- **Calentamiento en métricas:** PR, volumen, repeticiones totales y conteo de series excluyen las series con `isWarmup`.
- **Volumen:** `loadKg × reps`. Las series con `reps` nulo se excluyen del volumen.
- **PR:** máximo `loadKg` por ejercicio. Es la métrica "mejor peso", no el mejor volumen.
- **Valor actual:** mejor valor de la ventana actual, que es la semana ISO en curso. Si la ventana está vacía, se muestra el valor de la última semana con datos y se etiqueta la ventana (D7, cerrada).
- **Volumen semanal en Hoy:** semana ISO en curso, con hueco si no hay datos.
- **Métrica de la gráfica de Hoy:** la decide `defaultGraphMetric`, con volumen semanal como default.
- **1RM con Epley:** solo series con `reps >= 1`. Las series sin `reps` se omiten. Redondeo a 0,5 kg al mostrar.
- **Series por periodo:** conteo de series con al menos una métrica (`reps` o `loadKg`).
- **Buckets sin datos:** hueco en las gráficas de serie temporal, cero en el conteo de series por periodo.
- **Últimos 30 días:** ventana móvil desde hoy, 30 días naturales incluyendo hoy.
- **Sesiones libres:** cuentan en volumen global y conteo de series, y no cuentan en la adherencia de ningún plan ni en la frecuencia del detalle del plan, porque no tienen plan al que atribuirse (D8, cerrada).
- **Archivados:** excluidos de la lista por defecto, incluidos en el historial y en las gráficas del detalle del ejercicio.
- **Redondeo:** 2 decimales al mostrar y al guardar, medio arriba cuando hay más decimales. El valor guardado es la fuente de verdad, así que mostrar nunca redondea sobre un valor ya redondeado. El 1RM mantiene 0,5 kg porque es una estimación y no un dato registrado. La ida y vuelta kg → lb → kg no es estable con dos decimales, y se acepta como limitación (D10, cerrada).
- **Validación:** `reps >= 1` y `loadKg > 0`, con al menos una métrica. Se rechaza cero y negativos, sin techo. La validación la hace el repositorio antes del `CASCADE`; la UI no valida, muestra el error de `Outcome` (D6, cerrada).

## Import/export

- Los IDs se conservan en el JSON.
- Modo de importación: **fusión**. Si un nombre único colisiona, se reutiliza la entidad existente y se remapean las FKs. No se reemplaza la base de datos. En la colisión de nombre gana el nombre existente, porque en el import la base destino es el destino, la misma regla que D32. Los nombres duplicados dentro del propio fichero se resuelven con la primera ocurrencia, y las siguientes se mapean a ella, con el conflicto registrado en el resumen (D11, cerrada).
- **Algoritmo:** dos pasadas con un mapa `incomingId -> existingId`. Primero las entidades raíz (categories, exercises, plans), después las dependientes (planExercises, workouts, sets).
- **Colisión de ID con otro nombre:** se trata como entidad distinta y recibe un ID nuevo en la base destino.
- **Colisión en `unique(planId, exerciseId)`:** gana la fila existente y el conflicto se registra en el resumen de importación. Es la misma regla que la fusión manual de D32, así que se escribe una sola vez.
- Todo el import en una sola transacción, con backup automático previo en `filesDir/backups/`, retención de los 3 más recientes, sin acceso por SAF y con GZIP. Son una red de seguridad interna, sin UI de restauración, porque una UI de restauración obligaría resolver otra vez el algoritmo de fusión sobre un backup interno (D12, cerrada).
- **Versiones del formato:** el importador acepta `version <= current`. La versión solo sube con un cambio rompedor. Se rechaza un fichero cuyo campo `app` no es `iron`, y se rechaza con error una `version` mayor que `current`, porque sin esa comprobación un fichero de otra app con `version: 1` pasaría el filtro (D14, cerrada).
- **El formato se serializa con DTOs separados de las entidades.** El JSON es un formato público, así que renombrar una propiedad de una entidad no puede romper un backup existente.
- GZIP se detecta por magic bytes, no por extensión.
- CSV con BOM UTF-8 para abrirlo en Excel. El CSV exporta la ventana actual de la pantalla, no toda la tabla. Una ventana vacía exporta cabeceras sin filas, porque el CSV siempre tiene cabeceras, y respeta la exclusión del calentamiento de D9 (D15, cerrada).
- Los ajustes no entran en el backup de v1, porque viven en DataStore, fuera de la BD. Se acepta perderlos al migrar de dispositivo (D13, cerrada).

## Orden y paginación

- Ejercicios: por nombre. Historial: fecha descendente. Planes: por `sequence`.
- Historial agrupado por sesión, cargado de 30 sesiones por página. El agrupamiento usa `Workout.sessionDate`.
- Filtros del historial: ejercicio, plan, categoría, rango de fechas y sesiones libres.
- `archived` y `enabled = false` quedan excluidos por defecto, con filtro para mostrarlos.
- Búsqueda case-insensitive en la lista de ejercicios, en la barra de filtros.
