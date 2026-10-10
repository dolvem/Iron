<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Decisiones: estado tras la séptima revisión del 08/10/2026

Parte de `docs/README.md`. Los números D1–D64 son la referencia cruzada entre documentos. Los D1–D46 no cambian; D47–D61 se añaden al cerrar el bloque de build y distribución, D62 en la cuarta revisión, y D63 y D64 en la séptima revisión.

Aquí solo se guarda el estado y la justificación de cada decisión. La regla está en el documento de su ámbito, que se indica en la columna "Ámbito". La columna "Bloquea" referencia las iteraciones de `docs/README.md`.

Las cerradas se aplican como está escrito en el documento de su ámbito. El bloque de la capa de datos se cerró en la segunda revisión del 08/10/2026. El bloque de build y distribución se cerró en la tercera revisión del 08/10/2026. En la cuarta revisión se cierran D4, D5, D26 y D62, que son las cuatro decisiones de la iteración 2 que tenían un conflicto entre documentos. En la quinta revisión se cierran las siete pendientes de la iteración 2 (D6, D7, D8, D10, D17, D18, D46) y D37, que se adelanta porque el DataStore se escribe en la iteración 2. En la sexta revisión se cierran las dieciséis que quedaban: D11 a D16, D19, D28 a D30, D32 a D35, D39 y D40. En la séptima revisión se cierran D63 y D64, los dos defaults de ajustes que quedaban por decidir: el default de los formatos de fecha por idioma y el selector de idioma. Con esto no queda ninguna decisión pendiente.

## Resumen

| # | Decisión | Estado | Bloquea | Ámbito |
|---|---|---|---|---|
| D1 | Sesión abierta al cambiar de día | **Cerrada:** cierre automático al registrar la primera serie de un día distinto | 2 | rules_plan |
| D2 | Cómo se distingue una serie de calentamiento | **Cerrada:** `isWarmup` con valor automático por posición, editable en el detalle | 2 | rules_plan |
| D3 | Día de la sesión | **Cerrada:** `sessionDate` inmutable en `Workout` | 2 | rules_plan |
| D4 | Índices únicos case-insensitive | **Cerrada:** columna normalizada derivada `nameKey` con índice único, calculada en Kotlin con NFC y `Locale.ROOT` | 2 | rules_plan |
| D5 | Tipo de retorno de los repositorios | **Cerrada:** `Outcome` sellado, con preconsulta de conteo separada del borrado, y la violación de único de la BD mapeada a error de negocio | 2 | engineering_plan |
| D6 | Validación de métricas | **Cerrada:** `reps >= 1`, `loadKg > 0`, al menos una métrica, sin techo; valida el repositorio antes del `CASCADE` y la UI muestra el error | 2 | rules_plan |
| D7 | Ventana actual y buckets vacíos | **Cerrada:** semana ISO en curso; hueco, no cero; ventana vacía muestra la última semana con datos | 2 | rules_plan |
| D8 | Qué cuenta el progreso global | **Cerrada:** sesiones libres en volumen y conteo, no en adherencia ni en frecuencia | 2 | rules_plan |
| D9 | Gráficas y calentamiento | **Cerrada:** excluidas siempre, en PR, volumen, repeticiones y conteo | 2 | rules_plan |
| D10 | Redondeo y precisión de unidades | **Cerrada:** 2 decimales al mostrar y al guardar, medio arriba; 0,5 kg en 1RM | 2 | rules_plan |
| D11 | Algoritmo de fusión en el import | **Cerrada:** dos pasadas con mapa `incomingId -> existingId`, gana el nombre existente, y en duplicados del fichero gana la primera ocurrencia | 9 | rules_plan |
| D12 | Backup automático previo | **Cerrada:** `filesDir/backups/`, retención de los 3 más recientes, GZIP, sin SAF, sin UI de restauración | 9 | rules_plan |
| D13 | Ajustes en el backup | **Cerrada:** no entran en v1, y se acepta perderlos al migrar de dispositivo | 9 | data_plan |
| D14 | Versiones del formato de backup | **Cerrada:** el importador acepta `version <= current` y rechaza `app` distinto de `iron` | 9 | rules_plan |
| D15 | Alcance del export CSV | **Cerrada:** ventana actual de la pantalla; una ventana vacía exporta cabeceras sin filas | 9 | rules_plan |
| D16 | Convenciones del JSON | **Cerrada:** camelCase, nulls escritos como `null`, DTOs separados de las entidades | 9 | data_plan |
| D17 | Inyección de `Clock` y `ZoneId` | **Cerrada:** en `AppContainer` con defaults, inyectados por constructor en repositorios y Analytics | 2 | engineering_plan |
| D18 | Matriz de tests | **Cerrada:** la tabla de `engineering_plan`, más validación de métricas, ventana vacía y migración de claves | 2 | engineering_plan |
| D19 | Tests de Compose en v1 | **Cerrada:** ninguno, ni sobre Robolectric; el criterio de listo se cumple en emulador, con accesibilidad verificada a mano | 11 | engineering_plan |
| D20 | Versiones sin fijar en el catálogo | **Cerrada:** fijadas en `libs.versions.toml`, con KSP2 | 1 | engineering_plan |
| D21 | Licencia | **Cerrada:** GPL-3.0-or-later | 1 | README |
| D22 | Repositorio git y CI | **Cerrada:** GitHub público, job único `check` en push y PR, CI no firma | 1 | engineering_plan |
| D23 | `versionCode` y `versionName` | **Cerrada:** `iron.version` en `gradle.properties` como punto de verdad único | 1 | engineering_plan |
| D24 | Build reproducible y firma | **Cerrada:** seis reglas de determinismo, firma v2 + v3, clave local fuera del repo | 1 | engineering_plan |
| D25 | Icono de aplicación | **Cerrada:** adaptive icon con fondo `#1B1B1D`, foreground de paleta y capa monochrome | 1 | engineering_plan |
| D26 | Convención de nombres en la BD | **Cerrada:** camelCase en la BD y en el JSON, sin `@ColumnInfo`, con la normalización como columna propia | 2 | data_plan |
| D27 | ktlint y Lint | **Cerrada:** ktlint-gradle con `code_style = android`, Lint con warnings como error y baseline vacío | 1 | engineering_plan |
| D28 | Teclado decimal y separador | **Cerrada:** solo en modo teclado, redondeo al guardar, separador mostrado según el idioma activo | 5 | ux_plan |
| D29 | Placas por unidad | **Cerrada:** accesos rápidos de suma; kg 1,25 / 2,5 / 5 / 10 / 20 / 25; lb 2,5 / 5 / 10 / 25 / 35 / 45 | 5 | ux_plan |
| D30 | Comportamiento del editor de serie | **Cerrada:** se mantiene abierto, foco al siguiente campo, stepper de 1 a 100 con estado vacío | 5 | ux_plan |
| D31 | Búsqueda en la lista de ejercicios | **Cerrada:** caja de búsqueda case-insensitive en la barra de filtros | 3 | ux_plan |
| D32 | Fusión de ejercicios y categorías | **Cerrada:** el usuario elige el destino, sobrevive su nombre, gana la fila existente en el índice único, `archived` y `notes` el menos restrictivo | 3 | ux_plan |
| D33 | Borrado de plan con sesiones | **Cerrada:** la confirmación cuenta las sesiones que quedan como sesiones libres | 4 | ux_plan |
| D34 | Hoy recalculado a medianoche | **Cerrada:** ticker con `delayUntilMidnight`, trigger compartido con el cierre automático de D1 | 6 | ux_plan |
| D35 | Accesibilidad de las gráficas | **Cerrada:** sección plegable con la etiqueta del bucket y el valor de la métrica | 8 | ux_plan |
| D36 | Idioma base | **Cerrada:** `values/` en inglés, `values-es` en español | 1 | README |
| D37 | Enums de ajustes | **Cerrada:** `inputStyle`, formatos de fecha, 10 a 100 buckets, suavidad de 0 a 1 | 10 | data_plan |
| D38 | Orientación | **Cerrada:** portrait en el manifest, regla de Lint desactivada en `lint.xml` | 1 | ux_plan |
| D39 | Sesión vacía | **Cerrada:** una sesión abierta se elimina al borrar su última serie; una cerrada vacía se conserva y aparece en el historial | 5 | rules_plan |
| D40 | Ventana de deshacer | **Cerrada:** 6 s, solo en series, recrea la sesión, conserva `id` y `startedAt` | 5 | ux_plan |
| D41 | Checklist de la sesión | **Cerrada:** solo lectura, derivado de las series | 5 | ux_plan |
| D42 | Campos editables de una serie | **Cerrada:** métricas, notas, `isWarmup` y `timestamp`; el ejercicio no | 5 | ux_plan |
| D43 | Cerrar desde el historial | **Cerrada:** sí, y una sesión cerrada no se reabre | 7 | rules_plan |
| D44 | Cierre automático en el historial | **Cerrada:** se distingue del explícito | 7 | ux_plan |
| D45 | Sesiones libres en el historial | **Cerrada:** título fijo y filtro propio | 7 | ux_plan |
| D46 | Versionado de claves de DataStore | **Cerrada:** entero de versión con migración vacía, versión leída antes de cualquier acceso | 2 | data_plan |
| D47 | Visibilidad del repositorio | **Cerrada:** GitHub público | 1 | engineering_plan |
| D48 | Contenido de `.gitignore` | **Cerrada:** `schemas/` y `.tool-versions` se versionan | 1 | engineering_plan |
| D49 | CI: runner, JDK, triggers y caché | **Cerrada:** `ubuntu-latest`, JDK 25, push y PR, acciones por tag de major, caché de Gradle | 1 | engineering_plan |
| D50 | Plugin de ktlint y code style | **Cerrada:** ktlint-gradle con `code_style = android` | 1 | engineering_plan |
| D51 | Baseline de Lint y severidades | **Cerrada:** baseline vacío, warnings como error | 1 | engineering_plan |
| D52 | JDK de build | **Cerrada:** 25, con `jvmTarget` 17 | 1 | engineering_plan |
| D53 | `jvmTarget` | **Cerrada:** 17, aunque Gradle corra con JDK 25 | 1 | engineering_plan |
| D54 | Clave de firma | **Cerrada:** local, fuera del repo, CI no firma | 1 | engineering_plan |
| D55 | Versión de `apksigner` | **Cerrada:** build-tools 34.0.0 solo para firmar | 1 | engineering_plan |
| D56 | R8 y shrinkResources | **Cerrada:** minify sí, shrinkResources no | 1 | engineering_plan |
| D57 | REUSE y cabeceras SPDX | **Cerrada:** SPDX en cada fichero y carpeta `LICENSES/` | 1 | engineering_plan |
| D58 | Proceso de release | **Cerrada:** tags `v1.0.0`, GitHub Releases, `CHANGELOG.md` | 1 | engineering_plan |
| D59 | Metadatos de F-Droid | **Cerrada:** `fdroiddata` por merge request, categoría Sports, `Binaries` apuntando a las releases | 1 | engineering_plan |
| D60 | Diseño del icono | **Cerrada:** glifo de paleta de trazo simple | 1 | engineering_plan |
| D61 | AVD de referencia | **Cerrada:** API 31, imagen Google APIs. ABI por host: x86_64 en el emulador, arm64 en el dispositivo físico | 0 | engineering_plan |
| D62 | TypeConverters de `sessionDate` y `closedBy` | **Cerrada:** TEXT ISO para `sessionDate`, TEXT con enum para `closedBy`, `Plan.days` parseado en el dominio | 2 | data_plan |
| D63 | Default de los formatos de fecha | **Cerrada:** se resuelve por idioma efectivo; `shortDateFormat` es → `dd/MM/yyyy` y en → `MM/dd/yyyy`, `longDateFormat` completa en ambos | 2 | data_plan |
| D64 | Selector de idioma | **Cerrada:** `sistema` (default), `es` y `en`; `sistema` elige el idioma del sistema y cae en inglés si no está soportado | 2 | data_plan |
| D65 | Modo de tema en la iteración 1 | **Cerrada:** `ThemeMode` con `SYSTEM`/`LIGHT`/`DARK` en `ui/theme/`; `ThemeSource` devuelve `System` fijo en la iteración 1, el selector UI llega en la 10 | 1 | engineering_plan |
| D66 | Firma en Gradle | **Cerrada:** sin `signingConfig`; el APK se genera unsigned y se firma fuera de Gradle con `apksigner` (D55) y `keystore.properties` en `.gitignore` | 1 | engineering_plan |

## Sub-reglas que salen de las decisiones cerradas

- `endedAt` del cierre automático se fija en el momento del cierre, no en el timestamp de la serie que lo provoca.
- Deshacer una serie que causó un cierre automático revierte también el cierre.
- Cuando una serie provoca un cierre automático y además era la última de la sesión, el undo primero recrea la sesión y después revierte el cierre. En ese caso el undo no es un solo insert: recrea la fila padre.
- El undo recrea la sesión cuando el borrado de la última serie la eliminó, y conserva el `id` y el `startedAt` originales, porque deshacer devuelve el estado anterior y no crea uno nuevo.
- Una sesión que nunca tuvo series no existe, porque la apertura es perezosa. Una sesión abierta se elimina al borrar su última serie. Una cerrada a la que se le borran todas las series se conserva y aparece en el historial con un estado vacío.
- El botón de cerrar se oculta cuando la sesión tiene 0 series, porque no hay nada que cerrar.
- Los botones de placas son accesos rápidos de suma, no un inventario de placas físicas.
- La normalización del separador decimal es parsing de entrada. La visualización usa el separador del idioma activo, y las dos cosas son independientes.
- El ticker que recalcula Hoy a medianoche es el mismo que provoca el cierre automático de D1, así que hay una sola pieza.
- La alternativa textual de las gráficas es una sección plegable siempre disponible, y no depende de detectar si hay servicios de accesibilidad activos.
- La accesibilidad se verifica en emulador, porque D19 deja la UI fuera de los tests.
- `isWarmup` se fija en el insert según el número de series ya registradas para esa actividad en esa sesión, y no se recalcula al borrar o editar. Si se borra una serie de calentamiento, las restantes mantienen el flag.
- Quick log siempre genera series de trabajo, porque no hay contexto de plan.
- `sessionDate` es inmutable, se fija en la apertura perezosa en la zona local de ese momento, y el historial agrupa por esa columna.
- Con `values/` en inglés, `localeOverride` se aplica de forma explícita en Compose y no depende de la resolución de recursos. Los `plurals.xml` existen en las dos carpetas, con reglas distintas en cada idioma.
- El APK de release se construye desde el commit etiquetado limpio, sin artefactos de builds anteriores.
- La clave de firma se copia de seguridad. Perderla obliga a reinstalar a todos los usuarios, porque Android no permite actualizar con una clave distinta.
- `nameKey` se calcula en el repositorio con NFC y `Locale.ROOT`, en el insert y en el renombrar. Es la única forma de que la base de datos y el repositorio coincidan con acentos, porque el `COLLATE NOCASE` de SQLite y su `lower()` solo pliegan ASCII.
- `nameKey` es derivada y no entra en el JSON de backup: se excluye del DTO de D16.
- La búsqueda de D31 usa la misma columna normalizada, porque `LIKE` en SQLite solo es case-insensitive en ASCII.
- La ida y vuelta kg → lb → kg no es estable con dos decimales, y eso se acepta como limitación de D10.
- El valor guardado es la fuente de verdad: mostrar nunca redondea sobre un valor ya redondeado.
- La UI no valida métricas. El rechazo lo produce el repositorio antes del `CASCADE`, y el editor de serie muestra el error de `Outcome`.
- La migración de claves de DataStore solo puede operar sobre un listado conocido, porque Preferences no tiene esquema ni permite enumerar claves.
- El default de un ajuste solo se aplica cuando el campo está sin asignar; un valor explícito del usuario gana sobre el default por idioma.
- El default de los formatos de fecha se resuelve por idioma efectivo, tras `localeOverride`, y no por el idioma bruto del sistema.

## Detalle por decisión

### D1 a D3. Cerradas

- **D1:** regla en `docs/rules_plan.md` (Sesiones). La alternativa descartada era permitir sesiones multiday, que habría eliminado la frontera `(planId, día)`. La otra alternativa descartada era desplazar el inicio del día a las 4:00 mediante un ajuste; se rechaza porque entrenar después de medianoche es poco frecuente y una sesión que cruza medianoche se parte en dos, lo que se acepta como limitación. La regla afecta igual al quick log.
- **D2:** regla en `docs/rules_plan.md` (Progreso y métricas) y `docs/ux_plan.md`. Se descarta el toggle en el editor de serie: es el formulario más repetido de la app y no conviene cargarlo.
- **D3:** regla en `docs/rules_plan.md` (Tiempo y frontera de día). Con D1 deja de ser opcional, porque hace falta conocer el día de la sesión para decidir si una serie escribe en ella, y de paso elimina la limitación de la zona horaria.

### D4. Nombres únicos

Cerrada en la cuarta revisión. La opción de `COLLATE NOCASE` se descarta: el collation de SQLite y su `lower()` solo pliegan ASCII, así que `Péculo` y `péculo` serían dos valores distintos para el índice, y la divergencia entre la base de datos y el repositorio que se quería eliminar seguiría ahí.

Se cierra con una columna normalizada derivada `nameKey` en `Category` y `Exercise`, con índice único, calculada en Kotlin con NFC y `Locale.ROOT`. La normalización no la puede hacer SQLite, así que la hace una única función en `domain/model`, usada por el insert, el renombrar y la búsqueda. La base de datos aplica el mismo criterio que el repositorio, y la comprobación del repositorio queda solo para generar el mensaje de error.

Consecuencias: D26 se mantiene sin excepción, porque la normalización es una columna propia y no un cambio de nombre; `nameKey` no entra en el JSON de backup; y la búsqueda de D31 usa la misma columna.

### D5 a D10. Modelo de errores y definiciones de cálculo

- **D5:** cerrada en la cuarta revisión, desarrollada en `docs/engineering_plan.md` (Modelo de errores). El tipo se llama `Outcome`, porque `Result` choca con `kotlin.Result`. Se usa solo en las operaciones con error de negocio esperado (borrados, renombrar, fusión, inserción de serie, cierre de sesión); las lecturas devuelven Flow directo. La reserva de excepciones para I/O y base de datos se matiza: la violación de único se captura en el repositorio y se mapea a error de negocio, porque la comprobación previa no cubre la carrera entre la comprobación y el insert. Sin esto no se puede implementar la confirmación con conteo de afectados.
- **D6:** cerrada en la quinta revisión. Se rechaza cero y negativos, `reps >= 1` y `loadKg > 0`, con al menos una métrica. Valida el repositorio antes del `CASCADE`, que es donde la serie se inserta y donde el quick log entra por el mismo camino. La UI no valida: muestra el error que devuelve `Outcome`. Se descarta el techo: rechazar un valor alto obligaría a poner un límite que no corresponde con un peso real, y una entrada absurda por un dedo resbalado en el teclado decimal se acepta como limitación documentada.
- **D7:** cerrada en la quinta revisión. Semana ISO en curso, hueco en la serie temporal y cero en el conteo de series por periodo, como estaba. Se cierra el caso de la ventana vacía: se muestra el valor de la última semana con datos y se etiqueta la ventana, porque un hueco en la tarjeta de Hoy se lee como "no he entrenado" cuando la semana anterior sí tiene datos.
- **D8:** cerrada en la quinta revisión. Las sesiones libres cuentan en volumen global y conteo de series, y no cuentan en la adherencia de ningún plan ni en la frecuencia del detalle del plan. No cuentan en frecuencia porque `Workout.planId` es nulo, así que no hay plan al que atribuir la sesión. Frecuencia y adherencia se escriben por separado porque no son la misma métrica.
- **D9:** cerrada en la cuarta revisión: las métricas excluyen las series de calentamiento.
- **D10:** cerrada en la quinta revisión. Se cambia el criterio de 1 decimal: mostrar y guardar con 2 decimales, medio arriba cuando hay más. Así el redondeo de mostrar y el de guardar son el mismo criterio, y el valor guardado es la fuente de verdad: mostrar nunca redondea sobre un valor ya redondeado. El 1RM mantiene 0,5 kg porque es una estimación y no un dato registrado, y es lo que espera alguien mirando una placa de 2,5 kg. La ida y vuelta kg → lb → kg sigue siendo inestable con 2 decimales y se acepta como limitación.

### D11 a D16. Import y export

Cerradas en la sexta revisión. El formato está definido en `docs/data_plan.md`, el procedimiento en `docs/rules_plan.md`.

- **D11:** dos pasadas con mapa `incomingId -> existingId`, raíz primero y dependientes después, porque sin el mapa resuelto no se pueden remapear las FKs de `planExercises`, `workouts` y `sets`. En una colisión de nombre gana el nombre existente, porque en el import la base destino es el destino, igual que en la fusión manual de D32. Los nombres duplicados dentro del propio fichero se resuelven con la primera ocurrencia, y las siguientes se mapean a ella, coherente con el modo de fusión. La colisión en `unique(planId, exerciseId)` es la misma regla que D32: gana la fila existente y el conflicto se registra en el resumen.
- **D12:** `filesDir/backups/`, retención de los 3 más recientes, sin acceso por SAF, con GZIP. Son una red de seguridad interna, sin UI de restauración, porque una UI de restauración obligaría resolver otra vez el algoritmo de fusión sobre un backup interno.
- **D13:** los ajustes no entran en v1, coherente con que viven en DataStore, fuera de la BD. La consecuencia es que migrar de dispositivo pierde los ajustes, y se acepta.
- **D14:** el importador acepta `version <= current` y rechaza `app` distinto de `iron`. Una versión mayor que `current` se rechaza con error. Sin la comprobación de `app`, un fichero de otra app con `version: 1` pasaría el filtro.
- **D15:** ventana actual de la pantalla, no toda la tabla, porque el CSV refleja lo que se está viendo. Una ventana vacía exporta cabeceras sin filas, porque el CSV siempre tiene cabeceras. Respeta la exclusión del calentamiento de D9.
- **D16:** camelCase, nulls escritos como `null` y no omitidos, DTOs separados de las entidades, DTOs excluyen `nameKey`. Escribir nulls en lugar de omitirlos hace que un lector no tenga que distinguir entre campo ausente y campo nulo, y el JSON es más estable frente a cambios de esquema.

### D17 a D19. Testing

**D17 y D18 están cerradas** en la quinta revisión. **D19 se cierra en la sexta revisión.** La matriz está en `docs/engineering_plan.md`. D17 es la condición que hace deterministas los tests de frontera de día y semana ISO.

**D17:** se cierra con `Clock` y `ZoneId` creados en `AppContainer` con `Clock.systemUTC()` y `ZoneId.systemDefault()` como defaults, e inyectados por constructor en los repositorios y en `Analytics`. Se descarta crear el `Clock` dentro de cada repositorio, porque entonces el test tendría que reconstruir el repositorio entero. Hace falta los dos, no solo `Clock`: `sessionDate` se fija en la zona de apertura, así que la frontera de día no es determinista con solo `Clock`.

**D18:** se cierra con la matriz actual más tres filas: validación de métricas (D6), ventana actual vacía con fallback a la última semana con datos (D7), y migración de claves de DataStore (D46). Los dos casos añadidos en la cuarta revisión se confirman: sesión libre abierta por día con `planId IS NULL`, porque la matriz solo cubre "una sesión activa por plan" y la regla extiende la frontera a las sesiones libres, y la preconsulta de conteo antes del borrado, que es la pieza de D5 que hace falta para la confirmación. El caso de `closedBy` distinguyendo cierre automático de explícito ya estaba en la tabla y se confirma como parte de la matriz. En la sexta revisión se añade una fila: undo que recrea una sesión eliminada al borrar su última serie, porque con D39 el undo deja de ser un solo insert.

**D19:** en v1 no hay tests de Compose, ni sobre Robolectric, y el criterio de "listo" se cumple en emulador. La consecuencia es que la alternativa textual de D35 no tiene verificación automática y se verifica a mano, así que la accesibilidad se añade como item del criterio de "listo" en `docs/README.md`. CI no ejecuta nada de UI: el job único `check` de D49 corre ktlint, Lint y tests unitarios.

### D28 a D46. UX operativa

Cerradas en la sexta revisión, desarrolladas en `docs/ux_plan.md`. Las que ya estaban cerradas: D31 (búsqueda), D37 (enums de ajustes), D38 (orientación), D41 (checklist de solo lectura), D42 (campos editables de una serie), D43 (cierre desde el historial sin reapertura), D44 (cierre automático distinguido), D45 (sesiones libres con título fijo y filtro propio) y D46 (versionado de claves).

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
