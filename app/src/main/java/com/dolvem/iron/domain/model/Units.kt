// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

// D10: 2 decimales al mostrar y al guardar, medio arriba cuando hay más decimales. El valor
// guardado es la fuente de verdad, así que mostrar nunca redondea sobre un valor ya redondeado.
// La ida y vuelta kg -> lb -> kg no es estable con 2 decimales, y se acepta como limitación.
const val KG_PER_LB = 0.45359237

// Redondeo de 0,5 kg, el que espera alguien mirando una placa de 2,5 kg. Se usa solo en el 1RM,
// que es una estimación y no un dato registrado (D10).
fun round2(value: Double): Double = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

fun roundHalfKg(value: Double): Double =
    BigDecimal.valueOf(value * 2).setScale(0, RoundingMode.HALF_UP).toDouble() / 2.0

// kg es la unidad canónica; la conversión a lb es solo de display (D10).
fun kgToLb(loadKg: Double): Double = round2(loadKg / KG_PER_LB)

fun lbToKg(loadLb: Double): Double = round2(loadLb * KG_PER_LB)
