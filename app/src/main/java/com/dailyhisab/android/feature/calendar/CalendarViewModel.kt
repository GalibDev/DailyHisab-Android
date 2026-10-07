package com.dailyhisab.android.feature.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.repository.RoomFinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoomFinanceRepository(DailyHisabDatabase.getInstance(application))

    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
