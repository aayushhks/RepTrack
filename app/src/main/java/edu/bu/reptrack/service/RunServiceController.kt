package edu.bu.reptrack.service

/**
 * Starts the run service. There is no stop: the service stops itself as soon as the run ends,
 * after it has shown its notification, which Android requires of every foreground service.
 * An interface, so the run ViewModel can be tested without Android.
 */
interface RunServiceController {
    fun start()
}
