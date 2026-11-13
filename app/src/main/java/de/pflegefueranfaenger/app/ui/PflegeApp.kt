package de.pflegefueranfaenger.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.pflegefueranfaenger.app.AppViewModel
import de.pflegefueranfaenger.app.ui.lesson.LessonScreen
import de.pflegefueranfaenger.app.ui.lesson.SessionMode
import de.pflegefueranfaenger.app.ui.lookup.ArticleScreen
import de.pflegefueranfaenger.app.ui.lookup.LookupScreen
import de.pflegefueranfaenger.app.ui.lookup.TermScreen
import de.pflegefueranfaenger.app.ui.onboarding.DisclaimerScreen
import de.pflegefueranfaenger.app.ui.onboarding.PlacementChoiceScreen
import de.pflegefueranfaenger.app.ui.path.PathScreen
import de.pflegefueranfaenger.app.ui.profile.ProfileScreen
import de.pflegefueranfaenger.core.game.ProgressRules

private object Routes {
    const val PATH = "path"
    const val LOOKUP = "lookup"
    const val PROFILE = "profile"
    const val LESSON = "lesson/{id}"
    const val REVIEW = "review"
    const val PLACEMENT = "placement"
    const val ARTICLE = "article/{id}"
    const val TERM = "term/{id}"
    fun lesson(id: String) = "lesson/$id"
    fun article(id: String) = "article/$id"
    fun term(id: String) = "term/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.PATH, "Lernen", Icons.Filled.Home),
    Tab(Routes.LOOKUP, "Nachschlagen", Icons.Filled.Search),
    Tab(Routes.PROFILE, "Profil", Icons.Filled.Person),
)

@Composable
fun PflegeApp(vm: AppViewModel = viewModel()) {
    val content by vm.content.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    val c = content
    val p = progress
    if (c == null || p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!p.disclaimerAccepted) {
        DisclaimerScreen(onAccept = vm::acceptDisclaimer)
        return
    }

    val hearts = ProgressRules.heartsAt(p, now)
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route == null || tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBottomBar && p.placementPassed != null) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.PATH) {
            composable(Routes.PATH) {
                if (p.placementPassed == null) {
                    PlacementChoiceScreen(
                        onStartFromScratch = vm::startFromScratch,
                        onPlacementTest = { nav.navigate(Routes.PLACEMENT) },
                    )
                } else {
                    PathScreen(
                        content = c,
                        progress = p,
                        hearts = hearts,
                        onOpenLesson = { nav.navigate(Routes.lesson(it)) },
                        onStartReview = { nav.navigate(Routes.REVIEW) },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            composable(Routes.LOOKUP) {
                LookupScreen(
                    content = c,
                    onOpenArticle = { nav.navigate(Routes.article(it)) },
                    onOpenTerm = { nav.navigate(Routes.term(it)) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    content = c,
                    progress = p,
                    onSetDailyGoal = vm::setDailyGoal,
                    onPlacementTest = { nav.navigate(Routes.PLACEMENT) },
                    onReset = vm::resetProgress,
                    modifier = Modifier.padding(padding),
                )
            }
            composable(Routes.LESSON) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LessonScreen(
                    mode = SessionMode.Lesson(id),
                    content = c,
                    progress = p,
                    heartsNow = hearts,
                    minutesToNextHeart = ProgressRules.minutesToNextHeart(p, now),
                    onLoseHeart = vm::loseHeart,
                    onFinished = { vm.finishLesson(id, it) },
                    onClose = { nav.popBackStack() },
                    onOpenArticle = { nav.navigate(Routes.article(it)) },
                    onStartReview = { nav.popBackStack(); nav.navigate(Routes.REVIEW) },
                )
            }
            composable(Routes.REVIEW) {
                LessonScreen(
                    mode = SessionMode.Review,
                    content = c,
                    progress = p,
                    heartsNow = hearts,
                    minutesToNextHeart = null,
                    onLoseHeart = {},
                    onFinished = vm::finishReview,
                    onClose = { nav.popBackStack() },
                    onOpenArticle = { nav.navigate(Routes.article(it)) },
                    onStartReview = {},
                )
            }
            composable(Routes.PLACEMENT) {
                LessonScreen(
                    mode = SessionMode.Placement,
                    content = c,
                    progress = p,
                    heartsNow = hearts,
                    minutesToNextHeart = null,
                    onLoseHeart = {},
                    onFinished = vm::finishPlacement,
                    onClose = { nav.popBackStack() },
                    onOpenArticle = {},
                    onStartReview = {},
                )
            }
            composable(Routes.ARTICLE) { entry ->
                ArticleScreen(
                    content = c,
                    articleId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenTerm = { nav.navigate(Routes.term(it)) },
                )
            }
            composable(Routes.TERM) { entry ->
                TermScreen(content = c, termId = entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
