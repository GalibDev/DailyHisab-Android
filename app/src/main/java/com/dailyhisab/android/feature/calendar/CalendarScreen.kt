package com.dailyhisab.android.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(contentPadding: PaddingValues, viewModel: CalendarViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val monthRows = remember(month) { calendarRows(month) }
    val transactionsByDate = transactions.groupBy(FinanceTransaction::date)
    val selectedTransactions = transactionsByDate[selectedDate].orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Calendar", style = MaterialTheme.typography.headlineMedium) }
        item {
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1); selectedDate = month.atDay(1) }) {
                        Icon(Icons.Filled.ChevronLeft, "Previous month")
                    }
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { month = month.plusMonths(1); selectedDate = month.atDay(1) }) {
                        Icon(Icons.Filled.ChevronRight, "Next month")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                    }
                }
                monthRows.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val dayTransactions = date?.let(transactionsByDate::get).orEmpty()
                            val expense = dayTransactions.filter { it.type == TransactionType.Expense }.sumOf { it.amountMinor }
                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(0.82f).padding(2.dp)
                                    .clickable(enabled = date != null) { selectedDate = date!! },
                                shape = MaterialTheme.shapes.small,
                                color = if (date == selectedDate) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ) {
                                if (date != null) Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(date.dayOfMonth.toString(), fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal)
                                    if (expense > 0) Text(shortMoney(expense), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text(selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), style = MaterialTheme.typography.titleMedium) }
        if (selectedTransactions.isEmpty()) item {
            Text("No transactions on this date", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(selectedTransactions, key = FinanceTransaction::id) { transaction ->
            DailyHisabCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                        if (transaction.description.isNotBlank()) Text(transaction.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        money(transaction.amountMinor),
                        color = if (transaction.type == TransactionType.Expense) MaterialTheme.colorScheme.error else Color(0xFF079669),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

internal fun calendarRows(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = MutableList<LocalDate?>(leading) { null }
    (1..month.lengthOfMonth()).forEach { cells += month.atDay(it) }
    while (cells.size % 7 != 0) cells += null
    return cells.chunked(7)
}

private fun money(minor: Long) = "৳ " + BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
private fun shortMoney(minor: Long) = if (minor >= 100_000) "৳${minor / 100_000}k" else "৳${minor / 100}"
