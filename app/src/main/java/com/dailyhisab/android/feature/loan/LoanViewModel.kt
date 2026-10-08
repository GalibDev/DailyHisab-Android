package com.dailyhisab.android.feature.loan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.LoanEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dailyhisab.android.data.sync.CloudSyncSignals

class LoanViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DailyHisabDatabase.getInstance(application).financeDao()
    val loans = dao.observeLoans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(loan: LoanEntity) = viewModelScope.launch { dao.insertLoan(loan); CloudSyncSignals.localDataChanged() }
    fun delete(id: Long) = viewModelScope.launch { dao.deleteLoan(id); CloudSyncSignals.localDataChanged() }
    fun setReminder(loan: LoanEntity, enabled: Boolean) = save(
        loan.copy(reminderEnabled = enabled, updatedAtEpochMillis = System.currentTimeMillis()),
    )
}
