// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron

import android.content.Context
import androidx.room3.Room
import com.dolvem.iron.data.db.IronDatabase
import com.dolvem.iron.data.db.IronMigrations
import com.dolvem.iron.data.repo.Analytics
import com.dolvem.iron.data.repo.CategoryRepo
import com.dolvem.iron.data.repo.ExerciseRepo
import com.dolvem.iron.data.repo.PlanRepo
import com.dolvem.iron.data.repo.SetRepo
import com.dolvem.iron.data.repo.WorkoutRepo
import com.dolvem.iron.data.settings.SettingsDataStore
import java.time.Clock
import java.time.ZoneId

// Contenedor manual de dependencias, sin Hilt.
// D17: Clock y ZoneId se crean aquí con los defaults del sistema y se inyectan por constructor en los
// repositorios y en Analytics, así la frontera de día (D1) y la semana ISO (D9) son deterministas en los
// tests.
// La base de datos entra por constructor: en producción la construye create con Room.databaseBuilder sobre
// el contexto de la Application, y en los tests se pasa la de memoria. El contenedor no decide entre las
// dos, por eso es testeable. Lo mismo ocurre con SettingsDataStore (D65): en producción lo construye
// create, y en los tests se inyecta un DataStore sobre un fichero temporal.
// Iteración 3: los ViewModel se construyen con viewModelFactory { initializer { } } leyendo este contenedor
// desde la Application (IronApp), como fija el plan de ingeniería.
class AppContainer(
    val db: IronDatabase,
    val settings: SettingsDataStore,
    val clock: Clock = Clock.systemUTC(),
    val zone: ZoneId = ZoneId.systemDefault()
) {

    // D5: los repositorios reciben los DAOs de la base de datos. SetRepo, WorkoutRepo, ExerciseRepo y
    // CategoryRepo reciben también la base de datos porque D1, D8 y D32 exigen que el remapeo de las
    // FKs, el cierre de la víspera, la apertura del día y el insert estén en la misma transacción.
    val exerciseRepo: ExerciseRepo = ExerciseRepo(db, db.exerciseDao(), db.planExerciseDao())

    val categoryRepo: CategoryRepo = CategoryRepo(db, db.categoryDao())

    val planRepo: PlanRepo = PlanRepo(
        db.planDao(),
        db.planExerciseDao(),
        db.exerciseDao(),
        db.workoutDao(),
        clock,
        zone
    )

    val workoutRepo: WorkoutRepo = WorkoutRepo(db, db.workoutDao(), clock, zone)

    val setRepo: SetRepo = SetRepo(db, db.setDao(), db.workoutDao(), clock, zone)

    val analytics: Analytics = Analytics(db.setDao(), clock, zone)

    companion object {

        private const val DB_NAME = "iron.db"

        // D17: la base de datos de producción se construye con Room.databaseBuilder sobre la Application.
        // En los tests se inyecta Room.inMemoryDatabaseBuilder por constructor.
        // D48: el framework de migraciones se conecta aquí con addMigrations.
        // D51 y D24: nunca fallbackToDestructiveMigration en builds de release. Sin el fallback, si falta
        // un tramo entre IronMigrations.BASE_VERSION y la versión del @Database, Room lanza
        // IllegalStateException al abrir la base de datos en lugar de borrar las tablas. En debug tampoco
        // se usa, así el comportamiento es el mismo en los dos builds.
        // D46: SettingsDataStore.create registra la migración de claves, que se ejecuta antes del primer
        // acceso a los ajustes.
        fun create(context: Context): AppContainer = AppContainer(
            Room.databaseBuilder<IronDatabase>(context, DB_NAME)
                .addMigrations(*IronMigrations.migrations.toTypedArray())
                .build(),
            SettingsDataStore.create(context)
        )
    }
}
