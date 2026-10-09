// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron

import android.app.Application
import com.dolvem.iron.ui.theme.FixedThemeSource
import com.dolvem.iron.ui.theme.ThemeSource

class IronApp : Application() {

    // D65: en la iteración 1 la fuente es fija. En la iteración 2 es SettingsDataStore.
    val themeSource: ThemeSource = FixedThemeSource()
}
