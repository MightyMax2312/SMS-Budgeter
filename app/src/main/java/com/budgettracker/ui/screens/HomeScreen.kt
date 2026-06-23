package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isLoading: Boolean,
    currentFilter: TransactionFilter,
    onSyncClick: () -> Unit,
    onTransactionClick: (SlimTransaction) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onSavingsTargetChange: (Double) -> Unit,
    onDateChangeClick: () -> Unit,
    onManualAddClick: () -> Unit
) {
    val palette = StatementPalette
    val todayStart = remember { startOfTodayMillis() }
    val monthStart = remember { startOfMonthMillis() }
    val dailyTransactions = remember(uiState.transactions, todayStart) {
        uiState.transactions.filter { it.timestamp >= todayStart }
    }
    val monthTransactions = remember(uiState.transactions, monthStart) {
        uiState.transactions.filter { it.timestamp >= monthStart }
    }
    val historyTransactions = uiState.transactions
    val hasAnyData = uiState.totalTransactionCount > 0 ||
        uiState.totalCredits != 0.0 ||
        uiState.totalDebits != 0.0
    var showSavingsDialog by remember(hasAnyData, uiState.monthlySavingsTarget) {
        mutableStateOf(hasAnyData && uiState.monthlySavingsTarget <= 0.0)
    }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var showQuickActions by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.paper,
        floatingActionButton = {
            if (!isLoading) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(visible = showQuickActions) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmallActionFab(
                                icon = Icons.Default.DateRange,
                                label = "Calendar",
                                onClick = {
                                    showQuickActions = false
                                    onDateChangeClick()
                                }
                            )
                            SmallActionFab(
                                icon = Icons.Default.Add,
                                label = "Add more",
                                onClick = {
                                    showQuickActions = false
                                    onManualAddClick()
                                }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { showQuickActions = !showQuickActions },
                        containerColor = palette.cream,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            if (showQuickActions) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = "More actions"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(palette.paper)
        ) {
            SwipePageTabs(
                selectedPage = pagerState.currentPage,
                monthSpent = uiState.monthDebits,
                onPageClick = { page ->
                    coroutineScope.launch { pagerState.animateScrollToPage(page) }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageTransactions = when (page) {
                    0 -> dailyTransactions
                    1 -> monthTransactions
                    else -> historyTransactions
                }
                val pageTitle = when (page) {
                    0 -> "Today's activity"
                    1 -> "Month activity"
                    else -> "History from resync"
                }
                val pagePeriod = when (page) {
                    0 -> "today"
                    1 -> "this month"
                    else -> "in history"
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, top = 18.dp, end = 22.dp, bottom = 96.dp),
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
                    } else if (hasAnyData) {
                        item {
                            when (page) {
                                0 -> DailyPageSummary(
                                    uiState = uiState,
                                    currentFilter = currentFilter,
                                    onFilterChange = onFilterChange,
                                    onSavingsTargetClick = { showSavingsDialog = true }
                                )
                                1 -> MonthPageSummary(
                                    uiState = uiState,
                                    currentFilter = currentFilter,
                                    onFilterChange = onFilterChange,
                                    onSavingsTargetClick = { showSavingsDialog = true },
                                    onDateChangeClick = onDateChangeClick
                                )
                                else -> HistoryPageSummary(
                                    uiState = uiState,
                                    currentFilter = currentFilter,
                                    onFilterChange = onFilterChange,
                                    onDateChangeClick = onDateChangeClick
                                )
                            }
                        }
                        item {
                            TodayActivitySection(
                                title = pageTitle,
                                transactionCount = pageTransactions.size,
                                currentFilter = currentFilter,
                                onFilterChange = onFilterChange
                            )
                        }
                        if (pageTransactions.isNotEmpty()) {
                            items(
                                items = pageTransactions,
                                key = { tx -> "${page}_${tx.id}_${tx.timestamp}" }
                            ) { tx ->
                                TransactionRow(
                                    item = tx,
                                    onClick = { onTransactionClick(tx) }
                                )
                            }
                        } else {
                            item {
                                FilteredEmptyState(
                                    currentFilter = currentFilter,
                                    periodLabel = pagePeriod
                                )
                            }
                        }
                    } else {
                        item {
                            EmptyState(onSyncClick = onSyncClick)
                        }
                    }
                }
            }
        }
    }

    if (showSavingsDialog) {
        SavingsTargetDialog(
            currentTarget = uiState.monthlySavingsTarget,
            onDismiss = { showSavingsDialog = false },
            onSave = { amount ->
                onSavingsTargetChange(amount)
                showSavingsDialog = false
            }
        )
    }
}

private object StatementPalette {
    val paper = Color(0xFF070807)
    val paperDeep = Color(0xFF141614)
    val moss = Color(0xFF1A1D1A)
    val mossDark = Color(0xFF0D0F0D)
    val ink = Color(0xFFF4F5EF)
    val quietInk = Color(0xFF8F948D)
    val line = Color(0xFF2B302B)
    val clay = Color(0xFFE0704E)
    val cream = Color(0xFFA7F26D)
    val white = Color(0xFFFFFFFF)
}

@Composable
private fun SwipePageTabs(
    selectedPage: Int,
    monthSpent: Double,
    onPageClick: (Int) -> Unit
) {
    val palette = StatementPalette
    val tabs = listOf(
        PageTab("Daily", "Home"),
        PageTab("Month", formatCompactCurrency(monthSpent)),
        PageTab("History", "All")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.mossDark)
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedPage == index
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected) palette.ink else palette.paperDeep)
                    .border(
                        width = 1.dp,
                        color = if (selected) palette.ink else palette.line.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onPageClick(index) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (index) {
                    0 -> Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = if (selected) Color.Black else palette.ink,
                        modifier = Modifier.size(17.dp)
                    )
                    1 -> Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Color.Black else palette.cream)
                    )
                    else -> Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (selected) Color.Black else palette.ink,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = tab.title,
                        color = if (selected) Color.Black else palette.ink,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = tab.caption,
                        color = if (selected) Color.Black.copy(alpha = 0.58f) else palette.quietInk,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class PageTab(
    val title: String,
    val caption: String
)

@Composable
private fun SmallActionFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val palette = StatementPalette

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.paperDeep,
            contentColor = palette.ink
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            containerColor = palette.paperDeep,
            contentColor = palette.ink,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
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
                .background(palette.paperDeep),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SB",
                color = palette.ink,
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
                .clip(RoundedCornerShape(21.dp))
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
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Light
    )
    Text(
        text = "Here's your statement\nfor today",
        color = palette.ink,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun DailyPageSummary(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onSavingsTargetClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TodayPreviewCard(
            uiState = uiState,
            currentFilter = currentFilter,
            onFilterChange = onFilterChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
        )

        DailyAllowancePanel(
            uiState = uiState,
            onSavingsTargetClick = onSavingsTargetClick
        )
    }
}

@Composable
private fun MonthPageSummary(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onSavingsTargetClick: () -> Unit,
    onDateChangeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonthSpendingsCard(
                uiState = uiState,
                currentFilter = currentFilter,
                onFilterChange = onFilterChange,
                modifier = Modifier.weight(1f)
            )

            MonthlySavingsCard(
                uiState = uiState,
                onSavingsTargetClick = onSavingsTargetClick,
                modifier = Modifier.weight(1f)
            )
        }

        LastSevenDaysGraphCard(
            bars = uiState.lastSevenDaysDebits,
            onClick = { onFilterChange(TransactionFilter.DEBIT) }
        )

        MonthBalanceStrip(
            uiState = uiState,
            currentFilter = currentFilter,
            onFilterChange = onFilterChange,
            onDateChangeClick = onDateChangeClick
        )
    }
}

@Composable
private fun HistoryPageSummary(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDateChangeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BalanceStrip(
            uiState = uiState,
            currentFilter = currentFilter,
            onFilterChange = onFilterChange,
            onDateChangeClick = onDateChangeClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HistoryMetricCard(
                label = "Imported",
                value = uiState.totalTransactionCount.toString(),
                modifier = Modifier.weight(1f)
            )
            HistoryMetricCard(
                label = "Spent",
                value = formatCompactCurrency(uiState.totalDebits),
                modifier = Modifier.weight(1f)
            )
            HistoryMetricCard(
                label = "Credits",
                value = formatCompactCurrency(uiState.totalCredits),
                modifier = Modifier.weight(1f)
            )
        }

        HistoryRangeNote(selectedStartDate = uiState.selectedStartDate)
    }
}

@Composable
private fun TodayPreviewCard(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.moss)
            .border(
                width = 1.dp,
                color = if (currentFilter == TransactionFilter.DEBIT) palette.clay else palette.line.copy(alpha = 0.42f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onFilterChange(TransactionFilter.DEBIT) }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "TODAY",
                color = palette.quietInk,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTodayLabel(),
                color = palette.quietInk.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column {
            Text(
                text = formatStatementCurrency(uiState.todayBalance),
                color = if (uiState.todayBalance >= 0) palette.cream else palette.clay,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Net today",
                color = palette.quietInk,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MiniMetric(
                label = "Spent",
                amount = uiState.todayDebits,
                tint = palette.clay,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            MiniMetric(
                label = "Credit",
                amount = uiState.todayCredits,
                tint = palette.cream,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(TransactionFilter.CREDIT) }
            )
        }
    }
}

@Composable
private fun MonthSpendingsCard(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.paperDeep)
            .border(
                width = 1.dp,
                color = if (currentFilter == TransactionFilter.DEBIT) palette.clay else palette.line.copy(alpha = 0.55f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onFilterChange(TransactionFilter.DEBIT) }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "THIS MONTH",
                color = palette.ink,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${uiState.monthCount} SMS items",
                color = palette.quietInk,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column {
            Text(
                text = formatStatementCurrency(uiState.monthDebits),
                color = palette.ink,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Spent this month",
                color = palette.quietInk,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${formatStatementCurrency(uiState.averageDailyMonthSpending)} / day avg",
                color = palette.cream.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlySavingsCard(
    uiState: HomeUiState,
    onSavingsTargetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette
    val safeMonthlySpend = when {
        uiState.monthlySpendBudget > 0.0 -> uiState.monthlySpendBudget
        uiState.monthCredits > uiState.monthlySavingsTarget -> (uiState.monthCredits - uiState.monthlySavingsTarget)
        else -> 0.0
    }
    val progress = if (safeMonthlySpend > 0.0) {
        (uiState.monthDebits / safeMonthlySpend).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    val overBudget = safeMonthlySpend > 0.0 && uiState.monthDebits > safeMonthlySpend

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.moss)
            .border(1.dp, palette.line.copy(alpha = 0.42f), RoundedCornerShape(24.dp))
            .clickable { onSavingsTargetClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "SAFE SPEND",
                    color = palette.quietInk,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (safeMonthlySpend > 0.0) {
                        "${formatCompactCurrency(uiState.monthDebits)} / ${formatCompactCurrency(safeMonthlySpend)}"
                    } else {
                        "Set salary"
                    },
                    color = palette.cream,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Text(
                text = "Edit",
                color = palette.cream,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        SavingsProgressCircle(
            progress = progress,
            spent = uiState.monthDebits,
            safeMonthlySpend = safeMonthlySpend,
            overBudget = overBudget
        )

        Text(
            text = if (safeMonthlySpend > 0.0) {
                "${formatCompactCurrency(uiState.monthlySavingsTarget)} kept aside"
            } else {
                "Needs salary and target"
            },
            color = palette.quietInk,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun SavingsProgressCircle(
    progress: Double,
    spent: Double,
    safeMonthlySpend: Double,
    overBudget: Boolean
) {
    val palette = StatementPalette
    val percent = (progress * 100).toInt()

    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.fillMaxSize(),
            color = if (overBudget) palette.clay else palette.cream,
            trackColor = palette.mossDark.copy(alpha = 0.46f),
            strokeWidth = 7.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                color = if (overBudget) palette.clay else palette.cream,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = if (safeMonthlySpend > 0.0) formatCompactCurrency(spent) else "budget",
                color = palette.cream.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LastSevenDaysGraphCard(
    bars: List<com.budgettracker.ui.viewmodel.DailySpendBar>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette
    val initialIndex = remember(bars) { bars.indexOfLast { it.amount > 0.0 }.coerceAtLeast(0) }
    var selectedIndex by remember(bars) { mutableIntStateOf(initialIndex) }
    val selectedBar = bars.getOrElse(selectedIndex) {
        com.budgettracker.ui.viewmodel.DailySpendBar(label = "Today", amount = 0.0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.paperDeep)
            .border(1.dp, palette.line.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LAST 7 DAYS",
                    color = palette.ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${selectedBar.label} spend",
                    color = palette.quietInk,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Tap bars",
                color = palette.cream.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        LastSevenDaysBarChart(
            bars = bars,
            selectedIndex = selectedIndex,
            onBarClick = { index ->
                selectedIndex = index
                onClick()
            }
        )
    }
}

@Composable
private fun LastSevenDaysBarChart(
    bars: List<com.budgettracker.ui.viewmodel.DailySpendBar>,
    selectedIndex: Int,
    onBarClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette
    val maxAmount = bars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val safeIndex = selectedIndex.coerceIn(0, (bars.size - 1).coerceAtLeast(0))
    val selectedBar = bars.getOrElse(safeIndex) {
        com.budgettracker.ui.viewmodel.DailySpendBar(label = "Today", amount = 0.0)
    }
    val selectedRatio = (selectedBar.amount / maxAmount).toFloat().coerceIn(0f, 1f)
    val lineTopPadding = 18.dp + 78.dp * (1f - selectedRatio)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = lineTopPadding)
        ) {
            Text(
                text = formatStatementCurrency(selectedBar.amount),
                color = palette.ink,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(Modifier.height(6.dp))
            DottedGuideLine()
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, bar ->
                val actualRatio = (bar.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                val visibleRatio = if (bar.amount > 0.0) actualRatio.coerceAtLeast(0.14f) else 0.08f
                val selected = index == safeIndex

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onBarClick(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (selected) 0.84f else 0.68f)
                                .fillMaxHeight(visibleRatio)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    when {
                                        selected -> palette.clay
                                        bar.label == "Today" -> palette.cream
                                        else -> palette.cream.copy(alpha = 0.72f)
                                    }
                                )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (bar.label == "Today") "Now" else bar.label.take(3),
                        color = if (selected) palette.ink else palette.quietInk,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DottedGuideLine() {
    val palette = StatementPalette

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(26) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.line.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun HistoryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette

    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.paperDeep)
            .border(1.dp, palette.line.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(palette.cream)
        )
        Text(
            text = value,
            color = palette.ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = label,
            color = palette.quietInk,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryRangeNote(selectedStartDate: Long) {
    val palette = StatementPalette
    val rangeText = if (selectedStartDate > 0L) {
        "Showing imported SMS transactions since ${formatDate(selectedStartDate)}."
    } else {
        "Showing the full imported SMS transaction history."
    }

    Text(
        text = rangeText,
        color = palette.quietInk,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.42f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    )
}

@Composable
private fun MiniMetric(
    label: String,
    amount: Double,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val palette = StatementPalette

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            color = palette.paper.copy(alpha = 0.68f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = formatCompactCurrency(amount),
            color = tint,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun MonthBalanceStrip(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDateChangeClick: () -> Unit
) {
    val palette = StatementPalette

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.mossDark)
            .border(1.dp, palette.line.copy(alpha = 0.42f), RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.05f)
                .clickable { onFilterChange(TransactionFilter.ALL) }
        ) {
            Text(
                text = formatStatementCurrency(uiState.monthBalance),
                color = if (uiState.monthBalance >= 0) palette.cream else palette.clay,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Available this month",
                color = palette.ink.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(0.95f)
                .border(
                    width = 1.dp,
                    color = if (currentFilter == TransactionFilter.CREDIT) palette.cream.copy(alpha = 0.75f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onFilterChange(TransactionFilter.CREDIT) }
                .padding(8.dp)
        ) {
            Text(
                text = formatStatementCurrency(uiState.monthCredits),
                color = palette.ink,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Month credits",
                color = palette.quietInk,
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(
            onClick = onDateChangeClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.paperDeep)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Change start date",
                tint = palette.ink
            )
        }
    }
}

@Composable
private fun BalanceStrip(
    uiState: HomeUiState,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDateChangeClick: () -> Unit
) {
    val palette = StatementPalette

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.mossDark)
            .border(1.dp, palette.line.copy(alpha = 0.42f), RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.05f)
                .clickable { onFilterChange(TransactionFilter.ALL) }
        ) {
            Text(
                text = formatStatementCurrency(uiState.balance),
                color = if (uiState.balance >= 0) palette.cream else palette.clay,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Available",
                color = palette.cream.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(0.95f)
                .border(
                    width = 1.dp,
                    color = if (currentFilter == TransactionFilter.CREDIT) palette.cream.copy(alpha = 0.75f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onFilterChange(TransactionFilter.CREDIT) }
                .padding(8.dp)
        ) {
            Text(
                text = formatStatementCurrency(uiState.totalCredits),
                color = palette.cream,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "Credits imported",
                color = palette.cream.copy(alpha = 0.68f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(
            onClick = onDateChangeClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.paperDeep)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Change start date",
                tint = palette.cream
            )
        }
    }
}

@Composable
private fun DailyAllowancePanel(
    uiState: HomeUiState,
    onSavingsTargetClick: () -> Unit
) {
    val palette = StatementPalette
    val hasSavingsTarget = uiState.monthlySavingsTarget > 0.0
    val hasSalary = uiState.monthlySalary > 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.paperDeep)
            .border(1.dp, palette.line.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DailyAllowanceCircle(
            spent = uiState.todayDebits,
            allowed = uiState.dailyAllowedSpending,
            progress = uiState.dailyAllowanceProgress
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Daily spending limit",
                color = palette.ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = when {
                    !hasSavingsTarget -> "Set how much you want to save this month."
                    !hasSalary -> "Waiting for a salary SMS to calculate your daily limit."
                    else -> "${formatStatementCurrency(uiState.monthlySalary)} salary - ${formatStatementCurrency(uiState.monthlySavingsTarget)} savings target"
                },
                color = palette.quietInk,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when {
                    !hasSavingsTarget -> "The app will split the safe-to-spend balance across the month."
                    !hasSalary -> "Salary keywords include salary, payroll, wages, stipend, and remuneration."
                    else -> "${uiState.daysRemainingInMonth} days left. Overspending today lowers the next daily limit."
                },
                color = palette.cream.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }

        TextButton(
            onClick = onSavingsTargetClick,
            colors = ButtonDefaults.textButtonColors(contentColor = palette.ink)
        ) {
            Text(if (hasSavingsTarget) "Edit" else "Set")
        }
    }
}

@Composable
private fun DailyAllowanceCircle(
    spent: Double,
    allowed: Double,
    progress: Double
) {
    val palette = StatementPalette
    val overLimit = allowed > 0.0 && spent > allowed

    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.fillMaxSize(),
            color = if (overLimit) palette.clay else palette.cream,
            trackColor = palette.line,
            strokeWidth = 8.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${formatWholeAmount(spent)}/${formatWholeAmount(allowed)}",
                color = if (overLimit) palette.clay else palette.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "today",
                color = palette.quietInk,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun SavingsTargetDialog(
    currentTarget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val palette = StatementPalette
    var value by remember(currentTarget) {
        mutableStateOf(if (currentTarget > 0.0) formatPlainAmount(currentTarget) else "")
    }
    val amount = value.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.paperDeep,
        titleContentColor = palette.ink,
        textContentColor = palette.ink,
        title = {
            Text(
                text = "Monthly savings target",
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How much money do you want to save this month?",
                    color = palette.quietInk,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        value = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    singleLine = true,
                    label = { Text("Savings amount") },
                    prefix = { Text("Rs") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink,
                        focusedLabelColor = palette.cream,
                        unfocusedLabelColor = palette.quietInk,
                        cursorColor = palette.cream,
                        focusedBorderColor = palette.cream,
                        unfocusedBorderColor = palette.line,
                        focusedPrefixColor = palette.ink,
                        unfocusedPrefixColor = palette.quietInk
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(amount) },
                enabled = amount > 0.0,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.cream,
                    contentColor = Color.Black
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.ink)
            ) {
                Text("Later")
            }
        }
    )
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
            .background(palette.paperDeep, RoundedCornerShape(24.dp))
            .border(1.dp, palette.line, RoundedCornerShape(24.dp)),
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
            .border(1.dp, palette.line, RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (filter, label) ->
            val selected = currentFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) palette.ink else Color.Transparent)
                    .clickable { onFilterChange(filter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(Locale.getDefault()),
                    color = if (selected) Color.Black else palette.ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TodayActivitySection(
    title: String = "Today's activity",
    transactionCount: Int,
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit
) {
    val palette = StatementPalette
    val filterLabel = when (currentFilter) {
        TransactionFilter.ALL -> "all items"
        TransactionFilter.CREDIT -> "credits"
        TransactionFilter.DEBIT -> "debits"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.paperDeep)
            .border(1.dp, palette.line.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .padding(0.dp)
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(116.dp)
                .background(palette.cream)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = palette.ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$transactionCount $filterLabel shown",
                        color = palette.quietInk,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Live from SMS",
                    color = palette.cream,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            StatementFilter(
                currentFilter = currentFilter,
                onFilterChange = onFilterChange
            )
        }
    }
}

@Composable
private fun FilteredEmptyState(
    currentFilter: TransactionFilter,
    periodLabel: String = "today"
) {
    val palette = StatementPalette
    val label = when (currentFilter) {
        TransactionFilter.ALL -> "transactions"
        TransactionFilter.CREDIT -> "credit transactions"
        TransactionFilter.DEBIT -> "debit transactions"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "No $label $periodLabel",
            color = palette.ink,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Change the filter or resync if new SMS messages have arrived.",
            color = palette.quietInk,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TransactionRow(item: SlimTransaction, onClick: () -> Unit) {
    val palette = StatementPalette
    val arrow =
        if (item.isCredit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val arrowColor =
        if (item.isCredit) palette.cream else palette.clay

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.paperDeep)
            .border(1.dp, palette.line.copy(alpha = 0.36f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(palette.moss),
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
            color = if (item.isCredit) palette.cream else palette.clay,
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
            .background(palette.paperDeep, RoundedCornerShape(24.dp))
            .border(1.dp, palette.line, RoundedCornerShape(24.dp))
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

private fun formatCompactCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 10000000 -> "Rs" + formatCompactNumber(amount / 10000000) + "Cr"
        absAmount >= 100000 -> "Rs" + formatCompactNumber(amount / 100000) + "L"
        absAmount >= 1000 -> "Rs" + formatCompactNumber(amount / 1000) + "K"
        else -> formatStatementCurrency(amount)
    }
}

private fun formatCompactNumber(value: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    return format.format(value)
}

private fun formatWholeAmount(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return format.format(amount)
}

private fun formatPlainAmount(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        isGroupingUsed = false
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return format.format(amount)
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

private fun formatTodayLabel(): String {
    val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    return sdf.format(Date())
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

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}

private fun formatDateTime(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM, h:mm:ss a", Locale.getDefault())
    return sdf.format(Date(ts))
}
