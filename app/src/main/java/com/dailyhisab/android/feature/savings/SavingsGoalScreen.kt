package com.dailyhisab.android.feature.savings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.data.local.entity.SavingsGoalEntity
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun SavingsGoalScreen(
    contentPadding: PaddingValues,
    currency: String,
    back: () -> Unit,
    viewModel: SavingsGoalViewModel = viewModel(),
) {
    val goals by viewModel.goals.collectAsState()
    var editing by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var contributing by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Savings goals", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                FilledIconButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "New goal") }
            }
        }
        if (goals.isEmpty()) item {
            DailyHisabCard {
                Text("Start your first savings goal", fontWeight = FontWeight.Bold)
                Text("Set a target and deadline, then record deposits as you save.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { creating = true }, Modifier.fillMaxWidth()) { Text("Create goal") }
            }
        }
        items(goals, key = SavingsGoalEntity::id) { goal ->
            val progress = if (goal.targetMinor > 0) (goal.savedMinor.toFloat() / goal.targetMinor).coerceIn(0f, 1f) else 0f
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Deadline: ${LocalDate.ofEpochDay(goal.deadlineEpochDay)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = goal }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { viewModel.delete(goal.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(money(goal.savedMinor, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${(progress * 100).toInt()}% of ${money(goal.targetMinor, currency)}")
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { contributing = goal }, Modifier.fillMaxWidth()) { Text(if (progress >= 1f) "Goal completed" else "Add or withdraw money") }
            }
        }
    }
    if (creating || editing != null) GoalEditor(editing, currency, { creating = false; editing = null }) { viewModel.save(it); creating = false; editing = null }
    contributing?.let { goal -> ContributionDialog(goal, currency, { contributing = null }) { viewModel.contribute(goal, it); contributing = null } }
}

@Composable
private fun GoalEditor(existing: SavingsGoalEntity?, currency: String, dismiss: () -> Unit, save: (SavingsGoalEntity) -> Unit) {
    val context = LocalContext.current
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var target by remember(existing) { mutableStateOf(existing?.targetMinor?.let(::major).orEmpty()) }
    var deadline by remember(existing) { mutableStateOf(existing?.deadlineEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now().plusMonths(3)) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (existing == null) "Create savings goal" else "Edit savings goal") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Goal name") }, singleLine = true)
            OutlinedTextField(target, { target = it }, label = { Text("Target amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> deadline = LocalDate.of(y, m + 1, d) }, deadline.year, deadline.monthValue - 1, deadline.dayOfMonth).show() }, Modifier.fillMaxWidth()) { Text("Deadline: $deadline") }
            if (error) Text("Enter a valid name, amount and future deadline", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = {
            val minor = target.toMinor()
            if (title.isBlank() || minor == null || minor <= 0 || deadline.isBefore(LocalDate.now())) error = true
            else save(SavingsGoalEntity(existing?.id ?: 0, title.trim(), minor, existing?.savedMinor?.coerceAtMost(minor) ?: 0, deadline.toEpochDay(), System.currentTimeMillis()))
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ContributionDialog(goal: SavingsGoalEntity, currency: String, dismiss: () -> Unit, apply: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var withdraw by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Update ${goal.title}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!withdraw, { withdraw = false }, { Text("Add") })
                FilterChip(withdraw, { withdraw = true }, { Text("Withdraw") })
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { amount.toMinor()?.takeIf { it > 0 }?.let { apply(if (withdraw) -it else it) } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

private fun String.toMinor() = runCatching { BigDecimal(this).movePointRight(2).longValueExact() }.getOrNull()
private fun major(minor: Long) = BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
private fun money(minor: Long, currency: String) = (if (currency == "USD") "$" else "৳ ") + major(minor)
