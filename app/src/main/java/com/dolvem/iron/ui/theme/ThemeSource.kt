// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.theme

// D65: el modo de tema llega por esta interfaz. En la iteración 2 la implementación es
// SettingsDataStore, así la UI no cambia cuando se conecta la persistencia. FixedThemeSource de la
// iteración 1 se retira.
interface ThemeSource {

    val mode: ThemeMode
}
