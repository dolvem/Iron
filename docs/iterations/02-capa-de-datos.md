<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 2 — Capa de datos (cerrada)

Bloque de `docs/state/todo.md`. Decisiones del bloque: D4, D5, D6, D7, D8, D10, D17, D18, D26, D46 y D62.

## Room: entidades y esquema

- [x] Crear las 6 entidades en `data/db/`: `Category`, `Exercise`, `Plan`, `PlanExercise`, `Workout`, `ExerciseSet`, en camelCase y sin `@ColumnInfo` (D26).
- [x] Índices únicos en `nameKey` de `Category` y `Exercise` (D4).
- [x] `unique(planId, exerciseId)` en `PlanExercise`.
- [x] Índice compuesto `(planId, endedAt)` en `Workout`, para la restricción de una sesión activa por plan.
- [x] FKs con las acciones correctas: `SET NULL` en `Exercise.categoryId` y `Workout.planId`; `CASCADE` en `PlanExercise` y `ExerciseSet`; `workoutId` NOT NULL.
- [x] `exportSchema = true` y los JSON de esquema versionados en `schemas/` (D48).

## TypeConverters (D62)

- [x] `sessionDate` en TEXT con fecha ISO.
- [x] `closedBy` en TEXT con enum `ClosedBy` y su conversor.
- [x] `Plan.days` se queda como TEXT CSV; el parseo se hace en el modelo de dominio, no en el conversor.

## Modelo de dominio (`domain/model/`)

- [x] Función única de normalización de `nameKey` con NFC y `Locale.ROOT`, usada por el insert, el renombrar y la búsqueda (D4).
- [x] Conversión kg↔lb con 2 decimales, medio arriba (D10).
- [x] 1RM con Epley (`peso / (1 + reps/30)`) con comentario de referencia, solo `reps >= 1`, redondeo a 0,5 kg al mostrar.
- [x] Métricas: volumen (`loadKg × reps`, excluye `reps` nulo y calentamiento), PR como mejor peso, repeticiones totales, series por periodo (D9).
- [x] Semana ISO 8601, lunes como primer día, anclada a la zona local.
- [x] Parseo de `Plan.days`: CSV ISO 1–7 ordenado, cadena vacía significa "nunca".

## Modelo de errores (D5)

- [x] `Outcome` sellado con los errores de negocio esperables: nombre duplicado, sesión cerrada, plan con sesión abierta, serie sin métrica.
- [x] Capturar la violación de único en el repositorio y mapearla a error de negocio.
- [x] Preconsulta de conteo como método separado del borrado, para la confirmación con conteo de afectados.

## Repositorios (`data/repo/`)

- [x] Crear `ExerciseRepo`, `PlanRepo`, `WorkoutRepo`, `SetRepo` y `Analytics`.
- [x] Validación de métricas en el repositorio antes del `CASCADE`: `reps >= 1`, `loadKg > 0`, al menos una métrica, sin techo (D6).
- [x] Apertura perezosa de sesión con la primera serie; `startedAt` y `sessionDate` fijados en ese momento, en la zona local de apertura (D3).
- [x] Frontera de día: una serie después de medianoche cierra la sesión de la víspera y abre la del día en la misma transacción del insert (D1).
- [x] Una sesión activa por plan, extendida a una sesión libre abierta por día con `planId IS NULL`, con validación transaccional antes de crear la sesión.
- [x] Cierre explícito dentro del día y automático al cambiar de día, distinguidos por `closedBy` (D1).
- [x] `endedAt` del cierre automático fijado en el momento del cierre, no en el timestamp de la serie que lo provoca (D1).
- [x] `isWarmup` fijado en el insert por posición, no se recalcula al borrar o editar (D2).
- [x] Borrado de series: una sesión abierta se elimina al borrar su última serie; una cerrada vacía se conserva (D39).
- [x] Las lecturas devuelven Flow directo, sin `Outcome` (D5).

## Contenedor de dependencias

- [x] `AppContainer` instanciado en `IronApp`, con `IronDatabase`, `SettingsDataStore` y los repositorios.
- [x] `Clock` (`Clock.systemUTC()`) y `ZoneId` (`ZoneId.systemDefault()`) creados en `AppContainer` e inyectados por constructor en los repositorios y en `Analytics` (D17).

## DataStore (`data/settings/`)

- [x] Modelo `Settings` con los campos iniciales de `docs/spec/data.md`.
- [x] Defaults aplicados solo cuando el campo está sin asignar, con los formatos de fecha por idioma efectivo (D63) y `localeOverride` con `sistema` (`null`), `es` y `en` (D64).
- [x] Versionado de claves: entero de versión con función de migración vacía, versión leída antes de cualquier acceso, migración solo sobre un listado conocido de claves (D46).
- [x] Conectar `ThemeSource` a `SettingsDataStore`, sustituyendo `FixedThemeSource` de la iteración 1 (D65).

## Migraciones

- [x] Framework de migraciones desde v1, testeable con los JSON de esquema.
- [x] Sin `fallbackToDestructiveMigration` en builds de release.

## Tests de la iteración 2 (matriz D18)

- [x] Conversión kg y lb: ida y vuelta, redondeo a 2 decimales medio arriba, 0,5 kg en 1RM (JUnit4).
- [x] Epley: `reps >= 1`, series sin `reps` omitidas, redondeo (JUnit4).
- [x] Volumen: series con `reps` nulo excluidas, series con `isWarmup` excluidas (JUnit4).
- [x] Frontera de día: medianoche local, sesión que cruza medianoche, cambio de zona (JUnit4 con `Clock` y `ZoneId`).
- [x] Semana ISO: lunes como primer día, semanas 52 y 53, año bisiesto (JUnit4).
- [x] Ventana actual vacía: fallback a la última semana con datos y ventana etiquetada (JUnit4 con `Clock` y `ZoneId`).
- [x] Frontera de sesión: apertura perezosa, cierre al cambiar de día, quick log reutiliza la sesión libre (Room en memoria).
- [x] Sesión activa por plan: intento de crear una segunda sesión abierta para el mismo plan (Room en memoria).
- [x] Sesión libre por día: intento de crear una segunda sesión libre abierta para el mismo día (Room en memoria).
- [x] `closedBy`: cierre automático distinguido del explícito (Room en memoria).
- [x] `isWarmup`: valor automático por posición, no se recalcula al borrar (Room en memoria).
- [x] Nombres únicos: `Sentadilla` frente a `sentadilla` y `Péculo` frente a `péculo`, sobre `nameKey` (Room en memoria).
- [x] Validación de métricas: `reps = 0`, `loadKg = 0` y serie sin métrica, rechazados antes del `CASCADE` (Room en memoria).
- [x] Preconsulta de conteo antes del borrado de un ejercicio con series (Room en memoria).
- [x] Migración de claves de DataStore: versión leída antes del primer acceso, sobre un listado conocido (JUnit4).
- [x] Repositorios con Flow: emisiones y cancelación (Turbine).

## Notas del bloque

- El mapeo y la preconsulta solo se pueden ejercitar sobre un repositorio, así que el bloque arrastra el primer DAO y el primer repositorio: `ExerciseDao` y `ExerciseRepo`. `SetDao` y `WorkoutDao` están solo con el insert, porque una serie no se puede insertar sin sesión (FK con CASCADE a Workout). Las dos variantes de rechazo de D6 (`InvalidReps` e `InvalidLoad`) están declaradas en `Outcome` y se ejercitan con `SetRepo`, en el bloque de repositorios.
- En `Outcome` se añaden dos variantes que el plan de tareas no listaba, porque D6 exige rechazar `reps = 0` y `loadKg = 0` con el mismo tipo de retorno.
- La detección de la violación de único mira el mensaje, no el código de error: `androidx.sqlite.SQLiteException` es un typealias de `android.database.sqlite.SQLiteException` en Android, y Robolectric puede no incluir el código 2067 en el mensaje. Por eso la función es pura y se testea con JUnit4.
- Robolectric 4.17 sobre JDK 25 necesita `--add-exports java.base/jdk.internal.access=ALL-UNNAMED` en las tareas de test, porque su interceptor de `FileDescriptor` reflexiona sobre `jdk.internal.access.SharedSecrets`. El driver por defecto de Room 3 es `AndroidSQLiteDriver`, que es el que Robolectric shadowea, así que el builder de la BD en memoria no fija driver.
- Tests del bloque: `ConstraintMappingTest` (JUnit4, puro) y `ExerciseRepoTest` (Room en memoria, cuatro casos: duplicado con acentos, renombrar a un nombre existente, renombrar a un nombre nuevo, preconsulta antes del `CASCADE`).
