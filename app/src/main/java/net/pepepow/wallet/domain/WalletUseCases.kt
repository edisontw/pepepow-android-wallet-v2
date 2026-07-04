package net.pepepow.wallet.domain

import net.pepepow.wallet.data.Transaction
import net.pepepow.wallet.data.WalletRepository

class WalletUseCases(private val repository: WalletRepository) {
    
    fun calculateTotalSent(transactions: List<Transaction>): Double {
        return transactions.filter { it.isSend && !it.isPending }.sumOf { it.amount }
    }

    fun calculateTotalReceived(transactions: List<Transaction>): Double {
        return transactions.filter { !it.isSend && !it.isPending }.sumOf { it.amount }
    }

    fun isValidAddress(address: String): Boolean {
        return address.startsWith("P") && address.length >= 20
    }
}
