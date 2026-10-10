// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.dolvem.iron.data.db.ClosedBy
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.data.db.Workout
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D8: frecuencia del detalle del plan, la ventana móvil de 30 días naturales incluyendo hoy.
// D17: Clock y ZoneId por constructor, así la ventana es determinista. El instante fijo es el miércoles
// 14 de octubre de 2026, así que hoy es 2026-10-14 y la ventana empieza el 2026-09-15, que es el día 30
// contando desde hoy.
// D18: Robolectric solo por el contexto que necesita Room en memoria, sin driver.
@RunWith(RobolectricTestRunner::class)
class PlanFrequencyTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("Europe/Madrid"))
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 14)
    private val windowStart = today.minusDays(29)

    private lateinit var db: IronDatabase
    private lateinit var plans: PlanRepo
    private var planId = 0

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        plans = PlanRepo(db.planDao(), db.planExerciseDao(), db.exerciseDao(), db.workoutDao(), clock, zone)
        runBlocking {
            planId = db.planDao().insert(Plan(0, "Empuje", "1")).toInt()
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    // endedAt null es una sesión abierta. El insert va directo al DAO porque la frecuencia solo lee.
    private suspend fun insertSession(ownerPlanId: Int?, date: LocalDate, startedAt: Long, endedAt: Long?): Int =
        db.workoutDao().insert(Workout(0, startedAt, date, endedAt, ClosedBy.EXPLICIT, ownerPlanId)).toInt()

    // D8: la última sesión del plan es la más reciente por sessionDate descendente.
    @Test
    fun lastSessionIsTheMostRecentBySessionDate() = runBlocking {
        insertSession(planId, today.minusDays(10), 1000L, 2000L)
        insertSession(planId, today.minusDays(2), 3000L, 4000L)

        val frequency = plans.observeFrequency(planId).first()
        assertEquals(today.minusDays(2), frequency.lastSessionDate)
        assertEquals(2, frequency.sessionsLast30Days)
    }

    // El desempate del ORDER BY es startedAt descendente, así que con dos sesiones del mismo día la
    // última es la más reciente. Se comprueba sobre la lectura del DAO, porque PlanFrequency solo expone
    // la fecha.
    @Test
    fun sameSessionDateTiesBreakByStartedAtDescending() = runBlocking {
        val earlier = insertSession(planId, today, 1000L, 2000L)
        val later = insertSession(planId, today, 3000L, 4000L)

        val last = db.workoutDao().observeLastSessionForPlan(planId).first()
        assertEquals(later, last?.id)
        assertEquals(2, db.workoutDao().observeSessionCountFrom(planId, windowStart).first())
    }

    // D8: ventana móvil de 30 días naturales incluyendo hoy. El día 30 cuenta, el día 31 no.
    @Test
    fun windowCountsTheThirtiethDayAndExcludesTheThirtyFirst() = runBlocking {
        assertEquals(LocalDate.of(2026, 9, 15), windowStart)

        insertSession(planId, windowStart, 1000L, 2000L)
        insertSession(planId, windowStart.minusDays(1), 3000L, 4000L)

        val frequency = plans.observeFrequency(planId).first()
        assertEquals(1, frequency.sessionsLast30Days)
        // La última sesión sigue siendo la más reciente, aunque la otra esté fuera de la ventana.
        assertEquals(windowStart, frequency.lastSessionDate)
    }

    // D8: las sesiones libres no cuentan en la frecuencia de ningún plan, porque no hay plan al que
    // atribuirse la sesión.
    @Test
    fun freeSessionsAreExcludedFromFrequency() = runBlocking {
        insertSession(null, today, 1000L, 2000L)
        insertSession(planId, today.minusDays(3), 3000L, 4000L)

        val frequency = plans.observeFrequency(planId).first()
        assertEquals(1, frequency.sessionsLast30Days)
        assertEquals(today.minusDays(3), frequency.lastSessionDate)
    }

    // D8: la sesión abierta cuenta, porque es una sesión que ocurrió. El conteo no mira endedAt.
    @Test
    fun openSessionCountsInFrequency() = runBlocking {
        insertSession(planId, today, 1000L, null)

        val frequency = plans.observeFrequency(planId).first()
        assertEquals(1, frequency.sessionsLast30Days)
        assertEquals(today, frequency.lastSessionDate)
    }

    // D8: ventana vacía, lastSessionDate null y conteo 0.
    @Test
    fun emptyWindowHasNullLastSessionAndZeroCount() = runBlocking {
        // Una sesión de otro plan no aporta nada a la frecuencia de este.
        val other = db.planDao().insert(Plan(0, "Tirón", "2")).toInt()
        insertSession(other, today, 1000L, 2000L)

        val frequency = plans.observeFrequency(planId).first()
        assertNull(frequency.lastSessionDate)
        assertEquals(0, frequency.sessionsLast30Days)
    }

    // D18, fila "Repositorios con Flow": el combine conserva la naturaleza del Flow, una sesión nueva
    // reemite la frecuencia. Defecto encontrado: Room invalida las dos consultas por separado, así que
    // el orden de la reemisión no está garantizado y combine puede emitir primero el estado intermedio
    // con la última sesión sin resolver. Lo que se afirma es el estado final, aceptando el intermedio.
    @Test
    fun frequencyReemitsWhenASessionIsInserted() = runBlocking {
        plans.observeFrequency(planId).test {
            assertEquals(PlanFrequency(null, 0), awaitItem())

            insertSession(planId, today, 1000L, null)

            var item = awaitItem()
            while (item != PlanFrequency(today, 1)) {
                // El intermedio puede ser cualquiera de los dos, según qué consulta reemita primero:
                // el conteo resuelto con la última sesión pendiente, o la última sesión resuelta con
                // el conteo todavía a cero.
                assertTrue(item == PlanFrequency(null, 1) || item == PlanFrequency(today, 0))
                item = awaitItem()
            }

            cancelAndConsumeRemainingEvents()
        }
    }
}
