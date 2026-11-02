package edu.bu.reptrack

import android.app.Application
import edu.bu.reptrack.di.AppContainer

/** Creates the [AppContainer] once for the whole app. */
class RepTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
