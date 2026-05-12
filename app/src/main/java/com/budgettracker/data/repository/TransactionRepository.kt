package com.budgettracker.data.repository

import com.budgettracker.data.local.dao.TransactionDao
import com.budgettracker.data.local.entity.TransactionEntity
import com.budgettracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getTransactionsByBank(bankName: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByBank(bankName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction> {
        return transactionDao.getTransactionsInRange(startTime, endTime).map { it.toDomain() }
    }

    suspend fun getLastTransaction(): Transaction? {
        return transactionDao.getLastTransaction()?.toDomain()
    }

    suspend fun getLatestTimestamp(): Long? {
        return transactionDao.getLatestTimestamp()
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { TransactionEntity.fromDomain(it) })
    }

    suspend fun deleteAll() {
        transactionDao.deleteAll()
    }

    suspend fun getCount(): Int {
        return transactionDao.getCount()
    }

    suspend fun getTotalCredits(): Double {
        return transactionDao.getTotalByType("CREDIT") ?: 0.0
    }

    suspend fun getTotalDebits(): Double {
        return transactionDao.getTotalByType("DEBIT") ?: 0.0
    }
}