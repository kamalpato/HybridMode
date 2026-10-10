package com.hybridmode.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hybridmode.app.ui.home.HomeScreen
import com.hybridmode.app.ui.library.LibraryScreen
import com.hybridmode.app.ui.library.PhotoDetailScreen
import com.hybridmode.app.ui.navigation.MainTab
import com.hybridmode.app.ui.navigation.Routes
import com.hybridmode.app.ui.profile.ProfileScreen
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Forest800
import com.hybridmode.app.ui.theme.Mint400
import com.hybridmode.app.ui.upload.UploadScreen

@Composable
fun MainShell() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showChrome = currentRoute != Routes.CAPTURE
    val selectedTabRoute = when {
        currentRoute == Routes.HOME -> Routes.HOME
        currentRoute == Routes.REPORTS || currentRoute?.startsWith("report/") == true ->
            Routes.REPORTS
        currentRoute == Routes.PROFILE -> Routes.PROFILE
        else -> currentRoute
    }
    val showFab = currentRoute == Routes.HOME
    val dimens = rememberAppDimens()
    val useRail = dimens.useNavigationRail && showChrome

    fun openNewReport() {
        navController.navigate(Routes.CAPTURE) {
            launchSingleTop = true
        }
    }

    // Flat tab graph: never save/restore tab stacks. Capture used to get saved onto
    // Home via restoreState, so tapping Home after submit→Reports put Reports/Capture
    // back on top and looked "stuck".
    fun goTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) {
                inclusive = route == Routes.HOME
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    Row(Modifier.fillMaxSize()) {
        if (useRail) {
            AdaptiveNavigationRail(
                currentRoute = selectedTabRoute,
                onTab = ::goTab,
                onNewReport = ::openNewReport,
                showFab = showFab,
            )
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (showFab && !useRail) {
                    FloatingActionButton(
                        onClick = ::openNewReport,
                        shape = CircleShape,
                        containerColor = Forest800,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New report",
                        )
                    }
                }
            },
            bottomBar = {
                if (showChrome && !useRail) {
                    CompactNavigationBar(
                        currentRoute = selectedTabRoute,
                        onTab = ::goTab,
                    )
                }
            },
        ) { padding ->
            MainNavHost(
                navController = navController,
                openNewReport = ::openNewReport,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun CompactNavigationBar(
    currentRoute: String?,
    onTab: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        MainTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTab(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Forest800,
                    selectedTextColor = Forest800,
                    indicatorColor = Mint400.copy(alpha = 0.22f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun AdaptiveNavigationRail(
    currentRoute: String?,
    onTab: (String) -> Unit,
    onNewReport: () -> Unit,
    showFab: Boolean,
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        header = {
            if (showFab) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    FloatingActionButton(
                        onClick = onNewReport,
                        shape = CircleShape,
                        containerColor = Forest800,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "New report")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
    ) {
        MainTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { onTab(tab.route) },
                icon = {
                    Icon(tab.icon, contentDescription = tab.label)
                },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Forest800,
                    selectedTextColor = Forest800,
                    indicatorColor = Mint400.copy(alpha = 0.22f),
                ),
            )
        }
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    openNewReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCapture = openNewReport,
                onOpenReport = { id -> navController.navigate(Routes.reportDetail(id)) },
            )
        }
        composable(Routes.REPORTS) {
            LibraryScreen(
                onOpenPhoto = { id -> navController.navigate(Routes.reportDetail(id)) },
                onOpenCapture = openNewReport,
            )
        }
        composable(Routes.CAPTURE) {
            UploadScreen(
                onBack = { navController.popBackStack() },
                onFinished = {
                    // Drop Capture (and anything above Home), land on Reports. No saveState.
                    navController.navigate(Routes.REPORTS) {
                        popUpTo(Routes.HOME) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen()
        }
        composable(
            route = Routes.REPORT_DETAIL,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("reportId").orEmpty()
            PhotoDetailScreen(
                photoId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
