<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iron

App Android nativa de registro de entrenamiento de fuerza, en Kotlin y Jetpack Compose.

Licencia: GPL-3.0-or-later. Ver `LICENSE` y la carpeta `LICENSES/` (REUSE).

## Entorno

- JDK 25 fijado en `.tool-versions` (D52).
- Gradle 9.8.1 mediante el wrapper (D20).
- Android SDK 37.1 con build-tools 36.0.0 y 34.0.0 (esta última solo para firmar, D55).
- `local.properties` con `sdk.dir` y `ANDROID_AVD_HOME` exportado.

## Tareas

```bash
./gradlew ktlintCheck lint test build
```

CI ejecuta exactamente esas cuatro tareas en un único job (D49). CI no firma: genera el APK unsigned y la firma se hace fuera del build con `apksigner` de build-tools 34.0.0 (D66).
