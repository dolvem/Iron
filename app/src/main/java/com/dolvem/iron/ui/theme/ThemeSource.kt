// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.theme

// D65: el modo de tema llega por esta interfaz. En la iteración 2 la implementación
// es SettingsDataStore, así que la UI no cambia cuando se conecta la persistencia.
interface ThemeSource {

    val mode: ThemeMode
}

class FixedThemeSource : ThemeSource {

    override val mode: ThemeMode = ThemeMode.SYSTEM
}
