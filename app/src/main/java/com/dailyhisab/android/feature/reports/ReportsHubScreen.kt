package com.dailyhisab.android.feature.reports

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyhisab.android.feature.budget.BudgetScreen
import com.dailyhisab.android.feature.history.TransactionHistoryScreen

@Composable
fun ReportsHubScreen(contentPadding: PaddingValues) {
    var budgetMode by remember { mutableStateOf(false) }
    val nestedPadding = PaddingValues(
        top = contentPadding.calculateTopPadding() + 58.dp,
        bottom = contentPadding.calculateBottomPadding(),
    )
    Box(Modifier.fillMaxSize()) {
        if (budgetMode) BudgetScreen(nestedPadding)
        else TransactionHistoryScreen(nestedPadding)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(contentPadding).padding(horizontal = 20.dp)) {
            SegmentedButton(!budgetMode, { budgetMode = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("History") }
            SegmentedButton(budgetMode, { budgetMode = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Budgets") }
        }
    }
}
