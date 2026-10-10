package com.hybridmode.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hybridmode.app.ui.auth.LoginScreen
import com.hybridmode.app.ui.auth.RegisterScreen
import com.hybridmode.app.ui.main.MainShell
import com.hybridmode.app.ui.navigation.Routes
import com.hybridmode.app.ui.session.SessionViewModel
import com.hybridmode.app.ui.welcome.WelcomeScreen

@Composable
fun HybridModeRoot(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    if (sessionState.isBootstrapping) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (sessionState.session != null) Routes.MAIN else Routes.WELCOME

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onSignIn = { navController.navigate(Routes.LOGIN) },
                onCreateAccount = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateBack = { navController.popBackStack() },
                onLoggedIn = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateLogin = {
                    if (!navController.popBackStack(Routes.LOGIN, inclusive = false)) {
                        navController.navigate(Routes.LOGIN)
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainShell()
        }
    }

    LaunchedEffect(sessionState.session) {
        val route = navController.currentBackStackEntry?.destination?.route
        val onProtected = route == Routes.MAIN
        val onAuth = route in setOf(Routes.WELCOME, Routes.LOGIN, Routes.REGISTER)

        when {
            sessionState.session == null && onProtected -> {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(0) { inclusive = true }
                }
            }
            sessionState.session != null && onAuth -> {
                navController.navigate(Routes.MAIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}
