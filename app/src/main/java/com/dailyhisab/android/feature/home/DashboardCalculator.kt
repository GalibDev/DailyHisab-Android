package com.dailyhisab.android.feature.home

import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

data class DashboardSummary(
    val todayExpenseMinor: Long = 0,
    val monthExpenseMinor: Long = 0,
    val allExpenseMinor: Long = 0,
    val countedDays: Int = 0,
    val dailyAverageMinor: Long = 0,
)

fun calculateDashboardSummary(
    transactions: List<FinanceTransaction>,
    today: LocalDate = LocalDate.now(),
): DashboardSummary {
    val expenses = transactions.filter { it.type == TransactionType.Expense }
    val month = YearMonth.from(today)
    val monthExpenses = expenses.filter { YearMonth.from(it.date) == month && !it.date.isAfter(today) }
    val todayExpense = monthExpenses.filter { it.date == today }.sumOf(FinanceTransaction::amountMinor)
    val monthExpense = monthExpenses.sumOf(FinanceTransaction::amountMinor)
    val countedDays = (today.dayOfMonth - 1) + if (todayExpense > 0) 1 else 0

    return DashboardSummary(
        todayExpenseMinor = todayExpense,
        monthExpenseMinor = monthExpense,
        allExpenseMinor = expenses.filterNot { it.date.isAfter(today) }.sumOf(FinanceTransaction::amountMinor),
        countedDays = countedDays,
        dailyAverageMinor = if (countedDays > 0) monthExpense / countedDays else 0,
    )
}

