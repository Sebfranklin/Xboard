package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.studio.LexiconScreen
import com.example.ui.studio.SnippetsScreen
import com.example.ui.studio.StudioScreen
import com.example.ui.studio.StudioViewModel
import com.example.ui.studio.TuningScreen
import com.example.ui.theme.KeyCraftTheme

const val ROUTE_STUDIO = "studio"
const val ROUTE_SNIPPETS = "snippets"
const val ROUTE_LEXICON = "lexicon"
const val ROUTE_TUNING = "tuning"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val destinations = listOf(
    NavDestinationItem(ROUTE_STUDIO, R.string.nav_studio, Icons.Filled.Keyboard, Icons.Outlined.Keyboard, "nav_studio"),
    NavDestinationItem(ROUTE_SNIPPETS, R.string.nav_snippets, Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh, "nav_snippets"),
    NavDestinationItem(ROUTE_LEXICON, R.string.nav_lexicon, Icons.Filled.Book, Icons.Outlined.Book, "nav_lexicon"),
    NavDestinationItem(ROUTE_TUNING, R.string.nav_tuning, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_tuning")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeyCraftTheme {
                KeyCraftCompanionApp()
            }
        }
    }
}

@Composable
fun KeyCraftCompanionApp(studioViewModel: StudioViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_STUDIO

    val settings by studioViewModel.settings.collectAsStateWithLifecycle()
    val snippets by studioViewModel.snippets.collectAsStateWithLifecycle()
    val learnedWords by studioViewModel.learnedWords.collectAsStateWithLifecycle()
    val topBigrams by studioViewModel.topBigrams.collectAsStateWithLifecycle()
    val clipboardItems by studioViewModel.clipboardItems.collectAsStateWithLifecycle()
    val imeStatus by studioViewModel.imeStatus.collectAsStateWithLifecycle()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            viewModel = studioViewModel,
                            settings = settings,
                            snippets = snippets,
                            clipboardItems = clipboardItems,
                            imeStatus = imeStatus,
                            onNavigateToTuning = { navigateTo(ROUTE_TUNING) }
                        )
                    }
                    composable(ROUTE_SNIPPETS) {
                        SnippetsScreen(
                            snippets = snippets,
                            onSaveSnippet = { trig, exp, cat, id ->
                                studioViewModel.addOrUpdateSnippet(trig, exp, cat, id)
                            },
                            onToggleSnippet = { studioViewModel.toggleSnippet(it) },
                            onDeleteSnippet = { studioViewModel.deleteSnippet(it) }
                        )
                    }
                    composable(ROUTE_LEXICON) {
                        LexiconScreen(
                            learnedWords = learnedWords,
                            topBigrams = topBigrams,
                            onAddCustomWord = { studioViewModel.addCustomWord(it) },
                            onDeleteWord = { studioViewModel.deleteLearnedWord(it) },
                            onResetLearned = { studioViewModel.resetAutoLearnedMemory() }
                        )
                    }
                    composable(ROUTE_TUNING) {
                        TuningScreen(
                            settings = settings,
                            viewModel = studioViewModel
                        )
                    }
                }
            }
        }
    }
}
