package com.dailyhisab.android.feature.transaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.repository.RoomFinanceRepository
import com.dailyhisab.android.domain.model.DefaultCategories
import com.dailyhisab.android.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DailyHisabDatabase.getInstance(application)
    private val repository = RoomFinanceRepository(database)
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (database.financeDao().categoryCount() == 0) {
                DefaultCategories.forEach { repository.saveCategory(it) }
            }
        }
    }

    fun save(transaction: FinanceTransaction) {
        viewModelScope.launch { repository.saveTransaction(transaction) }
    }
}

