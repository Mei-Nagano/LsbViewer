package sb.linux.client.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bottom-tab navigation is serialized here so a tap received during a destination
 * transition is queued instead of being silently lost by the NavController.
 */
internal class BottomNavigationCoordinator(
    private val nav: NavHostController,
    private val scope: CoroutineScope,
    private val closeDrawer: suspend () -> Unit,
    private val onHomeReselect: () -> Unit,
) {
    private var pendingTarget: String? = null
    private var workerRunning = false

    /** Submit a tab intent. If several taps arrive together, the latest target wins. */
    fun submit(target: String) {
        pendingTarget = target
        if (workerRunning) return
        workerRunning = true
        scope.launch {
            try {
                while (isActive) {
                    val next = pendingTarget ?: break
                    pendingTarget = null
                    navigate(next)
                }
            } finally {
                workerRunning = false
                if (pendingTarget != null) submit(pendingTarget!!)
            }
        }
    }

    private suspend fun navigate(target: String) {
        closeDrawer()
        awaitNavigationReady()

        if (target == BottomDestination.NEW_TOPIC) {
            nav.navigate(BottomDestination.NEW_TOPIC) { launchSingleTop = true }
            delay(NAV_FADE_MS.toLong())
            return
        }

        val canonicalTarget = BottomDestination.canonical(target)
        val current = BottomDestination.canonical(
            nav.currentBackStackEntry?.destination?.route.orEmpty(),
        )
        if (canonicalTarget == current) {
            if (canonicalTarget == BottomDestination.HOME) onHomeReselect()
            return
        }

        nav.navigate(BottomDestination.destination(canonicalTarget)) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        delay(NAV_FADE_MS.toLong())
    }

    private suspend fun awaitNavigationReady() {
        repeat(90) {
            val state = nav.currentBackStackEntry?.lifecycle?.currentState
            if (state == null || state.isAtLeast(Lifecycle.State.RESUMED)) return
            delay(16)
        }
    }

}

/** Canonical names shared by selected-state rendering and navigation requests. */
internal object BottomDestination {
    const val HOME = "home"
    const val FORUMS = "forums"
    const val TOPIC_COLLECTIONS = "topicCollections"
    const val DIRECT_MESSAGES = "directMessages"
    const val ME = "me"
    const val NEW_TOPIC = "newTopic"

    fun canonical(route: String): String = when (route.substringBefore("?")) {
        "topicCollectionsRoot" -> TOPIC_COLLECTIONS
        "directMessagesRoot" -> DIRECT_MESSAGES
        else -> route.substringBefore("?")
    }

    fun destination(tab: String): String = when (canonical(tab)) {
        TOPIC_COLLECTIONS -> "topicCollectionsRoot"
        DIRECT_MESSAGES -> "directMessagesRoot"
        else -> canonical(tab)
    }
}
