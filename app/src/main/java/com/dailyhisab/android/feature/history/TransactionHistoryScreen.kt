package com.dailyhisab.android.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun TransactionHistoryScreen(contentPadding: PaddingValues, viewModel: TransactionHistoryViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<TransactionType?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<FinanceTransaction?>(null) }
    var deleting by remember { mutableStateOf<FinanceTransaction?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }

    val filtered = transactions.filter { transaction ->
        val start = startDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = endDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val categoryName = viewModel.categoryName(transaction.categoryId)
        (query.isBlank() || transaction.description.contains(query, true) || categoryName.contains(query, true)) &&
            (type == null || transaction.type == type) &&
            (categoryId == null || transaction.categoryId == categoryId) &&
            (start == null || !transaction.date.isBefore(start)) &&
            (end == null || !transaction.date.isAfter(end))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Transaction history", style = MaterialTheme.typography.headlineMedium) }
        item {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, null) }, label = { Text("Search") }, singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == null, { type = null }, { Text("All") })
                TransactionType.entries.forEach { item ->
                    FilterChip(type == item, { type = item }, { Text(item.name) })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(startDate, { startDate = it }, Modifier.weight(1f), label = { Text("From date") }, singleLine = true)
                OutlinedTextField(endDate, { endDate = it }, Modifier.weight(1f), label = { Text("To date") }, singleLine = true)
            }
        }
        item {
            Box {
                OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(categories.firstOrNull { it.id == categoryId }?.name ?: "All categories")
                }
                DropdownMenu(categoryMenu, { categoryMenu = false }) {
                    DropdownMenuItem({ Text("All categories") }, { categoryId = null; categoryMenu = false })
                    categories.forEach { category ->
                        DropdownMenuItem({ Text(category.name) }, { categoryId = category.id; categoryMenu = false })
                    }
                }
            }
        }
        if (filtered.isEmpty()) item { Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(filtered, key = FinanceTransaction::id) { transaction ->
            DailyHisabCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(viewModel.categoryName(transaction.categoryId), fontWeight = FontWeight.Bold)
                        Text("${transaction.date} • ${transaction.paymentMethod}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (transaction.description.isNotBlank()) Text(transaction.description)
                    }
                    Text(
                        formatMoney(transaction.amountMinor),
                        color = if (transaction.type == TransactionType.Expense) MaterialTheme.colorScheme.error else Color(0xFF079669),
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { editing = transaction }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { deleting = transaction }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }

    editing?.let { transaction ->
        EditTransactionDialog(transaction, categories, { editing = null }) { viewModel.save(it); editing = null }
    }
    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(transaction); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditTransactionDialog(
    transaction: FinanceTransaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (FinanceTransaction) -> Unit,
) {
    var amount by remember(transaction) { mutableStateOf(BigDecimal(transaction.amountMinor).movePointLeft(2).stripTrailingZeros().toPlainString()) }
    var date by remember(transaction) { mutableStateOf(transaction.date.toString()) }
    var description by remember(transaction) { mutableStateOf(transaction.description) }
    var categoryId by remember(transaction) { mutableStateOf(transaction.categoryId) }
    var categoryMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(date, { date = it }, label = { Text("Date") })
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                Box {
                    OutlinedButton(onClick = { categoryMenu = true }) { Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Category") }
                    DropdownMenu(categoryMenu, { categoryMenu = false }) {
                        categories.forEach { category -> DropdownMenuItem({ Text(category.name) }, { categoryId = category.id; categoryMenu = false }) }
                    }
                }
                if (error) Text("Enter a valid amount and date", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = runCatching { BigDecimal(amount).movePointRight(2).longValueExact() }.getOrNull()
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                if (minor == null || minor <= 0 || parsedDate == null) error = true
                else onSave(transaction.copy(amountMinor = minor, date = parsedDate, description = description.trim(), categoryId = categoryId, updatedAtEpochMillis = System.currentTimeMillis()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatMoney(amountMinor: Long): String = "৳ " + BigDecimal(amountMinor).movePointLeft(2).stripTrailingZeros().toPlainString()

