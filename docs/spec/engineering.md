<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Decisiones de ingeniería

Parte de `docs/README.md`. El estado de las decisiones (D1–D72) está en `docs/state/decisions.md`.

## Decisiones de ingeniería (cerradas)

- **Mono-módulo.** Un único módulo `app`, con paquetes por feature. Multi-módulo queda fuera de v1.
- **Inyección de dependencias:** contenedor manual. `IronApp` instancia `IronDatabase`, `SettingsDataStore` y `AppContainer` (repositorios). Los `ViewModel` se construyen con `viewModelFactory { initializer { } }` leyendo el contenedor desde la `Application`. Sin Hilt: evita un procesador de anotaciones adicional y acorta la curva.
- **Gradle:** Kotlin DSL y catálogo `gradle/libs.versions.toml`. Las versiones se fijan ahí, no en cada `dependency`.
- **JDK:** 25, con `jvmTarget` 17 (D52, D53, cerradas). AGP 9.x exige 17 como mínimo. El build server de F-Droid soporta 25 y 21 desde Debian Trixie, y 17 desde Bookworm, y advierte que las versiones que no son de la estable actual pueden dejar de estar disponibles. En este proyecto asdf solo ofrece `temurin-25.0.2+10.0.LTS`, así que se fija 25. La matriz de compatibilidad se confirma el 08/10/2026: Gradle 9.8.1 ejecuta el daemon sobre JVM 17–27 (Java 25 soportado para ejecutar Gradle desde 9.1.0), AGP 9.4 exige Gradle 9.6.0 y JDK 17 como mínimo sin tope superior, y Kotlin 2.3.20 soporta Java 25 desde 2.3.0 y exige Gradle ≥ 9.5.0. Las tres condiciones se cumplen, así que se mantiene 25 y no se baja a 21.
- **Fechas:** `java.time` es nativo desde API 26, así que con `minSdk` 31 no hace falta core library desugaring. Todo el cálculo de días, semanas ISO y agrupación de gráficas usa `java.time`, no `java.util.Date`.
- **Sin bifurcaciones por versión:** con `minSdk` 31 no hay `Build.VERSION.SDK_INT`, ni `@RequiresApi`, ni warnings de Lint por usar APIs por encima del mínimo. El código no se llena de comprobaciones de versión.
- **Testing:** JUnit4. Robolectric para código que toca APIs de Android, Room en memoria para DAOs, Turbine para Flow.
- **Room:** `exportSchema = true` y los JSON de esquema versionados en el repo, para poder testear migraciones. Nunca `fallbackToDestructiveMigration` en builds de release. Room no soporta índices parciales, así que la restricción de una sesión activa por plan se implementa con el índice compuesto `(planId, endedAt)` más la validación transaccional del repositorio; el cierre automático libera la restricción.
- **Calidad** (D27, D50, D51, cerradas): ktlint-gradle con `ktlint_code_style = android_studio` (el valor `android` de D50 no existe en ktlint 1.x), `.editorconfig` con `max_line_length = 120`, sin wildcard imports, UTF-8, newline final, indentación de 4 espacios, y `ktlint_function_naming_ignore_when_annotated_with = Composable` para permitir los composables en PascalCase. El plugin se aplica también en el módulo `app`, porque aplicado solo en la raíz no cubre los fuentes de los módulos. Android Lint con `abortOnError = true`, warnings como error, `checkReleaseBuilds = true`, `lint-baseline.xml` vacío desde la iteración 1, `lint.xml` para desactivar `LockedOrientationActivity` y `MissingTranslation` activo, más `DiscouragedApi` en el manifest (Android 16 ignora las orientaciones fijas), `NotShrinkingResources` (D56) y los tres avisos de versiones (`AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`), que no tienen sentido con un catálogo fijado por D20.
- **Distribución:** APK propio y F-Droid. Play Store queda fuera de v1. F-Droid exige build reproducible y dependencias libres de licencia privativa. Si algún día se publica en Play, el target mínimo exigido desde el 31/08/2026 es API 36, por eso el `targetSdk` se fija en 36 y no en 35. Con compileSdk 37, dejarlo en 36 activa `OldTargetApi` en Lint, así que `targetSdk` sube a 37.
- **Build reproducible** (D24, D56, cerradas): seis reglas. En AGP 9.4 el DSL ya no expone tres de ellas, así que el mapeo es este: `vcsInfo.include = false` en el build type de release (ya no vive en el bloque `android`); cruncher desactivado por propiedad `android.enableResourceOptimizations=false` (deprecated, pero funcional); densidades de vector drawables cumplidas de hecho, porque no hay PNG propio en el repo y `vectorDrawables` solo se expone en product flavors; APK determinista por propiedad `android.experimental.forceDeterministicApk=true`; tarea `ArtProfile` desactivada por nombre (`tasks.configureEach`), porque AGP 9.4 no la expone en el DSL y sí la crea; `shrinkResources = false` y `debuggable = false` en el build type. Minify con R8 sí. Firma v2 + v3, con el APK unsigned y la firma externa (D66). Con una app 100 % Kotlin y sin NDK el riesgo de no reproducibilidad es bajo, pero las seis reglas se escriben igual. Comprobado el 09/10/2026: dos builds de release consecutivos dan el mismo SHA-256.
- **Versionado** (D23, cerrada): `iron.version` en `gradle.properties` como punto de verdad único, `versionCode = major * 10000 + minor * 100 + patch`, `versionName` semver. `0.1.0` durante el desarrollo, `1.0.0` en el primer release.
- **Repositorio git** (D22, D47, D48, cerradas): GitHub público, porque GPL exige que el fuente esté disponible y el metadato de F-Droid apunta a la URL. `.gitignore` con `local.properties`, `build/`, `.idea/`, `.gradle/`, `*.jks`, `*.keystore`, `keystore.properties`, `*.apk` y `app/release/`. `schemas/` y `.tool-versions` se versionan.
- **Icono** (D25, D60, cerradas): adaptive icon con fondo `#1B1B1D`, foreground vectorial de paleta de trazo simple y capa monochrome. Sin PNG y sin densidades generadas.
- **Versiones a fijar en el catálogo** (D20, cerrada): Kotlin 2.3.20, KSP 2.3.10, Compose BOM 2026.09.00, Room 3.0.3, Vico 3.3.1, Robolectric 4.17, Turbine 1.2.1, Gradle 9.8.1, AGP 9.4.0. KSP ya no depende de la versión de Kotlin desde 2.3.0, pero se usa KSP2: KSP1 no es compatible con Kotlin 2.3.0+ ni con AGP 9.0+, y Room 3 lo exige.

## CI (D49, cerrada)

Un solo job `check` en `ubuntu-latest`, disparado en `push` y `pull_request`, con `ktlintCheck`, `lint`, `test` y `build`. JDK 25 con `actions/setup-java`, acciones fijadas por tag de major y caché de Gradle. CI no firma: genera el APK unsigned.

Con un módulo único, separar ktlint, Lint y tests en jobs paralelos no aporta nada.

## Modelo de errores (D5, cerrada en la cuarta revisión)

Los repositorios devuelven un `Outcome` sellado con los errores de negocio esperables (nombre duplicado, sesión cerrada, plan con sesión abierta, serie sin métrica). Se llama `Outcome` porque `Result` choca con `kotlin.Result`. Se usa solo en las operaciones con error de negocio esperado: borrados, renombrar, fusión, inserción de serie y cierre de sesión. Las lecturas devuelven Flow directo. La preconsulta de conteo es un método separado del borrado, y es lo que permite la confirmación con conteo de afectados.

Las excepciones se reservan para fallos de I/O y de base de datos, con una excepción: la violación de único se captura en el repositorio y se mapea al error de negocio, porque la comprobación previa no cubre la carrera entre la comprobación y el insert.

## Firmas de lectura de los DAOs (D67, cerrada)

Las consultas que pueden devolver varias filas usan `Flow<List<T>>`. `Flow<T>` queda solo para las consultas de una fila (`observeById`, `observeOpenForPlan`, `observeLastSessionForPlan`, `observeSessionCountFrom`), que son las que tienen sentido como fila única. Con `Flow<T>` sobre una consulta de varias filas, Room emite una fila por invalidación, así que el resto del resultado no llega a la pantalla.

## Recursos y locale en Compose (D72, cerrada)

En Compose se usa `LocalResources.current` en lugar de `LocalContext.current.getString`, y `LocalLocale.current.platformLocale` en lugar de `Locale.getDefault()`. No es solo un aviso de Lint: `Locale.getDefault()` no respeta `localeOverride` de D64, que se aplica de forma explícita en Compose. Las funciones que necesitan cadenas reciben `Resources` como parámetro.

## Estructura del proyecto (paquetes)

```
com.dolvem.iron
├── IronApp.kt, MainActivity.kt, theme/  # Material You, tema claro/oscuro/sistema
├── data/db/              # Room: entidades, DAOs, IronDatabase, conversores
├── data/settings/        # SettingsDataStore (tipo Settings con defaults)
├── data/repo/            # ExerciseRepo, PlanRepo, WorkoutRepo, SetRepo, Analytics
├── domain/model/         # modelos de dominio + conversiones de unidades (kg↔lb)
├── ui/navigation/        # Destination y IronNavHost: los cuatro destinos y las rutas de detalle
├── ui/components/        # editor de serie y FAB de quick log, compartidos entre pantallas
├── feature/today/        # Hoy: plan del día, sesión abierta, progreso global
├── feature/plans/        # CRUD planes + plan_exercises (orden, series máx, calentamiento, RPE)
├── feature/exercises/    # CRUD ejercicios + detalle con gráficas; categorías como filtro
├── feature/session/      # sesión activa: registrar series, progreso vs maxSets, cerrar sesión
├── feature/history/      # sesiones agrupadas + modo lista plana + filtros
├── feature/graph/        # Vico: componentes de gráfica compartidos (Hoy y EjercicioDetalle)
├── feature/quicklog/     # FAB contextual: registrar serie fuera del plan
├── feature/settings/     # tema, unidades, fechas, defaults de gráfica, estimación 1RM/reps
└── feature/data/         # export CSV gráficas, backup BD completo, import propio
```

## Testing

Para testear la frontera de día y la semana ISO hacen falta `Clock` y `ZoneId` inyectables (D17, cerrada en la quinta revisión). Se crean en `AppContainer` con `Clock.systemUTC()` y `ZoneId.systemDefault()` como defaults, y se inyectan por constructor en los repositorios y en `Analytics`. Sin ellos, esos tests no son deterministas, y hace falta los dos: `sessionDate` se fija en la zona de apertura, así que la frontera de día no es determinista solo con `Clock`.

El ticker que recalcula Hoy a medianoche (D34, cerrada en la sexta revisión) es el mismo que provoca el cierre automático de D1, así que hay una sola pieza. Con `Clock` y `ZoneId` inyectables es determinista en tests, y la matriz lo cubre en la fila de frontera de día.

| Regla | Caso que hay que cubrir | Herramienta |
|---|---|---|
| Conversión kg y lb | Ida y vuelta, redondeo a 2 decimales medio arriba, 0,5 kg en 1RM | JUnit4 |
| Epley | `reps >= 1`, series sin `reps` omitidas, redondeo | JUnit4 |
| Volumen | Series con `reps` nulo excluidas, series con `isWarmup` excluidas | JUnit4 |
| Frontera de día | Medianoche local, sesión que cruza medianoche, cambio de zona | JUnit4 con `Clock` y `ZoneId` |
| Semana ISO | Lunes como primer día, semanas 52 y 53, año bisiesto | JUnit4 |
| Ventana actual vacía | Fallback a la última semana con datos y ventana etiquetada | JUnit4 con `Clock` y `ZoneId` |
| Frontera de sesión | Apertura perezosa, cierre al cambiar de día, quick log reutiliza la sesión libre | Room en memoria |
| Undo de la última serie | Undo que recrea una sesión eliminada al borrar su última serie, conservando `id` y `startedAt` (D39 y D40, cerradas en la sexta revisión) | Room en memoria |
| Sesión activa por plan | Intento de crear una segunda sesión abierta para el mismo plan | Room en memoria |
| Sesión libre por día | Intento de crear una segunda sesión libre abierta para el mismo día, con `planId IS NULL` | Room en memoria |
| `closedBy` | Cierre automático distinguido del explícito | Room en memoria |
| `isWarmup` | Valor automático por posición, quick log siempre serie de trabajo, no se recalcula al borrar | Room en memoria |
| Nombres únicos | `Sentadilla` frente a `sentadilla` y `Péculo` frente a `péculo`, sobre la columna normalizada | Room en memoria |
| Validación de métricas | `reps = 0`, `loadKg = 0` y serie sin métrica, rechazados antes del `CASCADE` | Room en memoria |
| Preconsulta de conteo | Conteo de afectados antes del borrado de un ejercicio con series | Room en memoria |
| Import por fusión | Colisión de nombre, colisión de ID con otro nombre, remapeo de FKs | Room en memoria |
| Migraciones | v1 a v2 con los esquemas JSON versionados | Room |
| Migración de claves de DataStore | Versión leída antes del primer acceso, migración sobre un listado conocido de claves | JUnit4 |
| Repositorios con Flow | Emisiones y cancelación | Turbine |

No se testea: UI de Compose, Vico, SAF (D19, cerrada en la sexta revisión: en v1 no hay tests de Compose, ni sobre Robolectric, y el criterio de "listo" se cumple en emulador). La alternativa textual de las gráficas (D35) es un elemento de Compose, así que no tiene verificación automática y se verifica a mano en emulador, como item del criterio de "listo" en `docs/README.md`. CI no ejecuta nada de UI: el job único `check` de D49 corre ktlint, Lint y tests unitarios.

## Firma y distribución (D54, D55, D58, D59, cerradas)

- La clave se genera una vez, no entra en el repo ni en CI, y se copia de seguridad. Perderla obliga a reinstalar a todos los usuarios, porque Android no permite actualizar con una clave distinta.
- Credenciales en `keystore.properties`, gitignored.
- Firmar con `apksigner` de build-tools 34.0.0. Las versiones 35 y superiores generan APKs que `apksigcopier` no puede verificar, así que hay que instalar 34.0.0 en paralelo con 36.0.0.
- El APK se construye desde el commit etiquetado limpio, sin artefactos de builds anteriores.
- Release: tag `v1.0.0`, GitHub Releases con `Iron_v1.0.0.apk` y `CHANGELOG.md` en formato Keep a Changelog.
- Metadatos en `fdroiddata` por merge request: `License: GPL-3.0-or-later`, `Categories: Sports`, `AutoUpdateMode: Version`, `UpdateCheckMode: Tags`, `Binaries` apuntando a las releases y `AllowedAPKSigningKeys` con el SHA-256 de la certificación.

## REUSE (D57, cerrada)

Cabecera SPDX en cada fichero fuente, con la carpeta `LICENSES/` conteniendo el texto completo de GPL-3.0-or-later:

```
// SPDX-FileCopyrightText: 2026 <autor>
// SPDX-License-Identifier: GPL-3.0-or-later
```

El estado del entorno de desarrollo, con lo comprobado en la máquina, está en `docs/iterations/00-entorno.md`. Aquí solo las decisiones de stack y herramientas.
