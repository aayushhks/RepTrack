package edu.bu.reptrack.domain.auth

/** Which screen the app opens on. */
enum class FirstScreen {
    SIGN_IN,
    GOAL_SETUP,
    TABS,
    ;

    companion object {
        /**
         * Sign in comes first, unless the user is already signed in or chose to use the app
         * without an account. Then goal setup, unless it is done. Then the tabs.
         */
        fun decide(signedIn: Boolean, skippedSignIn: Boolean, setupDone: Boolean): FirstScreen = when {
            !signedIn && !skippedSignIn -> SIGN_IN
            !setupDone -> GOAL_SETUP
            else -> TABS
        }
    }
}
