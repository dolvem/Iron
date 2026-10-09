// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    // D50: ktlint-gradle en el proyecto raíz, con code_style android en .editorconfig.
    alias(libs.plugins.ktlint)
}
