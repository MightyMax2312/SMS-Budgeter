package com.budgettracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgettracker.ui.screens.HomeScreen
import com.budgettracker.ui.screens.MessagePopup
import com.budgettracker.ui.screens.OnboardingScreen
import com.budgettracker.ui.screens.PermissionScreen
import com.budgettracker.ui.theme.SMSBudgetTrackerTheme
import com.budgettracker.ui.viewmodel.MainViewModel

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
                var popupMsg by remember { mutableStateOf("") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !onboardingDone && hasSmsPermission -> {
                            OnboardingScreen(
                                onImportClick = { d -> vm.startBulkImport(d) },
                                isLoading = isLoading
                            )
                        }
                        onboardingDone -> {
                            HomeScreen(
                                uiState = uiState,
                                isLoading = isLoading,
                                onSyncClick = { vm.triggerManualSync() },
                                onTransactionClick = { msg ->
                                    popupMsg = msg
                                    showPopup = true
                                }
                            )
                            if (showPopup) {
                                MessagePopup(popupMsg) { showPopup = false }
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
            }
        }
    }

    private fun checkSmsPermission() {
        hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
    }
}