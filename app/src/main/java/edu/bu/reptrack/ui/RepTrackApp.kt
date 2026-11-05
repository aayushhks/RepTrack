package edu.bu.reptrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.bu.reptrack.ui.exercises.ExercisesScreen
import edu.bu.reptrack.ui.navigation.Routes
import edu.bu.reptrack.ui.navigation.TopLevelDestination
import edu.bu.reptrack.ui.progress.ProgressScreen
import edu.bu.reptrack.ui.session.WorkoutSessionScreen
import edu.bu.reptrack.ui.setup.GoalSetupRoute
import edu.bu.reptrack.ui.workouts.WorkoutsRoute

/** App root: waits for the saved profile flag, then opens on goal setup or on the tabs. */
@Composable
fun RepTrackApp(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory),
) {
    val setupDone by appViewModel.isSetupDone.collectAsStateWithLifecycle()

    when (val done = setupDone) {
        // DataStore takes a few milliseconds to read. Show the plain window background meanwhile.
        null -> Box(modifier.fillMaxSize())
        else -> {
            // Decide the first screen once, so finishing setup does not rebuild the nav graph.
            val startRoute = rememberSaveable {
                if (done) TopLevelDestination.START.route else Routes.GOAL_SETUP
            }
            RepTrackNavHost(startRoute = startRoute, modifier = modifier)
        }
    }
}

/** Bottom navigation bar plus the nav host. The bar is hidden during goal setup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepTrackNavHost(startRoute: String, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route ?: startRoute
    val showBottomBar = currentRoute != Routes.GOAL_SETUP

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Keep one copy of each tab and restore its state when we come back.
                                    // Workouts is always at the bottom of the stack, even when the
                                    // graph started on goal setup, so we pop up to it.
                                    popUpTo(TopLevelDestination.START.route) {
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            // consumeWindowInsets stops screens that use imePadding from adding the bar height twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.GOAL_SETUP) {
                GoalSetupRoute(
                    onFinished = {
                        if (navController.previousBackStackEntry != null) {
                            // Opened from the Workouts tab, so just go back to it.
                            navController.popBackStack()
                        } else {
                            // First launch: open the tabs, and remove setup so Back does not return to it.
                            navController.navigate(TopLevelDestination.START.route) {
                                popUpTo(Routes.GOAL_SETUP) { inclusive = true }
                            }
                        }
                    },
                )
            }

            // The Workouts tab is a nested graph: Workouts home, then a workout session on top.
            navigation(startDestination = Routes.WORKOUTS_HOME, route = TopLevelDestination.WORKOUTS.route) {
                composable(Routes.WORKOUTS_HOME) {
                    WorkoutsRoute(
                        onStartWorkout = { routineId -> navController.navigate(Routes.workoutSession(routineId)) },
                        onSetUpPlan = { navController.navigate(Routes.GOAL_SETUP) },
                    )
                }
                composable(
                    route = Routes.WORKOUT_SESSION,
                    arguments = listOf(navArgument(Routes.ARG_ROUTINE_ID) { type = NavType.StringType }),
                ) { entry ->
                    WorkoutSessionScreen(routineId = entry.arguments?.getString(Routes.ARG_ROUTINE_ID).orEmpty())
                }
            }
            composable(TopLevelDestination.EXERCISES.route) { ExercisesScreen() }
            composable(TopLevelDestination.PROGRESS.route) { ProgressScreen() }
        }
    }
}
