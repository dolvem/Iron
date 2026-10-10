// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dolvem.iron.R

// El FAB es un componente compartido entre pantallas, así que vive en un paquete común y no en
// feature/today (views_plan.md). Cada pantalla le da el contexto: desde EjercicioDetalle es ese
// ejercicio, desde Hoy el último usado (iteración 6), desde PlanDetalle el ejercicio del plan
// (iteración 4).
@Composable
fun QuickLogFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        // D35: contentDescription en los iconos. El target táctico de FloatingActionButton supera los
        // 48dp de fábrica.
        Icon(
            painter = painterResource(R.drawable.ic_quick_log),
            contentDescription = stringResource(R.string.quick_log_title)
        )
    }
}
