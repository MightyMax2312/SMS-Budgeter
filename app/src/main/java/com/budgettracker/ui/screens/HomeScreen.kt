package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.ui.viewmodel.HomeUiState
import com.budgettracker.ui.viewmodel.SlimTransaction
import com.budgettracker.ui.viewmodel.TransactionFilter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isLoading: Boolean,
    currentFilter: TransactionFilter,
    onSyncClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Budget Tracker") },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onSyncClick) {
                            Icon(Icons.Default.Refresh, "Sync",
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.transactions.isNotEmpty()) {
                SummaryCards(uiState)
                Spacer(Modifier.height(4.dp))
                FilterDropdown(
                    currentFilter = currentFilter,
                    onFilterChange = onFilterChange
                )
                Spacer(Modifier.height(8.dp))
                TransactionList(transactions = uiState.transactions) { msg ->
                    onTransactionClick(msg)
                }
            } else {
                emptyState(onSyncClick)
            }
        }
    }
}

@Composable
private fun TransactionList(
    transactions: List<SlimTransaction>,
    onTransactionClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(transactions, key = { "${it.id}_${it.timestamp}" }) { tx ->
            TransactionRow(item = tx, onClick = { onTransactionClick(tx.rawMessage) })
        }
    }
}

@Composable
private fun SummaryCards(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Balance", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text("₹${String.format("%.2f", uiState.balance)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (uiState.balance >= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Income", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("+₹${String.format("%.2f", uiState.totalCredits)}",
                        style = MaterialTheme.typography.titleMedium, color = Color(0xFF2D6A4F))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expense", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("-₹${String.format("%.2f", uiState.totalDebits)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(item: SlimTransaction, onClick: () -> Unit) {
    val arrow = if (item.isCredit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val arrowColor = if (item.isCredit) Color(0xFF2D6A4F) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(arrow, contentDescription = null,
                    tint = arrowColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.bankName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium)
                    Text(
                        "${if (item.isCredit) "Credited" else "Debited"} • ${formatDate(item.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${String.format("%.2f", item.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = arrowColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun emptyState(onSyncClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No transactions yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSyncClick) {
            Text("Sync SMS Messages")
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}