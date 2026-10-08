package com.dailyhisab.android.feature.savings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.SavingsGoalEntity
import com.dailyhisab.android.data.sync.CloudSyncSignals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DailyHisabDatabase.getInstance(application).financeDao()
    val goals = dao.observeSavingsGoals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(goal: SavingsGoalEntity) = viewModelScope.launch { dao.insertSavingsGoal(goal); CloudSyncSignals.localDataChanged() }
    fun delete(id: Long) = viewModelScope.launch { dao.deleteSavingsGoal(id); CloudSyncSignals.localDataChanged() }
    fun contribute(goal: SavingsGoalEntity, amountMinor: Long) = save(
        goal.copy(savedMinor = (goal.savedMinor + amountMinor).coerceIn(0, goal.targetMinor), updatedAtEpochMillis = System.currentTimeMillis()),
    )
}
