// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import androidx.room3.withWriteTransaction
import com.dolvem.iron.data.db.ClosedBy
import com.dolvem.iron.data.db.ExerciseSet
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.SetDao
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.data.db.WorkoutDao
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.localToday
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

// D40: resultado del borrado de una serie, para el undo de la iteración 5. Devuelve la serie borrada y la
// sesión eliminada con su id y startedAt originales cuando la sesión se elimina por ser la última serie
// (D39); la sesión es nula cuando se conserva.
data class DeletedSet(val set: ExerciseSet, val workout: Workout?)

// D17: Clock y ZoneId por constructor, así que la frontera de medianoche (D1) y la zona de la apertura
// perezosa (D3) son deterministas en los tests.
// El repositorio recibe la base de datos además del DAO porque D1 exige que el cierre de la víspera, la
// apertura del día y el insert de la serie ocurran en la misma transacción.
class SetRepo(
    private val db: IronDatabase,
    private val setDao: SetDao,
    private val workoutDao: WorkoutDao,
    private val clock: Clock,
    private val zone: ZoneId
) {

    // Serie en la sesión activa del plan (D1), o en la sesión libre del día cuando planId es nulo.
    // D2: warmupSets es la configuración del plan; sin asignar no hay calentamiento, porque el flag se
    // fija por posición dentro de ese número de series.
    suspend fun addSet(
        planId: Int?,
        exerciseId: Int,
        reps: Int?,
        loadKg: Double?,
        notes: String?,
        warmupSets: Int?
    ): Outcome<ExerciseSet> {
        val error = validate(reps, loadKg)
        if (error != null) return error
        return register(planId, exerciseId, reps, loadKg, notes, warmupSets)
    }

    // Quick log: reutiliza la sesión libre abierta del día; si está cerrada, crea una nueva. Siempre
    // serie de trabajo, nunca calentamiento, porque no hay contexto de plan (D2).
    suspend fun quickLog(exerciseId: Int, reps: Int?, loadKg: Double?, notes: String?): Outcome<ExerciseSet> {
        val error = validate(reps, loadKg)
        if (error != null) return error
        return register(null, exerciseId, reps, loadKg, notes, warmupSets = null)
    }

    // D42: una sesión cerrada no admite series nuevas, pero sus series se pueden editar y borrar.
    // D1: la serie se inserta en la sesión recibida, no se re-resuelve otra sesión. Si la sesión recibida
    // es de un día anterior, se cierra con ClosedBy.AUTO (el cierre lo produce el cambio de día, D62) y la
    // serie va a la sesión del día, que es la única sesión abierta para ese plan.
    suspend fun addSetToWorkout(
        workout: Workout,
        exerciseId: Int,
        reps: Int?,
        loadKg: Double?,
        notes: String?,
        warmupSets: Int?
    ): Outcome<ExerciseSet> {
        val error = validate(reps, loadKg)
        if (error != null) return error
        return db.withWriteTransaction {
            val today = localToday(clock, zone)
            val now = Instant.now(clock).toEpochMilli()
            val session =
                if (workout.sessionDate == today) {
                    workout
                } else {
                    workoutDao.update(workout.copy(endedAt = now, closedBy = ClosedBy.AUTO))
                    resolveSession(workout.planId, today, now)
                }
            insertSet(session, exerciseId, reps, loadKg, notes, now, warmupSets)
        }
    }

    // D39: al borrar la última serie de una sesión ABIERTA, la sesión se elimina; una sesión CERRADA vacía
    // se conserva. Borrar series de una sesión cerrada sí está permitido (D42).
    // D40: devuelve DeletedSet con la sesión eliminada (id y startedAt originales) para el undo.
    // D5: la lectura de la sesión usa el Flow del DAO con first(), porque el DAO solo expone las lecturas
    // como Flow.
    suspend fun deleteSet(set: ExerciseSet): Outcome<DeletedSet> = db.withWriteTransaction {
        val session = workoutDao.observeById(set.workoutId).first()
        setDao.delete(set)
        val deletedWorkout =
            if (session != null && session.endedAt == null && setDao.countForWorkout(set.workoutId) == 0) {
                workoutDao.delete(session)
                session
            } else {
                null
            }
        Outcome.Ok(DeletedSet(set, deletedWorkout))
    }

    // D42: los campos editables de una serie son las métricas, las notas, isWarmup y el timestamp; el
    // ejercicio no, porque la serie pertenece a la actividad elegida al registrarla. Editar series de una
    // sesión cerrada sí está permitido: lo prohibido es añadir.
    // D2: isWarmup se fija por posición en el insert y no se recalcula al editar, así que aquí es un campo
    // editable más.
    suspend fun editSet(
        set: ExerciseSet,
        reps: Int?,
        loadKg: Double?,
        notes: String?,
        isWarmup: Boolean,
        timestamp: Long
    ): Outcome<ExerciseSet> {
        val error = validate(reps, loadKg, set.id)
        if (error != null) return error
        val edited = set.copy(
            reps = reps,
            loadKg = loadKg,
            notes = notes,
            isWarmup = isWarmup,
            timestamp = timestamp
        )
        setDao.update(edited)
        return Outcome.Ok(edited)
    }

    // D6: reps >= 1, loadKg > 0 y al menos una métrica, sin techo. Se valida antes del insert, así que
    // nunca llega una serie inválida a la base de datos. El id todavía no existe en el insert, así que el
    // Outcome lleva 0; en la edición se usa el id de la serie (D42).
    private fun validate(reps: Int?, loadKg: Double?, setId: Int = 0): Outcome.Error? {
        if (reps != null && reps < 1) return Outcome.InvalidReps(setId)
        if (loadKg != null && loadKg <= 0.0) return Outcome.InvalidLoad(setId)
        if (reps == null && loadKg == null) return Outcome.SetWithoutMetric(setId)
        return null
    }

    // D1: cierre de la víspera, apertura del día e insert de la serie en la misma transacción.
    private suspend fun register(
        planId: Int?,
        exerciseId: Int,
        reps: Int?,
        loadKg: Double?,
        notes: String?,
        warmupSets: Int?
    ): Outcome<ExerciseSet> = db.withWriteTransaction {
        val today = localToday(clock, zone)
        val now = Instant.now(clock).toEpochMilli()
        val session = resolveSession(planId, today, now)
        insertSet(session, exerciseId, reps, loadKg, notes, now, warmupSets)
    }

    // D3: apertura perezosa, la sesión se crea con la primera serie, con startedAt y sessionDate en el
    // momento de apertura y en la zona local.
    private suspend fun resolveSession(planId: Int?, today: LocalDate, now: Long): Workout {
        val open = if (planId == null) workoutDao.openFreeForDate(today) else workoutDao.openForPlan(planId)
        if (open == null) return createSession(planId, today, now)
        if (open.sessionDate == today) return open
        // D1: endedAt es el momento del cierre, no el timestamp de la serie. closedBy es AUTO porque el
        // cierre lo produce el cambio de día (D62).
        val closedAt = Instant.now(clock).toEpochMilli()
        workoutDao.update(open.copy(endedAt = closedAt, closedBy = ClosedBy.AUTO))
        return createSession(planId, today, closedAt)
    }

    private suspend fun createSession(planId: Int?, today: LocalDate, startedAt: Long): Workout {
        // D1: la frontera la impone esta validación transaccional, porque Room no puede hacer un índice
        // parcial sobre endedAt IS NULL. Si falla, el estado es imposible y D5 lo deja como excepción.
        if (planId != null) {
            check(workoutDao.countOpenForPlan(planId) == 0) { "Ya hay una sesión abierta para el plan $planId" }
        } else {
            check(workoutDao.openFreeForDate(today) == null) { "Ya hay una sesión libre abierta para $today" }
        }
        // D62: una sesión abierta no está cerrada, así que el valor inicial es EXPLICIT, que es el cierre
        // previsto, y pasa a AUTO solo cuando lo cierra el cambio de día. Se fija en el insert porque la
        // columna no es nula.
        val draft = Workout(
            id = 0,
            startedAt = startedAt,
            sessionDate = today,
            closedBy = ClosedBy.EXPLICIT,
            planId = planId
        )
        return draft.copy(id = workoutDao.insert(draft).toInt())
    }

    // D2: isWarmup por posición, el conteo de series ya registradas de ese ejercicio en esa sesión. La
    // posición está dentro de warmupSets, que es la configuración del plan; sin warmupSets no hay
    // calentamiento. El flag no se recalcula al borrar o editar.
    private suspend fun insertSet(
        session: Workout,
        exerciseId: Int,
        reps: Int?,
        loadKg: Double?,
        notes: String?,
        timestamp: Long,
        warmupSets: Int?
    ): Outcome<ExerciseSet> {
        val position = setDao.countForWorkoutAndExercise(session.id, exerciseId)
        val isWarmup = warmupSets != null && position < warmupSets
        val draft = ExerciseSet(
            id = 0,
            exerciseId = exerciseId,
            workoutId = session.id,
            timestamp = timestamp,
            reps = reps,
            loadKg = loadKg,
            notes = notes,
            isWarmup = isWarmup
        )
        return Outcome.Ok(draft.copy(id = setDao.insert(draft).toInt()))
    }
}
