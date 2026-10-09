// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.navigation

import com.dolvem.iron.R

// Los cuatro destinos de datos del bottom nav. Ajustes no es un destino: es una ruta
// alcanzada desde la cabecera de Hoy.
enum class Destination(val route: String, val labelRes: Int, val iconRes: Int) {
    TODAY("today", R.string.nav_today, R.drawable.ic_nav_today),
    PLANS("plans", R.string.nav_plans, R.drawable.ic_nav_plans),
    EXERCISES("exercises", R.string.nav_exercises, R.drawable.ic_nav_exercises),
    HISTORY("history", R.string.nav_history, R.drawable.ic_nav_history)
}

const val SETTINGS_ROUTE = "settings"
