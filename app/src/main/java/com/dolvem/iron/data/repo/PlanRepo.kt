// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import androidx.sqlite.SQLiteException
import com.dolvem.iron.data.db.ExerciseDao
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.data.db.PlanDao
import com.dolvem.iron.data.db.PlanExercise
import com.dolvem.iron.data.db.PlanExerciseDao
import com.dolvem.iron.data.db.WorkoutDao
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.localToday
import com.dolvem.iron.domain.model.planMatchesDay
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// D5: la violación de unique(planId, exerciseId) se captura aquí y se mapea con mapConstraintViolation.
// Cualquier otra excepción se propaga, que es la reserva de excepciones para fallos de I/O y de base de
// datos. Las lecturas devuelven Flow directo, sin Outcome.
// D17: Clock y ZoneId por constructor solo para la coincidencia con hoy, que es lo que necesita el
// resaltado del plan de hoy. El parseo de Plan.days es del modelo de dominio (D62): el repositorio
// expone la cadena.
// ExerciseDao entra solo para el mensaje de error: DuplicateName lleva el nombre, y el nombre que la UI
// muestra en la colisión de la plantilla es el del ejercicio.
class PlanRepo(
    private val planDao: PlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val clock: Clock,
    private val zone: ZoneId
) {

    // Reglas de orden: los planes por sequence con los nulos al final, y los ejercicios del plan por
    // sequence. enabled = false queda excluido por defecto, con la variante para el filtro que lo muestra.
    fun observeAll(): Flow<Plan> = planDao.observeAll()

    fun observeById(planId: Int): Flow<Plan?> = planDao.observeById(planId)

    fun observeExercises(planId: Int): Flow<PlanExercise> = planExerciseDao.observeForPlan(planId)

    fun observeExercisesIncludingDisabled(planId: Int): Flow<PlanExercise> =
        planExerciseDao.observeForPlanIncludingDisabled(planId)

    fun observeExerciseById(planExerciseId: Int): Flow<PlanExercise?> = planExerciseDao.observeById(planExerciseId)

    // D62: el repositorio expone la cadena y el dominio la parsea. Aquí solo se resuelve la coincidencia
    // con hoy, que es lo que justifica Clock y ZoneId (D17). Cadena vacía significa "nunca".
    fun matchesToday(plan: Plan): Boolean = planMatchesDay(plan.days, localToday(clock, zone))

    // D5: la preconsulta de conteo es un método separado del borrado, y es una lectura, así que no
    // devuelve Outcome. Son las filas de la plantilla que se lleva el CASCADE de la FK de Plan.
    suspend fun affectedPlanExercises(planId: Int): Int = planDao.countPlanExercises(planId)

    // D33: las sesiones del plan no se borran, quedan como sesiones libres por el SET NULL de la FK, y la
    // confirmación de la iteración 4 muestra ese conteo.
    suspend fun affectedSessions(planId: Int): Int = workoutDao.countSessions(planId)

    // D62: days se recibe como la cadena canónica que escribe formatPlanDays en el dominio. En Plan no hay
    // índice único, así que no hay error de negocio esperado: el único fallo es I/O o base de datos.
    suspend fun createPlan(title: String?, days: String, sequence: Int?): Outcome<Plan> {
        val plan = Plan(id = 0, title = title, days = days, sequence = sequence)
        return Outcome.Ok(plan.copy(id = planDao.insert(plan).toInt()))
    }

    // Reglas de borrados: un plan con sesión abierta no se edita ni se borra hasta cerrar la sesión.
    suspend fun editPlan(plan: Plan, title: String?, days: String, sequence: Int?): Outcome<Plan> {
        val blocked = blockedByOpenSession(plan.id)
        if (blocked != null) return blocked
        val edited = plan.copy(title = title, days = days, sequence = sequence)
        planDao.update(edited)
        return Outcome.Ok(edited)
    }

    suspend fun deletePlan(plan: Plan): Outcome<Unit> {
        val blocked = blockedByOpenSession(plan.id)
        if (blocked != null) return blocked
        planDao.delete(plan)
        return Outcome.Ok(Unit)
    }

    // D5: unique(planId, exerciseId) es la regla que impide que un ejercicio aparezca dos veces en el
    // mismo plan, así que la violación tiene que llegar como excepción para mapearla aquí.
    // D37 fija el rango orientativo de targetRpe (1..10), pero Outcome no tiene una variante para el
    // rango, así que el repositorio no lo valida y se deja sin validar. Validarlo obligaría a añadir una
    // variante nueva, y D5 dice que las variantes son los errores de negocio esperables.
    suspend fun addExercise(
        planId: Int,
        exerciseId: Int,
        sequence: Int,
        maxSets: Int?,
        warmupSets: Int?,
        targetRpe: Int?,
        enabled: Boolean
    ): Outcome<PlanExercise> {
        val blocked = blockedByOpenSession(planId)
        if (blocked != null) return blocked
        val draft = PlanExercise(
            id = 0,
            planId = planId,
            exerciseId = exerciseId,
            sequence = sequence,
            maxSets = maxSets,
            warmupSets = warmupSets,
            targetRpe = targetRpe,
            enabled = enabled
        )
        try {
            return Outcome.Ok(draft.copy(id = planExerciseDao.insert(draft).toInt()))
        } catch (e: SQLiteException) {
            val error = mapConstraintViolation(exerciseName(exerciseId), e.message)
            if (error == null) throw e
            return error
        }
    }

    // Campos editables de la plantilla: sequence, maxSets, warmupSets, targetRpe y enabled. planId y
    // exerciseId no se editan, porque la fila es la relación entre el plan y el ejercicio.
    suspend fun editExercise(
        planExercise: PlanExercise,
        sequence: Int,
        maxSets: Int?,
        warmupSets: Int?,
        targetRpe: Int?,
        enabled: Boolean
    ): Outcome<PlanExercise> {
        val blocked = blockedByOpenSession(planExercise.planId)
        if (blocked != null) return blocked
        val edited = planExercise.copy(
            sequence = sequence,
            maxSets = maxSets,
            warmupSets = warmupSets,
            targetRpe = targetRpe,
            enabled = enabled
        )
        planExerciseDao.update(edited)
        return Outcome.Ok(edited)
    }

    // Borrar una fila de la plantilla no toca las series ya registradas, porque las series apuntan al
    // ejercicio y a la sesión, no a la plantilla. Aquí no hay error de negocio esperado salvo el bloqueo
    // de la sesión abierta.
    suspend fun deleteExercise(planExercise: PlanExercise): Outcome<Unit> {
        val blocked = blockedByOpenSession(planExercise.planId)
        if (blocked != null) return blocked
        planExerciseDao.delete(planExercise)
        return Outcome.Ok(Unit)
    }

    // Reglas de borrados: el plan con sesión abierta bloquea edición y borrado. La comprobación es una
    // lectura del DAO de sesiones, que es el único que sabe si la sesión está abierta.
    private suspend fun blockedByOpenSession(planId: Int): Outcome.Error? =
        if (workoutDao.countOpenForPlan(planId) > 0) Outcome.PlanHasOpenSession(planId) else null

    // El mensaje de DuplicateName necesita el nombre. Se lee por Flow con first(), porque el DAO solo
    // expone las lecturas como Flow (D5). Si el ejercicio ya no existe, se usa el id.
    private suspend fun exerciseName(exerciseId: Int): String =
        exerciseDao.observeById(exerciseId).first()?.name ?: exerciseId.toString()
}
