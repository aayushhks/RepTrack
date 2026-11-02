package edu.bu.reptrack

import edu.bu.reptrack.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun routesAreUniqueAndNotBlank() {
        val routes = TopLevelDestination.entries.map { it.route }
        assertTrue(routes.none { it.isBlank() })
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun appStartsOnWorkouts() {
        assertEquals(TopLevelDestination.WORKOUTS, TopLevelDestination.START)
    }

    @Test
    fun thereAreThreeTabs() {
        assertEquals(3, TopLevelDestination.entries.size)
    }
}
