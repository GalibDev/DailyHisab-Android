package com.dailyhisab.android.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReminderSettings(
    val dailyEnabled: Boolean = true,
    val dailyHour: Int = 20,
    val dailyMinute: Int = 0,
    val loanEnabled: Boolean = true,
    val loanHour: Int = 9,
    val loanMinute: Int = 0,
    val budgetEnabled: Boolean = true,
    val budgetThreshold: Int = 80,
)

class ReminderPreferences(context: Context) {
    private val values = context.getSharedPreferences("daily_hisab_reminders", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())
    val settings: StateFlow<ReminderSettings> = mutableSettings

    fun update(transform: (ReminderSettings) -> ReminderSettings) {
        val updated = transform(mutableSettings.value)
        values.edit()
            .putBoolean("dailyEnabled", updated.dailyEnabled)
            .putInt("dailyHour", updated.dailyHour)
            .putInt("dailyMinute", updated.dailyMinute)
            .putBoolean("loanEnabled", updated.loanEnabled)
            .putInt("loanHour", updated.loanHour)
            .putInt("loanMinute", updated.loanMinute)
            .putBoolean("budgetEnabled", updated.budgetEnabled)
            .putInt("budgetThreshold", updated.budgetThreshold)
            .apply()
        mutableSettings.value = updated
    }

    private fun read() = ReminderSettings(
        dailyEnabled = values.getBoolean("dailyEnabled", true),
        dailyHour = values.getInt("dailyHour", 20),
        dailyMinute = values.getInt("dailyMinute", 0),
        loanEnabled = values.getBoolean("loanEnabled", true),
        loanHour = values.getInt("loanHour", 9),
        loanMinute = values.getInt("loanMinute", 0),
        budgetEnabled = values.getBoolean("budgetEnabled", true),
        budgetThreshold = values.getInt("budgetThreshold", 80),
    )
}
