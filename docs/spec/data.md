<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Plan de datos: esquema Room, ajustes y formato

Parte de `docs/README.md`. Las reglas de comportamiento están en `docs/spec/rules.md` y el estado de las decisiones (D1–D72) en `docs/state/decisions.md`.

Cambiar este documento obliga a una migración.

## Esquema Room (6 tablas — sin timer, sin peso corporal)

| Entidad | Campos clave | Relaciones |
|---|---|---|
| `Category` | id, name (unique), nameKey (unique, normalizado en Kotlin) | — |
| `Exercise` | id, name (unique), nameKey (unique, normalizado en Kotlin), notes?, archived | categoryId → Category (**SET NULL**) |
| `Plan` | id, title?, days (CSV ISO 1–7, ver reglas), sequence? | — |
| `PlanExercise` | id, planId→Plan (**CASCADE**), exerciseId→Exercise (**CASCADE**), sequence, maxSets?, warmupSets?, targetRpe?, enabled; unique(planId, exerciseId) | sin campo de timers (descartado); RPE orientativo del plan |
| `Workout` | id, startedAt, sessionDate (inmutable), endedAt?, closedBy (`auto`/`explicit`) | planId → Plan (**SET NULL**, nullable); restricción: una sesión activa por plan (`endedAt IS NULL`) |
| `ExerciseSet` | id, exerciseId→Exercise (**CASCADE**), workoutId→Workout (**CASCADE**, NOT NULL), timestamp, reps?, loadKg?, notes?, isWarmup | métricas en kg; `isWarmup` se fija en el insert y no se recalcula |

El índice único está en `nameKey`, la columna normalizada de D4 (cerrada en la cuarta revisión), calculada en Kotlin con NFC y `Locale.ROOT`. SQLite no puede normalizar caracteres no ASCII, ni con `COLLATE NOCASE` ni con `lower()`, así que la normalización se hace en el repositorio y la base de datos aplica el mismo criterio. `nameKey` es derivada y no entra en el JSON de backup.

La convención de nombres en la BD es camelCase, sin `@ColumnInfo` (D26, cerrada en la cuarta revisión). El esquema se exporta con `exportSchema = true` y los JSON de esquema versionados en `schemas/`.

Los TypeConverters (D62, cerrada en la cuarta revisión): `sessionDate` en TEXT con fecha ISO, `closedBy` en TEXT con enum `ClosedBy`, y `Plan.days` parseado en el modelo de dominio.

### Diagrama de entidades

```mermaid
erDiagram
    Category {
        int id PK
        string name UK
        string nameKey UK "normalizado en Kotlin"
    }

    Exercise {
        int id PK
        string name UK
        string nameKey UK "normalizado en Kotlin"
        string? notes
        bool archived
        int categoryId FK "SET NULL"
    }

    Plan {
        int id PK
        string? title
        string days "texto de días"
        int? sequence
    }

    PlanExercise {
        int id PK
        int planId FK "CASCADE, UK(planId,exerciseId)"
        int exerciseId FK "CASCADE, UK(planId,exerciseId)"
        int sequence
        int? maxSets
        int? warmupSets
        int? targetRpe "1..10, orientativo"
        bool enabled
    }

    Workout {
        int id PK
        long startedAt
        string sessionDate "TEXT ISO, inmutable, zona de apertura"
        long? endedAt
        string closedBy "TEXT, auto o explicit"
        int planId FK "SET NULL, nullable"
    }

    ExerciseSet {
        int id PK
        int exerciseId FK "CASCADE"
        int workoutId FK "NOT NULL, CASCADE"
        long timestamp
        int? reps
        double? loadKg
        string? notes
        bool isWarmup
    }

    Category ||--o{ Exercise : "categoriza (SET NULL)"
    Plan ||--o{ PlanExercise : "contiene (CASCADE)"
    Exercise ||--o{ PlanExercise : "parte de (CASCADE)"
    Plan ||--o{ Workout : "origina (SET NULL)"
    Exercise ||--o{ ExerciseSet : "tiene series (CASCADE)"
    Workout ||--o{ ExerciseSet : "agrupa (CASCADE)"
```

### Conceptos clave de diseño

- **Ejercicio ≠ serie.** Un ejercicio es una identidad/configuración reutilizable; las series son instancias con métricas. El historial y las gráficas se agrupan por ejercicio aunque el usuario cambie planes o parámetros.
- **Plan ≠ sesión.** El plan es la plantilla. La sesión (`Workout`) se abre de forma perezosa con la primera serie registrada y se cierra de forma explícita, o automática al cambiar de día. Toda serie pertenece a una sesión: si no hay plan, se crea una sesión libre.
- **Frontera de sesión:** una sesión por `(planId, día)`. La frontera impide fusionar días distintos. Dentro del mismo día, la sesión de mañana y la de tarde se fusionan si no se cierra, y eso se acepta como consecuencia del cierre explícito.
- **Unidades canónicas:** todo se guarda en kg. La conversión a lb es solo de display y se aplica globalmente desde Ajustes.

## Ajustes (DataStore) — campos iniciales

`themeMode`, `strengthUnit` (kg/lb, con kg por defecto), `longDateFormat`, `shortDateFormat`, `localeOverride?`, `maxSets`, `warmupSets?`, `showCategories`, `showNotes`, `showGlobalProgress`, `showUnits`, `inputStyle`, `curveLines`, `curveSmoothness?`, `defaultGraphMetric`, `defaultGraphPeriod`, `defaultGraphLimit`, `defaultTimeBasedXAxis`, `repEstimation`, `groupHistory`.

Los defaults de gráfica viven aquí en lugar de en la entidad `Exercise`, y el estado de la gráfica es local a la pantalla. No hay campo `tabs`: los destinos del bottom nav son fijos.

Métricas de gráfica viables: mejor peso, volumen (peso × reps), repeticiones totales y series por periodo.

**Enums (D37, cerrada en la quinta revisión):** `inputStyle` (stepper o teclado), `longDateFormat` (completa o ISO), `shortDateFormat` (dd/MM/yyyy, dd-MM-yyyy, MM/dd/yyyy), `defaultGraphLimit` entre 10 y 100 buckets, `curveSmoothness` entre 0 y 1. Los dos rangos se confirman. Se cierran ahora, antes de la iteración 10 que les corresponde, porque el DataStore se escribe en la iteración 2 y cerrarlos más tarde obligaría a una migración de ajustes.

**Versionado de claves (D46, cerrada en la quinta revisión):** entero de versión con una función de migración vacía, que se rellena cuando haya el primer cambio real de clave. Preferences no tiene esquema ni permite enumerar claves, así que la migración solo puede operar sobre un listado conocido, y la versión se lee antes de cualquier acceso. Se descarta reconstruir el `Settings` en lectura.

**`localeOverride` (D64, cerrada en la séptima revisión):** el selector tiene tres valores, `sistema` (default), `es` y `en`. `sistema` elige el idioma del sistema y, si no está soportado, cae en inglés; en el modelo `null` equivale a `sistema`. Se aplica de forma explícita en Compose y no depende de la resolución de recursos. Con `values/` en inglés, un dispositivo con idioma inglés muestra inglés aunque el usuario prefiera español.

**Defaults (D63, cerrada en la séptima revisión):** el default se aplica solo cuando el campo está sin asignar; un valor explícito del usuario gana. Los dos formatos de fecha se resuelven por idioma efectivo, tras `localeOverride`, y no por el idioma bruto del sistema.

| Campo | Default |
|---|---|
| `themeMode` | sistema |
| `strengthUnit` | kg |
| `longDateFormat` | completa en ambos idiomas (la convención de lectura; ISO queda como alternativa) |
| `shortDateFormat` | es → `dd/MM/yyyy`, en → `MM/dd/yyyy` |
| `localeOverride` | `sistema` (`null`), con `es` y `en` como valores explícitos |
| `inputStyle` | teclado |
| `maxSets` | sin asignar |
| `warmupSets` | sin asignar |
| `showCategories` | sí |
| `showNotes` | sí |
| `showGlobalProgress` | sí |
| `showUnits` | sí |
| `curveLines` | sí |
| `curveSmoothness` | 0,5 |
| `defaultGraphMetric` | volumen |
| `defaultGraphPeriod` | semana |
| `defaultGraphLimit` | 10 |
| `defaultTimeBasedXAxis` | sí |
| `repEstimation` | sí |
| `groupHistory` | agrupado |

## Formato propio de datos (v1)

- **Backup completo:** JSON con `{ "app": "iron", "version": 1, "exportedAt": ..., "categories": [...], "exercises": [...], "plans": [...], "planExercises": [...], "workouts": [...], "sets": [...] }`, opcionalmente GZIP. Cada objeto de `planExercises` incluye `targetRpe?` (nullable) como parte de la entidad, `workouts` incluye `sessionDate` y `closedBy`, y `sets` incluye `isWarmup`. Los ajustes no entran (D13, cerrada en la sexta revisión): se acepta perderlos al migrar de dispositivo. El importador rechaza un fichero cuyo campo `app` no es `iron` (D14, cerrada en la sexta revisión).
- **Convenciones** (D16, cerrada en la sexta revisión): camelCase, nulls escritos como `null` y no omitidos, `exportedAt` en ISO 8601 con zona, timestamps de serie en epoch millis UTC. La serialización usa DTOs separados de las entidades, y los DTOs excluyen `nameKey`, que es derivada. Escribir nulls en lugar de omitirlos hace que un lector no tenga que distinguir entre campo ausente y campo nulo.
- **CSV de gráficas:** columnas `timestamp,exercise,metric,value` (métrica según selección), UTF-8 con BOM. Exporta la ventana actual de la pantalla, y una ventana vacía exporta cabeceras sin filas (D15, cerrada en la sexta revisión). Respeta la exclusión del calentamiento de D9.

El procedimiento de importación (fusión, colisiones, backup previo) está en `docs/spec/rules.md`.

## Localización

- Base `values/` en inglés y `values-es` en español (D36). Añadir un idioma es añadir una carpeta `values-XX`.
- Todas las cadenas en `strings.xml` desde el día 1.
- Los conteos de las confirmaciones usan `plurals.xml`, con reglas distintas en cada idioma. En español la cantidad `many` es obligatoria (Lint `MissingQuantity`).
- `localeOverride` se aplica de forma explícita en Compose y no depende de la resolución de recursos (D64).
