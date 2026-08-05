package com.budgettracker.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.budgettracker.data.datastore.SyncPreferences
import com.budgettracker.data.local.AppDatabase
import com.budgettracker.data.repository.TransactionRepository
import com.budgettracker.domain.model.BankFilter
import com.budgettracker.domain.model.Transaction
import com.budgettracker.domain.model.TransactionType
import com.budgettracker.worker.BulkImportWorker
import com.budgettracker.worker.SmsSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class TransactionFilter { ALL, CREDIT, DEBIT }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val syncPrefs = SyncPreferences(application)
    private val db = AppDatabase.getInstanceWithoutEncryption(application)
    private val repository = TransactionRepository(db.transactionDao())
    private val workManager = WorkManager.getInstance(application)
    private var foregroundPollJob: Job? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val repoTransactions = repository.getAllTransactions()

    val transactions = repoTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnboardingCompleted = syncPrefs.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank: StateFlow<String?> = _selectedBank.asStateFlow()

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun setBank(bank: String?) {
        _selectedBank.value = bank
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repoTransactions,
        _filter,
        _selectedBank,
        syncPrefs.monthlySavingsTarget,
        syncPrefs.selectedStartDate
    ) { txs: List<Transaction>, filter: TransactionFilter, selectedBank: String?, savingsTarget: Double, selectedStartDate: Long ->
        // Banks encountered in the data, most-active first
        val banks = BankFilter.deriveBanks(txs)
        val activeBank = selectedBank?.takeIf { it in banks }
        val bankFiltered = BankFilter.applyBankFilter(txs, activeBank)
        val filtered = when (filter) {
            TransactionFilter.CREDIT -> bankFiltered.filter { it.transactionType == TransactionType.CREDIT }
            TransactionFilter.DEBIT -> bankFiltered.filter { it.transactionType == TransactionType.DEBIT }
            TransactionFilter.ALL -> bankFiltered
        }
        val creds = txs.filter { it.transactionType == TransactionType.CREDIT }
        val debits = txs.filter { it.transactionType == TransactionType.DEBIT }
        val todayStart = startOfTodayMillis()
        val monthStart = startOfMonthMillis()
        val todayTxs = txs.filter { it.timestamp >= todayStart }
        val monthTxs = txs.filter { it.timestamp >= monthStart }
        val todayCredits = todayTxs
            .filter { it.transactionType == TransactionType.CREDIT }
            .sumOf { it.amount }
        val todayDebits = todayTxs
            .filter { it.transactionType == TransactionType.DEBIT }
            .sumOf { it.amount }
        val monthCredits = monthTxs
            .filter { it.transactionType == TransactionType.CREDIT }
            .sumOf { it.amount }
        val monthDebits = monthTxs
            .filter { it.transactionType == TransactionType.DEBIT }
            .sumOf { it.amount }
        val monthlySalary = monthTxs
            .filter { it.transactionType == TransactionType.CREDIT && isSalaryTransaction(it) }
            .sumOf { it.amount }
        val monthlySpendBudget = (monthlySalary - savingsTarget).coerceAtLeast(0.0)
        val spentBeforeToday = monthTxs
            .filter { it.transactionType == TransactionType.DEBIT && it.timestamp < todayStart }
            .sumOf { it.amount }
        val daysElapsed = daysElapsedInMonth()
        val daysRemaining = daysRemainingInMonthIncludingToday()
        val averageDailyMonthSpending = if (daysElapsed > 0) monthDebits / daysElapsed else 0.0
        val dailyAllowed = if (monthlySpendBudget > 0.0 && daysRemaining > 0) {
            ((monthlySpendBudget - spentBeforeToday).coerceAtLeast(0.0) / daysRemaining)
        } else {
            0.0
        }
        val lastSevenDaysDebits = buildLastSevenDaysDebits(monthTxs)
        val allowanceProgress = when {
            dailyAllowed <= 0.0 && todayDebits > 0.0 -> 1.0
            dailyAllowed <= 0.0 -> 0.0
            else -> (todayDebits / dailyAllowed).coerceIn(0.0, 1.0)
        }
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
                    source = tx.source,
                    smsId = tx.smsId,
                    smsThreadId = tx.smsThreadId,
                    smsAddress = tx.smsAddress,
                    smsDate = tx.smsDate
                )
            },
            totalCredits = creds.sumOf { it.amount },
            totalDebits = debits.sumOf { it.amount },
            balance = creds.sumOf { it.amount } - debits.sumOf { it.amount },
            transactionCount = filtered.size,
            totalTransactionCount = txs.size,
            todayCredits = todayCredits,
            todayDebits = todayDebits,
            todayBalance = todayCredits - todayDebits,
            todayCount = todayTxs.size,
            monthCredits = monthCredits,
            monthDebits = monthDebits,
            monthBalance = monthCredits - monthDebits,
            monthCount = monthTxs.size,
            monthlySavingsTarget = savingsTarget,
            monthlySalary = monthlySalary,
            monthlySpendBudget = monthlySpendBudget,
            spentBeforeToday = spentBeforeToday,
            dailyAllowedSpending = dailyAllowed,
            dailyAllowanceProgress = allowanceProgress,
            averageDailyMonthSpending = averageDailyMonthSpending,
            lastSevenDaysDebits = lastSevenDaysDebits,
            daysElapsedInMonth = daysElapsed,
            daysRemainingInMonth = daysRemaining,
            selectedStartDate = selectedStartDate,
            banks = banks,
            selectedBank = activeBank
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        schedulePeriodicSync()
    }

    fun startBulkImport(startTimeMillis: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncPrefs.setSelectedStartDate(startTimeMillis)
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
        enqueueSmsSync(showLoading = true)
    }

    /**
     * While the app is in the foreground, re-read SMS every 30 seconds.
     * An immediate sync runs first, then ticks at the 30s, 60s, ... marks.
     */
    fun startForegroundPolling() {
        foregroundPollJob?.cancel()
        foregroundPollJob = viewModelScope.launch {
            while (isActive) {
                if (!isOneTimeSyncInFlight()) {
                    enqueueSmsSync(showLoading = false)
                }
                delay(30_000)
            }
        }
    }

    fun stopForegroundPolling() {
        foregroundPollJob?.cancel()
        foregroundPollJob = null
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun updateMonthlySavingsTarget(amount: Double) {
        viewModelScope.launch {
            syncPrefs.setMonthlySavingsTarget(amount)
        }
    }

    fun addManualTransaction(
        dateMillis: Long,
        amount: Double,
        transactionType: TransactionType
    ) {
        viewModelScope.launch {
            val timestamp = mergeDateWithCurrentTime(dateMillis)
            val transaction = Transaction(
                source = "MANUAL",
                bankName = "Manual Entry",
                accountLast4 = "SELF",
                amount = amount,
                currency = "INR",
                transactionType = transactionType,
                timestamp = timestamp,
                rawMessage = buildManualMessage(amount, transactionType, timestamp),
                recipientName = null,
                category = "MANUAL",
                smsId = null,
                smsThreadId = null,
                smsAddress = null,
                smsDate = null,
                transactionFingerprint = buildManualFingerprint(amount, transactionType, timestamp)
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteManualTransaction(transactionId: Long) {
        if (transactionId <= 0L) return
        viewModelScope.launch {
            repository.deleteTransactionById(transactionId)
        }
    }

    fun resyncFromDate(startTimeMillis: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Step 1: Delete all existing transactions synchronously
                syncPrefs.setSelectedStartDate(startTimeMillis)
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

    private fun enqueueSmsSync(showLoading: Boolean) {
        if (!hasSmsPermission()) {
            if (showLoading) _isLoading.value = false
            return
        }
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            try {
                val req = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
                workManager.enqueueUniqueWork(
                    SmsSyncWorker.ONETIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
                if (showLoading) {
                    workManager.getWorkInfoByIdFlow(req.id).collect { info ->
                        if (info.state.isFinished) _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    /** True when a one-time SMS sync is still pending or running (REPLACE dedup + skip guard). */
    private suspend fun isOneTimeSyncInFlight(): Boolean {
        val infos = workManager.getWorkInfosForUniqueWorkFlow(SmsSyncWorker.ONETIME_WORK_NAME).first()
        return infos.any { !it.state.isFinished }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfMonthMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun daysRemainingInMonthIncludingToday(): Int {
        return Calendar.getInstance().let { calendar ->
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) + 1
        }
    }

    private fun daysElapsedInMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }

    private fun isSalaryTransaction(transaction: Transaction): Boolean {
        if (transaction.category.equals("SALARY", ignoreCase = true)) return true

        val lower = transaction.rawMessage.lowercase()
        val salaryKeywords = listOf(
            "salary",
            "payroll",
            "monthly pay",
            "wages",
            "stipend",
            "remuneration",
            "employee salary",
            "salary credit",
            "sal cr",
            "sal credited"
        )
        return salaryKeywords.any { lower.contains(it) }
    }

    private fun mergeDateWithCurrentTime(dateMillis: Long): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            set(Calendar.SECOND, now.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
        }.timeInMillis
    }

    private fun buildManualMessage(
        amount: Double,
        transactionType: TransactionType,
        timestamp: Long
    ): String {
        val direction = if (transactionType == TransactionType.CREDIT) "credited" else "debited"
        val stamp = java.text.SimpleDateFormat("dd MMM yyyy, h:mm:ss a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return "Manual transaction: Rs${String.format(java.util.Locale.US, "%.2f", amount)} $direction on $stamp."
    }

    private fun buildManualFingerprint(
        amount: Double,
        transactionType: TransactionType,
        timestamp: Long
    ): String {
        return listOf(
            "manual",
            System.currentTimeMillis(),
            timestamp,
            transactionType.name,
            String.format(java.util.Locale.US, "%.2f", amount)
        ).joinToString("|")
    }

    private fun buildLastSevenDaysDebits(monthTransactions: List<Transaction>): List<DailySpendBar> {
        val todayStart = startOfTodayMillis()
        val byDay = monthTransactions
            .asSequence()
            .filter { it.transactionType == TransactionType.DEBIT }
            .groupBy { startOfDayMillis(it.timestamp) }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        return (6 downTo 0).map { daysBack ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_MONTH, -daysBack)
            }.timeInMillis
            DailySpendBar(
                label = if (daysBack == 0) {
                    "Today"
                } else {
                    Calendar.getInstance()
                        .apply { timeInMillis = dayStart }
                        .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                        ?: ""
                },
                amount = byDay[dayStart] ?: 0.0
            )
        }
    }

    private fun startOfDayMillis(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
    val rawMessage: String = "",
    val smsId: Long? = null,
    val smsThreadId: Long? = null,
    val smsAddress: String? = null,
    val smsDate: Long? = null
)

data class HomeUiState(
    val transactions: List<SlimTransaction> = emptyList(),
    val totalCredits: Double = 0.0,
    val totalDebits: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0,
    val totalTransactionCount: Int = 0,
    val todayCredits: Double = 0.0,
    val todayDebits: Double = 0.0,
    val todayBalance: Double = 0.0,
    val todayCount: Int = 0,
    val monthCredits: Double = 0.0,
    val monthDebits: Double = 0.0,
    val monthBalance: Double = 0.0,
    val monthCount: Int = 0,
    val monthlySavingsTarget: Double = 0.0,
    val monthlySalary: Double = 0.0,
    val monthlySpendBudget: Double = 0.0,
    val spentBeforeToday: Double = 0.0,
    val dailyAllowedSpending: Double = 0.0,
    val dailyAllowanceProgress: Double = 0.0,
    val averageDailyMonthSpending: Double = 0.0,
    val lastSevenDaysDebits: List<DailySpendBar> = emptyList(),
    val daysElapsedInMonth: Int = 0,
    val daysRemainingInMonth: Int = 0,
    val selectedStartDate: Long = 0L,
    val banks: List<String> = emptyList(),
    val selectedBank: String? = null
)

data class DailySpendBar(
    val label: String,
    val amount: Double
)
