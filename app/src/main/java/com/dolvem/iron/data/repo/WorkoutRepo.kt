// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import androidx.room3.withWriteTransaction
import com.dolvem.iron.data.db.ClosedBy
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.data.db.WorkoutDao
import com.dolvem.iron.domain.model.Outcome
import com.dolvem.iron.domain.model.localToday
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

// D17: Clock y ZoneId por constructor, así que la frontera de medianoche (D1) y la zona de la apertura
// perezosa (D3) son deterministas en los tests.
// Recibe la base de datos además del DAO porque D1 y D8 exigen que el cierre de la víspera, la
// validación de la frontera y la apertura del día ocurran en la misma transacción.
class WorkoutRepo(
    private val db: IronDatabase,
    private val workoutDao: WorkoutDao,
    private val clock: Clock,
    private val zone: ZoneId
) {

    // D5: las lecturas devuelven Flow directo, sin Outcome. El historial ordena por sessionDate
    // descendente (reglas de orden).
    fun observeAll(): Flow<Workout> = workoutDao.observeAll()

    fun observeById(workoutId: Int): Flow<Workout?> = workoutDao.observeById(workoutId)

    fun observeOpenForPlan(planId: Int): Flow<Workout?> = workoutDao.observeOpenForPlan(planId)

    // D3: apertura perezosa, startedAt y sessionDate en el momento de apertura y en la zona local.
    // No devuelve Outcome: el único fallo posible es el estado imposible que detecta la validación de
    // D1 y D8, y D5 reserva las excepciones para esos casos.
    suspend fun openSession(planId: Int?): Workout =
        db.withWriteTransaction { openOrCreate(planId, localToday(clock, zone), Instant.now(clock).toEpochMilli()) }

    // D1: el cierre dentro del mismo día es EXPLICIT; si la sesión es de un día anterior, el cierre lo
    // produce el cambio de día y closedBy es AUTO. El historial los distingue (D44).
    // D43: una sesión cerrada no se reabre, así que cerrar una sesión ya cerrada es un no-op.
    // D39: cerrar una sesión con 0 series no tiene sentido, pero el repositorio lo acepta; el botón se
    // oculta en la UI.
    suspend fun closeSession(workout: Workout): Outcome<Workout> {
        if (workout.endedAt != null) return Outcome.Ok(workout)
        return db.withWriteTransaction {
            val closedBy = if (workout.sessionDate == localToday(clock, zone)) ClosedBy.EXPLICIT else ClosedBy.AUTO
            val closed = workout.copy(endedAt = Instant.now(clock).toEpochMilli(), closedBy = closedBy)
            workoutDao.update(closed)
            Outcome.Ok(closed)
        }
    }

    // D33: la confirmación de borrado de plan cuenta las sesiones que quedan como sesiones libres.
    // D5: la preconsulta es un método separado del borrado, y es una lectura, así que no devuelve
    // Outcome.
    suspend fun affectedSessions(planId: Int): Int = workoutDao.countSessions(planId)

    // Aquí no hay error de negocio esperado: el borrado de sesión solo puede fallar por I/O o por base
    // de datos, que son los casos que D5 deja en las excepciones. Las series de la sesión se van con el
    // CASCADE de la FK.
    suspend fun deleteWorkout(workout: Workout): Outcome<Unit> {
        workoutDao.delete(workout)
        return Outcome.Ok(Unit)
    }

    // D1: una sesión activa por plan, y una sesión libre abierta por día cuando planId es nulo.
    private suspend fun openOrCreate(planId: Int?, today: LocalDate, now: Long): Workout {
        val open = if (planId == null) workoutDao.openFreeForDate(today) else workoutDao.openForPlan(planId)
        if (open == null) return createSession(planId, today, now)
        if (open.sessionDate == today) return open
        // D1: endedAt es el momento del cierre, no el timestamp de la serie. closedBy es AUTO porque el
        // cierre lo produce el cambio de día (D62). D43: la sesión cerrada no se reabre, se abre otra.
        workoutDao.update(open.copy(endedAt = now, closedBy = ClosedBy.AUTO))
        return createSession(planId, today, now)
    }

    // D1 y D8: la frontera la impone esta validación transaccional, porque Room no puede hacer un índice
    // parcial sobre endedAt IS NULL. Si falla, el estado es imposible y D5 lo deja como excepción.
    private suspend fun createSession(planId: Int?, today: LocalDate, startedAt: Long): Workout {
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
}
