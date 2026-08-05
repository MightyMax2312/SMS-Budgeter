package com.budgettracker.domain.model

/**
 * Pure helpers for the bank ("section") filter.
 * Kept free of Android dependencies so they can be unit-tested.
 */
object BankFilter {

    /**
     * Distinct banks encountered in the data, most-transactions first.
     * Blank bank names (e.g. unparsed entries) are excluded.
     */
    fun deriveBanks(transactions: List<Transaction>): List<String> =
        transactions.groupingBy { it.bankName }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
            .filter { it.isNotBlank() }

    /**
     * Whether [typed] matches a bank that has real transactions in the data.
     * Comparison is case-insensitive; blank input is never valid.
     */
    fun validateBank(banks: List<String>, typed: String): Boolean {
        val query = typed.trim()
        if (query.isEmpty()) return false
        return banks.any { it.equals(query, ignoreCase = true) }
    }

    /**
     * The transactions for [bank], or all transactions when [bank] is null.
     */
    fun applyBankFilter(transactions: List<Transaction>, bank: String?): List<Transaction> =
        if (bank != null) transactions.filter { it.bankName == bank } else transactions
}
