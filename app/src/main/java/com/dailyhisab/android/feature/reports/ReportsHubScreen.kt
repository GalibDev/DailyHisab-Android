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
import com.dailyhisab.android.feature.loan.LoanScreen

@Composable
fun ReportsHubScreen(contentPadding: PaddingValues, initialSection: Int = 0) {
    var section by remember(initialSection) { mutableIntStateOf(initialSection) }
    val nestedPadding = PaddingValues(
        top = contentPadding.calculateTopPadding() + 58.dp,
        bottom = contentPadding.calculateBottomPadding(),
    )
    Box(Modifier.fillMaxSize()) {
        when (section) {
            0 -> ReportScreen(nestedPadding)
            1 -> BudgetScreen(nestedPadding)
            2 -> LoanScreen(nestedPadding)
            else -> TransactionHistoryScreen(nestedPadding)
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(contentPadding).padding(horizontal = 20.dp)) {
            listOf("Reports", "Budgets", "Loans", "History").forEachIndexed { index, label ->
                SegmentedButton(section == index, { section = index }, shape = SegmentedButtonDefaults.itemShape(index, 4)) { Text(label) }
            }
        }
    }
}
