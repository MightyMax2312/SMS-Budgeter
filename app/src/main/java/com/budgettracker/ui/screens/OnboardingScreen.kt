package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.budgettracker.ui.viewmodel.TransactionFilter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OnboardingScreen(
    onImportClick: (Long) -> Unit,
    isLoading: Boolean
) {
    var selectedDateMillis by remember { mutableLongStateOf(calculateDefaultStartDate()) }
    val datePickerDialog = remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Text("Welcome to", fontSize = 22.sp, fontWeight = FontWeight(400),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("SMS Budget Tracker", fontSize = 28.sp, fontWeight = FontWeight(700),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))

            Text("Automatically track your bank transactions from SMS messages.",
                fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.weight(1f, fill = false))

            Text("When should we start importing?", fontSize = 18.sp, fontWeight = FontWeight(600))

            Spacer(Modifier.height(16.dp))

            DateSelectionCard(
                selectedDate = dateFormatter.format(Date(selectedDateMillis)),
                onClick = { datePickerDialog.value = true }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onImportClick(selectedDateMillis) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Text("Start Import")
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            Text("Your data stays on your device.",
                fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (datePickerDialog.value) {
        StatementCalendarDialog(
            title = "Start date",
            confirmText = "Use date",
            initialDateMillis = selectedDateMillis,
            onDismiss = { datePickerDialog.value = false },
            onConfirm = { millis ->
                selectedDateMillis = millis
                datePickerDialog.value = false
            }
        )
    }
}

private fun calculateDefaultStartDate(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
private fun DateSelectionCard(selectedDate: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Start Date", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun FilterDropdown(
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        TransactionFilter.ALL to "All Transactions",
        TransactionFilter.CREDIT to "Credit Only",
        TransactionFilter.DEBIT to "Debit Only"
    )

    val selectedLabel = options.first { it.first == currentFilter }.second

    Card(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Show:", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { (filter, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onFilterChange(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}
