package com.dailyhisab.android.feature.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.repository.RoomFinanceRepository
import com.dailyhisab.android.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileStats(val totalExpenseMinor: Long = 0, val transactionCount: Int = 0, val activeDays: Int = 0)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoomFinanceRepository(DailyHisabDatabase.getInstance(application))
    val stats = repository.observeTransactions().map { rows ->
        val expenses = rows.filter { it.type == TransactionType.Expense }
        ProfileStats(expenses.sumOf { it.amountMinor }, rows.size, expenses.map { it.date }.distinct().size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())
}
