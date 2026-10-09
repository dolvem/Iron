// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron

import android.app.Application
import com.dolvem.iron.ui.theme.ThemeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class IronApp : Application() {

    // Contenedor manual, sin Hilt. Los ViewModel de la iteración 3 lo leen desde la Application con
    // viewModelFactory { initializer { } }.
    // Diferido: en el arranque de Robolectric el Application todavía no tiene contexto base, y Room
    // necesita getSystemService para resolver el journal mode al construir la BD.
    val container: AppContainer by lazy { AppContainer.create(this) }

    // D65: la fuente es SettingsDataStore y FixedThemeSource de la iteración 1 se retira. MainActivity no
    // cambia: sigue leyendo app.themeSource.mode en Compose. Como mode es una propiedad y DataStore es
    // reactivo, aquí se lanza el collector que mantiene el último valor conocido de la propiedad.
    val themeSource: ThemeSource by lazy { container.settings }

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        container.settings.observe(scope)
    }
}
