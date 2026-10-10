<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 0 — Entorno (cerrada)

Bloque de `docs/state/todo.md`. Decisiones del bloque: D61.

- [x] JDK 25 fijado en `.tool-versions`.
- [x] SDK en el SSD con `cmdline-tools/latest`, `platforms;android-37.1`, `build-tools;36.0.0` y `build-tools;34.0.0` (la que firma, D55).
- [x] AVD `iron_api31` con perfil Pixel 6, imagen Google APIs x86_64 (arm64 solo en el dispositivo físico, D61).
- [x] `local.properties` con `sdk.dir`, `ANDROID_HOME` y `ANDROID_AVD_HOME` exportados.
- [x] KVM activo: `kvm_amd` carga, `/dev/kvm` existe, usuario en el grupo `kvm`, AVD arranca con aceleración hardware.

## Notas del bloque

Comprobado el 08/10/2026 en esta máquina:

- asdf tiene el plugin de Java con `temurin-25.0.2+10.0.LTS` **instalada** (D52). Lo que faltaba era fijarla en `.tool-versions`, que es lo que hace que `java -version` resuelva dentro del proyecto.
- asdf tiene Gradle 9.4.1, pero el build usa el wrapper con 9.8.1 (D20), que es lo que ejecuta CI (D49). asdf no fija Gradle en este proyecto.
- La SDK se contiene en el SSD: `/media/dolvem/SSD/apps/Android/Sdk`, con `cmdline-tools/latest`. Están instalados `platforms;android-37.1` (el 36 se sustituyó por el 37.1 el 09/10/2026, porque las versiones fijadas en D20 exigen compileSdk 37), `build-tools;36.0.0`, `build-tools;34.0.0` (la que firma, D55), `platform-tools`, `emulator` y la imagen de sistema Google APIs para API 31. Las licencias se copiaron de `/usr/lib/android-sdk`, que ya tenía el SHA1 aceptado, así que la instalación es no interactiva.
- El AVD de referencia es `iron_api31`, con perfil Pixel 6, y sus datos están en `/media/dolvem/SSD/apps/Android/avd` mediante `ANDROID_AVD_HOME`.
- **La imagen arm64 de D61 no se puede ejecutar en esta máquina.** El emulador en host x86_64 rechaza las imágenes arm64: `Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host`. El AVD usa `system-images;android-31;google_apis;x86_64`. arm64 sigue siendo la ABI del teléfono con Android 15, que es el dispositivo de referencia, así que D61 hay que matizarlo: arm64 para el dispositivo físico, x86_64 para el emulador.
- `local.properties` con `sdk.dir` apuntando a la SDK del SSD, y `ANDROID_HOME` y `ANDROID_AVD_HOME` exportados en `~/.bashrc`.
- **Bloqueante resuelto el 08/10/2026:** SVM activado en la firmware, `kvm_amd` carga, `/dev/kvm` existe y el usuario está en el grupo `kvm`. El AVD arranca con aceleración hardware y `sys.boot_completed` es 1. La fase 0 está cerrada.
- Repositorio git iniciado el 09/10/2026 (D22). `git init` forma parte de la iteración 1.
