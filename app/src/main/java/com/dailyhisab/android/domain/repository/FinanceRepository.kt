package com.dailyhisab.android.domain.repository

import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceSummary
import com.dailyhisab.android.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FinanceRepository {
    fun observeTransactions(): Flow<List<FinanceTransaction>>
    fun observeTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<FinanceTransaction>>
    fun observeCategories(): Flow<List<Category>>
    fun observeSummaryBetween(startDate: LocalDate, endDate: LocalDate): Flow<FinanceSummary>
    suspend fun saveTransaction(transaction: FinanceTransaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun saveCategory(category: Category): Long
    suspend fun updateCategoryOrder(categoryIds: List<Long>)
}

