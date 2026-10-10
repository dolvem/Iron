// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.components

import android.content.res.Resources
import com.dolvem.iron.R
import com.dolvem.iron.domain.model.Outcome

// D6: la UI no valida, muestra el error que devuelve el repositorio. Este when exhaustivo sobre las
// variantes de Outcome es la única frontera entre el modelo de dominio y los recursos de strings.xml,
// como fija D5.
// Recibe Resources, no Context, porque las cadenas tienen que resolverse con la configuración actual
// (LocalResources.current en Compose). Es una función, no un composable, porque el error llega desde
// una coroutine.
fun outcomeMessage(resources: Resources, error: Outcome.Error): String = when (error) {
    is Outcome.DuplicateName -> resources.getString(R.string.error_duplicate_name, error.name)
    is Outcome.SessionClosed -> resources.getString(R.string.error_session_closed, error.workoutId)
    is Outcome.PlanHasOpenSession -> resources.getString(R.string.error_plan_has_open_session, error.planId)
    is Outcome.SetWithoutMetric -> resources.getString(R.string.error_set_without_metric)
    is Outcome.InvalidReps -> resources.getString(R.string.error_invalid_reps)
    is Outcome.InvalidLoad -> resources.getString(R.string.error_invalid_load)
}
