<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Plan: App "Iron" — Kotlin/Jetpack

Plan de desarrollo para una app Android nativa que implementa un subset de las funcionalidades de [Flexify](https://github.com/brandonp2412/Flexify) (ver `docs/analysis/flexify_analysis.md`), con identidad y formato propios. La estructura de vistas está rediseñada desde cero y no sigue la de Flexify.

Este documento es el índice. El detalle está repartido por ámbito, y cada dato vive en un solo sitio.

## Documentos

| Documento | Ámbito |
|---|---|
| `docs/README.md` | índice: qué es cada documento, convenciones, resumen de producto, stack |
| `docs/spec/data.md` | esquema Room, ajustes de DataStore, formato propio de ficheros, localización |
| `docs/spec/rules.md` | reglas de negocio y definiciones de cálculo |
| `docs/spec/views.md` | estructura de vistas: destinos, grafo, pantallas |
| `docs/spec/ux.md` | UX operativa: comportamiento de la UI |
| `docs/spec/engineering.md` | stack, paquetes, testing, calidad, distribución |
| `docs/state/decisions.md` | estado de las decisiones D1–D72 y su justificación |
| `docs/state/todo.md` | índice de iteraciones con el estado de cada bloque |
| `docs/iterations/` | un fichero por bloque: checkboxes y notas de verificación |
| `docs/analysis/flexify_analysis.md` | análisis de Flexify |

## Fuentes de verdad

- El estado de cada decisión solo vive en `docs/state/decisions.md`; la regla vive en el documento de su ámbito.
- El estado de cada iteración solo vive en su fichero de `docs/iterations/`, y el resumen en `docs/state/todo.md`.
- Las notas de verificación, los defectos y las decisiones de detalle solo viven en el fichero del bloque.
- El historial de revisiones solo se narra en `docs/state/decisions.md`.
- El criterio de "listo" solo está en `AGENTS.md`.
- Los IDs D1–D72 son estables y son la única referencia cruzada. Una decisión recibe ID cuando condiciona trabajo de iteraciones posteriores; las de detalle se quedan como nota, sin ID.

## Resumen de producto

- **Sin timer** de descanso: se elimina toda la superficie asociada (timer, sonido, vibración, notificaciones locales, pantalla siempre encendida).
- **Sin peso corporal:** se elimina la entidad `BodyWeight`, la feature `weight` y todo lo asociado (foto opcional, gráfica de peso).
- **Solo fuerza:** sin cardio. No se registran duración ni distancia, y la métrica RPM se elimina.
- **Unidades de peso:** solo kg y lb, con kg como unidad canónica y principal. Stone se descarta.
- **Nombre:** Iron · **Package name:** `com.dolvem.iron`
- **Formato propio** de import/export desde cero, sin compatibilidad con Flexify.
- **Idiomas:** español e inglés desde el inicio, arquitectura extensible. Selector `sistema` (default), `es` y `en`, con fallback a inglés (D64).
- **Material You** (dynamic color) como base visual.
- **Settings en DataStore** (Preferences), fuera de la BD.
- **BD vacía al primer arranque:** sin catálogo semilla ni planes por defecto.
- **Vico** para las gráficas.
- **Navegación:** cuatro destinos (Hoy, Planes, Ejercicios, Historial) con Ajustes como ruta desde la cabecera de Hoy.
- **Sesión perezosa:** se abre con la primera serie registrada y se cierra de forma explícita, o automática al cambiar de día. No hay paso de "iniciar plan".
- **Historial agrupado por sesión**, con la lista plana de registros como filtro.
- **Categorías** como filtro dentro de Ejercicios, no como vista propia.
- **Quick log como FAB contextual**, no como bloque de Hoy.
- **Adherencia sin porcentajes ni rachas:** progreso de series vs `maxSets` en la sesión, frecuencia en el detalle del plan, estado en Hoy.
- **Min SDK 31** (Android 12). El dispositivo de referencia es Android 15 y no se publicará a gran escala, así que se prioriza la simplificación sobre la cobertura: dynamic color nativo, `java.time` nativo, sin bifurcaciones por versión ni paleta de respaldo.

## Stack técnico

| Componente | Elección | Versión |
|---|---|---|
| Lenguaje/UI | Kotlin, Jetpack Compose + Material 3 (`dynamicColor`), Navigation Compose | Kotlin 2.3.20 (2.3.20), Compose BOM 2026.09.00 |
| Base de datos | Room 3 (KSP) — 6 entidades, unidad canónica kg, paquete `androidx.room3` | 3.0.3 |
| Ajustes | DataStore Preferences, con clave de versión | 1.2.1 |
| Gráficas | Vico (módulo `compose`) | 3.3.1 |
| Arquitectura | MVVM: `ViewModel` + coroutines/Flow, repositorios, `StateFlow` de UI state | — |
| Ficheros | ActivityResultContracts (SAF) para import/export | — |
| Build | Gradle + Kotlin DSL, catálogo `libs.versions.toml`, plugin de Gradle `androidx.room3` | Gradle 9.8.1, AGP 9.4.0, KSP 2.3.10 con KSP2, JDK 25 con `jvmTarget` 17 |
| Inyección | Contenedor manual en `IronApp` (sin Hilt) | — |
| Testing | JUnit4, Room en memoria, Turbine. Robolectric solo para código que toca APIs de Android | Robolectric 4.17, Turbine 1.2.1 |
| Min SDK | 31 · target 37 · compile 37.1 | — |

Las versiones se fijan en `gradle/libs.versions.toml` (D20). El detalle de cada elección está en `docs/spec/engineering.md`.

## Iteraciones

El orden de implementación es el de `docs/state/todo.md`, que es el índice con el estado de cada bloque. El criterio de "listo" que se aplica a cada bloque está en `AGENTS.md`.

Al borrar este repo, conservar todos los ficheros de `docs/`.
