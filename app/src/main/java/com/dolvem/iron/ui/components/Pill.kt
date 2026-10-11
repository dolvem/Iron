// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// D75: píldora de la línea de pills de la fila de lista: categoría y badge Archivado comparten estilo.
// Label `labelLarge` sobre `secondaryContainer`, shape full, con el padding de la píldora del rediseño
// de vistas en Penpot. La píldora se lee como badge, que es lo que justifica el shape full.
// D83: la píldora de "sin categoría" usa la pareja neutra de containers, la lectura M3 de un badge de baja
// énfasis. Las parejas se pasan como parámetros para que cada pill use la suya y no se crucen.
@Composable
fun Pill(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}
