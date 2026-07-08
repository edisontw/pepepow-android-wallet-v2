package net.pepepow.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import net.pepepow.wallet.data.FakeWalletRepository
import net.pepepow.wallet.data.RealWalletRepository
import net.pepepow.wallet.navigation.WalletNavGraph
import net.pepepow.wallet.viewmodel.*
import net.pepepow.wallet.ui.theme.PepepowWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = RealWalletRepository(applicationContext)

        setContent {
            PepepowWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val walletViewModel: WalletViewModel = viewModel { WalletViewModel(repository) }
                    val sendViewModel: SendViewModel = viewModel { SendViewModel(repository) }
                    val historyViewModel: HistoryViewModel = viewModel { HistoryViewModel(repository) }
                    val apiStatusViewModel: ApiStatusViewModel = viewModel { ApiStatusViewModel(repository) }
                    val consolidationViewModel: ConsolidationViewModel = viewModel { ConsolidationViewModel(repository) }

                    val navController = rememberNavController()

                    WalletNavGraph(
                        navController = navController,
                        walletViewModel = walletViewModel,
                        sendViewModel = sendViewModel,
                        historyViewModel = historyViewModel,
                        apiStatusViewModel = apiStatusViewModel,
                        consolidationViewModel = consolidationViewModel
                    )
                }
            }
        }
    }
}
