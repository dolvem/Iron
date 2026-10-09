// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // D50: ktlint-gradle en el proyecto raíz no cubre los fuentes de los módulos.
    alias(libs.plugins.ktlint)
}

// D23: iron.version en gradle.properties es el punto de verdad único.
val ironVersion = providers.gradleProperty("iron.version").get()
val ironParts = ironVersion.split(".")
val ironVersionCode = ironParts[0].toInt() * 10000 + ironParts[1].toInt() * 100 + ironParts[2].toInt()

android {
    namespace = "com.dolvem.iron"

    // D20: compileSdk 37.1. Las versiones fijadas en el catálogo (Compose BOM 2026.09.00,
    // core-ktx 1.19.1) exigen compileSdk 37, así que el SDK de compilación sube a 37.
    // targetSdk se queda en 36.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.dolvem.iron"
        minSdk = 31
        // D20: targetSdk 37. Con compileSdk 37, dejarlo en 36 activaba OldTargetApi.
        targetSdk = 37
        versionCode = ironVersionCode
        versionName = ironVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // D56: minify con R8 sí, shrinkResources no.
            isMinifyEnabled = true
            isShrinkResources = false
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // D24 regla 1: el APK no lleva información de VCS. En AGP 9.4 vcsInfo vive
            // en el build type, no en el bloque android.
            vcsInfo {
                include = false
            }
        }
    }

    // D53: jvmTarget 17 aunque Gradle corra con JDK 25 (D52).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // D24: las seis reglas de determinismo. Aquí las que el DSL de AGP 9.4 expone;
    // las otras tres (cruncher, APK determinista, ArtProfile) están en gradle.properties
    // porque AGP 9.4 ya no las expone en el bloque android. La regla de densidades se
    // cumple de hecho: no hay PNG en el repo, solo vectoriales.
    // D51: baseline vacío y warnings como error.
    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        baseline = file("lint-baseline.xml")
        lintConfig = file("lint.xml")
    }
}

// D24 regla 6: la tarea ArtProfile se desactiva. AGP 9.4 ya no la expone en el DSL, así
// que la única forma es por nombre de tarea.
tasks.configureEach {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // D19: en v1 no hay tests de UI, así que no se añaden Espresso ni Compose UI test.
    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
