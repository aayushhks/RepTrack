# RepTrack

RepTrack is an Android workout app for the gym and for home. You set a goal, log your sets quickly, track walks and runs with GPS, and see your progress toward your goal weight.

Team project for CS 501 Mobile Application Development, Fall 2026, Boston University.

## Team

- Aayush Kumar
- Patrick Kola

## Status

- Goal setup: on first launch the app asks for height, weight, goal weight, goal (lose weight, build muscle or stay fit) and gym or home. It suggests a starter plan and saves the profile on the phone with DataStore. The user can also skip it.
- Three tabs (Workouts, Exercises, Progress) that still show placeholder screens.

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
- Data sources: Room and DataStore on the phone, the wger API through Retrofit, Firebase for accounts and backup, and the fused location provider for runs

## Requirements

- Android Studio, a recent version that supports Android Gradle Plugin 8.13
- JDK 17 or newer (the JDK bundled with Android Studio works)
- Android SDK 36
- An emulator or phone running Android 8.0 (API 26) or newer

## Setup and run

1. Clone the repo: `git clone https://github.com/pitbull51067/RepTrack.git`
2. Open the folder in Android Studio and wait for Gradle sync to finish.
3. Pick an emulator or a phone and press Run.

To build from the command line: `./gradlew assembleDebug`

## Tests

- Unit tests: `./gradlew testDebugUnitTest`
  - form checks and starter plan rules
  - goal setup ViewModel, with a fake repository
  - DataStore profile repository, using a temp file (no emulator needed)
- Compose UI tests (needs a running emulator or a connected phone): `./gradlew connectedDebugAndroidTest`
  - tab navigation, and the bottom bar is hidden during goal setup
  - goal setup: fill the form and save, out of range error, skip

GitHub Actions builds the app and runs the unit tests on every push to `main` and on every pull request.

## Project structure

```
app/src/main/java/edu/bu/reptrack/
    MainActivity.kt          the only activity
    RepTrackApplication.kt   creates the AppContainer once per app start
    di/                      AppContainer, our simple manual dependency injection
    domain/profile/          profile model, form checks and starter plan rules (plain Kotlin)
    data/profile/            ProfileRepository and its DataStore version
    ui/RepTrackApp.kt        picks the first screen, bottom navigation bar and nav host
    ui/AppViewModel.kt       tells the app if goal setup is done
    ui/navigation/           tab destinations and other routes
    ui/setup/                goal setup screen and its ViewModel
    ui/workouts/             Workouts tab
    ui/exercises/            Exercises tab
    ui/progress/             Progress tab
    ui/components/           shared composables
    ui/theme/                colors, text styles and theme
```

## How we work

Each user story is a GitHub issue. We make one branch per issue and merge it through a pull request that the other person reviews. Nobody pushes straight to `main` after this first setup commit. The scrum master role rotates every week.

## Configuration and secrets

No API keys are needed yet. When we add Firebase, each developer will need their own `google-services.json`, which is listed in `.gitignore` and never committed. Setup steps will be added here at that point.

## Outside resources and licenses

- Tab icons: Material Design icons by Google, Apache License 2.0
- Jetpack libraries (Compose, Navigation, Lifecycle, Activity, DataStore): Apache License 2.0
- kotlinx.coroutines test library (tests only): Apache License 2.0
- The launcher icon was drawn for this project.
