# RepTrack

RepTrack is an Android workout app for the gym and for home. You set a goal, plan your week, log your sets quickly, track walks and runs with GPS, and see your progress toward your goal weight.

Team project for CS 501 Mobile Application Development, Fall 2026, Boston University.

## Team

- Aayush Kumar
- Patrick Kola

## Status

Feature-complete beta (Project Update 2, November 24, 2026).

- Sign in: the app opens on a sign-in screen with Firebase email and password (sign in or create an account). The user can also choose "Use without an account", and that choice is remembered. After that the app opens on goal setup, or on the tabs if setup is done.
- Goal setup: after sign in the app asks for height, weight, goal weight, goal (lose weight, build muscle or stay fit) and gym or home. It suggests a starter plan and saves the profile on the phone with DataStore. The user can also skip it.
- Workouts tab: today's card with the goal ring and the week strip, the routine that is up next, the week plan, the plan's routines and a card to start a walk or run. A gym or home switch changes the suggested plan.
- Week plan: each day of this week with what was done, what is today, what is coming and the rest days. On the suggested plan the user picks the training days. The user can also make their own plan, from the suggested one or from scratch: name their own routines, rename or delete them, and pick a routine or a rest day for each day of the week. It repeats every week.
- Routines: open any routine to add, swap, move or remove exercises. New exercises come from the wger catalog. A built-in routine can be reset.
- Workout session: log weight and reps for each exercise, edit or delete a set, and finish the workout. Bad input shows a message and saves nothing. A rest timer starts after each set, with plus or minus 15 seconds and skip. The info button opens how to do the exercise.
- How to do it: two photos of the exercise (start and end position) that fade from one to the other, plus numbered steps, from Free Exercise DB. The photo set is downloaded once and saved on the phone.
- Exercises tab: the wger exercise catalog, downloaded with Ktor, saved in Room and refreshed once a day. Search, filter by category, and open an exercise for its muscles, equipment, picture and description. It works offline from the saved copy, and shows loading, empty and error states with Try again.
- Walk or run: GPS tracking with the fused location provider in a foreground service, so the run keeps going with the screen off. It shows distance, time, pace and calories, and asks for location (and notifications on Android 13 and newer) only when a run starts. Bad GPS fixes are dropped. A finished run is saved with its route.
- Run page and story: the route drawn on a map (MapLibre with OpenFreeMap tiles) with the run's stats. "Share to story" makes a 1080 × 1920 picture of the run (map, dark or lime style) and opens Instagram's story screen, or the share sheet. It can hide the first and last 200 m of the route.
- Progress tab: log weight and see the trend toward the goal weight on a chart, this week's workouts, distance and calories, the workout history and the runs.
- Profile tab: body stats and goal, this week's totals, the theme (dark, light or follow the phone), and sign in or sign out.
- Accessibility and layout: every screen is checked with Google's Accessibility Test Framework in dark, light and at the largest font. Tablets get a side navigation rail, and text keeps a readable width.
- Known issues: at the largest font the plan card header on the Workouts tab and the bottom bar labels wrap badly. Some wger matches show odd equipment or muscles. Map tiles need an internet connection.

Work is tracked as GitHub issues on our project board.

## Planned features (final, December 8)

- Cloud backup of workouts, runs and weights with Cloud Firestore and WorkManager
- Sign in with Google
- Fixes for the known issues above, and a full outdoor run test on a phone

## Architecture

One activity with a Jetpack Compose UI and Navigation Compose. There are no XML layouts or Fragments. The layers are:

- Compose screens, one package per feature under `ui/`
- ViewModels that expose one StateFlow of UI state each, and keep input in SavedStateHandle so it survives rotation
- Repositories (interfaces) that hide where data comes from, created once in `AppContainer`
- Data sources: Room (workouts, runs and routes, weights, the exercise catalog and plans, database version 7 with ten tables) and DataStore (profile, theme and when the catalog was last downloaded) on the phone, the wger REST API and Free Exercise DB through Ktor, Firebase for accounts, and the fused location provider for runs

The run map is the only Android View: MapLibre's `MapView` inside `AndroidView`, with the screen's lifecycle passed to it.

## Requirements

- Android Studio, a recent version that supports Android Gradle Plugin 8.13
- JDK 17 or newer (the JDK bundled with Android Studio works)
- Android SDK 36
- An emulator or phone running Android 8.0 (API 26) or newer

## Setup and run

1. Clone the repo: `git clone https://github.com/pitbull51067/RepTrack.git`
2. Add `app/google-services.json` (see Configuration and secrets below).
3. Open the folder in Android Studio and wait for Gradle sync to finish.
4. Pick an emulator or a phone and press Run.

To build from the command line: `./gradlew assembleDebug`

To try a run on the emulator, start a run, then open Extended controls → Location → Routes and play a route.

## Tests

- Unit tests (334): `./gradlew testDebugUnitTest`
  - form checks, sign-in checks, starter plan rules, starter routines and weight helpers
  - GPS rules (`RunProgressTest`: fuzzy fixes, jumps and standing-still drift add no distance), run maths, polyline encoding and route shapes
  - week plan, own plans and routine edits, weight trend, rest timer and set input checks
  - matching exercises to the wger catalog and to Free Exercise DB
  - every ViewModel, with fake repositories, including errors, empty states and coming back after the app is killed (SavedStateHandle)
  - wger download and the once-a-day refresh (`RoomExerciseRepositoryTest`, with Ktor's mock engine), the DataStore profile repository (temp file) and the saved Free Exercise DB copy (temp folder)
  - fakes for every repository are in `testing/`, so no test talks to real Firebase, wger or GPS
- Compose UI tests and Room tests (195, need a running emulator or a connected phone): `./gradlew connectedDebugAndroidTest`
  - Room repositories for workouts, runs, weights and plans, against an in-memory database
  - every screen: sign in, goal setup, Workouts, the workout session (`WorkoutSessionTest` logs a set), week plan, routine, exercise picker, Exercises, exercise detail, how-to, run, run page, story, Progress and Profile
  - tab navigation and back behavior
  - the story picture renderer
  - accessibility checks (`AccessibilityChecksTest`, 47 tests): touch targets, text contrast and labels on every screen, in dark, light and at the largest font

GitHub Actions builds the app and runs the unit tests on every push to `main` and on every pull request. The device tests need an emulator, so we run them locally before merging.

## Project structure

```
app/src/main/java/edu/bu/reptrack/
    MainActivity.kt          the only activity
    RepTrackApplication.kt   creates the AppContainer once per app start
    di/                      AppContainer, our simple manual dependency injection
    domain/auth/             sign-in checks (CredentialsValidator) and which screen to open first (FirstScreen)
    domain/profile/          profile model, form checks, starter plan rules, weight helpers (plain Kotlin)
    domain/workout/          starter routines, routine edits, week plan, own plans, sessions and stats
    domain/exercise/         exercise model, catalog matching and how-to photo matching
    domain/run/              GPS rules (RunProgress), run maths, routes and polyline encoding
    domain/weight/           weight entries and the trend chart points
    domain/settings/         theme mode
    data/auth/               AuthRepository and its Firebase Authentication version
    data/profile/            ProfileRepository and its DataStore version
    data/workout/            WorkoutRepository, its Room version, the two entities and the DAO
    data/exercise/           ExerciseRepository, the wger API (Ktor), its Room cache and the refresh time
    data/demo/               how-to photos from Free Exercise DB, downloaded once and saved as a file
    data/plan/               PlanRepository: routine edits, training days and own plans in Room
    data/run/                RunRepository, its Room version and the RunRecorder for the run in progress
    data/location/           LocationTracker and its fused location version
    data/weight/             WeightRepository and its Room version
    data/db/                 RepTrackDatabase (Room, version 7, file reptrack.db)
    service/                 the foreground service that keeps a run going with the screen off
    ui/RepTrackApp.kt        picks the first screen, bottom bar or side rail, and nav host
    ui/AppViewModel.kt       picks the first screen: sign in, goal setup or the tabs
    ui/auth/                 sign-in screen and its ViewModel
    ui/navigation/           tab destinations and other routes (Workouts is a nested graph)
    ui/setup/                goal setup screen and its ViewModel
    ui/workouts/             Workouts tab and its ViewModel
    ui/plan/                 week plan, routine and exercise picker screens and their ViewModels
    ui/session/              workout session screen and its ViewModel
    ui/howto/                how-to screen and the fading photos
    ui/exercises/            Exercises tab and exercise detail
    ui/run/                  run screen, run page and the route map
    ui/run/story/            the story picture and sharing it
    ui/progress/             Progress tab and the weight chart
    ui/profile/              Profile tab
    ui/components/           shared composables
    ui/theme/                colors, text styles, shapes and theme
```

## How we work

Each user story is a GitHub issue. We make one branch per feature and merge it through a pull request that the other person reviews. All code goes through pull requests; only the presentation slides and README updates were committed straight to `main`. The scrum master role rotates every week.

## Configuration and secrets

Sign-in uses Firebase Authentication, so the build needs a `google-services.json` file in the `app/` folder. It is listed in `.gitignore` and is never committed. **The build fails without it.**

GitHub Actions gets the file from a repository secret named `GOOGLE_SERVICES_JSON`, which the workflow writes to `app/google-services.json` before building.

The wger API, Free Exercise DB and OpenFreeMap need no API key or account.

Tests never talk to real Firebase: they use `FakeAuthRepository`. Passwords are never saved: the sign-in screen keeps the password in memory only, not in saved state.

Run routes stay on the phone. A story picture leaves the phone only when the user taps Share.

## Outside resources and licenses

- Icons: Material Design icons by Google, Apache License 2.0. The plus, check, flame, play, run and stop icons were drawn for this project.
- Fonts: Archivo by The Archivo Project Authors and Barlow Condensed by The Barlow Project Authors, SIL Open Font License 1.1 (see `licenses/archivo-OFL.txt` and `licenses/barlow-condensed-OFL.txt`). We bundle static weights.
- Exercise catalog: data and pictures from wger (wger.de), Creative Commons Attribution-ShareAlike 4.0. The app shows this credit under the catalog.
- How-to photos and steps: Free Exercise DB by yuhonas (github.com/yuhonas/free-exercise-db), The Unlicense (public domain)
- Maps: MapLibre Android SDK, BSD 2-Clause License. Map tiles from OpenFreeMap (openfreemap.org), with map data © OpenStreetMap contributors (Open Database License) and the OpenMapTiles schema. The app shows this credit under every map.
- Jetpack libraries (Compose, Material 3, Navigation, Lifecycle, Activity, DataStore, Room): Apache License 2.0
- Firebase Authentication (Firebase BoM, `firebase-auth`) and the Google services Gradle plugin: Apache License 2.0
- Google Play services location (fused location provider): Android Software Development Kit License
- Ktor client, kotlinx.serialization and kotlinx.coroutines (including Play Services `await()` for Firebase tasks): Apache License 2.0
- Coil (image loading) and OkHttp: Apache License 2.0
- Tests only: kotlinx.coroutines test library, Ktor mock engine and Google's Accessibility Test Framework (through Compose UI test accessibility): Apache License 2.0
- The launcher icon was drawn for this project.
