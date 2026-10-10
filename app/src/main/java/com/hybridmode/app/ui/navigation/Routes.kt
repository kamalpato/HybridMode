package com.hybridmode.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val HOME = "home"
    const val REPORTS = "reports"
    const val CAPTURE = "capture"
    const val PROFILE = "profile"
    const val REPORT_DETAIL = "report/{reportId}"

    fun reportDetail(reportId: String) = "report/$reportId"
}

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(Routes.HOME, "Home", Icons.Outlined.Home),
    Reports(Routes.REPORTS, "Reports", Icons.Outlined.Description),
    Profile(Routes.PROFILE, "Profile", Icons.Outlined.Person),
}
