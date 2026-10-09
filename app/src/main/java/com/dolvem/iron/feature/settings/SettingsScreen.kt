// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R
import com.dolvem.iron.ui.components.ScreenPlaceholder

// Ruta desde la cabecera de Hoy. El selector de tema y el resto de ajustes se escriben
// en la iteración 10; el modo se resuelve hoy por ThemeSource (D65).
@Composable
fun SettingsScreen() {
    ScreenPlaceholder(stringResource(R.string.settings_empty))
}
