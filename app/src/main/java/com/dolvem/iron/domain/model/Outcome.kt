// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

// D5: los repositorios devuelven un Outcome sellado con los errores de negocio esperables. Se llama
// Outcome porque Result choca con kotlin.Result. Solo lo devuelven las operaciones con error de
// negocio esperado: borrados, renombrar, fusión, inserción de serie y cierre de sesión. Las lecturas
// devuelven Flow directo.
// El tipo no depende de Android: las cadenas de cada variante están en strings.xml y el ViewModel las
// elige con un when exhaustivo sobre estas variantes, que es la única frontera con los recursos.
sealed interface Outcome<out T> {
    data class Ok<out V>(val value: V) : Outcome<V>

    sealed interface Error : Outcome<Nothing>

    // D4: nombre duplicado, por la violación de único sobre nameKey, la columna normalizada.
    data class DuplicateName(val name: String) : Error

    // Una serie no puede añadirse a una sesión cerrada.
    data class SessionClosed(val workoutId: Int) : Error

    // Edición y borrado de plan bloqueados mientras hay una sesión abierta para ese plan.
    data class PlanHasOpenSession(val planId: Int) : Error

    // D6: serie sin métrica, rechazada antes del CASCADE.
    data class SetWithoutMetric(val setId: Int) : Error

    // D6: reps = 0 rechazado en el repositorio.
    data class InvalidReps(val setId: Int) : Error

    // D6: loadKg = 0 rechazado en el repositorio.
    data class InvalidLoad(val setId: Int) : Error
}
