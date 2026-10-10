<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Flexify — Análisis a Alto Nivel

Análisis del proyecto [Flexify](https://github.com/brandonp2412/Flexify) para servir de referencia al diseñar una aplicación similar con funcionalidades propias.

## Qué es

App de Flutter (multiplataforma: Android, iOS, Web, escritorio) para **registrar sesiones de gimnasio**. Es offline-first y privada: todos los datos viven en una base de datos local (Drift/SQLite), sin backend ni cuenta de usuario.

## Funcionalidades principales

| Área | Qué hace |
|---|---|
| **Planes de entrenamiento** | Crear planes con días de la semana, ejercicios y parámetros por ejercicio (series máximas, series de calentamiento, orden). Se puede "iniciar" un plan para registrar una sesión. |
| **Registro de series** | Registrar cada serie: peso, repeticiones, duración (cardio), distancia, pendiente, peso corporal, notas. Soporta fuerza y cardio con unidades configurables (kg/lb/stone, m/km/mi). |
| **Historial** | Listado cronológico de todas las series registradas, con filtros por ejercicio, plan, categoría, fecha. |
| **Gráficas** | Evolución por ejercicio: mejor peso, volumen, RPM, etc., agrupada por día/semana/mes/año. También gráfica global de progreso y de peso corporal. |
| **Temporizador de descanso** | Timer con sonido, vibración, notificaciones locales, pantalla siempre encendida. Se integra en el flujo de entrenamiento. |
| **Peso corporal** | Registro puntual de peso con foto opcional. |
| **Categorías** | Clasificación de ejercicios (ej: pecho, espalda) con gestión (crear, renombrar, fusionar, borrar). |
| **Importación/Exportación** | Export CSV de gráficas, backup completo de la base de datos (archivo), importación desde esos formatos. Copia automática opcional. |
| **Ajustes** | Tema claro/oscuro, unidades, formato de fechas, pestañas visibles, estimación de 1RM/reps, notificaciones, etc. |
| **Catálogo de ejercicios** | Lista de ejercicios conocidos (semilla) que se puede extender con los propios del usuario; cada ejercicio tiene imagen, categoría, descanso por defecto y configuración de gráfica. |

## Modelo de datos

8 tablas principales:

```
Categories ──< Exercises
                   │
                   ├──< PlanExercises >── Plans
                   │                        │
                   │                        │ (1 plan = 1 sesión activa)
                   │                        v
                   └──────────────> Workouts
                                        │
                                        v
                                  ExerciseSets
```

### Tablas y relaciones

| Tabla | Rol | Claves/relaciones |
|---|---|---|
| `categories` | Nombres reutilizables para clasificar ejercicios | `id`, `name` (unique) |
| `exercises` | **Identidad estable** del ejercicio: nombre, tipo (`strength`/`cardio`), unidad de display, imagen, descanso por defecto, config. de gráfica, archivado | `categoryId → categories.id` (nullable, ON DELETE SET NULL); `name` unique |
| `plans` | Un plan = una plantilla semanal | `days` (texto con días), `title`, `sequence` |
| `plan_exercises` | **Tabla puente** plan↔ejercicio con parámetros específicos del plan: orden (`sequence`), series máx, series de calentamiento, timers activados, habilitado | `planId → plans.id`, `exerciseId → exercises.id` (cascade); unique `(planId, exerciseId)` |
| `workouts` | Una **sesión concreta** iniciada (opcionalmente desde un plan) | `planId → plans.id` (nullable, SET NULL); índice único: solo una sesión activa por plan (`ended_at IS NULL`) |
| `exercise_sets` | **El dato central**: cada serie realizada | `exerciseId → exercises.id`, `workoutId → workouts.id` (nullable, SET NULL); métricas en unidades canónicas (kg, metros, ms) |
| `body_weights` | Mediciones de peso corporal con timestamp y foto opcional | independiente |
| `settings` | Fila única de preferencias del usuario (~45 campos) | sin relaciones |

### Conceptos clave de diseño

- **Ejercicio ≠ serie.** Un ejercicio es una identidad/configuración reutilizable; las series son instancias con métricas. Esto permite que el historial y las gráficas se agrupen por ejercicio aunque el usuario cambie planes o parámetros.
- **Plan ≠ sesión.** El plan es la plantilla; al "iniciarlo" se crea un `workout` que agrupa las series de esa sesión. Las series pueden existir sin workout (registro manual).
- **Unidades canónicas:** todo se guarda en kg/metros/milisegundos y se convierte a la unidad de display del ejercicio al mostrar.

## Flujo principal de uso

1. El usuario crea un **plan** con días y ejercicios (con parámetros por ejercicio).
2. Al entrenar, inicia el plan → se crea un **workout** activo.
3. Registra series (peso/reps/duración) que se vinculan al workout y al ejercicio.
4. El temporizador de descanso acompaña entre series.
5. Cierra la sesión → `ended_at` se rellena.
6. Ve historial y gráficas de progreso por ejercicio.

## Lo que NO tiene (candidatos a funcionalidades extra)

- Sin cuenta/autenticación ni sincronización en la nube
- Sin integración con wearables/sensores (Fitbit, Apple Health, etc.)
- Sin comunidad/social
- Sin programación de ejercicios por volumen progresivo automático
- Sin modo multiusuario/hogar compartido

## Referencias al código

| Área | Ubicación |
|---|---|
| Definición del esquema | `lib/database/` (una clase Drift por tabla) |
| Catálogo de ejercicios | `lib/database/exercise_catalog.dart` |
| Repositorio de series y conversiones de unidades | `lib/database/exercise_set_repository.dart` |
| Analítica/gráficas | `lib/database/exercise_analytics.dart`, `lib/graph/` |
| Planes y sesiones | `lib/plan/` |
| Historial | `lib/sets/` |
| Temporizador | `lib/timer/` |
| Import/export y backups | `lib/import_data.dart`, `lib/export_data.dart`, `lib/settings/backup_archive.dart` |
| Ajustes | `lib/settings/` |
