// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.repo

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.dolvem.iron.data.db.Exercise
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.Plan
import com.dolvem.iron.data.db.PlanExercise
import com.dolvem.iron.data.db.Workout
import com.dolvem.iron.domain.model.Outcome
import java.time.Clock
import java.time.Instant
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

// Iteración 4: CRUD de planes y de la plantilla PlanExercise.
// Reglas de borrados: un plan con sesión abierta bloquea edición y borrado, y la plantilla se bloquea
// por la misma razón.
// D5: la violación de unique(planId, exerciseId) se captura en el repositorio y se mapea a
// DuplicateName, y la preconsulta de conteo es un método separado del borrado.
// D17: Clock y ZoneId por constructor, aunque aquí solo los usa la coincidencia con hoy.
// D18: Robolectric solo por el contexto que necesita Room en memoria, sin driver.
@RunWith(RobolectricTestRunner::class)
class PlanRepoTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("Europe/Madrid"))
    private val zone = ZoneId.of("Europe/Madrid")

    private lateinit var db: IronDatabase
    private lateinit var plans: PlanRepo
    private lateinit var exercises: ExerciseRepo
    private lateinit var workouts: WorkoutRepo

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<IronDatabase>(context).build()
        plans = PlanRepo(db.planDao(), db.planExerciseDao(), db.exerciseDao(), db.workoutDao(), clock, zone)
        exercises = ExerciseRepo(db, db.exerciseDao(), db.planExerciseDao())
        workouts = WorkoutRepo(db, db.workoutDao(), clock, zone)
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun newPlan(title: String?, days: String, sequence: Int?): Plan =
        (plans.createPlan(title, days, sequence) as Outcome.Ok<Plan>).value

    private suspend fun newExercise(name: String): Exercise =
        (exercises.createExercise(name, null) as Outcome.Ok<Exercise>).value

    private suspend fun newRow(plan: Plan, exercise: Exercise, sequence: Int, enabled: Boolean): PlanExercise =
        (plans.addExercise(plan.id, exercise.id, sequence, null, null, null, enabled) as Outcome.Ok<PlanExercise>).value

    @Test
    fun createPlanAndEditTitleDaysAndSequence() = runBlocking {
        val plan = newPlan(null, "1,3,5", null)
        // Plan.title nulo se muestra como "Plan" en la UI, el repositorio lo deja nulo.
        assertNull(plan.title)

        val edited = (plans.editPlan(plan, "Empuje", "1,3,5", 2) as Outcome.Ok<Plan>).value
        assertEquals("Empuje", edited.title)
        // D62: days se guarda como la cadena canónica, el parseo es del modelo de dominio.
        assertEquals("1,3,5", edited.days)
        assertEquals(2, edited.sequence)

        assertEquals(edited, db.planDao().observeById(plan.id).first())
    }

    // Reglas de borrados: el plan con sesión abierta bloquea edición y borrado, y la plantilla también.
    @Test
    fun openSessionBlocksEditAndDelete() = runBlocking {
        val plan = newPlan("Empuje", "1", null)
        val exercise = newExercise("Press")
        val row = newRow(plan, exercise, 1, true)
        val open = workouts.openSession(plan.id)

        val blockedAdd = plans.addExercise(plan.id, exercise.id, 2, null, null, null, true)

        assertEquals(Outcome.PlanHasOpenSession(plan.id), plans.editPlan(plan, "Empuje", "1", 1))
        assertEquals(Outcome.PlanHasOpenSession(plan.id), plans.deletePlan(plan))
        assertEquals(Outcome.PlanHasOpenSession(plan.id), blockedAdd)
        assertEquals(Outcome.PlanHasOpenSession(plan.id), plans.editExercise(row, 2, 4, 1, 9, true))
        assertEquals(Outcome.PlanHasOpenSession(plan.id), plans.deleteExercise(row))

        // Con la sesión cerrada, las mismas operaciones devuelven Ok.
        workouts.closeSession(open)

        assertTrue(plans.editPlan(plan, "Empuje", "1", 1) is Outcome.Ok)
        assertTrue(plans.addExercise(plan.id, newExercise("Apertura").id, 2, null, null, null, true) is Outcome.Ok)
        assertTrue(plans.editExercise(row, 2, 4, 1, 9, true) is Outcome.Ok)
        assertEquals(Outcome.Ok(Unit), plans.deleteExercise(row))
        assertEquals(Outcome.Ok(Unit), plans.deletePlan(plan))
    }

    // D5 y D4: unique(planId, exerciseId) es la regla que impide que un ejercicio aparezca dos veces en
    // el mismo plan, y la violación se mapea a DuplicateName con el nombre del ejercicio. La
    // normalización de D4 pliega mayúsculas y acentos, así que el nombre que llega al mensaje es el
    // normalizado cuando el índice es nameKey, y aquí es el nombre del ejercicio.
    @Test
    fun duplicateExerciseInPlanReturnsDuplicateNameWithTheExerciseName() = runBlocking {
        val plan = newPlan("Empuje", "1", null)
        val exercise = newExercise("Péculo")
        assertEquals("péculo", exercise.nameKey)

        assertTrue(plans.addExercise(plan.id, exercise.id, 1, null, null, null, true) is Outcome.Ok)

        val repeated = plans.addExercise(plan.id, exercise.id, 2, null, null, null, true)
        assertEquals(Outcome.DuplicateName("Péculo"), repeated)

        // El mismo ejercicio en otro plan no es duplicado: el índice único es (planId, exerciseId).
        val other = newPlan("Tirón", "2", null)
        assertTrue(plans.addExercise(other.id, exercise.id, 1, null, null, null, true) is Outcome.Ok)
    }

    // D5: la preconsulta de conteo es un método separado del borrado. D33: las sesiones no se borran,
    // quedan como sesiones libres por el SET NULL de la FK, y ese es el conteo que la confirmación
    // muestra, porque es lo que el usuario pierde.
    @Test
    fun precountRunsBeforeCascadeAndSessionsSurviveAsFreeSessions() = runBlocking {
        val plan = newPlan("Empuje", "1", null)
        val exercise = newExercise("Press")
        newRow(plan, exercise, 1, true)
        val session = (workouts.closeSession(workouts.openSession(plan.id)) as Outcome.Ok<Workout>).value

        assertEquals(1, plans.affectedPlanExercises(plan.id))
        assertEquals(1, plans.affectedSessions(plan.id))

        assertEquals(Outcome.Ok(Unit), plans.deletePlan(plan))

        // El CASCADE se lleva la fila de la plantilla; la sesión sigue existiendo con planId null.
        assertEquals(0, plans.affectedPlanExercises(plan.id))
        assertEquals(0, plans.affectedSessions(plan.id))

        val after = db.workoutDao().observeById(session.id).first()
        assertEquals(session.id, after?.id)
        assertNull(after?.planId)
        assertEquals(session.sessionDate, after?.sessionDate)
    }

    // Reglas de orden: los planes por sequence con los nulos al final, porque la colación BINARY de
    // SQLite pone los nulos al principio y la clave IS NULL va delante en el ORDER BY.
    @Test
    fun plansAreOrderedBySequenceWithNullsLast() = runBlocking {
        newPlan("Sin secuencia", "", null)
        newPlan("Segundo", "1", 2)
        newPlan("Primero", "1", 1)

        // D18: con Flow<List<Plan>> la emisión inicial trae el resultado completo. Con Flow<Plan> Room
        // emite una fila por invalidación, así que take(3) se queda esperando una segunda emisión.
        val ordered = plans.observeAll().first()
        assertEquals(listOf("Primero", "Segundo", "Sin secuencia"), ordered.map { it.title })
    }

    // Reglas de orden: los ejercicios del plan por sequence.
    @Test
    fun planExercisesAreOrderedBySequence() = runBlocking {
        val plan = newPlan("Empuje", "1", null)
        val second = newRow(plan, newExercise("Apertura"), 2, true)
        val first = newRow(plan, newExercise("Press"), 1, true)

        assertEquals(listOf(first, second), plans.observeExercises(plan.id).first())
    }

    // Reglas de orden: enabled = false excluido por defecto, con la variante para el filtro que lo
    // muestra.
    @Test
    fun disabledRowsAreExcludedByDefaultAndIncludedWithTheFilter() = runBlocking {
        val plan = newPlan("Empuje", "1", null)
        val enabled = newRow(plan, newExercise("Press"), 1, true)
        val disabled = newRow(plan, newExercise("Apertura"), 2, false)

        assertEquals(listOf(enabled), plans.observeExercises(plan.id).first())
        assertEquals(listOf(enabled, disabled), plans.observeExercisesIncludingDisabled(plan.id).first())
    }
}
