// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import com.dolvem.iron.data.db.ExerciseSet

// D9: las métricas excluyen siempre las series de calentamiento, en PR, volumen, repeticiones
// totales y conteo de series por periodo.
// D6: una serie sin métrica (reps y loadKg nulos) no cuenta en el conteo por periodo.

fun hasMetric(set: ExerciseSet): Boolean = set.reps != null || set.loadKg != null

// Volumen: loadKg x reps. Las series con reps nulo se excluyen, así que una serie con solo peso
// no aporta volumen (D9).
fun volumeKg(sets: List<ExerciseSet>): Double =
    sets.filter { !it.isWarmup }.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) }

// PR: máximo loadKg por ejercicio, la métrica "mejor peso", no el mejor volumen (D9).
fun bestWeightKg(sets: List<ExerciseSet>): Double? = sets.filter { !it.isWarmup }.mapNotNull { it.loadKg }.maxOrNull()

fun totalReps(sets: List<ExerciseSet>): Int = sets.filter { !it.isWarmup }.sumOf { it.reps ?: 0 }

// Series por periodo: conteo de series con al menos una métrica (D9).
fun setsWithMetric(sets: List<ExerciseSet>): Int = sets.filter { !it.isWarmup && hasMetric(it) }.size
