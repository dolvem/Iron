// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dolvem.iron.feature.exercises.ExerciseDetailScreen
import com.dolvem.iron.feature.exercises.ExercisesScreen
import com.dolvem.iron.feature.history.HistoryScreen
import com.dolvem.iron.feature.plans.PlanDetailScreen
import com.dolvem.iron.feature.plans.PlansScreen
import com.dolvem.iron.feature.settings.SettingsScreen
import com.dolvem.iron.feature.today.TodayScreen

@Composable
fun IronNavHost() {
    val navController = rememberNavController()
    val currentEntry = navController.currentBackStackEntryAsState().value

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentEntry?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            // El target táctico de NavigationBarItem supera los 48dp de fábrica.
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.TODAY.route) {
                TodayScreen(onOpenSettings = { navController.navigate(SETTINGS_ROUTE) })
            }
            composable(Destination.PLANS.route) {
                PlansScreen(onOpenDetail = { planId -> navController.navigate("plan/$planId") })
            }
            // El parámetro se declara con IntType, el mismo criterio que exerciseId.
            composable(
                PLAN_DETAIL_ROUTE,
                arguments = listOf(navArgument("planId") { type = NavType.IntType })
            ) { entry ->
                val planId = entry.arguments?.getInt("planId") ?: 0
                PlanDetailScreen(planId)
            }
            composable(Destination.EXERCISES.route) {
                ExercisesScreen(onOpenDetail = { exerciseId -> navController.navigate("exercise/$exerciseId") })
            }
            // El parámetro se declara con IntType. Sin navArgument la ruta lo entrega como String y
            // getInt devuelve 0, así que el detalle siempre caía en el estado vacío.
            composable(
                EXERCISE_DETAIL_ROUTE,
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
            ) { entry ->
                val exerciseId = entry.arguments?.getInt("exerciseId") ?: 0
                ExerciseDetailScreen(exerciseId)
            }
            composable(Destination.HISTORY.route) {
                HistoryScreen()
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen()
            }
        }
    }
}
