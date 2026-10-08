package com.dailyhisab.android.feature.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.BudgetEntity
import com.dailyhisab.android.data.repository.toDomain
import com.dailyhisab.android.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.dailyhisab.android.data.sync.CloudSyncSignals

data class BudgetProgress(val budget: BudgetEntity, val spentMinor: Long)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DailyHisabDatabase.getInstance(application)
    private val dao = database.financeDao()

    val progress = combine(dao.observeBudgets(), dao.observeTransactions()) { budgets, rows ->
        val transactions = rows.map { it.toDomain() }
        budgets.map { budget ->
            val start = LocalDate.ofEpochDay(budget.startEpochDay)
            val end = LocalDate.ofEpochDay(budget.endEpochDay)
            BudgetProgress(
                budget,
                transactions.filter { it.type == TransactionType.Expense && it.date in start..end }.sumOf { it.amountMinor },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(budget: BudgetEntity) = viewModelScope.launch { dao.insertBudget(budget); CloudSyncSignals.localDataChanged() }
    fun delete(id: Long) = viewModelScope.launch { dao.deleteBudget(id); CloudSyncSignals.localDataChanged() }
}
