package edu.bu.reptrack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.auth.AuthError
import edu.bu.reptrack.data.auth.AuthUser
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.settings.ThemeMode
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WeekActivity
import edu.bu.reptrack.domain.workout.WorkoutSession
import edu.bu.reptrack.ui.auth.SignInScreen
import edu.bu.reptrack.ui.auth.SignInUiState
import edu.bu.reptrack.ui.exercises.ExerciseDetailScreen
import edu.bu.reptrack.ui.exercises.ExerciseDetailUiState
import edu.bu.reptrack.ui.exercises.ExercisesScreen
import edu.bu.reptrack.ui.exercises.ExercisesUiState
import edu.bu.reptrack.ui.howto.HowToScreen
import edu.bu.reptrack.ui.howto.HowToUiState
import edu.bu.reptrack.ui.plan.ExercisePickerScreen
import edu.bu.reptrack.ui.plan.ExercisePickerUiState
import edu.bu.reptrack.ui.plan.PlanScreen
import edu.bu.reptrack.ui.plan.PlanUiState
import edu.bu.reptrack.ui.plan.RoutineExerciseUi
import edu.bu.reptrack.ui.plan.RoutineScreen
import edu.bu.reptrack.ui.plan.RoutineUiState
import edu.bu.reptrack.ui.profile.ProfileScreen
import edu.bu.reptrack.ui.profile.ProfileUiState
import edu.bu.reptrack.ui.progress.HistoryItem
import edu.bu.reptrack.ui.progress.ProgressScreen
import edu.bu.reptrack.ui.progress.ProgressUiState
import edu.bu.reptrack.ui.progress.WeekTotals
import edu.bu.reptrack.ui.progress.summarizeWeights
import edu.bu.reptrack.ui.run.RunDetailScreen
import edu.bu.reptrack.ui.run.RunDetailUiState
import edu.bu.reptrack.ui.run.RunScreen
import edu.bu.reptrack.ui.run.RunUiState
import edu.bu.reptrack.ui.run.story.RunStoryScreen
import edu.bu.reptrack.ui.run.story.RunStoryUiState
import edu.bu.reptrack.ui.session.ExerciseUi
import edu.bu.reptrack.ui.session.RestTimerUi
import edu.bu.reptrack.ui.session.SessionError
import edu.bu.reptrack.ui.session.WorkoutSessionScreen
import edu.bu.reptrack.ui.session.WorkoutSessionUiState
import edu.bu.reptrack.ui.setup.GoalSetupScreen
import edu.bu.reptrack.ui.setup.GoalSetupUiState
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.workouts.WorkoutsScreen
import edu.bu.reptrack.ui.workouts.WorkoutsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Runs Google's Accessibility Test Framework over every main screen, in the dark and the light
 * theme and at the largest font size. A test fails on a touch target under 48 dp, text with too
 * little contrast, or a button with no label TalkBack can read.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityChecksTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Draws [content] on the theme's background, like in the app, then runs the checks. */
    private fun check(dark: Boolean = true, fontScale: Float? = null, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale == null) density else Density(density.density, fontScale),
            ) {
                RepTrackTheme(darkTheme = dark) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        content()
                    }
                }
            }
        }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    // ---- Sample data ----

    private val profile = UserProfile(178f, 82.4f, 78f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM)
    private val day = 24 * 60 * 60_000L
    private val now = 1_794_240_000_000 // Monday, November 9, 2026

    private fun workoutsState(): WorkoutsUiState {
        val plan = StarterPlan.suggest(profile.goal, profile.place)
        return WorkoutsUiState(
            isLoading = false,
            profile = profile,
            place = TrainingPlace.GYM,
            plan = plan,
            routines = StarterRoutines.forPlan(plan.type),
            week = WeekActivity.thisWeek(listOf(now - day), listOf(now), now, ZoneOffset.UTC),
            streakDays = 2,
            workoutsThisWeek = 1,
            goalProgress = 0.33f,
        )
    }

    private fun progressState(): ProgressUiState {
        val weights = listOf(84.6f, 84.1f, 83.2f, 82.4f)
            .mapIndexed { i, kg -> WeightEntry(id = i + 1L, loggedAt = now + i * 4 * day, weightKg = kg) }
        val session = WorkoutSession(7, "upper", "Upper body", startedAt = now, finishedAt = now + 52 * 60_000L)
        return ProgressUiState(
            isLoading = false,
            weight = summarizeWeights(weights, goalKg = 78f),
            week = WeekTotals(workouts = 1, distanceM = 5_020.0, calories = 410),
            items = listOf(
                HistoryItem.Run(RunSummary(4, now, now + 26 * 60_000L, 5_020.0, 26 * 60_000L, 410)),
                HistoryItem.Workout(SessionSummary(session, exerciseCount = 2, setCount = 3, totalVolumeKg = 1200f)),
            ),
        )
    }

    @Composable
    private fun Workouts() = WorkoutsScreen(
        state = workoutsState(),
        onPlaceSelected = {},
        onStartWorkout = {},
        onStartRun = {},
        onSetUpPlan = {},
    )

    @Composable
    private fun Progress(state: ProgressUiState = progressState()) = ProgressScreen(
        state = state,
        onStartWorkout = {},
        onLogWeight = {},
        onWeightInput = {},
        onSaveWeight = {},
        onDismissLogWeight = {},
        onDeleteWeight = {},
        onConfirmDelete = {},
        onDismissDelete = {},
        onRetry = {},
        onDismissMessage = {},
    )

    // ---- Workouts ----

    @Test
    fun workouts_dark() = check(dark = true) { Workouts() }

    @Test
    fun workouts_light() = check(dark = false) { Workouts() }

    @Test
    fun workouts_largestFont() = check(fontScale = 2f) { Workouts() }

    // ---- Progress ----

    @Test
    fun progress_dark() = check(dark = true) { Progress() }

    @Test
    fun progress_light() = check(dark = false) { Progress() }

    @Test
    fun progress_largestFont() = check(fontScale = 2f) { Progress() }

    @Test
    fun progress_errorState() = check { Progress(ProgressUiState(isLoading = false, loadFailed = true)) }

    // ---- Workout and run ----

    @Test
    fun workoutSession_withSetsRestAndAnError() = check {
        val sets = listOf(LoggedSet(1, 1, "Squat", 1, 60f, 10, 0), LoggedSet(2, 1, "Squat", 2, 60f, 10, 0))
        WorkoutSessionScreen(
            state = WorkoutSessionUiState(
                routineName = "Full body A",
                exercises = listOf(ExerciseUi("Squat", sets, weightInput = "60", repsInput = "10"), ExerciseUi("Bench press")),
                setCount = 2,
                totalVolumeKg = 1200f,
                rest = RestTimerUi(endsAt = System.currentTimeMillis() + 60_000, totalMs = 90_000),
                error = SessionError.SET_NOT_SAVED,
            ),
            onWeightChange = { _, _ -> },
            onRepsChange = { _, _ -> },
            onAddSet = {},
            onEditSet = {},
            onClose = {},
            onFinish = {},
            onAdjustRest = {},
            onSkipRest = {},
            onDismissError = {},
        )
    }

    @Test
    fun run_whileRunning() = check {
        RunScreen(
            state = RunUiState(isRunning = true, elapsedMs = 754_000, distanceM = 2_410.0, paceSecondsPerKm = 313, calories = 182),
            onStart = {},
            onFinish = {},
            onClose = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun run_permissionDenied() = check {
        RunScreen(state = RunUiState(permissionDenied = true), onStart = {}, onFinish = {}, onClose = {}, onOpenSettings = {})
    }

    // ---- A saved run on its map, and its story ----

    private fun runDetailState() = RunDetailUiState(
        isLoading = false,
        run = RunSummary(4, now, now + 26 * 60_000L, 5_020.0, 26 * 60_000L, 410),
        route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.09)),
        dayPart = DayPart.MORNING,
    )

    // A plain box stands in for the map, which needs the internet and a graphics driver.
    @Composable
    private fun RunDetail(state: RunDetailUiState = runDetailState()) = RunDetailScreen(
        state = state,
        onBack = {},
        onShare = {},
        onRetry = {},
        map = { _, mapModifier -> Box(mapModifier) },
    )

    @Composable
    private fun Story(state: RunStoryUiState = RunStoryUiState(isLoading = false, run = runDetailState().run, hasRoute = true, imagePath = "/cache/stories/run_4.png")) =
        RunStoryScreen(
            state = state,
            instagramAvailable = true,
            onClose = {},
            onStyleSelected = {},
            onHideEndsChange = {},
            onShareInstagram = {},
            onShareMore = {},
            onRetry = {},
            preview = { _, description, previewModifier -> Box(previewModifier.semantics { contentDescription = description }) },
        )

    @Test
    fun runDetail_dark() = check(dark = true) { RunDetail() }

    @Test
    fun runDetail_light() = check(dark = false) { RunDetail() }

    @Test
    fun runDetail_largestFont() = check(fontScale = 2f) { RunDetail() }

    @Test
    fun runDetail_withoutARoute() = check { RunDetail(runDetailState().copy(route = emptyList())) }

    @Test
    fun story_dark() = check(dark = true) { Story() }

    @Test
    fun story_light() = check(dark = false) { Story() }

    @Test
    fun story_largestFont() = check(fontScale = 2f) { Story() }

    @Test
    fun story_whileDrawing() = check { Story(RunStoryUiState(isLoading = false, run = runDetailState().run, hasRoute = true, isDrawing = true)) }

    // ---- Exercises and one exercise ----

    private val squat = Exercise(
        id = 1,
        name = "Squat",
        description = "Stand with your feet shoulder-width apart.\nLower until your thighs are level.",
        category = "Legs",
        muscles = listOf("Quads", "Glutes"),
        equipment = listOf("Barbell"),
        imageUrl = null,
    )

    private fun exercisesState() = ExercisesUiState(
        isLoading = false,
        exercises = listOf(squat, squat.copy(id = 2, name = "Push-up", category = "Chest", equipment = emptyList())),
        categories = listOf("Chest", "Legs"),
        hasSaved = true,
    )

    @Composable
    private fun Exercises(state: ExercisesUiState = exercisesState()) = ExercisesScreen(
        state = state,
        onQueryChange = {},
        onCategoryClick = {},
        onClearSearch = {},
        onRetry = {},
        onOpenExercise = {},
    )

    @Composable
    private fun ExerciseDetail(state: ExerciseDetailUiState = ExerciseDetailUiState(isLoading = false, exercise = squat)) =
        ExerciseDetailScreen(state = state, onBack = {})

    @Test
    fun exercises_dark() = check(dark = true) { Exercises() }

    @Test
    fun exercises_light() = check(dark = false) { Exercises() }

    @Test
    fun exercises_largestFont() = check(fontScale = 2f) { Exercises() }

    @Test
    fun exercises_offlineWithSavedList() = check { Exercises(exercisesState().copy(refreshFailed = true)) }

    @Test
    fun exercises_offlineWithNothingSaved() = check {
        Exercises(ExercisesUiState(isLoading = false, refreshFailed = true))
    }

    @Test
    fun exercises_noMatches() = check { Exercises(exercisesState().copy(exercises = emptyList(), query = "zzz")) }

    @Test
    fun exerciseDetail_dark() = check(dark = true) { ExerciseDetail() }

    @Test
    fun exerciseDetail_light() = check(dark = false) { ExerciseDetail() }

    @Test
    fun exerciseDetail_largestFont() = check(fontScale = 2f) { ExerciseDetail() }

    // ---- Week plan, a routine and the catalog picker ----

    private fun planState(): PlanUiState {
        val plan = StarterPlan.suggest(FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM)
        val monday = LocalDate.parse("2026-11-23")
        return PlanUiState(
            isLoading = false,
            plan = plan,
            trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            customDays = true,
            week = (0L until 7L).map { offset ->
                val date = monday.plusDays(offset)
                when (offset) {
                    0L -> PlanDay(date, false, true, "full_body_a", "Full body A", PlanDayStatus.DONE)
                    2L -> PlanDay(date, true, true, "full_body_b", "Full body B", PlanDayStatus.TODAY)
                    4L -> PlanDay(date, false, true, "full_body_a", "Full body A", PlanDayStatus.PLANNED)
                    else -> PlanDay(date, false, false, null, null, PlanDayStatus.REST)
                }
            },
            routines = StarterRoutines.forPlan(plan.type),
        )
    }

    @Composable
    private fun Plan() = PlanScreen(state = planState(), onBack = {}, onToggleDay = {}, onResetDays = {}, onOpenRoutine = {})

    private val benchPress = Exercise(73, "Bench Press", "", "Chest", listOf("Chest", "Triceps"), listOf("Barbell"), null)

    @Composable
    private fun Routine() = RoutineScreen(
        state = RoutineUiState(
            isLoading = false,
            routineId = "upper",
            name = "Upper body",
            exercises = listOf(RoutineExerciseUi("Bench press", benchPress), RoutineExerciseUi("Barbell row", null)),
            isEdited = true,
        ),
        onBack = {},
        onStart = {},
        onAdd = {},
        onSwap = {},
        onRemove = {},
        onMoveUp = {},
        onMoveDown = {},
        onReset = {},
        onHowTo = {},
    )

    @Composable
    private fun Picker() = ExercisePickerScreen(
        state = ExercisePickerUiState(
            isLoading = false,
            routineName = "Upper body",
            replacing = "Bench press",
            query = "",
            sameMuscles = listOf(benchPress.copy(id = 2, name = "Dumbbell Bench Press", equipment = listOf("Dumbbell"))),
            results = listOf(benchPress.copy(id = 3, name = "Dips", equipment = emptyList())),
        ),
        onBack = {},
        onQueryChange = {},
        onPick = {},
    )

    private val squatDemo = ExerciseDemo(
        id = "Barbell_Squat",
        name = "Barbell Squat",
        imageUrls = listOf("https://example.invalid/0.jpg", "https://example.invalid/1.jpg"),
        steps = listOf("Stand under the bar with your feet shoulder width apart.", "Squat down, then stand back up."),
        primaryMuscles = listOf("quadriceps"),
        equipment = "barbell",
    )

    @Composable
    private fun HowTo(state: HowToUiState = HowToUiState(name = "Squat", isLoading = false, demo = squatDemo, catalog = benchPress.copy(name = "Squat"))) =
        HowToScreen(state = state, onBack = {}, onRetry = {})

    @Test
    fun howTo_dark() = check(dark = true) { HowTo() }

    @Test
    fun howTo_light() = check(dark = false) { HowTo() }

    @Test
    fun howTo_largestFont() = check(fontScale = 2f) { HowTo() }

    @Test
    fun howTo_failed() = check { HowTo(HowToUiState(name = "Squat", isLoading = false, loadFailed = true)) }

    @Test
    fun exerciseDetail_withHowToPhotos() = check { ExerciseDetail(ExerciseDetailUiState(isLoading = false, exercise = squat, demo = squatDemo)) }

    @Test
    fun weekPlan_dark() = check(dark = true) { Plan() }

    @Test
    fun weekPlan_light() = check(dark = false) { Plan() }

    @Test
    fun weekPlan_largestFont() = check(fontScale = 2f) { Plan() }

    @Test
    fun routine_dark() = check(dark = true) { Routine() }

    @Test
    fun routine_light() = check(dark = false) { Routine() }

    @Test
    fun routine_largestFont() = check(fontScale = 2f) { Routine() }

    @Test
    fun picker_dark() = check(dark = true) { Picker() }

    @Test
    fun picker_light() = check(dark = false) { Picker() }

    // ---- Profile, sign in and setup ----

    @Test
    fun profile_light() = check(dark = false) {
        ProfileScreen(
            state = ProfileUiState(
                isLoading = false,
                user = AuthUser("u1", "aayush@bu.edu"),
                profile = profile,
                workoutsThisWeek = 3,
                volumeThisWeekKg = 9370f,
                themeMode = ThemeMode.LIGHT,
            ),
            onEditStats = {},
            onSignIn = {},
            onSignOut = {},
            onThemeChange = {},
        )
    }

    @Test
    fun signIn_withAnError() = check {
        SignInScreen(
            state = SignInUiState(email = "runner@bu.edu", password = "secret1", authError = AuthError.NETWORK),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onToggleMode = {},
            onSkip = {},
        )
    }

    @Test
    fun goalSetup() = check {
        GoalSetupScreen(
            state = GoalSetupUiState(height = "178", weight = "82", goalWeight = "76", canSave = true),
            onHeightChange = {},
            onWeightChange = {},
            onGoalWeightChange = {},
            onGoalSelected = {},
            onPlaceSelected = {},
            onStart = {},
            onSkip = {},
        )
    }
}
