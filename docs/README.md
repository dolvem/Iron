<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Plan: App "Iron" — Kotlin/Jetpack

Plan de desarrollo para una app Android nativa que implementa un subset de las funcionalidades de [Flexify](https://github.com/brandonp2412/Flexify) (ver `docs/analysis/flexify_analysis.md`), con identidad y formato propios.

La estructura de vistas está rediseñada desde cero y no sigue la de Flexify: ver `docs/views_plan.md`.

Este documento es el índice: decisiones de producto, stack técnico, iteraciones, criterio de "listo" y riesgos. El detalle está repartido por ámbito.

## Documentos

| Documento | Ámbito |
|---|---|
| `docs/README.md` | índice: decisiones de producto, stack, iteraciones, criterio de "listo", riesgos |
| `docs/data_plan.md` | esquema Room, ajustes de DataStore, formato propio de ficheros |
| `docs/rules_plan.md` | reglas de negocio y definiciones de cálculo |
| `docs/engineering_plan.md` | decisiones de ingeniería, paquetes, testing, calidad, distribución, entorno |
| `docs/ux_plan.md` | UX operativa |
| `docs/decisions_plan.md` | estado de las decisiones D1–D62 |
| `docs/views_plan.md` | estructura de vistas |
| `docs/todo.md` | TODO: tareas individuales por iteración (0 a 11) |
| `docs/analysis/flexify_analysis.md` | análisis de Flexify |

Convención de referencias: los IDs D1–D62 son estables y son la única referencia cruzada. El estado de cada decisión solo vive en `docs/decisions_plan.md`; la regla vive en el documento de su ámbito.

## Decisiones confirmadas

- **Sin timer** de descanso: se elimina toda la superficie asociada (timer, sonido, vibración, notificaciones locales, pantalla siempre encendida).
- **Sin peso corporal:** se elimina la entidad `BodyWeight`, la feature `weight` y todo lo asociado (foto opcional, gráfica de peso corporal).
- **Nombre:** Iron · **Package name:** `com.dolvem.iron`
- **Formato propio** de import/export desde cero (sin compatibilidad con Flexify).
- **Idiomas:** español e inglés desde el inicio, arquitectura extensible a más idiomas. El selector de idioma es `sistema` (default), `es` y `en`; `sistema` elige el idioma del sistema y cae en inglés si no está soportado (D64, cerrada en la séptima revisión).
- **Material You** (dynamic color) como base visual.
- **Settings en DataStore** (Preferences), fuera de la BD.
- **BD vacía al primer arranque**: sin catálogo semilla ni planes por defecto.
- **Vico** para las gráficas.
- **Navegación:** cuatro destinos (Hoy, Planes, Ejercicios, Historial) con Ajustes como ruta desde la cabecera de Hoy. Ver `docs/views_plan.md`.
- **Solo fuerza:** sin cardio. No se registran duración ni distancia, y la métrica RPM se elimina.
- **Unidades de peso:** solo kg y lb, con kg como unidad canónica y principal. Stone se descarta.
- **Sesión perezosa:** se abre con la primera serie registrada y se cierra de forma explícita, o automática al cambiar de día. No hay paso de "iniciar plan".
- **Historial agrupado por sesión**, con la lista plana de registros como filtro.
- **Categorías** como filtro dentro de Ejercicios, no como vista propia.
- **Quick log como FAB contextual**, no como bloque de Hoy: escribe en la sesión abierta o crea una sesión libre para hoy.
- **Adherencia sin porcentajes ni rachas:** progreso de series vs `maxSets` en la sesión, frecuencia en el detalle del plan, estado en Hoy.
- **Min SDK:** 31 (Android 12). El dispositivo de referencia es Android 15 y no se publicará a gran escala, así que se prioriza la simplificación sobre la cobertura: dynamic color nativo, `java.time` nativo, sin bifurcaciones por versión ni paleta de respaldo.

El estado de las decisiones que estaban abiertas, con las cerradas en la revisión del 08/10/2026, está en `docs/decisions_plan.md`. El bloque de build y distribución se cerró en la tercera revisión del 08/10/2026; las decisiones nuevas de ese bloque son D47 a D61. En la cuarta revisión se cierran D4, D5, D26 y D62, las cuatro de la iteración 2 que tenían conflicto entre documentos, y se añade D62. En la quinta revisión se cierran las siete pendientes de la iteración 2 (D6, D7, D8, D10, D17, D18, D46) y D37, que se adelanta porque el DataStore se escribe en la iteración 2. En la sexta revisión se cierran las dieciséis que quedaban: D11 a D16, D19, D28 a D30, D32 a D35, D39 y D40. En la séptima revisión se cierran D63 y D64, los dos defaults de ajustes que quedaban por decidir: el default de los formatos de fecha por idioma y el selector de idioma. No queda ninguna decisión pendiente.

## Stack técnico

| Componente | Elección | Versión |
|---|---|---|
| Lenguaje/UI | Kotlin, Jetpack Compose + Material 3 (`dynamicColor`), Navigation Compose | Kotlin 2.3.x (2.3.20), Compose BOM 2026.09.00 |
| Base de datos | Room 3 (KSP) — 6 entidades, unidad canónica kg, paquete `androidx.room3` | 3.0.3 |
| Ajustes | DataStore Preferences, con clave de versión | — |
| Gráficas | Vico (módulo `compose`) | 3.3.1 |
| Arquitectura | MVVM: `ViewModel` + coroutines/Flow, repositorios, `StateFlow` de UI state | — |
| Ficheros | ActivityResultContracts (SAF) para import/export | — |
| Build | Gradle + Kotlin DSL, catálogo `libs.versions.toml`, plugin de Gradle `androidx.room3` | Gradle 9.8.1, AGP 9.4.0, KSP 2.3.10 con KSP2, JDK 25 con `jvmTarget` 17 |
| Inyección | Contenedor manual en `IronApp` (sin Hilt) | — |
| Testing | JUnit4, Room en memoria, Turbine. Robolectric solo para código que toca APIs de Android | Robolectric 4.17, Turbine 1.2.1 |
| Min SDK | 31 · target/compile 36 | — |

## Funcionalidades por iteración (orden de implementación)

0. **Entorno:** JDK 25 fijado en `.tool-versions`, SDK 36, build-tools 36.0.0 y build-tools 34.0.0, un AVD con API 31 y imagen Google APIs x86_64 en el emulador (arm64 en el dispositivo físico, D61), `local.properties` y `ANDROID_AVD_HOME`. Sin esto, el criterio de listo no se puede cumplir. Este bloque está cerrado el 08/10/2026, con KVM activo.
1. **Scaffolding:** proyecto, Material You, bottom nav con los 4 destinos (Hoy, Planes, Ejercicios, Historial), Ajustes como ruta desde la cabecera de Hoy, tema claro/oscuro/sistema. Incluye `git init`, `.gitignore`, `LICENSE` (GPL-3.0-or-later) con la carpeta `LICENSES/` de REUSE, `README.md` de la raíz (que enlaza a `docs/README.md`), `.editorconfig`, cabecera SPDX en cada fichero, icono de aplicación, orientación bloqueada en vertical e idioma base en inglés. Este bloque está cerrado: D21, D22, D23, D24, D25, D27, D36, D38 y D47 a D61.
2. **Capa de datos:** Room + DAOs + repositorios + DataStore; migraciones desde v1. Aquí se cierran las definiciones de cálculo, el modelo de errores (`Outcome` sellado con preconsulta de conteo), `Clock` y `ZoneId` inyectables en `AppContainer`, los TypeConverters y el versionado de claves de DataStore. Este bloque está cerrado: D4, D5, D6, D7, D8, D10, D17, D18, D26, D46 y D62.
3. **Ejercicios:** CRUD de ejercicios y categorías (crear/renombrar/fusionar/borrar); detalle con datos, gráficas y últimos registros; archivado. Las categorías se gestionan desde la barra de filtros de la lista, con caja de búsqueda.
4. **Planes:** CRUD planes y ejercicios del plan (orden, series máx, calentamiento, RPE objetivo orientativo, habilitado) y frecuencia: última sesión y sesiones en los últimos 30 días.
5. **Sesión:** apertura perezosa con la primera serie; registrar series (peso+reps); progreso por ejercicio frente a `maxSets` y `warmupSets`; notas; edición/borrado; cierre explícito dentro del día y automático al cambiar de día. Checklist de solo lectura. El RPE objetivo (`targetRpe`) se muestra como referencia orientativa junto al ejercicio, sin marcarse por serie.
6. **Hoy:** plan del día, sesión abierta, progreso global, FAB de quick log contextual y estado vacío guiado para el primer arranque. La coincidencia con `plan.days` se recalcula al cambiar el día.
7. **Historial:** sesiones agrupadas por `sessionDate` con su detalle, más el modo lista plana de registros; filtros (ejercicio, plan, categoría, rango de fechas, sesiones libres). Cierre de sesión desde el detalle.
8. **Gráficas (Vico):** métricas por ejercicio (mejor peso, volumen, repeticiones totales, series por periodo) con agrupación día/semana/mes/año; opciones de curva, límite y eje X temporal. Calentamiento excluido, y alternativa textual accesible.
9. **Import/Export propio:** export CSV de gráficas; backup completo (archivo JSON/GZIP de la BD) e importación desde ese formato.
10. **Ajustes completos:** unidad de fuerza, formatos de fecha, defaults de gráfica, estimación 1RM/reps (fórmula Epley documentada en comentario), idioma.
11. **Pulido:** estados vacíos, validaciones, accesibilidad, tests unitarios (conversiones, repositorios, analítica).

## Criterio de "listo" por iteración

- `./gradlew build` limpio, sin warnings relevantes.
- Tests unitarios pasando para la lógica nueva (conversiones, analítica, repositorios).
- Flujo navegable en emulador Android.
- Accesibilidad verificada en emulador: targets táctiles de 48dp, `contentDescription` en iconos, escala de fuente y la alternativa textual de los valores por bucket en las gráficas (D35, cerrada en la sexta revisión).

## Notas / riesgos

- **1RM/reps:** usar Epley (`peso / (1 + reps/30)`) con comentario de referencia a la fórmula, como exige el estándar de documentación del proyecto.
- **RPE (Rate of Perceived Exertion):** escala 1–10 que indica qué tan cerca está una serie del fallo: RPE 10 = fallo (no podrías hacer otra rep), RPE 9 ≈ 1 rep al fallo, RPE 6 ≈ 4 reps al fallo. En Iron es un **objetivo orientativo** definido en `PlanExercise.targetRpe?` al configurar el plan; se muestra como referencia durante la sesión y no se registra ni marca por serie.
- **Restricción de workout activo por plan:** Room no soporta índices parciales; se implementa con índice compuesto `(planId, endedAt)` + validación transaccional en el repositorio antes de crear una sesión. Sigue siendo válida con el cierre automático, porque el cierre libera la restricción.
- **Snapshot:** el histórico refleja la configuración actual del plan, no la de entonces. Para v1 se acepta como limitación documentada.
- **Localización extensible:** todas las cadenas en `strings.xml` desde el día 1; añadir un idioma = nueva carpeta `values-XX`. La base es `values/` en inglés y `values-es` en español. Los conteos de las confirmaciones usan `plurals.xml`, con reglas distintas en cada idioma.
- **Licencia:** GPL-3.0-or-later, obligatoria para F-Droid.
- **Firma y reproducibilidad:** firmar con `apksigner` de build-tools 34.0.0, porque las versiones 35 y superiores generan APKs que `apksigcopier` no puede verificar. Las seis reglas de determinismo están en `docs/engineering_plan.md`.

Al borrar este repo, conservar todos los ficheros de `docs/`.
