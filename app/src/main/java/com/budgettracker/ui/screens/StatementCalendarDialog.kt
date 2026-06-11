package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatementCalendarDialog(
    title: String,
    confirmText: String,
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minDateMillis: Long = firstDayOfYearMillis(2020),
    maxDateMillis: Long = todayStartMillis()
) {
    val palette = CalendarPalette
    val minDate = remember(minDateMillis) { startOfDayMillis(minDateMillis) }
    val maxDate = remember(maxDateMillis) { startOfDayMillis(maxDateMillis) }
    val initialDate = remember(initialDateMillis, minDate, maxDate) {
        initialDateMillis.coerceDayIn(minDate, maxDate)
    }

    var selectedDate by remember(initialDate) { mutableLongStateOf(initialDate) }
    val minMonth = remember(minDate) { monthStartMillis(minDate) }
    val maxMonth = remember(maxDate) { monthStartMillis(maxDate) }
    val initialMonth = remember(initialDate) { monthStartMillis(initialDate) }
    val monthCount = remember(minMonth, maxMonth) { monthsBetween(minMonth, maxMonth) + 1 }
    val initialPage = remember(initialMonth, minMonth) { monthsBetween(minMonth, initialMonth) }
    val pagerState = rememberPagerState(initialPage = initialPage) { monthCount }
    val coroutineScope = rememberCoroutineScope()
    val visibleMonth = monthForPage(minMonth, pagerState.currentPage)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = palette.paper,
            contentColor = palette.ink,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, palette.line, RoundedCornerShape(8.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = title,
                    color = palette.ink,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(Date(selectedDate)),
                    color = palette.quietInk,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(18.dp))

                MonthHeader(
                    visibleMonth = visibleMonth,
                    minDate = minDate,
                    maxDate = maxDate,
                    onPrevious = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onNext = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    WeekHeader()
                    Spacer(Modifier.height(8.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp),
                        beyondBoundsPageCount = 1,
                        pageSpacing = 14.dp
                    ) { page ->
                        CalendarGrid(
                            visibleMonth = monthForPage(minMonth, page),
                            selectedDate = selectedDate,
                            minDate = minDate,
                            maxDate = maxDate,
                            onDaySelected = { selectedDate = it }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.ink
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedDate) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.ink,
                            contentColor = palette.cream
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    visibleMonth: Long,
    minDate: Long,
    maxDate: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val palette = CalendarPalette
    val canGoPrevious = monthStartMillis(visibleMonth) > monthStartMillis(minDate)
    val canGoNext = monthStartMillis(visibleMonth) < monthStartMillis(maxDate)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.paperDeep)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = if (canGoPrevious) palette.ink else palette.quietInk.copy(alpha = 0.45f)
            )
        }

        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(visibleMonth)),
            modifier = Modifier.weight(1f),
            color = palette.ink,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.paperDeep)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = if (canGoNext) palette.ink else palette.quietInk.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun WeekHeader() {
    val palette = CalendarPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                color = palette.quietInk,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    visibleMonth: Long,
    selectedDate: Long,
    minDate: Long,
    maxDate: Long,
    onDaySelected: (Long) -> Unit
) {
    val month = Calendar.getInstance().apply { timeInMillis = visibleMonth }
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val today = todayStartMillis()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(7) { columnIndex ->
                    val day = rowIndex * 7 + columnIndex - firstDayOffset + 1
                    if (day in 1..daysInMonth) {
                        val dateMillis = dayMillis(visibleMonth, day)
                        val enabled = dateMillis in minDate..maxDate
                        val selected = sameDay(dateMillis, selectedDate)
                        val isToday = sameDay(dateMillis, today)

                        DayCell(
                            day = day,
                            enabled = enabled,
                            selected = selected,
                            isToday = isToday,
                            onClick = { onDaySelected(dateMillis) }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: Int,
    enabled: Boolean,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val palette = CalendarPalette
    val background = when {
        selected -> palette.ink
        isToday -> palette.paperDeep
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> palette.ink
        isToday -> palette.line
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = when {
                selected -> palette.cream
                enabled -> palette.ink
                else -> palette.quietInk.copy(alpha = 0.35f)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private object CalendarPalette {
    val paper = Color(0xFFC9CEB9)
    val paperDeep = Color(0xFFB8BEA8)
    val ink = Color(0xFF18261E)
    val quietInk = Color(0xFF828978)
    val line = Color(0xFF7F8775)
    val cream = Color(0xFFE4E6D4)
}

private fun Long.coerceDayIn(minDate: Long, maxDate: Long): Long {
    return startOfDayMillis(this).coerceIn(minDate, maxDate)
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

private fun todayStartMillis(): Long = startOfDayMillis(System.currentTimeMillis())

private fun firstDayOfYearMillis(year: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun monthStartMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun addMonths(millis: Long, delta: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.MONTH, delta)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}

private fun monthForPage(minMonthMillis: Long, page: Int): Long {
    return addMonths(minMonthMillis, page)
}

private fun monthsBetween(startMonthMillis: Long, endMonthMillis: Long): Int {
    val start = Calendar.getInstance().apply { timeInMillis = startMonthMillis }
    val end = Calendar.getInstance().apply { timeInMillis = endMonthMillis }
    val startIndex = start.get(Calendar.YEAR) * 12 + start.get(Calendar.MONTH)
    val endIndex = end.get(Calendar.YEAR) * 12 + end.get(Calendar.MONTH)
    return endIndex - startIndex
}

private fun dayMillis(monthMillis: Long, day: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = monthMillis
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun sameDay(left: Long, right: Long): Boolean {
    val leftCalendar = Calendar.getInstance().apply { timeInMillis = left }
    val rightCalendar = Calendar.getInstance().apply { timeInMillis = right }
    return leftCalendar.get(Calendar.YEAR) == rightCalendar.get(Calendar.YEAR) &&
        leftCalendar.get(Calendar.DAY_OF_YEAR) == rightCalendar.get(Calendar.DAY_OF_YEAR)
}
