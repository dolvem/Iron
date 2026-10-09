// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dolvem.iron.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// D18, fila "Migración de claves de DataStore": la versión se lee antes del primer acceso, la
// migración solo opera sobre el listado conocido de claves (D46), y el default se aplica solo cuando
// el campo está sin asignar (D63), con localeOverride es/en/sistema (D64).
// El DataStore entra por constructor sobre un fichero temporal, como la base de datos en
// AppContainer, así que el test no toca ninguna API de Android y no necesita Robolectric.
class SettingsDataStoreTest {

    // Clave fuera del listado conocido de D46: la migración no puede operar sobre ella.
    private val legacyKey = stringPreferencesKey("legacyTheme")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    private fun newDataStore(): DataStore<Preferences> {
        val file = File.createTempFile("iron_settings", ".preferences_pb")
        files.add(file)
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @Test
    fun versionIsReadBeforeFirstAccessAndMigrationOnlyTouchesKnownKeys() = runBlocking {
        val datastore = newDataStore()
        datastore.edit { prefs ->
            prefs[SettingsKeys.version] = SettingsKeys.BASE_VERSION
            prefs[SettingsKeys.themeMode] = ThemeMode.DARK.name
            prefs[legacyKey] = "light"
        }

        val settings = SettingsDataStore(datastore, Locale("es")).settings.first()
        assertEquals(ThemeMode.DARK, settings.themeMode)

        val stored = datastore.data.first()
        assertEquals(SettingsKeys.CURRENT_VERSION, stored[SettingsKeys.version])
        assertEquals(ThemeMode.DARK.name, stored[SettingsKeys.themeMode])
        // D46: la migración solo puede operar sobre el listado conocido, así que una clave fuera de él
        // no se toca; se conserva tal cual.
        assertEquals("light", stored[legacyKey])
    }

    // D46: con la versión ya actualizada la migración no vuelve a ejecutarse, así que una clave
    // fuera del listado conocido sobrevive a un segundo acceso.
    @Test
    fun migrationOnlyRunsWhenTheVersionChanges() = runBlocking {
        val datastore = newDataStore()
        val store = SettingsDataStore(datastore, Locale("es"))

        store.settings.first()

        datastore.edit { prefs -> prefs[legacyKey] = "light" }
        store.settings.first()

        assertEquals("light", datastore.data.first()[legacyKey])
    }

    // D63: el default se aplica solo cuando el campo está sin asignar; un valor explícito gana.
    @Test
    fun defaultAppliesOnlyWhenTheFieldIsUnassigned() = runBlocking {
        val store = SettingsDataStore(newDataStore(), Locale("es"))

        val defaults = store.settings.first()
        assertEquals(ShortDateFormat.DD_MM_SLASH, defaults.shortDateFormat)

        store.setShortDateFormat(ShortDateFormat.DD_MM_DASH)
        assertEquals(ShortDateFormat.DD_MM_DASH, store.settings.first().shortDateFormat)
    }

    // D64: sistema (null) elige el idioma del sistema y cae en inglés si no está soportado.
    @Test
    fun shortDateFormatResolvesByEffectiveLanguage() = runBlocking {
        val spanish = SettingsDataStore(newDataStore(), Locale("es")).settings.first()
        assertEquals(ShortDateFormat.DD_MM_SLASH, spanish.shortDateFormat)

        val english = SettingsDataStore(newDataStore(), Locale("en")).settings.first()
        assertEquals(ShortDateFormat.MM_DD_SLASH, english.shortDateFormat)

        val unsupported = SettingsDataStore(newDataStore(), Locale("fr")).settings.first()
        assertEquals(ShortDateFormat.MM_DD_SLASH, unsupported.shortDateFormat)

        // El override explícito gana sobre el idioma del sistema.
        val store = SettingsDataStore(newDataStore(), Locale("es"))
        store.setLocaleOverride(LocaleOverride.EN)
        assertEquals(ShortDateFormat.MM_DD_SLASH, store.settings.first().shortDateFormat)
    }

    // D63: longDateFormat es completa en ambos idiomas.
    @Test
    fun longDateFormatIsFullInBothLanguages() = runBlocking {
        val spanish = SettingsDataStore(newDataStore(), Locale("es")).settings.first()
        val english = SettingsDataStore(newDataStore(), Locale("en")).settings.first()
        assertEquals(LongDateFormat.FULL, spanish.longDateFormat)
        assertEquals(LongDateFormat.FULL, english.longDateFormat)
    }

    // D63: maxSets y warmupSets tienen default "sin asignar", así que llegan null.
    @Test
    fun unassignedOptionalFieldsStayNull() = runBlocking {
        val store = SettingsDataStore(newDataStore(), Locale("es"))

        val defaults = store.settings.first()
        assertNull(defaults.maxSets)
        assertNull(defaults.warmupSets)

        store.setMaxSets(5)
        val assigned = store.settings.first()
        assertEquals(5, assigned.maxSets)
        assertNull(assigned.warmupSets)
    }

    // D65: mode sale de Settings.themeMode, con el default SYSTEM hasta la primera emisión.
    @Test
    fun themeSourceModeReadsSettingsThemeMode() = runBlocking {
        val store = SettingsDataStore(newDataStore(), Locale("es"))
        assertEquals(ThemeMode.SYSTEM, store.mode)

        store.setThemeMode(ThemeMode.DARK)
        store.settings.first()
        assertEquals(ThemeMode.DARK, store.mode)
    }
}
