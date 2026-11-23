package edu.bu.reptrack.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import edu.bu.reptrack.ui.exercises.ExerciseDetailRoute
import edu.bu.reptrack.ui.exercises.ExercisesRoute
import edu.bu.reptrack.ui.howto.HowToRoute
import edu.bu.reptrack.ui.navigation.Routes
import edu.bu.reptrack.ui.plan.ExercisePickerRoute
import edu.bu.reptrack.ui.plan.PlanRoute
import edu.bu.reptrack.ui.plan.RoutineRoute
import edu.bu.reptrack.ui.navigation.TopLevelDestination
import edu.bu.reptrack.ui.profile.ProfileRoute
import edu.bu.reptrack.ui.progress.ProgressRoute
import edu.bu.reptrack.ui.session.WorkoutSessionRoute
import edu.bu.reptrack.ui.setup.GoalSetupRoute
import edu.bu.reptrack.ui.run.RunDetailRoute
import edu.bu.reptrack.ui.run.RunRoute
import edu.bu.reptrack.ui.run.story.RunStoryRoute
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
private val fullScreenRoutes = setOf(
    Routes.SIGN_IN,
    Routes.GOAL_SETUP,
    Routes.WORKOUT_SESSION,
    Routes.RUN,
    Routes.RUN_STORY,
    // Opened from a workout, which is full screen too.
    Routes.HOW_TO,
)

/**
 * Switches tabs the way the bottom bar does. Android's rule: Workouts home is always at the
 * bottom of the stack, so Back from any other tab lands there, and Back from Workouts leaves
 * the app. We pop up to the Workouts home *screen* (not the Workouts graph), keep one copy of
 * each tab, and each tab keeps its own state.
 */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(Routes.WORKOUTS_HOME) {
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
    val showTabs = currentRoute !in fullScreenRoutes
    // Material's breakpoint: from 600 dp wide (tablets, foldables, phones in landscape) the tabs
    // move to a rail on the left, so they do not stretch across a wide screen.
    val useRail = LocalConfiguration.current.screenWidthDp >= RAIL_MIN_WIDTH_DP
    val isSelected = { destination: TopLevelDestination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showTabs && !useRail) {
                NavigationBar(modifier = Modifier.testTag("nav_bar")) {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = isSelected(destination),
                            onClick = { navController.navigateToTab(destination) },
                            icon = {
                                // The label below already names the tab, so the icon is decorative.
                                Icon(painterResource(destination.iconRes), contentDescription = null)
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            // The selected tab gets the lime pill, like every main action in the app.
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(
            // consumeWindowInsets stops screens that use imePadding from adding the bar height twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            if (showTabs && useRail) {
                NavigationRail(
                    modifier = Modifier.testTag("nav_rail"),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    // Centered vertically, so the tabs sit where a thumb reaches on a tablet.
                    Spacer(Modifier.weight(1f))
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = isSelected(destination),
                            onClick = { navController.navigateToTab(destination) },
                            icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.weight(1f),
                // Material "fade through" between tabs: quick fade out, then fade in. The default is a slow
                // 700 ms crossfade that feels like a web page loading.
                enterTransition = { fadeIn(tween(durationMillis = 210, delayMillis = 90)) },
                exitTransition = { fadeOut(tween(durationMillis = 90)) },
                popEnterTransition = { fadeIn(tween(durationMillis = 210, delayMillis = 90)) },
                popExitTransition = { fadeOut(tween(durationMillis = 90)) },
            ) {
                composable(Routes.SIGN_IN) {
                    SignInRoute(
                        onFinished = {
                            if (navController.previousBackStackEntry != null) {
                                // Opened from the Profile tab, so just go back to it.
                                navController.popBackStack()
                            } else {
                                // First launch or after signing out: go on to goal setup (or the tabs if setup
                                // is already done) and remove sign in so Back does not return to it.
                                val next = if (isSetupDone) TopLevelDestination.START.route else Routes.GOAL_SETUP
                                navController.navigate(next) {
                                    popUpTo(Routes.SIGN_IN) { inclusive = true }
                                }
                            }
                        },
                    )
                }

                composable(
                    route = Routes.GOAL_SETUP,
                    enterTransition = { slideInFromRight() },
                    popExitTransition = { slideOutToRight() },
                ) {
                    GoalSetupRoute(
                        onFinished = {
                            if (navController.previousBackStackEntry != null) {
                                // Opened from the Workouts or Profile tab, so just go back to it.
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
                            onStartRun = { navController.navigate(Routes.RUN) { launchSingleTop = true } },
                            onSetUpPlan = { navController.navigate(Routes.GOAL_SETUP) },
                            onOpenPlan = { place -> navController.navigate(Routes.weekPlan(place.name)) { launchSingleTop = true } },
                            onOpenRoutine = { routineId -> navController.navigate(Routes.routine(routineId)) { launchSingleTop = true } },
                        )
                    }
                    // The week plan: training days, and which routine goes on which day.
                    composable(
                        route = Routes.WEEK_PLAN,
                        arguments = listOf(navArgument(Routes.ARG_PLACE) { type = NavType.StringType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        PlanRoute(
                            onBack = { navController.popBackStack() },
                            onOpenRoutine = { routineId -> navController.navigate(Routes.routine(routineId)) },
                        )
                    }
                    // One routine: its exercises from the catalog, and the changes the user can make.
                    composable(
                        route = Routes.ROUTINE,
                        arguments = listOf(navArgument(Routes.ARG_ROUTINE_ID) { type = NavType.StringType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        RoutineRoute(
                            onBack = { navController.popBackStack() },
                            onStart = { routineId -> navController.navigate(Routes.workoutSession(routineId)) },
                            onAdd = { routineId -> navController.navigate(Routes.routinePick(routineId)) },
                            onSwap = { routineId, index -> navController.navigate(Routes.routinePick(routineId, index)) },
                            onHowTo = { name -> navController.navigate(Routes.howTo(name)) },
                        )
                    }
                    // The catalog, to add an exercise to a routine or swap one. It closes itself once saved.
                    composable(
                        route = Routes.ROUTINE_PICK,
                        arguments = listOf(
                            navArgument(Routes.ARG_ROUTINE_ID) { type = NavType.StringType },
                            navArgument(Routes.ARG_REPLACE_INDEX) {
                                type = NavType.IntType
                                defaultValue = -1
                            },
                        ),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        ExercisePickerRoute(onBack = { navController.popBackStack() })
                    }
                    // How to do an exercise, by name: photos and steps. From a routine or during a workout.
                    composable(
                        route = Routes.HOW_TO,
                        arguments = listOf(navArgument(Routes.ARG_EXERCISE_NAME) { type = NavType.StringType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        HowToRoute(onBack = { navController.popBackStack() })
                    }
                    // A walk or run, full screen like a workout. The recording itself outlives this
                    // screen, so leaving it by mistake asks first instead of losing the run.
                    composable(
                        route = Routes.RUN,
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        RunRoute(
                            onFinished = { runId ->
                                // Close the run, then show it on its map in the Progress tab, like
                                // Strava does, so it can be shared right away. Back goes to the history.
                                navController.popBackStack()
                                navController.navigateToTab(TopLevelDestination.PROGRESS)
                                if (runId != null) navController.navigate(Routes.runDetail(runId))
                            },
                            onLeave = { navController.popBackStack() },
                        )
                    }
                    // A workout slides in from the right. With predictive back, it follows your finger
                    // when you swipe back, like other Android apps.
                    composable(
                        route = Routes.WORKOUT_SESSION,
                        arguments = listOf(navArgument(Routes.ARG_ROUTINE_ID) { type = NavType.StringType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        // The routine id reaches the ViewModel through its SavedStateHandle.
                        WorkoutSessionRoute(
                            onFinished = {
                                // Close the session, then show the saved workout on the Progress tab.
                                navController.popBackStack()
                                navController.navigateToTab(TopLevelDestination.PROGRESS)
                            },
                            onLeave = { navController.popBackStack() },
                            onHowTo = { name -> navController.navigate(Routes.howTo(name)) },
                        )
                    }
                }
                // The Exercises tab is a nested graph too: the list, then one exercise on top. The
                // list keeps its search while the detail is open.
                navigation(startDestination = Routes.EXERCISES_HOME, route = TopLevelDestination.EXERCISES.route) {
                    composable(Routes.EXERCISES_HOME) {
                        ExercisesRoute(
                            onOpenExercise = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                        )
                    }
                    composable(
                        route = Routes.EXERCISE_DETAIL,
                        arguments = listOf(navArgument(Routes.ARG_EXERCISE_ID) { type = NavType.IntType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        ExerciseDetailRoute(onBack = { navController.popBackStack() })
                    }
                }
                // The Progress tab is a nested graph too: the history, then a saved run on its map,
                // then its story picture. Each opens on top, so Back walks back through them.
                navigation(startDestination = Routes.PROGRESS_HOME, route = TopLevelDestination.PROGRESS.route) {
                    composable(Routes.PROGRESS_HOME) {
                        ProgressRoute(
                            onStartWorkout = { navController.navigateToTab(TopLevelDestination.WORKOUTS) },
                            onOpenRun = { runId -> navController.navigate(Routes.runDetail(runId)) },
                        )
                    }
                    composable(
                        route = Routes.RUN_DETAIL,
                        arguments = listOf(navArgument(Routes.ARG_RUN_ID) { type = NavType.LongType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) { entry ->
                        // The run id also reaches the ViewModel, through its SavedStateHandle.
                        val runId = entry.arguments?.getLong(Routes.ARG_RUN_ID) ?: 0L
                        RunDetailRoute(
                            onBack = { navController.popBackStack() },
                            onShare = { navController.navigate(Routes.runStory(runId)) { launchSingleTop = true } },
                        )
                    }
                    composable(
                        route = Routes.RUN_STORY,
                        arguments = listOf(navArgument(Routes.ARG_RUN_ID) { type = NavType.LongType }),
                        enterTransition = { slideInFromRight() },
                        popExitTransition = { slideOutToRight() },
                    ) {
                        RunStoryRoute(onClose = { navController.popBackStack() })
                    }
                }
                composable(TopLevelDestination.PROFILE.route) {
                    ProfileRoute(
                        // Goal setup opens with the saved stats filled in, so it works as an edit screen.
                        onEditStats = { navController.navigate(Routes.GOAL_SETUP) },
                        onSignIn = { navController.navigate(Routes.SIGN_IN) },
                        onSignedOut = {
                            // Clear everything, so Back from sign in leaves the app instead of showing the tabs.
                            navController.navigate(Routes.SIGN_IN) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** From this width the tabs move from the bottom bar to a side rail (Material's medium window size). */
private const val RAIL_MIN_WIDTH_DP = 600

private const val SLIDE_MILLIS = 300

/** Screen opened on top of a tab: slides in from the right. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight() =
    slideIntoContainer(SlideDirection.Start, tween(SLIDE_MILLIS))

/** Same screen going away with Back: slides out to the right. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight() =
    slideOutOfContainer(SlideDirection.End, tween(SLIDE_MILLIS))
