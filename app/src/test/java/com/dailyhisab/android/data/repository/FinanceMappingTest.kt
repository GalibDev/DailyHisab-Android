package com.dailyhisab.android.data.repository

import com.dailyhisab.android.domain.model.FinanceSummary
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FinanceMappingTest {
    @Test
    fun transactionRoundTripPreservesMoneyAndDate() {
        val transaction = FinanceTransaction(
            id = 7,
            amountMinor = 125_50,
            type = TransactionType.Expense,
            categoryId = 3,
            date = LocalDate.of(2026, 10, 7),
            description = "Lunch",
            paymentMethod = "bKash",
            createdAtEpochMillis = 100,
            updatedAtEpochMillis = 200,
        )

        assertEquals(transaction, transaction.toEntity().toDomain())
    }

    @Test
    fun balanceIsIncomeMinusExpense() {
        assertEquals(25_00, FinanceSummary(expenseMinor = 75_00, incomeMinor = 100_00).balanceMinor)
    }
}

