package com.dailyhisab.android.feature.home

import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardCalculatorTest {
    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun todayCountsOnlyAfterAnExpenseIsAdded() {
        val before = calculateDashboardSummary(emptyList(), today)
        val after = calculateDashboardSummary(listOf(expense(250_00, today)), today)

        assertEquals(6, before.countedDays)
        assertEquals(7, after.countedDays)
        assertEquals(250_00, after.todayExpenseMinor)
    }

    @Test
    fun futureExpensesNeverAffectDashboard() {
        val summary = calculateDashboardSummary(
            listOf(expense(700_00, today.plusDays(1)), expense(300_00, today.minusDays(1))),
            today,
        )

        assertEquals(300_00, summary.monthExpenseMinor)
        assertEquals(300_00, summary.allExpenseMinor)
        assertEquals(50_00, summary.dailyAverageMinor)
    }

    private fun expense(amountMinor: Long, date: LocalDate) = FinanceTransaction(
        amountMinor = amountMinor,
        type = TransactionType.Expense,
        categoryId = 1,
        date = date,
    )
}

