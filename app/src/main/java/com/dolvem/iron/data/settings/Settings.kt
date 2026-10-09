// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.settings

import com.dolvem.iron.ui.theme.ThemeMode
import java.util.Locale

// Modelo de Ajustes con los campos iniciales de docs/data_plan.md. El modelo guarda los valores ya
// resueltos: los defaults (D63) se aplican en la lectura, solo cuando el campo está sin asignar.
// maxSets y warmupSets siguen siendo nullables porque su default es "sin asignar". curveSmoothness
// es nullable en el modelo, pero el default 0,5 se aplica cuando está sin asignar, así que en la
// lectura resuelta nunca llega null. localeOverride es null cuando el valor es sistema (D64).
data class Settings(
    val themeMode: ThemeMode,
    val strengthUnit: StrengthUnit,
    val longDateFormat: LongDateFormat,
    val shortDateFormat: ShortDateFormat,
    val localeOverride: LocaleOverride?,
    val maxSets: Int?,
    val warmupSets: Int?,
    val showCategories: Boolean,
    val showNotes: Boolean,
    val showGlobalProgress: Boolean,
    val showUnits: Boolean,
    val inputStyle: InputStyle,
    val curveLines: Boolean,
    val curveSmoothness: CurveSmoothness?,
    val defaultGraphMetric: GraphMetric,
    val defaultGraphPeriod: GraphPeriod,
    val defaultGraphLimit: GraphLimit,
    val defaultTimeBasedXAxis: Boolean,
    val repEstimation: Boolean,
    val groupHistory: GroupHistory
)

// D63: el default se aplica solo cuando el campo está sin asignar; un valor explícito del usuario
// gana. Los dos formatos de fecha se resuelven por idioma efectivo, tras localeOverride.
object SettingsDefaults {

    val themeMode: ThemeMode = ThemeMode.SYSTEM
    val strengthUnit: StrengthUnit = StrengthUnit.KG
    val inputStyle: InputStyle = InputStyle.KEYBOARD
    val showCategories: Boolean = true
    val showNotes: Boolean = true
    val showGlobalProgress: Boolean = true
    val showUnits: Boolean = true
    val curveLines: Boolean = true
    val curveSmoothness: CurveSmoothness = CurveSmoothness(0.5)
    val defaultGraphMetric: GraphMetric = GraphMetric.VOLUME
    val defaultGraphPeriod: GraphPeriod = GraphPeriod.WEEK
    val defaultGraphLimit: GraphLimit = GraphLimit(10)
    val defaultTimeBasedXAxis: Boolean = true
    val repEstimation: Boolean = true
    val groupHistory: GroupHistory = GroupHistory.GROUPED

    // D63: completa en ambos idiomas; ISO queda como alternativa del usuario.
    fun longDateFormat(language: Language): LongDateFormat = LongDateFormat.FULL

    // D63: es → dd/MM/yyyy, en → MM/dd/yyyy.
    fun shortDateFormat(language: Language): ShortDateFormat = when (language) {
        Language.ES -> ShortDateFormat.DD_MM_SLASH
        Language.EN -> ShortDateFormat.MM_DD_SLASH
    }
}

// D64: sistema (null) elige el idioma del sistema; si no está soportado, cae en inglés. Se aplica
// de forma explícita y no depende de la resolución de recursos.
fun effectiveLanguage(override: LocaleOverride?, systemLocale: Locale): Language = when (override) {
    LocaleOverride.ES -> Language.ES
    LocaleOverride.EN -> Language.EN
    null -> if (systemLocale.language == "es") Language.ES else Language.EN
}
