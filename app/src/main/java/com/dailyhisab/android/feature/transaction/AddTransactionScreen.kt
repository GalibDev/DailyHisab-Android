package com.dailyhisab.android.feature.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.core.designsystem.DailyHisabPrimaryButton
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun AddTransactionScreen(
    contentPadding: PaddingValues,
    onManageCategories: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (categoryId == null) categoryId = categories.firstOrNull()?.id
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Add transaction", style = MaterialTheme.typography.headlineMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TransactionType.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = type == item,
                    onClick = { type = item },
                    shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                ) { Text(item.name) }
            }
        }

        DailyHisabCard {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' }; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                prefix = { Text("৳ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Box {
                OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Category, null)
                    Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Select category", Modifier.padding(start = 8.dp))
                }
                DropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = { categoryId = category.id; categoryExpanded = false },
                        )
                    }
                }
            }
            TextButton(onClick = onManageCategories) { Text("Manage categories") }
            OutlinedTextField(
                dateText, { dateText = it; error = null }, Modifier.fillMaxWidth(),
                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                description, { description = it }, Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") }, minLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            Text("Payment method", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "bKash", "Nagad").forEach { method ->
                    FilterChip(paymentMethod == method, { paymentMethod = method }, { Text(method) })
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (saved) Text("Transaction saved successfully", color = MaterialTheme.colorScheme.tertiary)
        DailyHisabPrimaryButton(
            text = "Save ${type.name}",
            onClick = {
                val amountMinor = runCatching { BigDecimal(amount).movePointRight(2).longValueExact() }.getOrNull()
                val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                when {
                    amountMinor == null || amountMinor <= 0 -> error = "Enter a valid amount"
                    categoryId == null -> error = "Select a category"
                    date == null -> error = "Enter a valid date"
                    else -> {
                        viewModel.save(FinanceTransaction(
                            amountMinor = amountMinor,
                            type = type,
                            categoryId = categoryId!!,
                            date = date,
                            description = description.trim(),
                            paymentMethod = paymentMethod,
                        ))
                        amount = ""; description = ""; error = null; saved = true
                    }
                }
            },
        )
    }
}

