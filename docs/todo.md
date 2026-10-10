<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# TODO: tareas por iteración

Parte de `docs/README.md`. Cada iteración es un bloque de tareas individuales. Las referencias D son la única referencia cruzada; el estado de cada decisión está en `docs/decisions_plan.md`.

El criterio de "listo" de `docs/README.md` se aplica a cada iteración: build limpio, tests de la lógica nueva, flujo navegable en emulador y accesibilidad verificada en emulador.

Las iteraciones 0 y 1 están cerradas y se marcan como completadas.

## Iteración 0 — Entorno (cerrada)

- [x] JDK 25 fijado en `.tool-versions`.
- [x] SDK en el SSD con `cmdline-tools/latest`, `platforms;android-37.1`, `build-tools;36.0.0` y `build-tools;34.0.0` (la que firma, D55).
- [x] AVD `iron_api31` con perfil Pixel 6, imagen Google APIs x86_64 (arm64 solo en el dispositivo físico, D61).
- [x] `local.properties` con `sdk.dir`, `ANDROID_HOME` y `ANDROID_AVD_HOME` exportados.
- [x] KVM activo: `kvm_amd` carga, `/dev/kvm` existe, usuario en el grupo `kvm`, AVD arranca con aceleración hardware.

## Iteración 1 — Scaffolding (cerrada)

- [x] Proyecto mono-módulo `app`, Kotlin DSL, catálogo `libs.versions.toml` con versiones fijadas (D20).
- [x] Material You (dynamic color), tema claro/oscuro/sistema con `ThemeMode` y `ThemeSource` fijo (D65).
- [x] Bottom nav con los 4 destinos (Hoy, Planes, Ejercicios, Historial) y Ajustes como ruta desde la cabecera de Hoy.
- [x] `git init` y `.gitignore` (D22, D48).
- [x] `LICENSE` GPL-3.0-or-later con la carpeta `LICENSES/` de REUSE y cabecera SPDX en cada fichero (D21, D57).
- [x] `README.md` de la raíz enlazando a `docs/README.md`.
- [x] `.editorconfig` con `max_line_length = 120` y ktlint-gradle con `code_style = android_studio` (D27, D50).
- [x] Android Lint con warnings como error, `lint-baseline.xml` vacío y `lint.xml` (D51).
- [x] Icono de aplicación: adaptive icon con fondo `#1B1B1D`, foreground vectorial y capa monochrome (D25, D60).
- [x] Orientación bloqueada en vertical, regla `LockedOrientationActivity` desactivada (D38).
- [x] Idioma base en inglés: `values/` en inglés y `values-es` en español (D36).
- [x] Versionado `iron.version` en `gradle.properties` con `versionCode` semver (D23).
- [x] CI: job único `check` en `ubuntu-latest` con ktlint, Lint, test y build (D49).

## Iteración 2 — Capa de datos

### Room: entidades y esquema

- [x] Crear las 6 entidades en `data/db/`: `Category`, `Exercise`, `Plan`, `PlanExercise`, `Workout`, `ExerciseSet`, en camelCase y sin `@ColumnInfo` (D26).
- [x] Índices únicos en `nameKey` de `Category` y `Exercise` (D4).
- [x] `unique(planId, exerciseId)` en `PlanExercise`.
- [x] Índice compuesto `(planId, endedAt)` en `Workout`, para la restricción de una sesión activa por plan.
- [x] FKs con las acciones correctas: `SET NULL` en `Exercise.categoryId` y `Workout.planId`; `CASCADE` en `PlanExercise` y `ExerciseSet`; `workoutId` NOT NULL.
- [x] `exportSchema = true` y los JSON de esquema versionados en `schemas/` (D48).

### TypeConverters (D62)

Los dos conversores se escriben con las entidades, porque Room no compila una columna de estos tipos sin ellos.

- [x] `sessionDate` en TEXT con fecha ISO.
- [x] `closedBy` en TEXT con enum `ClosedBy` y su conversor.
- [x] `Plan.days` se queda como TEXT CSV; el parseo se hace en el modelo de dominio, no en el conversor.

### Modelo de dominio (`domain/model/`)

- [x] Función única de normalización de `nameKey` con NFC y `Locale.ROOT`, usada por el insert, el renombrar y la búsqueda (D4).
- [x] Conversión kg↔lb con 2 decimales, medio arriba (D10).
- [x] 1RM con Epley (`peso / (1 + reps/30)`) con comentario de referencia, solo `reps >= 1`, redondeo a 0,5 kg al mostrar.
- [x] Métricas: volumen (`loadKg × reps`, excluye `reps` nulo y calentamiento), PR como mejor peso, repeticiones totales, series por periodo (D9).
- [x] Semana ISO 8601, lunes como primer día, anclada a la zona local.
- [x] Parseo de `Plan.days`: CSV ISO 1–7 ordenado, cadena vacía significa "nunca".

### Modelo de errores (D5)

- [x] `Outcome` sellado con los errores de negocio esperables: nombre duplicado, sesión cerrada, plan con sesión abierta, serie sin métrica.
- [x] Capturar la violación de único en el repositorio y mapearla a error de negocio.
- [x] Preconsulta de conteo como método separado del borrado, para la confirmación con conteo de afectados.

El mapeo y la preconsulta solo se pueden ejercitar sobre un repositorio, así que el bloque arrastra el primer DAO y el primer repositorio: `ExerciseDao` y `ExerciseRepo`. `SetDao` y `WorkoutDao` están solo con el insert, porque una serie no se puede insertar sin sesión (FK con CASCADE a Workout). Las dos variantes de rechazo de D6 (`InvalidReps` e `InvalidLoad`) están declaradas en `Outcome` y se ejercitan con `SetRepo`, en el bloque de repositorios.

En `Outcome` se añaden dos variantes que el TODO no listaba, porque D6 exige rechazar `reps = 0` y `loadKg = 0` con el mismo tipo de retorno.

La detección de la violación de único mira el mensaje, no el código de error: `androidx.sqlite.SQLiteException` es un typealias de `android.database.sqlite.SQLiteException` en Android, y Robolectric puede no incluir el código 2067 en el mensaje. Por eso la función es pura y se testea con JUnit4.

Robolectric 4.17 sobre JDK 25 necesita `--add-exports java.base/jdk.internal.access=ALL-UNNAMED` en las tareas de test, porque su interceptor de `FileDescriptor` reflexiona sobre `jdk.internal.access.SharedSecrets`. El driver por defecto de Room 3 es `AndroidSQLiteDriver`, que es el que Robolectric shadowea, así que el builder de la BD en memoria no fija driver.

Tests del bloque: `ConstraintMappingTest` (JUnit4, puro) y `ExerciseRepoTest` (Room en memoria, cuatro casos: duplicado con acentos, renombrar a un nombre existente, renombrar a un nombre nuevo, preconsulta antes del `CASCADE`).

### Repositorios (`data/repo/`)

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

### Contenedor de dependencias

- [x] `AppContainer` instanciado en `IronApp`, con `IronDatabase`, `SettingsDataStore` y los repositorios.
- [x] `Clock` (`Clock.systemUTC()`) y `ZoneId` (`ZoneId.systemDefault()`) creados en `AppContainer` e inyectados por constructor en los repositorios y en `Analytics` (D17).

### DataStore (`data/settings/`)

- [x] Modelo `Settings` con los campos iniciales de `docs/data_plan.md`.
- [x] Defaults aplicados solo cuando el campo está sin asignar, con los formatos de fecha por idioma efectivo (D63) y `localeOverride` con `sistema` (`null`), `es` y `en` (D64).
- [x] Versionado de claves: entero de versión con función de migración vacía, versión leída antes de cualquier acceso, migración solo sobre un listado conocido de claves (D46).
- [x] Conectar `ThemeSource` a `SettingsDataStore`, sustituyendo `FixedThemeSource` de la iteración 1 (D65).

### Migraciones

- [x] Framework de migraciones desde v1, testeable con los JSON de esquema.
- [x] Sin `fallbackToDestructiveMigration` en builds de release.

### Tests de la iteración 2 (matriz D18)

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

## Iteración 3 — Ejercicios (cerrada)

- [x] CRUD de ejercicios: crear, renombrar, borrar, con confirmación y conteo de series afectadas en el borrado.
- [x] CRUD de categorías: crear, renombrar, fusionar, borrar (`SET NULL` en los ejercicios).
- [x] Fusión de ejercicios y categorías: el usuario elige el destino, sobrevive su nombre, se remapean las FKs, en `archived` y `notes` gana el menos restrictivo con las notas concatenadas (D32).
- [x] Archivado: excluidos de la lista por defecto, con filtro para mostrarlos, y sus métricas siguen disponibles en el detalle (D32).
- [x] Pantalla Ejercicios: lista optimizada para filtrar y acceder, ordenada por nombre, `LazyColumn` con `key`.
- [x] Barra de filtros con caja de búsqueda case-insensitive que usa `nameKey` (D31); las categorías se gestionan desde ahí, como sheet, no como ruta.
- [x] Pantalla EjercicioDetalle: identidad (nombre, categoría, notas), últimos registros, estadísticas (PR, valor actual, volumen semanal).
- [x] Quick log FAB desde EjercicioDetalle: preselecciona ese ejercicio.
- [x] ViewModel con `StateFlow` leyendo el repositorio desde `AppContainer`.

### Notas del bloque

- Tests del bloque: `MergeTest` (reglas puras D32), `CategoryRepoTest` (borrado con `SET NULL` y
  fusión con `origin && destination`), `ExerciseMergeTest` (remapeo de series, conflictos de
  `plan_exercises` contados antes de reasignar, `origin && destination`), `ExerciseSearchTest`
  (prefijo `nameKey` y filtro por categoría).
- `AnalyticsTest` gana la fila de `windowBestWeight` (ventana de 30 días, D7).
- Archivado, dividido: se marca la exclusión de la lista por defecto, verificada por test y en emulador (la
  fila archivada desaparece, y aparece con el filtro con la etiqueta `Archived`). La inclusión en el
  historial se mueve a la iteración 7 y la de las gráficas del detalle a la iteración 8, que son donde se
  escriben esas pantallas. La mitad de datos queda fijada por `AnalyticsTest.archivedExerciseStillProducesMetrics`:
  `Analytics` lee por `exerciseId` sin mirar `archived`, así que el detalle de un archivado conserva PR,
  valor actual y volumen semanal. Pendiente de verificar en emulador: la etiqueta `Archived` en la
  identidad del detalle, que hoy solo se pinta en la fila de la lista.
- `FlowTest.cancellingTheCollectorStopsEmissions` fallaba de forma intermitente: Turbine arranca el
  colector antes del insert, y Room lanza `IllegalStateException` ("The query result was empty, but
  expected a single row") sobre un `Flow<Exercise>` con tabla vacía. Se inserta la fila antes de
  `test {}`; cuatro ejecuciones con `--rerun-tasks` limpias.
- Verificado en emulador (AVD `iron_api31`, densidad 420, `uiautomator dump` y capturas): flujo
  navegable Ejercicios → detalle → quick log; targets medidos de 48 dp en el icono de crear, el icono de
  acciones de fila y los `FilterChip` (el chip visual es de 32 dp, el contenedor clicable llega a 48),
  fila 64 dp, bottom nav 97x80 dp; `contentDescription` en los iconos; escala de fuente 1,3 sin recortes;
  dynamic color activo. Los iconos del bottom nav no muestran content-desc en el árbol de accesibilidad:
  Compose lo fusiona con la etiqueta del item, que es el nombre accesible.
- Dos defectos encontrados en esa pasada y corregidos:
  - El orden por nombre usaba `ORDER BY name`, que con la colación BINARY de SQLite pone `Zapada` antes
    que `gemelo`. Los cuatro queries (tres de `ExerciseDao` y uno de `CategoryDao`) ordenan ahora por
    `nameKey`. Tests: `ExerciseSearchTest.orderUsesNameKeySoItIsCaseInsensitive` y
    `CategoryRepoTest.orderUsesNameKeySoItIsCaseInsensitive`.
  - `IronNavHost` leía `exerciseId` con `getInt` sin declarar `navArgument`: la ruta lo entrega como
    String y devolvía 0, así que el detalle siempre caía en el estado vacío y el FAB no llegaba a
    pintarse. Se declara con `NavType.IntType`.
- El error de D6 en el quick log se muestra dentro del sheet, no en el snackbar de la pantalla: el sheet
  tapa el `SnackbarHost` y el editor se queda abierto tras el rechazo (D30), así que el mensaje no llegaba
  a verse. El `SnackbarHost` del detalle se quita porque no tenía otro uso, y el mensaje se limpia al
  guardar una serie válida. El texto lleva `liveRegion = Polite` para que se anuncie al aparecer, porque
  `Role` no tiene variante `Error` en Compose 1.12.
- El quick log se verificó en emulador: serie con solo `reps` y serie con peso y `reps`. La coma se
  normaliza a punto al guardar (D28), se muestran 2 decimales (D10), la serie sin `loadKg` queda fuera
  del volumen (D9), y el orden de los últimos registros es descendente.
- Las gráficas de EjercicioDetalle no se escriben en esta iteración: pertenecen a la iteración 8
  (`feature/graph/`, D35), y ahí está el checkbox que las cubre, con los archivados incluidos.
- Quick log: solo el FAB y el sheet mínimo de registro. El editor completo (D28–D30) es la iteración 5.
- Lint Compose obliga a `LocalResources.current` en lugar de `LocalContext.current.getString` y a
  `LocalLocale.current.platformLocale` en lugar de `Locale.getDefault()`. `outcomeMessage` recibe
  `Resources` por eso.
- `plurals.xml` en español necesita la cantidad `many` (`MissingQuantity`);
  `error_plan_has_open_session` se reescribió para evitar `PluralsCandidate`.
- `viewModelFactory` e `initializer` están en `androidx.lifecycle.viewmodel` (lifecycle 2.11), no en
  `androidx.lifecycle.viewmodel.compose`; se añaden `lifecycle-runtime-compose` y
  `lifecycle-viewmodel-compose` al `libs.versions.toml` (D20).
- El diálogo de creación no tenía disparador: `createOpen` se cerraba pero nunca se abría, así que el
  CRUD de crear era inalcanzable desde la UI. Se añade un `IconButton` en la barra de filtros con
  `ic_add.xml` y `contentDescription` (`exercises_create_action` en los dos idiomas). La lista es de
  filtrar y acceder, así que la acción es pequeña y no un FAB (views_plan.md). El nombre se envía con
  la categoría del filtro activo.
- Los candidatos de fusión se leen con la variante de la búsqueda que muestra `archived`, no con la
  lista filtrada: si el destino archivado no fuera alcanzable, la regla del menos restrictivo de D32 no
  se podría aplicar nunca. Se lee con query vacía, porque el filtro de la lista no debe limitar el
  destino que el usuario elige. Test: `ExerciseSearchTest.mergeCandidatesIgnoreTheQueryAndIncludeArchived`.
- El detalle es de solo lectura en esta iteración. `docs/views_plan.md` dice que EjercicioDetalle contiene
  el CRUD, pero el CRUD está solo en las filas de la lista. Decidido: el menú de acciones de la identidad
  se escribe en la iteración 11, y ahí está el checkbox.
- El filtro vive en el `MutableStateFlow` del ViewModel, así que se pierde con la muerte del proceso. La
  regla de `SavedStateHandle` está asignada a la iteración 11.
- Los ViewModel no tienen test: D18 y D19 dejan la UI fuera de la matriz, así que el bloque se verifica
  con las cuatro tareas de CI.
- REUSE: los ficheros que no admiten cabecera se cubren con los sidecar `.license`
  (`app/schemas/.../1.json.license` y `gradle/wrapper/gradle-wrapper.jar.license`). `reuse` no está
  instalado y el job `check` de CI no lo ejecuta, así que la comprobación es manual.

## Iteración 4 — Planes

- [ ] CRUD de planes y de `PlanExercise`: orden, `maxSets`, `warmupSets`, `targetRpe`, `enabled`.
- [ ] Frecuencia en el detalle: última sesión y sesiones en los últimos 30 días (ventana móvil desde hoy, 30 días naturales incluyendo hoy).
- [ ] Las sesiones libres no cuentan en la frecuencia ni en la adherencia de ningún plan (D8).
- [ ] Pantalla Planes: lista ordenada por `sequence` (nulos al final), con el plan de hoy resaltado.
- [ ] Pantalla PlanDetalle: solo configuración, bloqueada mientras hay una sesión abierta.
- [ ] `Plan.title` nulo se muestra como `Plan`.
- [ ] Borrado de plan: confirmación que avisa que las sesiones quedan como sesiones libres, con conteo de esas sesiones; bloqueado mientras haya una sesión abierta (D33).
- [ ] Quick log FAB desde PlanDetalle: preselecciona el ejercicio del plan.

## Iteración 5 — Sesión

- [ ] Pantalla SesiónDetalle: checklist de solo lectura derivado de las series frente a `maxSets`, progreso por ejercicio, volumen de la sesión, botón de cierre.
- [ ] Editor de serie compartido en `ui/components/`: se mantiene abierto tras registrar, foco al siguiente campo (D30).
- [ ] Entrada numérica: peso con teclado decimal con autofocus, `reps` entero con stepper de 1 a 100 y estado vacío (D30).
- [ ] Separador decimal: coma y punto normalizados a punto, solo en modo teclado; el redondeo ocurre al guardar (D28).
- [ ] Botones de placas como accesos rápidos de suma: kg 1,25 / 2,5 / 5 / 10 / 20 / 25; lb 2,5 / 5 / 10 / 25 / 35 / 45 (D29).
- [ ] El editor muestra el error que devuelve el repositorio por `Outcome`, no valida por su cuenta (D6).
- [ ] Campos editables de una serie: métricas, notas, `isWarmup` y `timestamp`; el ejercicio no (D42).
- [ ] Cierre explícito dentro del día; el botón de cierre se oculta con 0 series (D39).
- [ ] Deshacer en snackbar, ventana de 6 s, solo en series: recrea la sesión eliminada con el `id` y el `startedAt` originales, y revierte el cierre automático si la serie lo provocó (D40).
- [ ] `targetRpe` mostrado como referencia orientativa junto al ejercicio, sin marcarse por serie.
- [ ] Quick log: exige `reps` o `loadKg`, escribe en la sesión abierta o crea una sesión libre para hoy, siempre serie de trabajo.

## Iteración 6 — Hoy

- [ ] Bloque 1: plan de hoy (todos los planes que coinciden con `plan.days`), sesión abierta y aviso de hace cuántos días fue la última sesión.
- [ ] Bloque 2: progreso global, una sola gráfica (volumen semanal o 1RM estimado), con `defaultGraphMetric`.
- [ ] Ticker con `delayUntilMidnight` que recalcula la coincidencia al cambiar el día y provoca el cierre automático de D1; es una sola pieza (D34). Determinista en tests con `Clock` y `ZoneId`.
- [ ] Con la app muerta en segundo plano, se recalcula al relanzar.
- [ ] Estado vacío guiado en el primer arranque: crear ejercicio, crear plan.
- [ ] Quick log FAB contextual: desde Hoy preselecciona el último ejercicio usado.
- [ ] Semana ISO en curso para el volumen semanal, con hueco si no hay datos.

## Iteración 7 — Historial

- [ ] Vista principal: sesiones agrupadas por `sessionDate`, fecha descendente, cargadas de 30 sesiones por página.
- [ ] Detalle de sesión: sus series, editables y eliminables, con el editor de serie compartido.
- [ ] Modo lista plana de registros como filtro, no como vista principal.
- [ ] Filtros: ejercicio, plan, categoría, rango de fechas y sesiones libres (D45).
- [ ] Sesiones libres con título fijo y filtro propio.
- [ ] Cierre de sesión desde el detalle del historial; una sesión cerrada no se reabre (D43).
- [ ] El cierre automático se distingue del explícito en el historial (D44).
- [ ] Ejercicios archivados incluidos en el historial (regla de archivados, D32).
- [ ] Sesión cerrada con 0 series aparece con un estado vacío en el detalle (D39).

## Iteración 8 — Gráficas (Vico)

- [ ] Componentes de gráfica compartidos en `feature/graph/`, usados por Hoy y EjercicioDetalle.
- [ ] Gráficas en EjercicioDetalle, con los ejercicios archivados incluidos (regla de archivados, D32).
- [ ] Métricas: mejor peso, volumen, repeticiones totales, series por periodo. Cuatro, no más.
- [ ] Agrupación día/semana/mes/año; opciones de curva, límite (10 a 100 buckets, D37) y eje X temporal.
- [ ] Calentamiento excluido de todas las métricas (D9).
- [ ] Buckets sin datos: hueco en la serie temporal, cero en el conteo de series por periodo (D7).
- [ ] Alternativa textual accesible: sección plegable siempre disponible con la etiqueta del bucket y el valor de la métrica (D35), verificada a mano en emulador.

## Iteración 9 — Import/export propio

- [ ] Export CSV de gráficas: columnas `timestamp,exercise,metric,value`, UTF-8 con BOM, ventana actual de la pantalla, ventana vacía exporta cabeceras sin filas, respeta la exclusión del calentamiento (D15), por SAF.
- [ ] Backup completo: JSON/GZIP de la BD con formato v1, DTOs separados de las entidades, camelCase, nulls escritos como `null`, `exportedAt` en ISO 8601 con zona, timestamps en epoch millis UTC, DTOs excluyen `nameKey` (D16).
- [ ] Importación por fusión: dos pasadas con mapa `incomingId -> existingId`, raíz primero y dependientes después, gana el nombre existente, duplicados del fichero resueltos con la primera ocurrencia, colisión de ID con otro nombre recibe ID nuevo, `unique(planId, exerciseId)` gana la fila existente con conflicto en el resumen (D11).
- [ ] Import en una sola transacción, con backup automático previo en `filesDir/backups/`, retención de los 3 más recientes, GZIP, sin SAF, sin UI de restauración (D12).
- [ ] Versiones del formato: acepta `version <= current`, rechaza `app` distinto de `iron` y `version` mayor con error (D14).
- [ ] GZIP detectado por magic bytes, no por extensión.
- [ ] Los ajustes no entran en el backup (D13).

## Iteración 10 — Ajustes completos

- [ ] Pantalla de Ajustes: unidad de fuerza, formatos de fecha, defaults de gráfica, estimación 1RM/reps, idioma.
- [ ] Selector de tema en la UI, conectado a `ThemeSource` (D65).
- [ ] Selector de idioma: `sistema` (default), `es` y `en`, aplicado de forma explícita en Compose, con fallback a inglés si el idioma del sistema no está soportado (D64).
- [ ] Defaults de formatos de fecha por idioma efectivo, tras `localeOverride` (D63).
- [ ] Los `plurals.xml` con reglas distintas en cada idioma.

## Iteración 11 — Pulido

- [ ] Estados vacíos en todos los listados y pantallas.
- [ ] Validaciones: el error siempre viene del `Outcome` del repositorio, la UI no valida (D6).
- [ ] Accesibilidad verificada en emulador: targets táctiles de 48dp, `contentDescription` en iconos, escala de fuente, contraste con colores dinámicos, alternativa textual de las gráficas (D35).
- [ ] Filtros y selección de gráfica en `SavedStateHandle`; estado local de pantalla con `rememberSaveable`.
- [ ] `LazyColumn` con `key` en todos los listados.
- [ ] Menú de acciones en la identidad de EjercicioDetalle: renombrar, borrar y archivar (views_plan.md); el detalle pasa de solo lectura a editable.
- [ ] Tests unitarios pendientes de la matriz D18: undo que recrea una sesión eliminada al borrar su última serie (Room en memoria), import por fusión con colisión de nombre, colisión de ID con otro nombre y remapeo de FKs (Room en memoria).
- [ ] Build reproducible verificado con las seis reglas (D24, D56) y firma externa con `apksigner` de build-tools 34.0.0 (D55, D66).
