package com.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgettracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE bankName = :bankName ORDER BY timestamp DESC")
    fun getTransactionsByBank(bankName: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTransaction(): TransactionEntity?

    @Query("SELECT MAX(timestamp) FROM transactions")
    suspend fun getLatestTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = :type")
    suspend fun getTotalByType(type: String): Double?
}

