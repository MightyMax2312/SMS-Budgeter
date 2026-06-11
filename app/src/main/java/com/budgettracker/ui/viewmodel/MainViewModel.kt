package com.budgettracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.budgettracker.data.datastore.SyncPreferences
import com.budgettracker.data.local.AppDatabase
import com.budgettracker.data.repository.TransactionRepository
import com.budgettracker.domain.model.Transaction
import com.budgettracker.domain.model.TransactionType
import com.budgettracker.worker.BulkImportWorker
import com.budgettracker.worker.SmsSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class TransactionFilter { ALL, CREDIT, DEBIT }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val syncPrefs = SyncPreferences(application)
    private val db = AppDatabase.getInstanceWithoutEncryption(application)
    private val repository = TransactionRepository(db.transactionDao())
    private val workManager = WorkManager.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val repoTransactions = repository.getAllTransactions()

    val transactions = repoTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnboardingCompleted = syncPrefs.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    val uiState: StateFlow<HomeUiState> = combine(repoTransactions, _filter) { txs: List<Transaction>, filter: TransactionFilter ->
        val filtered = when (filter) {
            TransactionFilter.CREDIT -> txs.filter { it.transactionType == TransactionType.CREDIT }
            TransactionFilter.DEBIT -> txs.filter { it.transactionType == TransactionType.DEBIT }
            TransactionFilter.ALL -> txs
        }
        val creds = txs.filter { it.transactionType == TransactionType.CREDIT }
        val debits = txs.filter { it.transactionType == TransactionType.DEBIT }
        HomeUiState(
            transactions = filtered.map { tx ->
                SlimTransaction(
                    id = tx.id,
                    bankName = tx.bankName,
                    accountLast4 = tx.accountLast4,
                    amount = tx.amount,
                    currency = tx.currency,
                    isCredit = tx.transactionType == TransactionType.CREDIT,
                    timestamp = tx.timestamp,
                    rawMessage = tx.rawMessage,
                    source = tx.source
                )
            },
            totalCredits = creds.sumOf { it.amount },
            totalDebits = debits.sumOf { it.amount },
            balance = creds.sumOf { it.amount } - debits.sumOf { it.amount },
            transactionCount = filtered.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        schedulePeriodicSync()
    }

    fun startBulkImport(startTimeMillis: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = OneTimeWorkRequestBuilder<BulkImportWorker>()
                    .setInputData(workDataOf("start_time" to startTimeMillis)).build()
                workManager.enqueueUniqueWork(
                    BulkImportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
                workManager.getWorkInfoByIdFlow(req.id).collect { info ->
                    if (info.state.isFinished) {
                        _isLoading.value = false
                        if (info.state == WorkInfo.State.SUCCEEDED) syncPrefs.setOnboardingCompleted(true)
                    }
                }
            } catch (e: Exception) { _isLoading.value = false }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
                workManager.enqueueUniqueWork(
                    SmsSyncWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
                workManager.getWorkInfoByIdFlow(req.id).collect { info ->
                    if (info.state.isFinished) _isLoading.value = false
                }
            } catch (e: Exception) { _isLoading.value = false }
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun resyncFromDate(startTimeMillis: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Step 1: Delete all existing transactions synchronously
                repository.deleteAll()

                // Step 2: Enqueue worker to import from the selected date
                val req = OneTimeWorkRequestBuilder<BulkImportWorker>()
                    .setInputData(workDataOf("start_time" to startTimeMillis)).build()
                workManager.enqueueUniqueWork(
                    BulkImportWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
                workManager.getWorkInfoByIdFlow(req.id).collect { info ->
                    if (info.state.isFinished) {
                        _isLoading.value = false
                        if (info.state == WorkInfo.State.SUCCEEDED) {
                            syncPrefs.setOnboardingCompleted(true)
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val work = PeriodicWorkRequestBuilder<SmsSyncWorker>(
            60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(
            SmsSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work
        )
    }
}

data class SlimTransaction(
    val id: Long = 0,
    val source: String = "",
    val bankName: String = "",
    val accountLast4: String = "0000",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val isCredit: Boolean = true,
    val timestamp: Long = 0,
    val rawMessage: String = ""
)

data class HomeUiState(
    val transactions: List<SlimTransaction> = emptyList(),
    val totalCredits: Double = 0.0,
    val totalDebits: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0
)
