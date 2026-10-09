// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.settings

import kotlin.jvm.JvmInline

// D37: enums de Ajustes, cerrados en la quinta revisión. Viven en este paquete porque el DataStore
// se escribe en la iteración 2 y cerrarlos más tarde obligaría a una migración de claves (D46).
// En Preferences se guardan como String; los dos rangos confirmados de D37 se modelan como value
// classes porque el rango forma parte del tipo, y entran como Int y Double.

enum class StrengthUnit {
    KG,
    LB
}

enum class InputStyle {
    KEYBOARD,
    STEPPER
}

// D37: completa o ISO. El default es completa en ambos idiomas (D63).
enum class LongDateFormat {
    FULL,
    ISO
}

// D37: los tres formatos cortos. El default depende del idioma efectivo (D63).
enum class ShortDateFormat {
    DD_MM_SLASH,
    DD_MM_DASH,
    MM_DD_SLASH
}

// D37: las cuatro métricas viables de gráfica (docs/data_plan.md).
enum class GraphMetric {
    BEST_WEIGHT,
    VOLUME,
    TOTAL_REPS,
    SETS_PER_PERIOD
}

enum class GraphPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

// D45: agrupado es la vista principal del historial; la lista plana es un filtro.
enum class GroupHistory {
    GROUPED,
    FLAT
}

// D64: el selector tiene tres valores, sistema (default), es y en. En el modelo null equivale a
// sistema, así que aquí solo están los dos valores explícitos.
enum class LocaleOverride {
    ES,
    EN
}

// Idioma efectivo tras localeOverride, el que resuelve los defaults de fecha (D63).
enum class Language {
    ES,
    EN
}

// D37: entre 10 y 100 buckets.
@JvmInline
value class GraphLimit(val buckets: Int) {

    init {
        require(buckets in MIN..MAX) { "buckets fuera del rango de D37: $buckets" }
    }

    companion object {

        const val MIN = 10
        const val MAX = 100
    }
}

// D37: entre 0 y 1.
@JvmInline
value class CurveSmoothness(val value: Double) {

    init {
        require(value in MIN..MAX) { "smoothness fuera del rango de D37: $value" }
    }

    companion object {

        const val MIN = 0.0
        const val MAX = 1.0
    }
}
