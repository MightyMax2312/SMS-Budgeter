package com.budgettracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgettracker.data.datastore.SyncPreferences
import com.budgettracker.data.datastore.dataStore
import com.budgettracker.data.local.AppDatabase
import com.budgettracker.data.local.entity.TransactionEntity
import com.budgettracker.data.repository.SmsRepository
import com.budgettracker.domain.usecase.BankMessageParser
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SmsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncPrefs = SyncPreferences(context)
    private val smsRepository = SmsRepository(context.contentResolver)
    private val parser = BankMessageParser()
    private val db = AppDatabase.getInstanceWithoutEncryption(context)

    override suspend fun doWork(): Result {
        return try {
            val lastSyncTime = syncPrefs.lastSmsSyncTime.first()
            val currentTime = System.currentTimeMillis()

            val messages = smsRepository.getSmsMessages(lastSyncTime, currentTime)
            val transactions = parser.parseMultiple(messages)

            if (transactions.isNotEmpty()) {
                val entities = transactions.map { tx ->
                    TransactionEntity.fromDomain(tx)
                }
                db.transactionDao().insertTransactions(entities)
            }

            syncPrefs.updateLastSmsSync(currentTime)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "sms_sync_worker"
    }
}

class BulkImportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncPrefs = SyncPreferences(context)
    private val smsRepository = SmsRepository(context.contentResolver)
    private val parser = BankMessageParser()
    private val db = AppDatabase.getInstanceWithoutEncryption(context)

    override suspend fun doWork(): Result {
        return try {
            val inputDays = inputData.getLong("import_days", 30L)
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(inputDays)

            val messages = smsRepository.getSmsMessages(startTime, endTime)
            val transactions = parser.parseMultiple(messages)

            if (transactions.isNotEmpty()) {
                val entities = transactions.map { tx ->
                    TransactionEntity.fromDomain(tx)
                }
                db.transactionDao().insertTransactions(entities)
            }

            syncPrefs.setOnboardingCompleted(true)
            syncPrefs.updateLastSmsSync(endTime)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "bulk_import_worker"
    }
}