package com.dailyhisab.android.feature.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.repository.RoomFinanceRepository
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoomFinanceRepository(DailyHisabDatabase.getInstance(application))
    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(transaction: FinanceTransaction) = viewModelScope.launch { repository.saveTransaction(transaction) }
    fun delete(transaction: FinanceTransaction) = viewModelScope.launch { repository.deleteTransaction(transaction.id) }
    fun categoryName(id: Long): String = categories.value.firstOrNull { it.id == id }?.name ?: "Unknown"
    fun category(id: Long): Category? = categories.value.firstOrNull { it.id == id }
}

