package com.dailyhisab.android.domain.model

import java.time.LocalDate

enum class TransactionType { Expense, Income }

data class FinanceTransaction(
    val id: Long = 0,
    val amountMinor: Long,
    val type: TransactionType,
    val categoryId: Long,
    val date: LocalDate,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val position: Int,
    val isDefault: Boolean = false,
)

data class FinanceSummary(
    val expenseMinor: Long = 0,
    val incomeMinor: Long = 0,
) {
    val balanceMinor: Long get() = incomeMinor - expenseMinor
}

