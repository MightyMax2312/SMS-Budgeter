package com.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.budgettracker.domain.model.Transaction
import com.budgettracker.domain.model.TransactionType
import com.budgettracker.data.local.entity.TransactionEntity


@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,
    val bankName: String,
    val accountLast4: String,
    val amount: Double,
    val currency: String,
    val transactionType: String,
    val timestamp: Long,
    val rawMessage: String,
    val recipientName: String?,
    val category: String?
){
    fun toDomain(): Transaction{
        return Transaction(
            id = id,
            source = source,
            bankName = bankName,
            accountLast4 = accountLast4,
            amount = amount,
            currency = currency,
            transactionType = TransactionType.valueOf(transactionType),
            timestamp = timestamp,
            rawMessage = rawMessage,
            recipientName = recipientName,
            category = category

        )
    }
    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity{
            return TransactionEntity(
                id = transaction.id,
                source = transaction.source,
                bankName = transaction.bankName,
                accountLast4 = transaction.accountLast4,
                amount = transaction.amount,
                currency = transaction.currency,
                transactionType = transaction.transactionType.name,
                timestamp = transaction.timestamp,
                rawMessage = transaction.rawMessage,
                recipientName = transaction.recipientName,
                category = transaction.category
            )
        }
    }


}