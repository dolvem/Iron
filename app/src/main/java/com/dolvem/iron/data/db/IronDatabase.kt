// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase

// D48: exportSchema = true y los JSON de esquema versionados en schemas/, para testear migraciones.
// D5: los seis DAOs siguen el mismo patrón: ABORT en el insert y el update, lecturas con Flow directo
// sin Outcome, y la preconsulta de conteo como método separado del borrado. El orden de las lecturas
// es el de las reglas de orden (ejercicios por nombre, planes por sequence con los nulos al final).
@Database(
    entities = [
        Category::class,
        Exercise::class,
        Plan::class,
        PlanExercise::class,
        Workout::class,
        ExerciseSet::class
    ],
    version = 1,
    exportSchema = true
)
@ColumnTypeConverters(Converters::class)
abstract class IronDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun planDao(): PlanDao

    abstract fun planExerciseDao(): PlanExerciseDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun setDao(): SetDao
}
