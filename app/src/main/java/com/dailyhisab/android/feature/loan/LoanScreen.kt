package com.dailyhisab.android.feature.loan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.data.local.entity.LoanEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class LoanDirection { Borrowed, Lent }
enum class LoanStatus { Active, DueSoon, Overdue, Paid }

@Composable
fun LoanScreen(contentPadding: PaddingValues, viewModel: LoanViewModel = viewModel()) {
    val loans by viewModel.loans.collectAsState()
    var filter by remember { mutableStateOf<LoanStatus?>(null) }
    var editing by remember { mutableStateOf<LoanEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<LoanEntity?>(null) }
    val today = LocalDate.now()
    val filtered = loans.filter { filter == null || loanStatus(it, today) == filter }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Loans & dues", style = MaterialTheme.typography.headlineMedium)
                    Text("Borrowed and lent money in one place", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { creating = true }) { Text("Add") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(filter == null, { filter = null }, { Text("All") })
                listOf(LoanStatus.DueSoon, LoanStatus.Overdue, LoanStatus.Paid).forEach { status ->
                    FilterChip(filter == status, { filter = status }, { Text(status.name) })
                }
            }
        }
        if (filtered.isEmpty()) item { Text("No matching loans", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(filtered, key = LoanEntity::id) { loan ->
            val status = loanStatus(loan, today)
            val remaining = (loan.amountMinor - loan.repaidMinor).coerceAtLeast(0)
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(loan.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${loan.direction} • Due ${LocalDate.ofEpochDay(loan.dueEpochDay)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(status)
                    IconButton(onClick = { editing = loan }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { deleting = loan }) { Icon(Icons.Filled.Delete, "Delete") }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (loan.amountMinor <= 0) 0f else (loan.repaidMinor.toFloat() / loan.amountMinor).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Repaid ${money(loan.repaidMinor)}")
                    Text("Remaining ${money(remaining)}", fontWeight = FontWeight.Bold)
                }
                if (loan.note.isNotBlank()) Text(loan.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Due reminder", Modifier.padding(start = 8.dp).weight(1f))
                    Switch(loan.reminderEnabled, { viewModel.setReminder(loan, it) })
                }
            }
        }
    }

    if (creating || editing != null) LoanEditor(editing, { creating = false; editing = null }) {
        viewModel.save(it); creating = false; editing = null
    }
    deleting?.let { loan ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete loan?") },
            text = { Text("This loan and its repayment progress will be removed.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(loan.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LoanEditor(existing: LoanEntity?, onDismiss: () -> Unit, onSave: (LoanEntity) -> Unit) {
    var person by remember(existing) { mutableStateOf(existing?.personName ?: "") }
    var amount by remember(existing) { mutableStateOf(existing?.amountMinor?.let(::minorInput) ?: "") }
    var repaid by remember(existing) { mutableStateOf(existing?.repaidMinor?.let(::minorInput) ?: "0") }
    var direction by remember(existing) { mutableStateOf(existing?.direction?.let(LoanDirection::valueOf) ?: LoanDirection.Borrowed) }
    var dueDate by remember(existing) { mutableStateOf(existing?.dueEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: LocalDate.now().plusDays(7).toString()) }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var reminder by remember(existing) { mutableStateOf(existing?.reminderEnabled ?: true) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add loan" else "Update loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(person, { person = it }, label = { Text("Person name") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("Total amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (existing != null) OutlinedTextField(repaid, { repaid = it }, label = { Text("Amount repaid") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoanDirection.entries.forEach { value -> FilterChip(direction == value, { direction = value }, { Text(value.name) }) }
                }
                OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due date YYYY-MM-DD") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Note") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable due reminder", Modifier.weight(1f))
                    Switch(reminder, { reminder = it })
                }
                if (error) Text("Enter valid name, amounts and due date", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val totalMinor = parseMinor(amount)
            val repaidMinor = parseMinor(repaid)
            val due = runCatching { LocalDate.parse(dueDate) }.getOrNull()
            if (person.isBlank() || totalMinor == null || totalMinor <= 0 || repaidMinor == null || repaidMinor < 0 || repaidMinor > totalMinor || due == null) error = true
            else onSave(LoanEntity(existing?.id ?: 0, person.trim(), totalMinor, repaidMinor, direction.name, due.toEpochDay(), note.trim(), reminder))
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StatusBadge(status: LoanStatus) {
    val color = when (status) {
        LoanStatus.Paid -> Color(0xFF079669)
        LoanStatus.Overdue -> MaterialTheme.colorScheme.error
        LoanStatus.DueSoon -> Color(0xFFF59E0B)
        LoanStatus.Active -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(status.name, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun loanStatus(loan: LoanEntity, today: LocalDate): LoanStatus {
    if (loan.repaidMinor >= loan.amountMinor) return LoanStatus.Paid
    val days = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(loan.dueEpochDay))
    return when {
        days < 0 -> LoanStatus.Overdue
        days <= 3 -> LoanStatus.DueSoon
        else -> LoanStatus.Active
    }
}

private fun parseMinor(value: String) = runCatching { BigDecimal(value).movePointRight(2).longValueExact() }.getOrNull()
private fun minorInput(value: Long) = BigDecimal(value).movePointLeft(2).stripTrailingZeros().toPlainString()
private fun money(value: Long) = "৳ " + minorInput(value)
