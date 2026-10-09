// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet

// Referencia: fórmula de Epley, 1RM = peso / (1 + reps / 30). Es la fórmula documentada en
// docs/README.md, la única que usa Iron. Como el 1RM es una estimación y no un dato registrado,
// mantiene el redondeo de 0,5 kg de D10, no el de 2 decimales.
// Solo series con reps >= 1; las series sin reps se omiten (D10).
fun epley1rmKg(loadKg: Double, reps: Int): Double? {
    if (reps < 1) return null
    return loadKg / (1.0 + reps / 30.0)
}

// 1RM por serie, con el redondeo de mostrar aplicado. Una serie sin métrica no aporta 1RM.
fun epley1rmKg(set: ExerciseSet): Double? {
    val reps = set.reps ?: return null
    val loadKg = set.loadKg ?: return null
    return epley1rmKg(loadKg, reps)?.let { roundHalfKg(it) }
}
