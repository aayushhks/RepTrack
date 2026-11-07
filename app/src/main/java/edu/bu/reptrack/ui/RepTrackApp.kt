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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.bu.reptrack.domain.auth.FirstScreen
import edu.bu.reptrack.ui.auth.SignInRoute
import edu.bu.reptrack.ui.exercises.ExercisesScreen
import edu.bu.reptrack.ui.navigation.Routes
import edu.bu.reptrack.ui.navigation.TopLevelDestination
import edu.bu.reptrack.ui.progress.ProgressRoute
import edu.bu.reptrack.ui.session.WorkoutSessionRoute
import edu.bu.reptrack.ui.setup.GoalSetupRoute
import edu.bu.reptrack.ui.workouts.WorkoutsRoute

/** App root: waits for the saved sign-in and profile state, then opens on sign in, goal setup or the tabs. */
@Composable
fun RepTrackApp(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory),
) {
    val firstScreen by appViewModel.firstScreen.collectAsStateWithLifecycle()
    val setupDone by appViewModel.isSetupDone.collectAsStateWithLifecycle()

    when (val first = firstScreen) {
        // Firebase and DataStore take a moment to answer. Show the plain window background meanwhile.
        null -> Box(modifier.fillMaxSize())
        else -> {
            // Decide the first screen once, so signing in or finishing setup does not rebuild the nav graph.
            val startRoute = rememberSaveable { first.route() }
            RepTrackNavHost(
                startRoute = startRoute,
                modifier = modifier,
                isSetupDone = setupDone == true,
            )
        }
    }
}

private fun FirstScreen.route(): String = when (this) {
    FirstScreen.SIGN_IN -> Routes.SIGN_IN
    FirstScreen.GOAL_SETUP -> Routes.GOAL_SETUP
    FirstScreen.TABS -> TopLevelDestination.START.route
}

/** Screens that take the whole screen, so the bottom bar is hidden on them. */
private val fullScreenRoutes = setOf(Routes.SIGN_IN, Routes.GOAL_SETUP, Routes.WORKOUT_SESSION)

/**
 * Switches tabs the way the bottom bar does: one copy of each tab, and each tab keeps its
 * own state. Workouts is always at the bottom of the stack, so we pop up to it.
 */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(TopLevelDestination.START.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Bottom navigation bar plus the nav host. The bar is hidden on sign in, goal setup and
 * during a workout. [isSetupDone] tells sign in where to go next: goal setup, or the tabs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepTrackNavHost(
    startRoute: String,
    modifier: Modifier = Modifier,
    isSetupDone: Boolean = false,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route ?: startRoute
    val showBottomBar = currentRoute !in fullScreenRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination) },
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
            composable(Routes.SIGN_IN) {
                SignInRoute(
                    onFinished = {
                        // Signed in, created an account or skipped. Go on to goal setup (or the tabs if
                        // setup is already done) and remove sign in so Back does not return to it.
                        val next = if (isSetupDone) TopLevelDestination.START.route else Routes.GOAL_SETUP
                        navController.navigate(next) {
                            popUpTo(Routes.SIGN_IN) { inclusive = true }
                        }
                    },
                )
            }

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
                ) {
                    // The routine id reaches the ViewModel through its SavedStateHandle.
                    WorkoutSessionRoute(
                        onFinished = {
                            // Close the session, then show the saved workout on the Progress tab.
                            navController.popBackStack()
                            navController.navigateToTab(TopLevelDestination.PROGRESS)
                        },
                        onLeave = { navController.popBackStack() },
                    )
                }
            }
            composable(TopLevelDestination.EXERCISES.route) { ExercisesScreen() }
            composable(TopLevelDestination.PROGRESS.route) {
                ProgressRoute(onStartWorkout = { navController.navigateToTab(TopLevelDestination.WORKOUTS) })
            }
        }
    }
}
