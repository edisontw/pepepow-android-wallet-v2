package net.pepepow.wallet.navigation

sealed class WalletRoutes(val route: String) {
    object Welcome : WalletRoutes("welcome")
    object CreateWallet : WalletRoutes("create_wallet")
    object BackupWarning : WalletRoutes("backup_warning")
    object RestoreWallet : WalletRoutes("restore_wallet")
    object Dashboard : WalletRoutes("dashboard")
    object Send : WalletRoutes("send")
    object Receive : WalletRoutes("receive")
    object History : WalletRoutes("history")
    object Settings : WalletRoutes("settings")
    object ApiStatus : WalletRoutes("api_status")
    object Consolidate : WalletRoutes("consolidate")
}
