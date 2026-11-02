package edu.bu.reptrack.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import edu.bu.reptrack.ui.exercises.ExercisesScreen
import edu.bu.reptrack.ui.navigation.TopLevelDestination
import edu.bu.reptrack.ui.progress.ProgressScreen
import edu.bu.reptrack.ui.workouts.WorkoutsScreen

/** App root: the bottom navigation bar and the nav host for the three tabs. */
@Composable
fun RepTrackApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Keep one copy of each tab and restore its state when we come back.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            // The label below already names the tab, so the icon is decorative.
                            Icon(painterResource(destination.iconRes), contentDescription = null)
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.WORKOUTS.route) { WorkoutsScreen() }
            composable(TopLevelDestination.EXERCISES.route) { ExercisesScreen() }
            composable(TopLevelDestination.PROGRESS.route) { ProgressScreen() }
        }
    }
}
