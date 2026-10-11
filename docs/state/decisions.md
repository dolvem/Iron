<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Decisiones: estado tras la séptima revisión del 08/10/2026 y las decisiones de implementación

Parte de `docs/README.md`. Los números D1–D85 son la referencia cruzada entre documentos. Los D1–D46 no cambian; D47–D61 se añaden al cerrar el bloque de build y distribución, D62 en la cuarta revisión, D63 y D64 en la séptima revisión, D65 y D66 al escribir la iteración 1, D67 a D72 durante la implementación de las iteraciones 3 y 4, D73 a D76 en el rediseño de vistas de Penpot, D77 al implementar la iteración 4.5, D78 a D82 al rediseñar el filtrado de Ejercicios sobre el spec de M3, que revisan D74 y D77, D83 al cerrar la píldora de sin categoría, que revisa D75, D84 al descartar el item "Todas" del menú del chip, que revisa D78, D85 al cerrar el contenedor de los menús y D86 al cerrar el leading icon de los items de acción. Este documento es el único que narra las revisiones; los demás enlazan aquí y no repiten qué se cerró en cada una.

Aquí solo se guarda el estado y la justificación de cada decisión. La regla está en el documento de su ámbito, que se indica en la columna "Ámbito". La columna "Bloquea" referencia las iteraciones de `docs/state/todo.md`. Las decisiones de detalle que no condicionan iteraciones posteriores no reciben ID: se quedan como nota en `docs/iterations/`.

Las cerradas se aplican como está escrito en el documento de su ámbito. El bloque de la capa de datos se cerró en la segunda revisión del 08/10/2026. El bloque de build y distribución se cerró en la tercera revisión del 08/10/2026. En la cuarta revisión se cierran D4, D5, D26 y D62, que son las cuatro decisiones de la iteración 2 que tenían un conflicto entre documentos. En la quinta revisión se cierran las siete pendientes de la iteración 2 (D6, D7, D8, D10, D17, D18, D46) y D37, que se adelanta porque el DataStore se escribe en la iteración 2. En la sexta revisión se cierran las dieciséis que quedaban: D11 a D16, D19, D28 a D30, D32 a D35, D39 y D40. En la séptima revisión se cierran D63 y D64, los dos defaults de ajustes que quedaban por decidir: el default de los formatos de fecha por idioma y el selector de idioma. Con esto no quedaba ninguna decisión pendiente de diseño; las que han salido al escribir el código son D65 a D72.

## Resumen

| # | Decisión | Estado | Bloquea | Ámbito |
|---|---|---|---|---|
| D1 | Sesión abierta al cambiar de día | **Cerrada:** cierre automático al registrar la primera serie de un día distinto | 2 | rules |
| D2 | Cómo se distingue una serie de calentamiento | **Cerrada:** `isWarmup` con valor automático por posición, editable en el detalle | 2 | rules |
| D3 | Día de la sesión | **Cerrada:** `sessionDate` inmutable en `Workout` | 2 | rules |
| D4 | Índices únicos case-insensitive | **Cerrada:** columna normalizada derivada `nameKey` con índice único, calculada en Kotlin con NFC y `Locale.ROOT` | 2 | rules |
| D5 | Tipo de retorno de los repositorios | **Cerrada:** `Outcome` sellado, con preconsulta de conteo separada del borrado, y la violación de único de la BD mapeada a error de negocio | 2 | engineering |
| D6 | Validación de métricas | **Cerrada:** `reps >= 1`, `loadKg > 0`, al menos una métrica, sin techo; valida el repositorio antes del `CASCADE` y la UI muestra el error | 2 | rules |
| D7 | Ventana actual y buckets vacíos | **Cerrada:** semana ISO en curso; hueco, no cero; ventana vacía muestra la última semana con datos | 2 | rules |
| D8 | Qué cuenta el progreso global | **Cerrada:** sesiones libres en volumen y conteo, no en adherencia ni en frecuencia | 2 | rules |
| D9 | Gráficas y calentamiento | **Cerrada:** excluidas siempre, en PR, volumen, repeticiones y conteo | 2 | rules |
| D10 | Redondeo y precisión de unidades | **Cerrada:** 2 decimales al mostrar y al guardar, medio arriba; 0,5 kg en 1RM | 2 | rules |
| D11 | Algoritmo de fusión en el import | **Cerrada:** dos pasadas con mapa `incomingId -> existingId`, gana el nombre existente, y en duplicados del fichero gana la primera ocurrencia | 9 | rules |
| D12 | Backup automático previo | **Cerrada:** `filesDir/backups/`, retención de los 3 más recientes, GZIP, sin SAF, sin UI de restauración | 9 | rules |
| D13 | Ajustes en el backup | **Cerrada:** no entran en v1, y se acepta perderlos al migrar de dispositivo | 9 | data |
| D14 | Versiones del formato de backup | **Cerrada:** el importador acepta `version <= current` y rechaza `app` distinto de `iron` | 9 | rules |
| D15 | Alcance del export CSV | **Cerrada:** ventana actual de la pantalla; una ventana vacía exporta cabeceras sin filas | 9 | rules |
| D16 | Convenciones del JSON | **Cerrada:** camelCase, nulls escritos como `null`, DTOs separados de las entidades | 9 | data |
| D17 | Inyección de `Clock` y `ZoneId` | **Cerrada:** en `AppContainer` con defaults, inyectados por constructor en repositorios y Analytics | 2 | engineering |
| D18 | Matriz de tests | **Cerrada:** la tabla de `docs/spec/engineering.md`, más validación de métricas, ventana vacía y migración de claves | 2 | engineering |
| D19 | Tests de Compose en v1 | **Cerrada:** ninguno, ni sobre Robolectric; el criterio de listo se cumple en emulador, con accesibilidad verificada a mano | 11 | engineering |
| D20 | Versiones sin fijar en el catálogo | **Cerrada:** fijadas en `libs.versions.toml`, con KSP2 | 1 | engineering |
| D21 | Licencia | **Cerrada:** GPL-3.0-or-later | 1 | README |
| D22 | Repositorio git y CI | **Cerrada:** GitHub público, job único `check` en push y PR, CI no firma | 1 | engineering |
| D23 | `versionCode` y `versionName` | **Cerrada:** `iron.version` en `gradle.properties` como punto de verdad único | 1 | engineering |
| D24 | Build reproducible y firma | **Cerrada:** seis reglas de determinismo, firma v2 + v3, clave local fuera del repo | 1 | engineering |
| D25 | Icono de aplicación | **Cerrada:** adaptive icon con fondo `#1B1B1D`, foreground de paleta y capa monochrome | 1 | engineering |
| D26 | Convención de nombres en la BD | **Cerrada:** camelCase en la BD y en el JSON, sin `@ColumnInfo`, con la normalización como columna propia | 2 | data |
| D27 | ktlint y Lint | **Cerrada:** ktlint-gradle con `code_style = android_studio`, Lint con warnings como error y baseline vacío | 1 | engineering |
| D28 | Teclado decimal y separador | **Cerrada:** solo en modo teclado, redondeo al guardar, separador mostrado según el idioma activo | 5 | ux |
| D29 | Placas por unidad | **Cerrada:** accesos rápidos de suma; kg 1,25 / 2,5 / 5 / 10 / 20 / 25; lb 2,5 / 5 / 10 / 25 / 35 / 45 | 5 | ux |
| D30 | Comportamiento del editor de serie | **Cerrada:** se mantiene abierto, foco al siguiente campo, stepper de 1 a 100 con estado vacío | 5 | ux |
| D31 | Búsqueda en la lista de ejercicios | **Cerrada:** caja de búsqueda case-insensitive en la barra de filtros | 3 | ux |
| D32 | Fusión de ejercicios y categorías | **Cerrada:** el usuario elige el destino, sobrevive su nombre, gana la fila existente en el índice único, `archived` y `notes` el menos restrictivo | 3 | ux |
| D33 | Borrado de plan con sesiones | **Cerrada:** la confirmación cuenta las sesiones que quedan como sesiones libres | 4 | ux |
| D34 | Hoy recalculado a medianoche | **Cerrada:** ticker con `delayUntilMidnight`, trigger compartido con el cierre automático de D1 | 6 | ux |
| D35 | Accesibilidad de las gráficas | **Cerrada:** sección plegable con la etiqueta del bucket y el valor de la métrica | 8 | ux |
| D36 | Idioma base | **Cerrada:** `values/` en inglés, `values-es` en español | 1 | README |
| D37 | Enums de ajustes | **Cerrada:** `inputStyle`, formatos de fecha, 10 a 100 buckets, suavidad de 0 a 1 | 10 | data |
| D38 | Orientación | **Cerrada:** portrait en el manifest, regla de Lint desactivada en `lint.xml` | 1 | ux |
| D39 | Sesión vacía | **Cerrada:** una sesión abierta se elimina al borrar su última serie; una cerrada vacía se conserva y aparece en el historial | 5 | rules |
| D40 | Ventana de deshacer | **Cerrada:** 6 s, solo en series, recrea la sesión, conserva `id` y `startedAt` | 5 | ux |
| D41 | Checklist de la sesión | **Cerrada:** solo lectura, derivado de las series | 5 | ux |
| D42 | Campos editables de una serie | **Cerrada:** métricas, notas, `isWarmup` y `timestamp`; el ejercicio no | 5 | ux |
| D43 | Cerrar desde el historial | **Cerrada:** sí, y una sesión cerrada no se reabre | 7 | rules |
| D44 | Cierre automático en el historial | **Cerrada:** se distingue del explícito | 7 | ux |
| D45 | Sesiones libres en el historial | **Cerrada:** título fijo y filtro propio | 7 | ux |
| D46 | Versionado de claves de DataStore | **Cerrada:** entero de versión con migración vacía, versión leída antes de cualquier acceso | 2 | data |
| D47 | Visibilidad del repositorio | **Cerrada:** GitHub público | 1 | engineering |
| D48 | Contenido de `.gitignore` | **Cerrada:** `schemas/` y `.tool-versions` se versionan | 1 | engineering |
| D49 | CI: runner, JDK, triggers y caché | **Cerrada:** `ubuntu-latest`, JDK 25, push y PR, acciones por tag de major, caché de Gradle | 1 | engineering |
| D50 | Plugin de ktlint y code style | **Cerrada:** ktlint-gradle con `code_style = android_studio`; el valor `android` no existe en ktlint 1.x, así que la decisión se corrige al valor real | 1 | engineering |
| D51 | Baseline de Lint y severidades | **Cerrada:** baseline vacío, warnings como error | 1 | engineering |
| D52 | JDK de build | **Cerrada:** 25, con `jvmTarget` 17 | 1 | engineering |
| D53 | `jvmTarget` | **Cerrada:** 17, aunque Gradle corra con JDK 25 | 1 | engineering |
| D54 | Clave de firma | **Cerrada:** local, fuera del repo, CI no firma | 1 | engineering |
| D55 | Versión de `apksigner` | **Cerrada:** build-tools 34.0.0 solo para firmar | 1 | engineering |
| D56 | R8 y shrinkResources | **Cerrada:** minify sí, shrinkResources no | 1 | engineering |
| D57 | REUSE y cabeceras SPDX | **Cerrada:** SPDX en cada fichero y carpeta `LICENSES/` | 1 | engineering |
| D58 | Proceso de release | **Cerrada:** tags `v1.0.0`, GitHub Releases, `CHANGELOG.md` | 1 | engineering |
| D59 | Metadatos de F-Droid | **Cerrada:** `fdroiddata` por merge request, categoría Sports, `Binaries` apuntando a las releases | 1 | engineering |
| D60 | Diseño del icono | **Cerrada:** glifo de paleta de trazo simple | 1 | engineering |
| D61 | AVD de referencia | **Cerrada:** API 31, imagen Google APIs. ABI por host: x86_64 en el emulador, arm64 en el dispositivo físico | 0 | engineering |
| D62 | TypeConverters de `sessionDate` y `closedBy` | **Cerrada:** TEXT ISO para `sessionDate`, TEXT con enum para `closedBy`, `Plan.days` parseado en el dominio | 2 | data |
| D63 | Default de los formatos de fecha | **Cerrada:** se resuelve por idioma efectivo; `shortDateFormat` es → `dd/MM/yyyy` y en → `MM/dd/yyyy`, `longDateFormat` completa en ambos | 2 | data |
| D64 | Selector de idioma | **Cerrada:** `sistema` (default), `es` y `en`; `sistema` elige el idioma del sistema y cae en inglés si no está soportado | 2 | data |
| D65 | Modo de tema en la iteración 1 | **Cerrada:** `ThemeMode` con `SYSTEM`/`LIGHT`/`DARK` en `ui/theme/`; `ThemeSource` devuelve `System` fijo en la iteración 1, el selector UI llega en la 10 | 1 | engineering |
| D66 | Firma en Gradle | **Cerrada:** sin `signingConfig`; el APK se genera unsigned y se firma fuera de Gradle con `apksigner` (D55) y `keystore.properties` en `.gitignore` | 1 | engineering |
| D67 | Firma de lectura de los DAOs | **Cerrada:** `Flow<List<T>>` en las consultas de varias filas; `Flow<T>` solo para una fila | 7 | engineering |
| D68 | Orden de los listados | **Cerrada:** por `nameKey`, no por `name`, por la colación BINARY de SQLite | 3 | rules |
| D69 | CRUD en EjercicioDetalle | **Cerrada:** el CRUD está en las filas de la lista; el menú de acciones de la identidad se escribe en la iteración 11 | 11 | views |
| D70 | Error de `Outcome` en un sheet | **Cerrada:** se muestra dentro del sheet, porque el sheet tapa el `SnackbarHost` de la pantalla | 5 | ux |
| D71 | Nombre accesible de los selectores | **Cerrada:** `semantics(mergeDescendants = true)` en las filas clicables, para que el control tenga nombre propio | 11 | ux |
| D72 | Recursos y locale en Compose | **Cerrada:** `LocalResources` y `LocalLocale.current.platformLocale`, no `LocalContext` ni `Locale.getDefault()` | 10 | engineering |
| D73 | Diálogos según M3 estricto | **Cerrada:** actions como text buttons alineados al borde trailing, 24dp entre contenido y actions, 8dp entre botones, altura 36dp, label `primary` o `error` | 4.5 a 11 | ux |
| D74 | Filtro de categoría en Ejercicios | **Revisada por D78:** se descarta la línea de filtro activo y el icono de filtro dentro de la search bar | 4.5 y 7 | ux |
| D75 | Fila de lista en Ejercicios | **Cerrada:** nombre dominante, píldora de categoría debajo, oculta con esa categoría filtrada, badge Archivado en la misma línea | 4.5 y 7 | views |
| D76 | Categoría por defecto en la creación | **Cerrada:** dropdown outlined, solo estado cerrado, valor por defecto = categoría del filtro activo | 4.5 | ux |
| D77 | Crear en Ejercicios como FAB | **Revisada por D80:** el FAB pasa a FAB menu con dos items | 4.5 | views |
| D78 | Fila de chips en Ejercicios | **Cerrada:** dos chips bajo la search bar, Categoría y Archivado; el de categoría abre un menú con las opciones de filtrado y el CRUD de la categoría seleccionada | 4.5 y 7 | ux |
| D79 | Trailing icon de la search bar | **Cerrada:** un solo icono X con doble función: borra el texto con contenido, cancela y colapsa con texto vacío | 4.5 y 7 | ux |
| D80 | Crear en Ejercicios como FAB menu | **Cerrada:** FAB menu con Crear ejercicio y Crear categoría; en código `DropdownMenu` anclado sobre el FAB, porque el componente Expressive no está en el BOM fijado | 4.5 | views |
| D81 | Filtro de archivado excluyente | **Cerrada:** dos estados, ocultos por defecto y solo archivados; no hay estado "todos incluidos" | 4.5 y 7 | rules |
| D82 | CRUD de categorías | **Cerrada:** en el menú del chip de categoría, solo para la categoría seleccionada | 4.5 | views |
| D83 | Píldora de sin categoría | **Cerrada:** pareja neutra `surface_container` + `on_surface_variant`, que revisa D75 para ese caso | 4.5 y 7 | views |
| D84 | Menú del chip de categoría | **Cerrada:** sin el item "Todas", porque el chip es el filtro y no una opción de filtrado; limpiar es la X, con un item de menú que mantiene la acción alcanzable. Revisa D78 | 4.5 | ux |
| D85 | Contenedor de los menús | **Cerrada:** shape `large` (16dp) con `surfaceContainerLow`, la lectura del vertical menu expressive; el default de Compose es el baseline de 4dp con `surfaceContainer` | 4.5 | ux |
| D86 | Leading icon en los items de acción de los menús | **Cerrada:** 24dp en `on_surface_variant` en los items de acción de los cinco menús; los items que son opciones de selección no llevan icono | 4.5 | ux |

## Detalle por decisión

### D1 a D3. Cerradas

- **D1:** regla en `docs/spec/rules.md` (Sesiones). La alternativa descartada era permitir sesiones multiday, que habría eliminado la frontera `(planId, día)`. La otra alternativa descartada era desplazar el inicio del día a las 4:00 mediante un ajuste; se rechaza porque entrenar después de medianoche es poco frecuente y una sesión que cruza medianoche se parte en dos, lo que se acepta como limitación. La regla afecta igual al quick log.
- **D2:** regla en `docs/spec/rules.md` (Progreso y métricas) y `docs/spec/ux.md`. Se descarta el toggle en el editor de serie: es el formulario más repetido de la app y no conviene cargarlo.
- **D3:** regla en `docs/spec/rules.md` (Tiempo y frontera de día). Con D1 deja de ser opcional, porque hace falta conocer el día de la sesión para decidir si una serie escribe en ella, y de paso elimina la limitación de la zona horaria.

### D4. Nombres únicos

Cerrada en la cuarta revisión. La opción de `COLLATE NOCASE` se descarta: el collation de SQLite y su `lower()` solo pliegan ASCII, así que `Péculo` y `péculo` serían dos valores distintos para el índice, y la divergencia entre la base de datos y el repositorio que se quería eliminar seguiría ahí.

Se cierra con una columna normalizada derivada `nameKey` en `Category` y `Exercise`, con índice único, calculada en Kotlin con NFC y `Locale.ROOT`. La normalización no la puede hacer SQLite, así que la hace una única función en `domain/model`, usada por el insert, el renombrar y la búsqueda. La base de datos aplica el mismo criterio que el repositorio, y la comprobación del repositorio queda solo para generar el mensaje de error.

Consecuencias: D26 se mantiene sin excepción, porque la normalización es una columna propia y no un cambio de nombre; `nameKey` no entra en el JSON de backup; y la búsqueda de D31 usa la misma columna.

### D5 a D10. Modelo de errores y definiciones de cálculo

- **D5:** cerrada en la cuarta revisión, desarrollada en `docs/spec/engineering.md` (Modelo de errores). El tipo se llama `Outcome`, porque `Result` choca con `kotlin.Result`. Se usa solo en las operaciones con error de negocio esperado (borrados, renombrar, fusión, inserción de serie, cierre de sesión); las lecturas devuelven Flow directo. La reserva de excepciones para I/O y base de datos se matiza: la violación de único se captura en el repositorio y se mapea a error de negocio, porque la comprobación previa no cubre la carrera entre la comprobación y el insert. Sin esto no se puede implementar la confirmación con conteo de afectados.
- **D6:** cerrada en la quinta revisión. Se rechaza cero y negativos, `reps >= 1` y `loadKg > 0`, con al menos una métrica. Valida el repositorio antes del `CASCADE`, que es donde la serie se inserta y donde el quick log entra por el mismo camino. La UI no valida: muestra el error que devuelve `Outcome`. Se descarta el techo: rechazar un valor alto obligaría a poner un límite que no corresponde con un peso real, y una entrada absurda por un dedo resbalado en el teclado decimal se acepta como limitación documentada.
- **D7:** cerrada en la quinta revisión. Semana ISO en curso, hueco en la serie temporal y cero en el conteo de series por periodo, como estaba. Se cierra el caso de la ventana vacía: se muestra el valor de la última semana con datos y se etiqueta la ventana, porque un hueco en la tarjeta de Hoy se lee como "no he entrenado" cuando la semana anterior sí tiene datos.
- **D8:** cerrada en la quinta revisión. Las sesiones libres cuentan en volumen global y conteo de series, y no cuentan en la adherencia de ningún plan ni en la frecuencia del detalle del plan. No cuentan en frecuencia porque `Workout.planId` es nulo, así que no hay plan al que atribuir la sesión. Frecuencia y adherencia se escriben por separado porque no son la misma métrica.
- **D9:** cerrada en la cuarta revisión: las métricas excluyen las series de calentamiento.
- **D10:** cerrada en la quinta revisión. Se cambia el criterio de 1 decimal: mostrar y guardar con 2 decimales, medio arriba cuando hay más. Así el redondeo de mostrar y el de guardar son el mismo criterio, y el valor guardado es la fuente de verdad: mostrar nunca redondea sobre un valor ya redondeado. El 1RM mantiene 0,5 kg porque es una estimación y no un dato registrado, y es lo que espera alguien mirando una placa de 2,5 kg. La ida y vuelta kg → lb → kg sigue siendo inestable con 2 decimales y se acepta como limitación.

### D11 a D16. Import y export

Cerradas en la sexta revisión. El formato está definido en `docs/spec/data.md`, el procedimiento en `docs/spec/rules.md`.

- **D11:** dos pasadas con mapa `incomingId -> existingId`, raíz primero y dependientes después, porque sin el mapa resuelto no se pueden remapear las FKs de `planExercises`, `workouts` y `sets`. En una colisión de nombre gana el nombre existente, porque en el import la base destino es el destino, igual que en la fusión manual de D32. Los nombres duplicados dentro del propio fichero se resuelven con la primera ocurrencia, y las siguientes se mapean a ella, coherente con el modo de fusión. La colisión en `unique(planId, exerciseId)` es la misma regla que D32: gana la fila existente y el conflicto se registra en el resumen.
- **D12:** `filesDir/backups/`, retención de los 3 más recientes, sin acceso por SAF, con GZIP. Son una red de seguridad interna, sin UI de restauración, porque una UI de restauración obligaría resolver otra vez el algoritmo de fusión sobre un backup interno.
- **D13:** los ajustes no entran en v1, coherente con que viven en DataStore, fuera de la BD. La consecuencia es que migrar de dispositivo pierde los ajustes, y se acepta.
- **D14:** el importador acepta `version <= current` y rechaza `app` distinto de `iron`. Una versión mayor que `current` se rechaza con error. Sin la comprobación de `app`, un fichero de otra app con `version: 1` pasaría el filtro.
- **D15:** ventana actual de la pantalla, no toda la tabla, porque el CSV refleja lo que se está viendo. Una ventana vacía exporta cabeceras sin filas, porque el CSV siempre tiene cabeceras. Respeta la exclusión del calentamiento de D9.
- **D16:** camelCase, nulls escritos como `null` y no omitidos, DTOs separados de las entidades, DTOs excluyen `nameKey`. Escribir nulls en lugar de omitirlos hace que un lector no tenga que distinguir entre campo ausente y campo nulo, y el JSON es más estable frente a cambios de esquema.

### D17 a D19. Testing

**D17 y D18 están cerradas** en la quinta revisión. **D19 se cierra en la sexta revisión.** La matriz está en `docs/spec/engineering.md`. D17 es la condición que hace deterministas los tests de frontera de día y semana ISO.

**D17:** se cierra con `Clock` y `ZoneId` creados en `AppContainer` con `Clock.systemUTC()` y `ZoneId.systemDefault()` como defaults, e inyectados por constructor en los repositorios y en `Analytics`. Se descarta crear el `Clock` dentro de cada repositorio, porque entonces el test tendría que reconstruir el repositorio entero. Hace falta los dos, no solo `Clock`: `sessionDate` se fija en la zona de apertura, así que la frontera de día no es determinista con solo `Clock`.

**D18:** se cierra con la matriz actual más tres filas: validación de métricas (D6), ventana actual vacía con fallback a la última semana con datos (D7), y migración de claves de DataStore (D46). Los dos casos añadidos en la cuarta revisión se confirman: sesión libre abierta por día con `planId IS NULL`, porque la matriz solo cubre "una sesión activa por plan" y la regla extiende la frontera a las sesiones libres, y la preconsulta de conteo antes del borrado, que es la pieza de D5 que hace falta para la confirmación. El caso de `closedBy` distinguyendo cierre automático de explícito ya estaba en la tabla y se confirma como parte de la matriz. En la sexta revisión se añade una fila: undo que recrea una sesión eliminada al borrar su última serie, porque con D39 el undo deja de ser un solo insert.

**D19:** en v1 no hay tests de Compose, ni sobre Robolectric, y el criterio de "listo" se cumple en emulador. La consecuencia es que la alternativa textual de D35 no tiene verificación automática y se verifica a mano, así que la accesibilidad se añade como item del criterio de "listo" en `docs/README.md`. CI no ejecuta nada de UI: el job único `check` de D49 corre ktlint, Lint y tests unitarios.

### D28 a D46. UX operativa

Cerradas en la sexta revisión, desarrolladas en `docs/spec/ux.md`. Las que ya estaban cerradas: D31 (búsqueda), D37 (enums de ajustes), D38 (orientación), D41 (checklist de solo lectura), D42 (campos editables de una serie), D43 (cierre desde el historial sin reapertura), D44 (cierre automático distinguido), D45 (sesiones libres con título fijo y filtro propio) y D46 (versionado de claves).

Justificaciones que no están en la regla:

- **D28:** la normalización solo tiene efecto en modo teclado, porque con `inputStyle = stepper` no se escribe un separador. El campo acepta lo que el usuario escribe y el redondeo ocurre al guardar, que es lo coherente con D6, que no pone techo y prohíbe validar en la UI. Mostrar según el idioma activo mantiene la entrada y la presentación como dos cosas distintas.
- **D29:** se trata como accesos rápidos de suma y no como inventario de placas. Se amplía el conjunto con 1,25 kg y 25 kg en kg, y 35 lb en lb, porque son las placas con las que se ajusta el redondeo.
- **D30:** el stepper con estado vacío mantiene `reps` nullable y D6 intacta. La alternativa descartada era hacer `reps` obligatorio en modo stepper, que habría quitado el registro con solo peso.
- **D31:** con un catálogo propio de cientos de ejercicios, filtrar sin búsqueda no es usable.
- **D32:** el usuario elige el destino, y el nombre que sobrevive depende de esa elección. En `unique(planId, exerciseId)` gana la fila existente, la misma regla que D11. En `archived` y `notes` gana el estado menos restrictivo, porque fusionar un ejercicio activo en uno archivado lo archiva y lo saca de la lista por defecto, y eso no se puede leer en el conteo de la confirmación.
- **D33:** `Workout.planId` es SET NULL, así que las sesiones quedan como sesiones libres y la confirmación tiene que avisarlo. El conteo cuenta esas sesiones, porque es lo que el usuario pierde; contar solo las filas del plan no avisa del cambio real. El borrado sigue bloqueado mientras haya una sesión abierta, así que la confirmación solo aplica a sesiones cerradas.
- **D34:** el ticker con `delayUntilMidnight` cubre la app abierta a medianoche con la pantalla encendida, y sirve de trigger para el cierre automático de D1, así que hay una sola pieza. Con la app muerta en segundo plano se recalcula al relanzar. La alternativa descartada era recalcular solo en `onResume`, que no actualiza la pantalla si el usuario deja la app abierta. Con `Clock` y `ZoneId` de D17 el ticker es determinista en tests.
- **D35:** Vico no es accesible por defecto, de ahí la alternativa textual. Se elige una sección plegable siempre disponible, porque comprobar el estado de los servicios de accesibilidad para cambiar el layout es frágil.
- **D38:** bloquear vertical es coherente con el alcance de pantalla.
- **D39:** una sesión que nunca tuvo series no existe, porque la apertura es perezosa. Una sesión abierta se elimina al borrar su última serie, porque una sesión abierta con 0 series bloquearía la restricción de una sesión activa por plan al día siguiente. La alternativa descartada era conservarla abierta, que dejaba la restricción bloqueada por una sesión sin contenido.
- **D40:** 6 s, que es uno de los valores oficiales de los snackbar de Material 3. El undo recrea la sesión cuando D39 la eliminó, y conserva `id` y `startedAt`, porque deshacer devuelve el estado anterior. El undo en entidades grandes se descarta, porque sería un undo transaccional con remapeo de FKs.
- **D42:** cambiar el ejercicio rompería la identidad longitudinal del ejercicio.
- **D46:** no es UX, pero cierra el grupo. Cerrada en la quinta revisión con un entero de versión y una función de migración vacía, que se rellena cuando haya el primer cambio real de clave. Preferences no tiene esquema ni permite enumerar claves, así que la migración solo puede operar sobre un listado conocido, y la versión se lee antes de cualquier acceso. Se descarta reconstruir el `Settings` en lectura, porque eso mezclaría la migración con la lectura normal.

### D47 a D61. Build y distribución, detalle

- **D47:** un repo privado impediría la publicación en F-Droid.
- **D48:** `schemas/` se versiona porque los tests de migraciones usan los JSON de esquema. `.tool-versions` se versiona porque fija el JDK del proyecto.
- **D49:** un solo job. Separar ktlint, Lint y tests en jobs paralelos no aporta nada con un módulo único.
- **D50:** ktlint-gradle en lugar del plugin oficial, porque mantiene la caché y no levanta un daemon aparte.
- **D51:** el baseline se crea vacío en la iteración 1, no después. Un baseline heredado es lo que hace que Lint se relaje.
- **D52:** JDK 25 es el único que asdf ofrece en este proyecto y el que usa la Debian Trixie del build server de F-Droid. JDK 17 sigue siendo LTS, pero viene de Bookworm y puede dejar de estar disponible. La matriz de compatibilidad se confirma el 08/10/2026: Gradle 9.8.1 ejecuta el daemon sobre JVM 17–27, AGP 9.4 exige Gradle 9.6.0 y JDK 17 como mínimo sin tope superior, y Kotlin 2.3.20 soporta Java 25 desde 2.3.0 y exige Gradle ≥ 9.5.0. Las tres condiciones se cumplen, así que se mantiene 25.
- **D53:** `jvmTarget` 17 aunque Gradle corra con 25, porque el bytecode lo consume D8 y la compatibilidad con Android no mejora por encima de 17.
- **D54:** la clave no entra en CI. F-Droid verifica la reproducibilidad copiando la firma del APK, y eso exige que el APK sea idéntico antes y después de firmar.
- **D55:** `apksigner` de build-tools 34.0.0. Las versiones 35 y superiores generan APKs que `apksigcopier` no puede verificar. Es una restricción, no una preferencia: hay que instalar 34.0.0 en paralelo con 36.0.0.
- **D56:** el resource shrinker tiene comportamiento no determinista, así que en v1 se deja fuera.
- **D57:** REUSE completo. GPL lo exige y F-Droid lo valora.
- **D58:** el tag `v1.0.0` y el nombre `Iron_v1.0.0.apk` son los que `Binaries` y `UpdateCheckMode: Tags` esperan en el metadato.
- **D59:** la categoría Sports es donde está el resto de apps de registro de entrenamiento.
- **D60:** glifo de paleta de trazo simple, sin PNG.
- **D61:** API 31 porque es el suelo real de la app. Un AVD de API 36 no prueba el minSdk. La ABI la decide el host: el emulador en x86_64 rechaza las imágenes arm64 (`Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host`), así que el AVD usa x86_64. arm64 es la ABI del teléfono de referencia, que es donde se verifica el comportamiento real.

### D62. TypeConverters

Cerrada en la cuarta revisión. Son esquema de la iteración 2 y afectan al JSON de backup, así que había que decidirlos antes de escribir los DAOs.

- **`sessionDate`:** TEXT con fecha ISO. Es legible en los JSON de `schemas/`, coincide con el formato del backup, y el orden lexicográfico es correcto para agrupar y ordenar el historial. Se descarta epoch day en INTEGER: es más compacto, pero ilegible en el esquema.
- **`closedBy`:** TEXT con enum `ClosedBy` y TypeConverter. Room no soporta enums sin conversor. TEXT es legible en el esquema y en el backup; INTEGER 0/1 sería opaco.
- **`Plan.days`:** se queda como TEXT CSV, y el parseo se hace en el modelo de dominio, no en el conversor, porque el conversor solo mueve el valor.

### D63 y D64. Defaults de ajustes

Cerradas en la séptima revisión del 08/10/2026. Son los defaults del objeto `Settings` que hacen falta para escribir `SettingsDataStore` en la iteración 2. D37 fija los enums y los rangos, pero no el valor inicial de cada campo.

- **D63:** el default de los dos formatos de fecha se resuelve por idioma efectivo, no por el idioma bruto del sistema. `shortDateFormat`: es → `dd/MM/yyyy`, en → `MM/dd/yyyy`. `longDateFormat`: completa en ambos idiomas, porque la elección entre completa e ISO no depende del idioma y la convención de lectura es completa en los dos. El default solo se aplica cuando el campo está sin asignar; un valor explícito del usuario gana. "Idioma efectivo" es el idioma tras aplicar `localeOverride`, así que forzar español en un dispositivo inglés da `dd/MM/yyyy`.
- **D64:** el selector de idioma tiene tres valores, `sistema` (default), `es` y `en`. `sistema` elige el idioma del sistema y, si no está soportado, cae en inglés. En el modelo `null` equivale a `sistema`, coherente con D36 y con `values/` en inglés como base. Se descarta el nombre `auto`: `sistema` describe el comportamiento con el fallback explícito a inglés.

### D65. Modo de tema

Cerrada al escribir la iteración 1. La iteración 1 no tiene ajustes persistentes, pero conviene fijar ya la forma del modelo para no romperla en la iteración 10.

- **`ThemeMode`:** enum con `SYSTEM`, `LIGHT` y `DARK` en `ui/theme/`. `IronTheme(mode: ThemeMode = ThemeMode.SYSTEM)` resuelve el claro/oscuro.
- **`ThemeSource`:** la UI no recibe el modo directamente; lo recibe por esta interfaz. En la iteración 1 la implementación es `FixedThemeSource`, que devuelve `System` fijo. En la iteración 2 se conecta a `SettingsDataStore` (D37 fija el enum de ajustes de tema). El selector de UI se escribe en la iteración 10.
- Se descarta la bifurcación por `Build.VERSION.SDK_INT >= S` para la paleta de respaldo: con minSdk 31 (D61) dynamic color está disponible siempre, así que no hay paleta de respaldo.
- Se descarta `Theme.Material.Light` como parent en `themes.xml`: Material You resuelve el color en Compose, y el XML solo necesita un tema base day/night sin barra de acción.

### D66. Firma en Gradle

Cerrada al escribir la iteración 1. D54 y D55 fijan la clave y la versión de `apksigner`, pero no decían dónde se firma.

- **Sin `signingConfig` en Gradle.** El APK de release se genera unsigned (`app-release-unsigned.apk`) y se firma fuera del build con `apksigner` de build-tools 34.0.0 (D55).
- `keystore.properties` y los `.jks`/`.keystore` están en `.gitignore` (D48). CI no firma (D22).
- Ventaja de reproducibilidad: el APK unsigned es idéntico antes y después de firmar, que es lo que `apksigcopier` necesita para verificar la firma en F-Droid (D54).

### D67 a D72. Decisiones de implementación

Cerradas durante la implementación de las iteraciones 3 y 4. Son decisiones que el plan no preveía: salen de lo que se encuentra al escribir el código. Las que no condicionan iteraciones posteriores no tienen ID y están en `docs/iterations/`.

- **D67:** con `Flow<T>` Room emite una fila por invalidación, así que una consulta de varias filas deja de ser alcanzable desde la pantalla: solo se recibe la primera emisión. `Flow<List<T>>` es la firma que ya usan `CategoryDao`, `ExerciseDao.search` y `SetDao`, así que se unifica en lugar de dejar las dos conviviendo. Los tests que pasan con `Flow<T>` lo hacen porque provocan la invalidación con una escritura; con la lista se afirma el resultado completo en la emisión inicial.
- **D68:** con la colación BINARY de SQLite, `ORDER BY name` pone `Zapada` antes que `gemelo`, así que el orden no es case-insensitive. Se ordena por `nameKey`, la misma columna de D4, en los cuatro queries de `ExerciseDao` y `CategoryDao`. Es la única forma de que el orden coincida con la unicidad.
- **D69:** `docs/spec/views.md` dice que EjercicioDetalle contiene el CRUD, pero la lista está optimizada para filtrar y acceder, y el CRUD es relevante en el setup inicial y luego casi nunca. El CRUD se queda en las filas de la lista, que es donde está verificado, y el menú de acciones de la identidad se escribe en la iteración 11. El detalle es solo lectura hasta entonces.
- **D70:** el sheet tapa el `SnackbarHost` de la pantalla, y el editor se queda abierto tras el rechazo (D30), así que el mensaje no llegaba a verse. El error se muestra dentro del sheet. La regla afecta al editor de serie de la iteración 5 y al detalle de sesión de la iteración 7, que usan el mismo componente.
- **D71:** un `RadioButton` dentro de una fila clicable no tiene nombre accesible propio: `uiautomator` lo marca `NAF="true"`. TalkBack lo lee por el `Text` hermano, pero el control por sí solo no lo tiene, y eso no cumple el criterio de "listo". Se arregla con `semantics(mergeDescendants = true)` en la fila. La regla aplica a todos los selectores futuros.
- **D72:** Lint de Compose obliga a `LocalResources.current` en lugar de `LocalContext.current.getString`, y a `LocalLocale.current.platformLocale` en lugar de `Locale.getDefault()`. No es solo un aviso de estilo: `Locale.getDefault()` no respeta `localeOverride` de D64, que se aplica de forma explícita en Compose. Por eso `outcomeMessage` recibe `Resources`.

### D73. Diálogos según M3 estricto

Cerrada en el rediseño de vistas de Penpot. Los diálogos de las iteraciones 3 y 4 ya implementados se ajustan en la iteración 4.5.

Se elige la lectura estricta del spec de Material 3 como base del diseño para mantener coherencia con el estándar de la industria. La práctica común de destacar la acción principal con un botón filled se descarta: el spec dice que los diálogos usan text buttons porque la ausencia de contenedor unifica la acción con el texto del diálogo. Los dos diálogos rediseñados (creación y confirmación) deben quedar coherentes entre sí, y lo estaban en estructura pero no en el estilo del botón principal.

La regla completa está en `docs/spec/ux.md` (Diálogos).

### D74 a D76. Decisiones del rediseño de vistas (Penpot)

Cerradas en el rediseño de la pantalla de Ejercicios en Penpot, aplicadas en la iteración 4.5.

- **D74:** la fila de chips se descarta porque duplica el acceso al sheet: el icono de filtro dentro de la search bar mantiene la barra como único punto de entrada. La línea de filtro activo solo aparece cuando hay filtro, y el label es el tipo de filtro, no "Filtrando", porque la línea enuncia lo que está activo, no que existe un filtro. El sheet se renombra a "Filtros" porque ahora tiene dos secciones, Categoría y Estado, y Archivados deja de ser un chip. **Revisada por D78:** el argumento se cae cuando el sheet deja de ser el acceso al filtro, y la línea de filtro activo no es un componente del spec.
- **D75:** el nombre es el texto dominante y la categoría pasa a píldora debajo. La píldora se oculta cuando esa categoría es el filtro activo, porque el chip de categoría de D78 ya enuncia la categoría y la píldora sería redundante en cada fila. El badge Archivado convive en la misma línea de pills.
- **D76:** la regla anterior decía que la creación no heredaba la categoría del filtro, porque el filtro cambiaba lo que se ve, no lo que se asigna. Con el chip de categoría de D78 la intención queda clara, así que el dropdown de creación se prerrellena con la categoría del filtro activo. El filtro pasa a influir en el valor propuesto sin dejar de ser solo un filtro.

### D77. Crear en Ejercicios como FAB

Cerrada al implementar la iteración 4.5. La regla anterior presentaba el crear como una acción pequeña de la barra, no como un FAB, porque la lista es de filtrar y acceder. Con la search bar de D74 la barra quedó ocupada por el acceso al filtro, así que el crear pasa a un FAB: es la única acción persistente de la pantalla, y el FAB es el contenedor estándar de la acción principal en M3. El estado vacío guiado del board mantiene su botón de crear, que se implementa en su bloque. **Revisada por D80:** el FAB sigue siendo el contenedor, pero pasa a FAB menu, porque la creación de categoría necesitaba un acceso y el menú del chip queda para el CRUD.

### D78 a D82. Rediseño del filtrado sobre el spec de M3

Cerradas al contrastar la pantalla de Ejercicios con el spec de M3, en la iteración 4.5. El board de Penpot se rediseña con estas reglas.

- **D78:** el filtro de contenido se hace con chips, que es el componente que M3 define para filtrar, no con una línea de texto. La línea de filtro activo de D74 era un patrón ad hoc. Además el acceso al filtro en un sheet cuesta dos toques y tapa la lista; con chips es un toque y el contenido sigue visible. El chip de categoría abre un `DropdownMenu` porque el catálogo puede tener muchas categorías y una fila de chips con una chip por categoría no cabe en la ventana compacta. El chip de archivado es un toggle, que es la forma natural de un filtro booleano.
- **D79:** M3 permite hasta dos trailing icons en la search bar, y el patrón estándar es la X para borrar el texto. La barra de Iron no tenía forma de limpiar la búsqueda: había que borrar carácter a carácter. Se elige un solo icono con doble función, porque las dos acciones no son simultáneas: con texto se borra, con texto vacío se cancela. Dos iconos habrían cargado la barra para un gesto que casi nunca se da en el mismo momento.
- **D80:** la creación de categoría estaba resuelta como item de acción dentro del sheet, y con D78 el sheet desaparece. Las dos creaciones de la pantalla pasan a un FAB menu: son acciones emparentadas, y M3 define el FAB menu para un grupo de acciones relacionadas. En código se implementa con `FloatingActionButton` + `DropdownMenu` anclado sobre el FAB, porque `FloatingActionButtonMenu` y `ToggleFloatingActionButton` no existen en material3 1.4.0, la versión que fija el BOM 2026.09.00: están en la línea 1.5.0, hoy en beta, y el catálogo de versiones (D20) no fija betas. Es una desviación del componente Expressive, no de la regla.
- **D81:** el chip de archivado obliga a elegir entre "mostrar archivados" y "solo archivados". Se cierra con excluyente, porque un toggle de dos estados no puede expresar tres, y porque el estado "todos incluidos" obligaría a un tercer valor en el filtro. Se acepta como limitación que con el chip activado no se ve a la vez un ejercicio archivado y uno activo. La regla anterior era inclusiva, y el código la implementaba como inclusiva, así que el DAO cambia.
- **D82:** el CRUD de categorías estaba pendiente de acceso. Con el chip como acceso al filtro, el menú del chip es el sitio natural: el chip aporta el contexto de la categoría, así que los items de CRUD solo aparecen para la categoría seleccionada. El sheet se elimina. Los diálogos de confirmación con conteo (D40) y de destino de fusión (D32) siguen en la pantalla, no en el menú.

### D83. Píldora de sin categoría en la fila de lista

Cerrada en la iteración 4.5, al contrastar la fila de lista con el spec de M3. Revisa D75 solo para el caso de la píldora de sin categoría.

- **D83:** la píldora de sin categoría se diferenciaba de las pills de categoría solo por el texto, con el mismo contenedor. Se cierra con la pareja neutra `surface_container` + `on_surface_variant`, que es la lectura M3 de un badge de baja énfasis y una pareja válida en la tabla de pairing de la skill. Se descarta `tertiary_container`: diferencia por tono, no por énfasis, y M3 lo reserva para los acentos. Se descarta `surface_container_low`: queda demasiado cerca de `surface` y la píldora apenas se distingue en la lista. Se descarta mantener `on_surface` sobre `surface_container`: da más contraste, pero la píldora se ve menos tenue. La píldora se lee como badge, que es lo que justifica el shape `full`. El contraste de la pareja neutra es 6,56:1 en light y 9,34:1 en dark con la semilla azul acero del board, por encima del mínimo de 4,5:1 de D35, y pendiente de medir en emulador con dynamic color activo. La información no depende solo del color, porque el label enuncia la categoría vacía.

### D84. El menú del chip no lista "Todas"

Cerrada al implementar la fila de chips de D78 en la iteración 4.5. Revisa D78 solo en el contenido del menú.

- **D84:** el item "Todas" se descarta, porque el chip es el acceso al filtro, no una opción de filtrado, y las opciones del menú son categorías: "Todas" no es una categoría, es el estado sin filtro. Ese estado lo enuncia el label del chip, "Categoría", y limpiar es la X del chip. El X es un clicable anidado que TalkBack fusiona dentro del chip, así que la acción se replica como item de menú cuando hay filtro activo, en la línea de D71, para que siga alcanzable. Se descarta dejar solo la X: la acción dependería del comportamiento de TalkBack con los clicables anidados, que no se puede verificar sin emulador.

### D85. El contenedor de los menús

Cerrada al contrastar los menús de Ejercicios con el spec de M3 en la iteración 4.5.

- **D85:** el contenedor del `DropdownMenu` pasa a `large` (16dp) con `surfaceContainerLow`, los valores del vertical menu expressive, que es la variante que M3 recomienda para diseños nuevos. Se descarta el baseline de 4dp con `surfaceContainer`: sigue disponible en el spec, pero es la lectura antigua, y el shape casi cuadrado no encaja con la filosofía de Iron. El default de Compose es el baseline, así que los dos valores se pasan explícitos en los cinco menús de la app. Se descarta implementar el shape morphing de los items: requiere `DropdownMenuGroup` con `MenuDefaults.itemShape`, y los menús de Iron son acciones, no items seleccionables ni submenús. El item se queda en 48dp de target por D35, por encima de los 44dp del spec. La elevación se mantiene en nivel 2, el mismo token del expressive, con la sombra como convención de plataforma. Pendiente de medir el contraste de `on_surface` sobre `surface_container_low` en emulador con dynamic color activo.

### D86. Leading icon en los items de acción de los menús

Cerrada al contrastar los menús con el spec de M3 en la iteración 4.5.

- **D86:** el leading icon es un elemento opcional de la anatomía del menú, y en los menús de Iron ayuda a escanear un grupo de acciones. Se cierra con los valores del spec: 24dp, `on_surface_variant`, 16dp de espacio leading y 12dp de gap hasta el texto, que son también los defaults que Compose aplica en `DropdownMenuItem`, así que no se fuerza tamaño ni color y solo se pasa el slot. Se aplica a los cinco menús por coherencia con D85, que ya los unificó. Se descarta el icono en los items que son opciones de selección, porque no representan una acción y romperían la alineación con el conteo de resultados. Se descarta la librería `material-icons-core`: de los seis glifos solo trae dos, y los otros cinco obligarían a `material-icons-extended`, una dependencia que el propio androidx desaconseja por su tamaño, y el proyecto ya copia los paths de Material Symbols en drawables propios, así que el glifo es el mismo sin la dependencia. El item destructivo no se tiñe con `error`: el spec no define un color de error para los items de menú, y el `error` de D73 es una regla de diálogos. Pendiente de verificar en emulador el ancho del popup con iconos en la ventana compacta, el contraste de `on_surface_variant` sobre `surface_container_low` con dynamic color activo y que TalkBack lea el item solo con el texto.

## Consecuencias del rediseño de vistas

No tienen ID: son las consecuencias que el rediseño de `docs/spec/views.md` tuvo sobre el esquema, y ya están reflejadas en `docs/spec/data.md`.

- `ExerciseSet.workoutId` es obligatorio, porque con el historial agrupado por sesión las series huérfanas rompen el agrupamiento. La sesión se crea de forma perezosa al registrar.
- Se eliminan `durationMs`, `distanceM` y las conversiones m/km/mi: solo fuerza, con kg como unidad canónica. La métrica RPM desaparece de las gráficas.
- Se elimina `Exercise.displayUnit`: con una sola dimensión de medida, la unidad global de Ajustes es suficiente.
- `graphMetric`, `graphPeriod` y `graphLimit` salen de la entidad `Exercise` y pasan a Ajustes como defaults; el estado de la gráfica es local a la pantalla.
- Métricas de gráfica viables: mejor peso, volumen (peso × reps), repeticiones totales y series por periodo. Cuatro, no más.
- Limitación aceptada para v1: el histórico refleja la configuración actual del plan, no la de entonces. Un snapshot en `Workout` es la única forma de que el pasado sea fiel.


