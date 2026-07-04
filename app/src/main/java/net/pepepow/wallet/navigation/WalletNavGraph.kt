package net.pepepow.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.pepepow.wallet.viewmodel.*
import net.pepepow.wallet.ui.screens.*

@Composable
fun WalletNavGraph(
    navController: NavHostController,
    walletViewModel: WalletViewModel,
    sendViewModel: SendViewModel,
    historyViewModel: HistoryViewModel,
    apiStatusViewModel: ApiStatusViewModel
) {
    val isWalletCreated by walletViewModel.isWalletCreated.collectAsState()
    val startDestination = if (isWalletCreated) WalletRoutes.Dashboard.route else WalletRoutes.Welcome.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(WalletRoutes.Welcome.route) {
            WelcomeScreen(onNavigateToCreate = {
                walletViewModel.startCreateWallet()
                navController.navigate(WalletRoutes.CreateWallet.route)
            })
        }
        composable(WalletRoutes.CreateWallet.route) {
            CreateWalletScreen(viewModel = walletViewModel, onNavigateToBackupWarning = {
                navController.navigate(WalletRoutes.BackupWarning.route)
            })
        }
        composable(WalletRoutes.BackupWarning.route) {
            BackupWarningScreen(viewModel = walletViewModel, onNavigateToDashboard = {
                walletViewModel.confirmBackup()
                navController.navigate(WalletRoutes.Dashboard.route) {
                    popUpTo(WalletRoutes.Welcome.route) { inclusive = true }
                }
            })
        }
        composable(WalletRoutes.Dashboard.route) {
            DashboardScreen(
                walletViewModel = walletViewModel,
                historyViewModel = historyViewModel,
                apiStatusViewModel = apiStatusViewModel,
                onNavigateToSend = { navController.navigate(WalletRoutes.Send.route) },
                onNavigateToReceive = { navController.navigate(WalletRoutes.Receive.route) },
                onNavigateToHistory = { navController.navigate(WalletRoutes.History.route) },
                onNavigateToSettings = { navController.navigate(WalletRoutes.Settings.route) },
                onNavigateToApiStatus = { navController.navigate(WalletRoutes.ApiStatus.route) }
            )
        }
        composable(WalletRoutes.Send.route) {
            SendScreen(
                walletViewModel = walletViewModel,
                sendViewModel = sendViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(WalletRoutes.Receive.route) {
            ReceiveScreen(
                walletViewModel = walletViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(WalletRoutes.History.route) {
            HistoryScreen(
                historyViewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(WalletRoutes.Settings.route) {
            SettingsScreen(
                walletViewModel = walletViewModel,
                onNavigateBack = { navController.popBackStack() },
                onResetWallet = {
                    walletViewModel.clearWallet()
                    navController.navigate(WalletRoutes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(WalletRoutes.ApiStatus.route) {
            ApiStatusScreen(
                walletViewModel = walletViewModel,
                apiStatusViewModel = apiStatusViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
