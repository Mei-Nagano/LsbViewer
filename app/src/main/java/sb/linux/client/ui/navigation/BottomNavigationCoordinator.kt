package sb.linux.client.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 根导航切换串行化：导航过渡期间到达的点击排队而不是被 NavController 丢弃。
 * 目的地解析统一走 [AppDestinations]，不再维护第二份 route/显示名映射。
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

        // 发帖是动作，不是可选中目的地：进入原站编辑器后立即返回，不改变根选中态。
        if (AppDestinationId.canonical(target) == AppDestinations.ACTION_NEW_TOPIC) {
            nav.navigate(AppDestinations.ACTION_NEW_TOPIC) { launchSingleTop = true }
            delay(NAV_FADE_MS.toLong())
            return
        }

        val destination = AppDestinations.byId(AppDestinationId.canonical(target)) ?: return
        val current = AppDestinationId.canonical(
            nav.currentBackStackEntry?.destination?.route.orEmpty(),
        )
        if (destination.id == current) {
            // 重选社区沿用既有刷新行为；其他根只保持当前位置。
            if (destination.id == AppDestinations.home.id) onHomeReselect()
            return
        }

        nav.navigate(destination.route) {
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
