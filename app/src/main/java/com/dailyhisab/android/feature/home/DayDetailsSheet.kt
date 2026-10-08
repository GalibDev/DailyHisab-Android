package com.dailyhisab.android.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsSheet(
    transactions: List<FinanceTransaction>,
    categories: List<Category>,
    dismiss: () -> Unit,
    addExpense: () -> Unit,
) {
    var fullScreen by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val todayHasExpense = transactions.any { it.type == TransactionType.Expense && it.date == today }
    val lastDay = if (todayHasExpense) today.dayOfMonth else (today.dayOfMonth - 1).coerceAtLeast(0)
    val days = (1..lastDay).map(month::atDay).reversed()

    val content: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("এই মাসের দিনের হিসাব", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("তারিখ অনুযায়ী মোট খরচ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { fullScreen = true }) { Icon(Icons.Filled.OpenInFull, "Full screen") }
            IconButton(onClick = dismiss) { Icon(Icons.Filled.Close, "Close") }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        if (selectedDate != null) {
            val date = selectedDate!!
            val rows = transactions.filter { it.date == date && it.type == TransactionType.Expense }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = { selectedDate = null }) { Text("সব তারিখ") }
            }
            if (rows.isEmpty()) Text("এই তারিখে কোনো খরচ নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
            rows.forEach { row ->
                ListItem(
                    headlineContent = { Text(categories.firstOrNull { it.id == row.categoryId }?.name ?: "Unknown", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(row.description.ifBlank { row.paymentMethod }) },
                    trailingContent = { Text(money(row.amountMinor), fontWeight = FontWeight.Bold) },
                )
            }
        } else {
            if (days.isEmpty()) Text("আজ expense যোগ করলে আজকের দিন count হবে।", color = MaterialTheme.colorScheme.onSurfaceVariant)
            days.forEach { date ->
                val amount = transactions.filter { it.date == date && it.type == TransactionType.Expense }.sumOf { it.amountMinor }
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = { Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(if (amount == 0L) "কোনো খরচ হয়নি" else "বিস্তারিত দেখতে চাপুন") },
                    trailingContent = { Text(money(amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
                TextButton(onClick = { selectedDate = date }, modifier = Modifier.align(Alignment.End)) { Text("বিস্তারিত") }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = addExpense, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("খরচ যোগ করুন") }
    }

    if (fullScreen) {
        Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Column(content = content) }
                }
            }
        }
    } else {
        ModalBottomSheet(onDismissRequest = dismiss) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                content = content,
            )
        }
    }
}

private fun money(minor: Long) = "৳ " + BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
