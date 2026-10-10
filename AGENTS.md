<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# AGENTS.md

## Checkboxes de `docs/todo.md`

- Los checkboxes se marcan como `[x]` en el momento en que se completa cada punto, no al final de la iteración.
- Un punto se marca solo cuando está verificado: `./gradlew ktlintCheck lint test build` limpio con ese cambio, las mismas cuatro tareas que ejecuta CI (D49).
- No se usa `ktlintFormat` como atajo para pasar el gate: se arregla el código y se vuelve a comprobar.
- Los puntos que solo se verifican en emulador (flujo navegable, targets de 48dp, `contentDescription`, escala de fuente, alternativa textual de las gráficas, D35) no se marcan: se dejan sin marcar y se reportan como pendientes de verificación manual.
- Durante la implementación se editan solo los checkboxes y las notas de `docs/todo.md`. Los demás documentos del plan no se tocan.
- Si un bloque queda en rojo, el checkbox se deja sin marcar y se indica en el resumen, para que el documento refleje el estado real.
- El trabajo se ejecuta bloque a bloque, de forma secuencial, y cada bloque marca los suyos antes de pasar al siguiente.

## Reglas de código

- Cabecera SPDX en cada fichero nuevo, también en los que no son fuente (REUSE, D57).
- Comentarios en español, identificadores en inglés. En un comentario se cita el ID de la decisión (`// D23: …`), no se replica la regla.
- Las versiones se fijan en `gradle/libs.versions.toml`, nunca en cada `dependency` (D20).
- Min SDK 31: sin `Build.VERSION.SDK_INT`, sin `@RequiresApi`, sin desugaring. Fechas solo con `java.time`, nunca `java.util.Date`.
- Room: nunca `fallbackToDestructiveMigration` en builds de release; las migraciones se testean con los JSON de `schemas/` versionados (D48).
- `lint-baseline.xml` se mantiene vacío: un warning se corrige, no se silencia añadiéndolo al baseline ni a `lint.xml` (D51).
- Modelo de errores: `Outcome` solo en las operaciones con error de negocio esperado; las lecturas devuelven Flow directo; la UI no valida, el error siempre viene del repositorio (D5, D6).
- Testing: JUnit4, Room en memoria, Turbine para Flow, Robolectric solo para código que toca APIs de Android. En v1 no hay tests de Compose (D18, D19). `Clock` y `ZoneId` se inyectan desde `AppContainer`, nunca se usan inline en código testeado (D17).
- Las seis reglas de build reproducible (D24, D56) no se tocan.

## Git

- `docs/` se versiona: el plan de desarrollo sube al repositorio.
- No se commitean `local.properties`, claves, `keystore.properties` ni APKs (D48, D54).
