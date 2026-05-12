package com.budgettracker.domain.model

data class SyncResult(
    val newTransactionsCount: Int,
    val errorsCount: Int,
    val lastSyncTimestamp: Long
)