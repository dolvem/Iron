// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Iteración 2: Room 3 solo genera Kotlin y exige KSP, con KSP2 (D20).
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
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

    // Los tests de Room en memoria necesitan ApplicationProvider, que lee el manifiesto y los
    // recursos fusionados. Sin esto, Robolectric no los tiene en el classpath.
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric 4.17 sobre JDK 25: el interceptor de FileDescriptor reflexiona sobre
        // jdk.internal.access.SharedSecrets, que java.base no exporta a los módulos anónimos.
        unitTests.all {
            it.jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED")
        }
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

// D48: el plugin de Room exige schemaDirectory. Los JSON de esquema se versionan en el repo,
// porque los tests de migraciones los usan.
room3 {
    schemaDirectory("$projectDir/schemas")
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
    // Iteración 3: los ViewModel con viewModelFactory { initializer { } } leyendo el contenedor desde
    // la Application, y collectAsStateWithLifecycle en las pantallas.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Iteración 2: Room 3, paquete androidx.room3 (D20). El compilador va por KSP.
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)

    // Iteración 2: Ajustes en DataStore (Preferences), versión fijada en el catálogo (D20).
    implementation(libs.androidx.datastore.preferences)

    // D19: en v1 no hay tests de UI, así que no se añaden Espresso ni Compose UI test.
    testImplementation(libs.junit)
    // D18: Robolectric solo para el código que toca APIs de Android, que aquí es el contexto que
    // necesita Room en memoria. Turbine se añade con la fila de repositorios con Flow.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
