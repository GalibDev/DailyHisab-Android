package com.dailyhisab.android.feature.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

private enum class ReportPeriod { Daily, Weekly, Monthly, Yearly, Custom }

@Composable
fun ReportScreen(contentPadding: PaddingValues, viewModel: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var analytics by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var period by remember { mutableStateOf(ReportPeriod.Monthly) }
    var customStart by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val range = when (period) {
        ReportPeriod.Daily -> today to today
        ReportPeriod.Weekly -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to today
        ReportPeriod.Monthly -> YearMonth.from(today).atDay(1) to today
        ReportPeriod.Yearly -> today.withDayOfYear(1) to today
        ReportPeriod.Custom -> customStart to customEnd
    }
    val rows = transactions.filter { it.type == type && it.date in range.first..range.second }
    val total = rows.sumOf { it.amountMinor }
    val monthlyRows = transactions.filter { YearMonth.from(it.date) == YearMonth.from(today) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Reports & analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Button(
                onClick = { ReportExporter.shareMonthlyCard(context, monthlyRows, categories) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(8.dp)); Text("মাসিক হিসাব card share করুন") }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!analytics, { analytics = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Reports") }
                SegmentedButton(analytics, { analytics = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Analytics") }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, item ->
                    SegmentedButton(type == item, { type = item }, SegmentedButtonDefaults.itemShape(index, 2)) { Text(item.name) }
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReportPeriod.entries.forEach { item -> FilterChip(period == item, { period = item }, { Text(item.name.take(3)) }, Modifier.weight(1f)) }
                }
                if (period == ReportPeriod.Custom) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", customStart, Modifier.weight(1f)) { customStart = it; if (customEnd.isBefore(it)) customEnd = it }
                        DateButton("To", customEnd, Modifier.weight(1f)) { customEnd = it; if (customStart.isAfter(it)) customStart = it }
                    }
                }
            }
        }
        item {
            DailyHisabCard {
                Text("${period.name} ${type.name} Report", fontWeight = FontWeight.Bold)
                Text(money(total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${rows.size} transaction rows • ${range.first} to ${range.second}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!analytics) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button({ ReportExporter.sharePdf(context, "${period.name} ${type.name} report", rows, categories) }, Modifier.weight(1f)) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(4.dp)); Text("PDF") }
                        OutlinedButton({ ReportExporter.shareExcel(context, "${period.name} ${type.name} report", rows, categories) }, Modifier.weight(1f)) { Icon(Icons.Filled.GridOn, null); Spacer(Modifier.width(4.dp)); Text("Excel") }
                    }
                }
            }
        }
        if (analytics) {
            item { AnalyticsCard(rows, categories) }
        } else if (rows.isEmpty()) {
            item { Text("No ${type.name.lowercase()} found for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(rows, key = FinanceTransaction::id) { row -> ReportRow(row, categories) }
        }
    }
}

@Composable
private fun DateButton(label: String, date: LocalDate, modifier: Modifier, update: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, year, month, day -> update(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, modifier = modifier) { Text("$label: $date") }
}

@Composable
private fun AnalyticsCard(rows: List<FinanceTransaction>, categories: List<Category>) {
    val totals = rows.groupBy { it.categoryId }.mapValues { (_, values) -> values.sumOf { it.amountMinor } }.toList().sortedByDescending { it.second }.take(6)
    val maximum = totals.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    DailyHisabCard {
        Text("Category breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (totals.isEmpty()) Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        totals.forEachIndexed { index, (categoryId, amount) ->
            val color = listOf(Color(0xFF1641A3), Color(0xFFFF7518), Color(0xFF00A56B), Color(0xFF8B5CF6), Color(0xFFE83E8C), Color(0xFF0891B2))[index]
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Unknown", Modifier.weight(1f))
                Text(money(amount), fontWeight = FontWeight.SemiBold)
            }
            Canvas(Modifier.fillMaxWidth().height(10.dp).padding(top = 3.dp)) {
                drawRoundRect(Color.LightGray.copy(alpha = .3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
                drawRoundRect(color, size = size.copy(width = size.width * (amount.toFloat() / maximum.toFloat())), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            }
        }
    }
}

@Composable
private fun ReportRow(row: FinanceTransaction, categories: List<Category>) {
    DailyHisabCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(categories.firstOrNull { it.id == row.categoryId }?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                Text("${row.date} • ${row.paymentMethod}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (row.description.isNotBlank()) Text(row.description)
            }
            Text(money(row.amountMinor), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun money(minor: Long) = "৳ " + BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
