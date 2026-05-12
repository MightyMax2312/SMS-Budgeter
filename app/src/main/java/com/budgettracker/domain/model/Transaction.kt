package com.budgettracker.domain.model

data class Transaction(
    val id: Long = 0,
    val source: String,             //SMS, GMAIL, MANUAL, etc.
    val bankName: String,
    val accountLast4: String,       //Account Number
    val amount: Double,
    val currency: String,
    val transactionType: TransactionType,  //DEBIT or CREDIT
    val timestamp: Long,
    val rawMessage: String,
    val recipientName: String? = null,
    val category: String? = null
)