package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import java.util.*

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isLoading: Boolean,
    currentFilter: TransactionFilter,
    onSyncClick: () -> Unit,
    onTransactionClick: (String, Long) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onDateChangeClick: () -> Unit
) {
    val palette = StatementPalette

    Scaffold(
        containerColor = palette.paper,
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = onDateChangeClick,
                    containerColor = palette.moss,
                    contentColor = palette.paper,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Change start date"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(palette.paper),
            contentPadding = PaddingValues(start = 22.dp, top = 26.dp, end = 22.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatementHeader(
                    isLoading = isLoading,
                    onSyncClick = onSyncClick
                )
            }

            if (isLoading && uiState.transactions.isEmpty()) {
                item {
                    LoadingStatement()
                }
            } else if (uiState.transactions.isNotEmpty()) {
                item {
                    StatementSummary(
                        uiState = uiState,
                        currentFilter = currentFilter,
                        onFilterChange = onFilterChange,
                        onDateChangeClick = onDateChangeClick
                    )
                }
                item {
                    StatementFilter(
                        currentFilter = currentFilter,
                        onFilterChange = onFilterChange
                    )
                }
                items(
                    items = uiState.transactions,
                    key = { tx -> "${tx.id}_${tx.timestamp}" }
                ) { tx ->
                    TransactionRow(
                        item = tx,
                        onClick = { onTransactionClick(tx.rawMessage, tx.timestamp) }
                    )
                }
            } else {
                item {
                    EmptyState(onSyncClick = onSyncClick)
                }
            }
        }
    }
}

private object StatementPalette {
    val paper = Color(0xFFC9CEB9)
    val paperDeep = Color(0xFFB8BEA8)
    val moss = Color(0xFF6F7B69)
    val mossDark = Color(0xFF536051)
    val ink = Color(0xFF18261E)
    val quietInk = Color(0xFF828978)
    val line = Color(0xFF7F8775)
    val clay = Color(0xFFE0704E)
    val cream = Color(0xFFE4E6D4)
}

@Composable
private fun StatementHeader(
    isLoading: Boolean,
    onSyncClick: () -> Unit
) {
    val palette = StatementPalette

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.mossDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SB",
                color = palette.cream,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onSyncClick,
            enabled = !isLoading,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.paperDeep)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = palette.ink,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Sync",
                    tint = palette.ink
                )
            }
        }
    }

    Spacer(Modifier.height(28.dp))

    Text(
        text = "Hello,",
        color = palette.quietInk,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Light
    )
    Text(
        text = "Here Your Statement\nfor Today",
        color = palette.ink,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun StatementSummary(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDateChangeClick: () -> Unit
) {
    val palette = StatementPalette
    val spent = uiState.totalDebits
    val available = uiState.balance
    val cash = uiState.totalCredits

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.line)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(bottomEnd = 4.dp))
                    .background(palette.moss)
                    .border(
                        width = 1.dp,
                        color = if (currentFilter == TransactionFilter.DEBIT) palette.clay else Color.Transparent,
                        shape = RoundedCornerShape(bottomEnd = 4.dp)
                    )
                    .clickable { onFilterChange(TransactionFilter.DEBIT) }
                    .padding(18.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "SPENT",
                    color = palette.paper.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatStatementCurrency(spent),
                    color = palette.cream,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            BarcodeBars(
                modifier = Modifier
                    .weight(0.85f)
                    .height(86.dp)
                    .padding(start = 8.dp, top = 6.dp),
                color = palette.line
            )
        }

        Spacer(Modifier.height(46.dp))

        Text(
            text = formatStatementCurrency(available),
            color = if (available >= 0) palette.ink else palette.clay,
            fontSize = 50.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier.clickable { onFilterChange(TransactionFilter.ALL) }
        )
        Text(
            text = "AVAILABLE",
            color = palette.ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onFilterChange(TransactionFilter.ALL) }
        )

        Spacer(Modifier.height(34.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(palette.moss)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .padding(22.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = if (currentFilter == TransactionFilter.CREDIT) palette.cream.copy(alpha = 0.75f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onFilterChange(TransactionFilter.CREDIT) }
                            .padding(6.dp)
                    ) {
                        Text(
                            text = formatStatementCurrency(cash),
                            color = palette.cream,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "CASH",
                            color = palette.cream.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = onDateChangeClick,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(palette.paperDeep.copy(alpha = 0.58f))
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Change start date",
                            tint = palette.cream
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    BarcodeBars(
                        modifier = Modifier
                            .width(112.dp)
                            .height(52.dp),
                        color = palette.cream.copy(alpha = 0.46f),
                        tallEvery = 3
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(palette.clay)
            )
        }
    }
}

@Composable
private fun BarcodeBars(
    modifier: Modifier = Modifier,
    color: Color,
    tallEvery: Int = 6
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Top
    ) {
        repeat(18) { index ->
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight(if (index % tallEvery == 0) 0.96f else 0.72f)
                    .background(color.copy(alpha = if (index % tallEvery == 0) 0.9f else 0.62f))
            )
        }
    }
}

@Composable
private fun LoadingStatement() {
    val palette = StatementPalette

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .border(1.dp, palette.line, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = palette.ink,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Syncing transactions...",
                color = palette.ink,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StatementFilter(
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit
) {
    val palette = StatementPalette
    val options = listOf(
        TransactionFilter.ALL to "All",
        TransactionFilter.CREDIT to "Credit",
        TransactionFilter.DEBIT to "Debit"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (filter, label) ->
            val selected = currentFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) palette.ink else Color.Transparent)
                    .clickable { onFilterChange(filter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(Locale.getDefault()),
                    color = if (selected) palette.cream else palette.ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(item: SlimTransaction, onClick: () -> Unit) {
    val palette = StatementPalette
    val arrow =
        if (item.isCredit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val arrowColor =
        if (item.isCredit) palette.mossDark else palette.clay

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.paperDeep)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.paper),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                arrow,
                contentDescription = null,
                tint = arrowColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.bankName.ifBlank { item.source.ifBlank { "Bank SMS" } },
                color = palette.ink,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (item.isCredit) "Credited" else "Debited"} / ${formatDateTime(item.timestamp)} / ${item.accountLast4}",
                color = palette.quietInk,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = formatStatementCurrency(item.amount),
            color = arrowColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyState(onSyncClick: () -> Unit) {
    val palette = StatementPalette

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .padding(26.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No statement yet",
            color = palette.ink,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Sync SMS messages to build your transaction view.",
            color = palette.quietInk,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSyncClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.ink,
                contentColor = palette.cream
            )
        ) {
            Text("Sync SMS Messages")
        }
    }
}

private fun formatStatementCurrency(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 2
    }
    return "Rs" + format.format(amount)
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}

private fun formatDateTime(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM, h:mm:ss a", Locale.getDefault())
    return sdf.format(Date(ts))
}
