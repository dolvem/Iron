<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# AGENTS.md

## Checkboxes de `docs/iterations/`

- Cada iteración tiene un fichero en `docs/iterations/` con sus checkboxes y sus notas. `docs/state/todo.md` es solo el índice con el estado de cada bloque.
- Los checkboxes se marcan como `[x]` en el momento en que se completa cada punto, no al final de la iteración.
- Un punto se marca solo cuando está verificado: `./gradlew ktlintCheck lint test build` limpio con ese cambio, las mismas cuatro tareas que ejecuta CI (D49).
- No se usa `ktlintFormat` como atajo para pasar el gate: se arregla el código y se vuelve a comprobar.
- Los puntos que solo se verifican en emulador (flujo navegable, targets de 48dp, `contentDescription`, escala de fuente, alternativa textual de las gráficas, D35) no se marcan: se dejan sin marcar y se reportan como pendientes de verificación manual.
- Si un bloque queda en rojo, el checkbox se deja sin marcar y se indica en el resumen, para que el documento refleje el estado real.
- El trabajo se ejecuta bloque a bloque, de forma secuencial, y cada bloque marca los suyos antes de pasar al siguiente.

## Criterio de "listo" por bloque

- `./gradlew build` limpio, sin warnings relevantes.
- Tests unitarios pasando para la lógica nueva (conversiones, analítica, repositorios).
- Flujo navegable en emulador Android.
- Accesibilidad verificada en emulador: targets táctiles de 48dp, `contentDescription` en iconos, escala de fuente, contraste con colores dinámicos y la alternativa textual de los valores por bucket en las gráficas (D35).

## Documentos

- Las notas de verificación, los defectos encontrados y las decisiones de detalle se escriben en el fichero del bloque, bajo un encabezado "Notas del bloque".
- El estado de cada decisión solo vive en `docs/state/decisions.md`; la regla vive en el documento de su ámbito (`docs/spec/`). El historial de revisiones solo se narra en `docs/state/decisions.md`, y el estado de cada iteración solo en su fichero y en `docs/state/todo.md`.
- Una decisión recibe ID D cuando condiciona trabajo de iteraciones posteriores. Las decisiones de detalle se quedan como nota en el fichero del bloque, sin ID.
- Cuando una decisión cambia una regla, se actualiza el documento de su ámbito citando el ID en el momento en que se decide, no al final de la iteración: una regla que contradice al fichero del bloque deja de ser la fuente de verdad.

## Reglas de código

- Cabecera SPDX en cada fichero nuevo, también en los que no son fuente (REUSE, D57).
- Comentarios en español, identificadores en inglés. En un comentario se cita el ID de la decisión (`// D23: …`), no se replica la regla.
- Las versiones se fijan en `gradle/libs.versions.toml`, nunca en cada `dependency` (D20).
- Min SDK 31: sin `Build.VERSION.SDK_INT`, sin `@RequiresApi`, sin desugaring. Fechas solo con `java.time`, nunca `java.util.Date`.
- Room: nunca `fallbackToDestructiveMigration` en builds de release; las migraciones se testean con los JSON de `schemas/` versionados (D48).
- `lint-baseline.xml` se mantiene vacío: un warning se corrige, no se silencia añadiéndolo al baseline ni a `lint.xml` (D51).
- Modelo de errores: `Outcome` solo en las operaciones con error de negocio esperado; las lecturas devuelven Flow directo; la UI no valida, el error siempre viene del repositorio (D5, D6).
- Testing: JUnit4, Room en memoria, Turbine para Flow, Robolectric solo para código que toca APIs de Android. En v1 no hay tests de Compose (D18, D19). `Clock` y `ZoneId` se inyectan desde `AppContainer`, nunca se usan inline en código testeado (D17).
- Firmas de lectura: las consultas de varias filas devuelven `Flow<List<T>>`; `Flow<T>` solo para una fila (D67).
- Compose: `LocalResources.current` y `LocalLocale.current.platformLocale`, no `LocalContext` ni `Locale.getDefault()` (D72).
- Las seis reglas de build reproducible (D24, D56) no se tocan.

## Git

- `docs/` se versiona: el plan de desarrollo sube al repositorio.
- No se commitean `local.properties`, claves, `keystore.properties` ni APKs (D48, D54).
