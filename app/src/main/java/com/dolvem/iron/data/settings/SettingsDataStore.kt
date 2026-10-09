// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dolvem.iron.ui.theme.ThemeMode
import com.dolvem.iron.ui.theme.ThemeSource
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// D46: Preferences no tiene esquema ni permite enumerar claves, así que la migración solo puede
// operar sobre el listado conocido. La propia clave de versión no entra en el listado: es la que se
// lee antes de cualquier acceso.
object SettingsKeys {

    val version: Preferences.Key<Int> = intPreferencesKey("version")

    val themeMode: Preferences.Key<String> = stringPreferencesKey("themeMode")
    val strengthUnit: Preferences.Key<String> = stringPreferencesKey("strengthUnit")
    val longDateFormat: Preferences.Key<String> = stringPreferencesKey("longDateFormat")
    val shortDateFormat: Preferences.Key<String> = stringPreferencesKey("shortDateFormat")
    val localeOverride: Preferences.Key<String> = stringPreferencesKey("localeOverride")
    val maxSets: Preferences.Key<Int> = intPreferencesKey("maxSets")
    val warmupSets: Preferences.Key<Int> = intPreferencesKey("warmupSets")
    val showCategories: Preferences.Key<Boolean> = booleanPreferencesKey("showCategories")
    val showNotes: Preferences.Key<Boolean> = booleanPreferencesKey("showNotes")
    val showGlobalProgress: Preferences.Key<Boolean> = booleanPreferencesKey("showGlobalProgress")
    val showUnits: Preferences.Key<Boolean> = booleanPreferencesKey("showUnits")
    val inputStyle: Preferences.Key<String> = stringPreferencesKey("inputStyle")
    val curveLines: Preferences.Key<Boolean> = booleanPreferencesKey("curveLines")
    val curveSmoothness: Preferences.Key<Double> = doublePreferencesKey("curveSmoothness")
    val defaultGraphMetric: Preferences.Key<String> = stringPreferencesKey("defaultGraphMetric")
    val defaultGraphPeriod: Preferences.Key<String> = stringPreferencesKey("defaultGraphPeriod")
    val defaultGraphLimit: Preferences.Key<Int> = intPreferencesKey("defaultGraphLimit")
    val defaultTimeBasedXAxis: Preferences.Key<Boolean> = booleanPreferencesKey("defaultTimeBasedXAxis")
    val repEstimation: Preferences.Key<Boolean> = booleanPreferencesKey("repEstimation")
    val groupHistory: Preferences.Key<String> = stringPreferencesKey("groupHistory")

    // D46: el único listado sobre el que la migración puede operar.
    val known: List<Preferences.Key<*>> = listOf(
        themeMode, strengthUnit, longDateFormat, shortDateFormat, localeOverride, maxSets, warmupSets,
        showCategories, showNotes, showGlobalProgress, showUnits, inputStyle, curveLines,
        curveSmoothness, defaultGraphMetric, defaultGraphPeriod, defaultGraphLimit,
        defaultTimeBasedXAxis, repEstimation, groupHistory
    )

    const val BASE_VERSION = 0
    const val CURRENT_VERSION = 1
}

// D65: SettingsDataStore es la implementación de ThemeSource, la que sustituye a FixedThemeSource de
// la iteración 1. El DataStore entra por constructor, como la base de datos en AppContainer, así el
// test puede inyectar uno sobre un fichero temporal.
class SettingsDataStore(
    private val datastore: DataStore<Preferences>,
    private val systemLocale: Locale = Locale.getDefault()
) : ThemeSource {

    // D65: ThemeSource.mode es una propiedad y DataStore es reactivo. Se resuelve con el último valor
    // conocido: cada emisión del Flow actualiza cachedMode, y observe() lanza el collector. IronApp
    // llama a observe() en onCreate con su propio scope, así MainActivity no cambia: sigue leyendo
    // app.themeSource.mode en Compose. Antes de la primera emisión, mode devuelve el default SYSTEM
    // (D63). La recomposición cuando el usuario cambia el tema llega por el Flow de la pantalla de
    // Ajustes (iteración 10).
    private var cachedMode: ThemeMode = SettingsDefaults.themeMode

    override val mode: ThemeMode
        get() = cachedMode

    // D46: la versión se lee antes de cualquier acceso a las claves. La migración solo puede operar
    // sobre el listado conocido, y se descarta reconstruir el Settings en lectura: trabaja sobre
    // claves, no sobre el modelo.
    val settings: Flow<Settings> = flow {
        val snapshot = datastore.data.first()
        val version = snapshot[SettingsKeys.version] ?: SettingsKeys.BASE_VERSION
        if (version != SettingsKeys.CURRENT_VERSION) {
            val migrated = migrate(snapshot, version)
            datastore.edit { prefs ->
                migrated.forEach { (key, value) -> putUnchecked(prefs, key, value) }
                prefs[SettingsKeys.version] = SettingsKeys.CURRENT_VERSION
            }
        }
        emitAll(datastore.data.map { resolve(it) })
    }.onEach { cachedMode = it.themeMode }

    fun observe(scope: CoroutineScope) {
        scope.launch { settings.collect { } }
    }

    // D46: función de migración vacía, que se rellena cuando haya el primer cambio real de clave.
    // Hasta entonces solo conserva las claves del listado conocido, porque fuera de él Preferences no
    // permite enumerar nada.
    private fun migrate(source: Preferences, from: Int): Map<Preferences.Key<*>, Any> =
        source.asMap().filterKeys { SettingsKeys.known.contains(it) }

    @Suppress("UNCHECKED_CAST")
    private fun putUnchecked(prefs: MutablePreferences, key: Preferences.Key<*>, value: Any) {
        prefs[key as Preferences.Key<Any>] = value
    }

    // D63: un null borra la clave, así el campo vuelve a estar sin asignar y el default vuelve a
    // aplicarse. Los enums se guardan con su nombre; los dos rangos de D37 entran como Int y Double.
    private suspend fun <T> put(key: Preferences.Key<T>, value: T?) {
        datastore.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = put(SettingsKeys.themeMode, mode.name)

    suspend fun setStrengthUnit(unit: StrengthUnit) = put(SettingsKeys.strengthUnit, unit.name)

    suspend fun setLongDateFormat(format: LongDateFormat) = put(SettingsKeys.longDateFormat, format.name)

    suspend fun setShortDateFormat(format: ShortDateFormat) = put(SettingsKeys.shortDateFormat, format.name)

    suspend fun setLocaleOverride(override: LocaleOverride?) = put(SettingsKeys.localeOverride, override?.name)

    suspend fun setMaxSets(sets: Int?) = put(SettingsKeys.maxSets, sets)

    suspend fun setWarmupSets(sets: Int?) = put(SettingsKeys.warmupSets, sets)

    suspend fun setShowCategories(show: Boolean) = put(SettingsKeys.showCategories, show)

    suspend fun setShowNotes(show: Boolean) = put(SettingsKeys.showNotes, show)

    suspend fun setShowGlobalProgress(show: Boolean) = put(SettingsKeys.showGlobalProgress, show)

    suspend fun setShowUnits(show: Boolean) = put(SettingsKeys.showUnits, show)

    suspend fun setInputStyle(style: InputStyle) = put(SettingsKeys.inputStyle, style.name)

    suspend fun setCurveLines(show: Boolean) = put(SettingsKeys.curveLines, show)

    suspend fun setCurveSmoothness(smoothness: CurveSmoothness?) = put(SettingsKeys.curveSmoothness, smoothness?.value)

    suspend fun setDefaultGraphMetric(metric: GraphMetric) = put(SettingsKeys.defaultGraphMetric, metric.name)

    suspend fun setDefaultGraphPeriod(period: GraphPeriod) = put(SettingsKeys.defaultGraphPeriod, period.name)

    suspend fun setDefaultGraphLimit(limit: GraphLimit) = put(SettingsKeys.defaultGraphLimit, limit.buckets)

    suspend fun setDefaultTimeBasedXAxis(timeBased: Boolean) = put(SettingsKeys.defaultTimeBasedXAxis, timeBased)

    suspend fun setRepEstimation(estimation: Boolean) = put(SettingsKeys.repEstimation, estimation)

    suspend fun setGroupHistory(history: GroupHistory) = put(SettingsKeys.groupHistory, history.name)

    // D63: el default se aplica solo cuando la clave está sin asignar; un valor explícito gana. Los
    // dos formatos de fecha se resuelven por idioma efectivo, tras localeOverride (D64).
    private fun resolve(prefs: Preferences): Settings {
        val override = enumOrNull<LocaleOverride>(prefs[SettingsKeys.localeOverride])
        val language = effectiveLanguage(override, systemLocale)
        val smoothness = prefs[SettingsKeys.curveSmoothness]
        val limit = prefs[SettingsKeys.defaultGraphLimit]

        return Settings(
            themeMode = enumOr(prefs[SettingsKeys.themeMode], SettingsDefaults.themeMode),
            strengthUnit = enumOr(prefs[SettingsKeys.strengthUnit], SettingsDefaults.strengthUnit),
            longDateFormat = enumOr(prefs[SettingsKeys.longDateFormat], SettingsDefaults.longDateFormat(language)),
            shortDateFormat = enumOr(prefs[SettingsKeys.shortDateFormat], SettingsDefaults.shortDateFormat(language)),
            localeOverride = override,
            maxSets = prefs[SettingsKeys.maxSets],
            warmupSets = prefs[SettingsKeys.warmupSets],
            showCategories = or(prefs[SettingsKeys.showCategories], SettingsDefaults.showCategories),
            showNotes = or(prefs[SettingsKeys.showNotes], SettingsDefaults.showNotes),
            showGlobalProgress = or(prefs[SettingsKeys.showGlobalProgress], SettingsDefaults.showGlobalProgress),
            showUnits = or(prefs[SettingsKeys.showUnits], SettingsDefaults.showUnits),
            inputStyle = enumOr(prefs[SettingsKeys.inputStyle], SettingsDefaults.inputStyle),
            curveLines = or(prefs[SettingsKeys.curveLines], SettingsDefaults.curveLines),
            curveSmoothness = smoothness?.let { CurveSmoothness(it) } ?: SettingsDefaults.curveSmoothness,
            defaultGraphMetric = enumOr(prefs[SettingsKeys.defaultGraphMetric], SettingsDefaults.defaultGraphMetric),
            defaultGraphPeriod = enumOr(prefs[SettingsKeys.defaultGraphPeriod], SettingsDefaults.defaultGraphPeriod),
            defaultGraphLimit = limit?.let { GraphLimit(it) } ?: SettingsDefaults.defaultGraphLimit,
            defaultTimeBasedXAxis = or(
                prefs[SettingsKeys.defaultTimeBasedXAxis],
                SettingsDefaults.defaultTimeBasedXAxis
            ),
            repEstimation = or(prefs[SettingsKeys.repEstimation], SettingsDefaults.repEstimation),
            groupHistory = enumOr(prefs[SettingsKeys.groupHistory], SettingsDefaults.groupHistory)
        )
    }

    private fun <T> or(raw: T?, default: T): T = raw ?: default

    private inline fun <reified T : Enum<T>> enumOr(raw: String?, default: T): T = enumOrNull(raw) ?: default

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    companion object {

        private const val FILE_NAME = "iron_settings.preferences_pb"

        // D46: el fichero vive en el directorio de DataStore, la ubicación que DataStore usa desde
        // API 30. La migración de claves la ejecuta el propio store, porque Preferences no permite
        // enumerar claves, así que solo puede operar sobre el listado conocido.
        fun create(context: Context, scope: CoroutineScope = defaultScope()): SettingsDataStore {
            val datastore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(File(context.dataDir, "datastore"), FILE_NAME) }
            )
            return SettingsDataStore(datastore)
        }

        fun defaultScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
