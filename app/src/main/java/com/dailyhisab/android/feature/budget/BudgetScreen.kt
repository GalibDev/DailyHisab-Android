package com.dailyhisab.android.feature.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.data.local.entity.BudgetEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class BudgetPeriod { Daily, Monthly, Yearly, Custom }

@Composable
fun BudgetScreen(contentPadding: PaddingValues, viewModel: BudgetViewModel = viewModel()) {
    val budgets by viewModel.progress.collectAsState()
    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Budgets", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { creating = true }) { Text("New budget") }
            }
        }
        if (budgets.isEmpty()) item { Text("No budget created yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(budgets, key = { it.budget.id }) { item ->
            val remaining = item.budget.amountMinor - item.spentMinor
            val ratio = if (item.budget.amountMinor <= 0) 0f else (item.spentMinor.toFloat() / item.budget.amountMinor).coerceIn(0f, 1f)
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.budget.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${LocalDate.ofEpochDay(item.budget.startEpochDay)} — ${LocalDate.ofEpochDay(item.budget.endEpochDay)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = item.budget }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { viewModel.delete(item.budget.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spent ${money(item.spentMinor)}")
                    Text("Remaining ${money(remaining)}", color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    if (creating || editing != null) BudgetEditor(editing, { creating = false; editing = null }) {
        viewModel.save(it); creating = false; editing = null
    }
}

@Composable
private fun BudgetEditor(existing: BudgetEntity?, onDismiss: () -> Unit, onSave: (BudgetEntity) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "My budget") }
    var amount by remember(existing) { mutableStateOf(existing?.amountMinor?.let { BigDecimal(it).movePointLeft(2).toPlainString() } ?: "") }
    var period by remember(existing) { mutableStateOf(existing?.period?.let { BudgetPeriod.valueOf(it) } ?: BudgetPeriod.Monthly) }
    var start by remember(existing) { mutableStateOf(existing?.startEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: LocalDate.now().toString()) }
    var end by remember(existing) { mutableStateOf(existing?.endEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: LocalDate.now().toString()) }
    var error by remember { mutableStateOf(false) }
    val effectiveRange = runCatching { periodRange(period, LocalDate.parse(start), LocalDate.parse(end)) }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create budget" else "Edit budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Budget name") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("Target amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BudgetPeriod.entries.forEach { value -> FilterChip(period == value, { period = value }, { Text(value.name) }) }
                }
                if (period == BudgetPeriod.Custom) {
                    OutlinedTextField(start, { start = it }, label = { Text("Start YYYY-MM-DD") }, singleLine = true)
                    OutlinedTextField(end, { end = it }, label = { Text("End YYYY-MM-DD") }, singleLine = true)
                }
                effectiveRange?.let { Text("Period: ${it.first} — ${it.second}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (error) Text("Enter a valid name, amount and date range", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val minor = runCatching { BigDecimal(amount).movePointRight(2).longValueExact() }.getOrNull()
            if (name.isBlank() || minor == null || minor <= 0 || effectiveRange == null) error = true
            else onSave(BudgetEntity(existing?.id ?: 0, name.trim(), minor, period.name, effectiveRange.first.toEpochDay(), effectiveRange.second.toEpochDay()))
        }) { Text(if (existing == null) "Create" else "Save changes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal fun periodRange(period: BudgetPeriod, start: LocalDate, customEnd: LocalDate): Pair<LocalDate, LocalDate>? = when (period) {
    BudgetPeriod.Daily -> start to start
    BudgetPeriod.Monthly -> YearMonth.from(start).let { it.atDay(1) to it.atEndOfMonth() }
    BudgetPeriod.Yearly -> start.withDayOfYear(1) to start.withDayOfYear(start.lengthOfYear())
    BudgetPeriod.Custom -> if (customEnd.isBefore(start)) null else start to customEnd
}

private fun money(minor: Long) = "৳ " + BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
