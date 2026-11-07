# RepTrack

RepTrack is an Android workout app for the gym and for home. You set a goal, log your sets quickly, track walks and runs with GPS, and see your progress toward your goal weight.

Team project for CS 501 Mobile Application Development, Fall 2026, Boston University.

## Team

- Aayush Kumar
- Patrick Kola

## Status

- Sign in: the app opens on a sign-in screen with Firebase email and password (sign in or create an account). The user can also choose "Use without an account", and that choice is remembered. After that the app opens on goal setup, or on the tabs if setup is done.
- Goal setup: after sign in the app asks for height, weight, goal weight, goal (lose weight, build muscle or stay fit) and gym or home. It suggests a starter plan and saves the profile on the phone with DataStore. The user can also skip it.
- Workouts tab: shows the suggested plan, current and goal weight, a gym or home switch, and the routines for the plan. Starting a routine opens the workout session screen inside the Workouts tab (set logging comes next).
- Room database for workouts: two tables (`workout_sessions` and `workout_sets`, deleting a session deletes its sets) behind a `WorkoutRepository`. It handles set numbering, renumbering after a delete, finishing a session and a history query with totals. The logging and history screens will use it next.
- Exercises and Progress tabs still show placeholder screens.

Work is tracked as GitHub issues on our project board.

## Planned features (MVP)

- Account with cloud backup (Firebase Authentication and Cloud Firestore)
- Body stats, goal weight and a weight progress chart
- Starter routine based on your goal
- Gym and home routines, set logging and a rest timer, saved with Room
- Exercise catalog and exercise swaps from the wger API
- GPS distance tracking for walks and runs, with calorie estimates

## Architecture

One activity with a Jetpack Compose UI and Navigation Compose. The planned layers are:

- Compose screens, one package per feature under `ui/`
- ViewModels that expose state with StateFlow
- Repositories that hide where data comes from
- Data sources: Room (workouts) and DataStore (profile) on the phone, the wger API through Retrofit, Firebase for accounts and backup, and the fused location provider for runs

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

## Tests

- Unit tests: `./gradlew testDebugUnitTest`
  - form checks, starter plan rules, starter routines and weight helpers
  - goal setup and Workouts ViewModels, with a fake repository
  - DataStore profile repository, using a temp file (no emulator needed)
  - sign-in ViewModel (checks, errors, email kept in saved state and the password never saved), sign-in checks and first-screen rules, with `FakeAuthRepository` (no real Firebase)
  - `FakeWorkoutRepository` (in `testing/`) is an in-memory stand-in for ViewModel tests
- Compose UI tests and Room tests (need a running emulator or a connected phone): `./gradlew connectedDebugAndroidTest`

  - Room workout repository, against an in-memory database: set numbering, renumbering after a delete, finishing (empty sessions are deleted), history order and totals, and cascade delete
  - sign-in screen: sign in, switch to create account, bad email, error message, use without an account
  - tab navigation, the bottom bar is hidden during goal setup, and starting a workout opens the session inside the Workouts tab
  - Workouts tab: plan and weight shown, start a routine, set up a plan, switch gym or home
  - goal setup: fill the form and save, out of range error, skip

GitHub Actions builds the app and runs the unit tests on every push to `main` and on every pull request.

## Project structure

```
app/src/main/java/edu/bu/reptrack/
    MainActivity.kt          the only activity
    RepTrackApplication.kt   creates the AppContainer once per app start
    di/                      AppContainer, our simple manual dependency injection
    domain/auth/             sign-in checks (CredentialsValidator) and which screen to open first (FirstScreen)
    domain/profile/          profile model, form checks, starter plan rules, weight helpers (plain Kotlin)
    domain/workout/          built-in starter routines, plus WorkoutSession, LoggedSet and SessionSummary
    data/auth/               AuthRepository and its Firebase Authentication version
    data/profile/            ProfileRepository and its DataStore version
    data/workout/            WorkoutRepository, its Room version, the two entities and the DAO
    data/db/                 RepTrackDatabase (Room, version 1, file reptrack.db)
    ui/RepTrackApp.kt        picks the first screen, bottom navigation bar and nav host
    ui/AppViewModel.kt       picks the first screen: sign in, goal setup or the tabs
    ui/auth/                 sign-in screen and its ViewModel
    ui/navigation/           tab destinations and other routes (Workouts is a nested graph)
    ui/setup/                goal setup screen and its ViewModel
    ui/workouts/             Workouts tab and its ViewModel
    ui/session/              workout session screen
    ui/exercises/            Exercises tab
    ui/progress/             Progress tab
    ui/components/           shared composables
    ui/theme/                colors, text styles and theme
```

## How we work

Each user story is a GitHub issue. We make one branch per issue and merge it through a pull request that the other person reviews. Nobody pushes straight to `main` after this first setup commit. The scrum master role rotates every week.

## Configuration and secrets

Sign-in uses Firebase Authentication, so the build needs a `google-services.json` file in the `app/` folder. It is listed in `.gitignore` and is never committed. **The build fails without it.**

1. Ask Patrick for the file, or open the RepTrack project in the Firebase console, go to Project settings, then Your apps, pick the Android app `edu.bu.reptrack` and download `google-services.json`.
2. Put it at `app/google-services.json`.

GitHub Actions gets the file from a repository secret named `GOOGLE_SERVICES_JSON`, which the workflow writes to `app/google-services.json` before building.

Tests never talk to real Firebase: they use `FakeAuthRepository`. Passwords are never saved: the sign-in screen keeps the password in memory only, not in saved state.

## Outside resources and licenses

- Tab icons and the show/hide password icons: Material Design icons by Google, Apache License 2.0
- Font: Archivo by The Archivo Project Authors, SIL Open Font License 1.1 (see `licenses/archivo-OFL.txt`). We bundle static weights cut from the variable font.
- Jetpack libraries (Compose, Navigation, Lifecycle, Activity, DataStore, Room): Apache License 2.0
- Firebase Authentication (Firebase BoM, `firebase-auth`) and the Google services Gradle plugin: Apache License 2.0
- kotlinx.coroutines Play Services (`await()` for Firebase tasks): Apache License 2.0
- kotlinx.coroutines test library (tests only): Apache License 2.0
- The launcher icon was drawn for this project.
