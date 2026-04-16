package com.offlineai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.offlineai.ui.screens.ChatScreen
import com.offlineai.ui.screens.SettingsScreen
import com.offlineai.viewmodel.ChatViewModel

/** Route identifiers */
object Routes {
    const val CHAT     = "chat"
    const val SETTINGS = "settings"
}

/**
 * Defines the app's navigation graph.
 * NavHost renders the current destination and handles back-stack management.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: ChatViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT
    ) {
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel   = viewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
