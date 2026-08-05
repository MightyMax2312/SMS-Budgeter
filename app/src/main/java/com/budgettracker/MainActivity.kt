package com.budgettracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableLongStateOf
import com.budgettracker.ui.screens.HomeScreen
import com.budgettracker.ui.screens.ManualTransactionDialog
import com.budgettracker.ui.screens.MessagePopup
import com.budgettracker.ui.screens.OnboardingScreen
import com.budgettracker.ui.screens.PermissionScreen
import com.budgettracker.ui.screens.StatementCalendarDialog
import com.budgettracker.ui.theme.SMSBudgetTrackerTheme
import com.budgettracker.ui.viewmodel.MainViewModel
import com.budgettracker.ui.viewmodel.SlimTransaction

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) hasSmsPermission = true
    }

    private var hasSmsPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSmsPermission()

        setContent {
            SMSBudgetTrackerTheme {
                val vm: MainViewModel = viewModel()
                val uiState by vm.uiState.collectAsState()
                val isLoading by vm.isLoading.collectAsState()
                val onboardingDone by vm.isOnboardingCompleted.collectAsState()

                var showPopup by remember { mutableStateOf(false) }
                var popupTransaction by remember { mutableStateOf<SlimTransaction?>(null) }
                var showResyncPicker by remember { mutableStateOf(false) }
                var showManualDatePicker by remember { mutableStateOf(false) }
                var showManualEntryDialog by remember { mutableStateOf(false) }
                var manualEntryDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

                val filter by vm.filter.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !onboardingDone && hasSmsPermission -> {
                            OnboardingScreen(
                                onImportClick = { d -> vm.startBulkImport(d) },
                                isLoading = isLoading
                            )
                        }
                        onboardingDone -> {
                            LaunchedEffect(Unit) {
                                vm.syncWhenHomeScreenOpens()
                            }
                            HomeScreen(
                                uiState = uiState,
                                isLoading = isLoading,
                                currentFilter = filter,
                                onSyncClick = { vm.triggerManualSync() },
                                onTransactionClick = { tx ->
                                    popupTransaction = tx
                                    showPopup = true
                                },
                                onFilterChange = { vm.setFilter(it) },
                                onSavingsTargetChange = { vm.updateMonthlySavingsTarget(it) },
                                onDateChangeClick = { showResyncPicker = true },
                                onManualAddClick = {
                                    manualEntryDate = System.currentTimeMillis()
                                    showManualDatePicker = true
                                }
                            )
                            val selectedTransaction = popupTransaction
                            if (showPopup && selectedTransaction != null) {
                                MessagePopup(
                                    transactionId = selectedTransaction.id,
                                    source = selectedTransaction.source,
                                    bankName = selectedTransaction.bankName,
                                    accountLast4 = selectedTransaction.accountLast4,
                                    amount = selectedTransaction.amount,
                                    isCredit = selectedTransaction.isCredit,
                                    rawMessage = selectedTransaction.rawMessage,
                                    timestamp = selectedTransaction.timestamp,
                                    smsId = selectedTransaction.smsId,
                                    smsThreadId = selectedTransaction.smsThreadId,
                                    smsAddress = selectedTransaction.smsAddress,
                                    onDismiss = {
                                        showPopup = false
                                        popupTransaction = null
                                    },
                                    onDeleteManualEntry = {
                                        vm.deleteManualTransaction(selectedTransaction.id)
                                        showPopup = false
                                        popupTransaction = null
                                    }
                                )
                            }
                        }
                        else -> {
                            PermissionScreen(
                                onRequestPermission = { requestSmsPermission() },
                                onSkip = {}
                            )
                        }
                    }
                }

                if (showResyncPicker) {
                    StatementCalendarDialog(
                        title = "Resync from",
                        confirmText = "Resync",
                        initialDateMillis = System.currentTimeMillis(),
                        onDismiss = { showResyncPicker = false },
                        onConfirm = { millis ->
                            showResyncPicker = false
                            vm.resyncFromDate(millis)
                        },
                        onManualAddClick = { millis ->
                            showResyncPicker = false
                            manualEntryDate = millis
                            showManualEntryDialog = true
                        }
                    )
                }

                if (showManualEntryDialog) {
                    ManualTransactionDialog(
                        dateMillis = manualEntryDate,
                        onDismiss = { showManualEntryDialog = false },
                        onSave = { amount, transactionType ->
                            showManualEntryDialog = false
                            vm.addManualTransaction(
                                dateMillis = manualEntryDate,
                                amount = amount,
                                transactionType = transactionType
                            )
                        }
                    )
                }

                if (showManualDatePicker) {
                    StatementCalendarDialog(
                        title = "Choose date",
                        confirmText = "Continue",
                        initialDateMillis = manualEntryDate,
                        onDismiss = { showManualDatePicker = false },
                        onConfirm = { millis ->
                            showManualDatePicker = false
                            manualEntryDate = millis
                            showManualEntryDialog = true
                        }
                    )
                }
            }
        }
    }

    private fun checkSmsPermission() {
        hasSmsPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
    }
}
