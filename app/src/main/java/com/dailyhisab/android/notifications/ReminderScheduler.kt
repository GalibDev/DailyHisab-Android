package com.dailyhisab.android.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val DAILY = "daily-expense-reminder"
    private const val LOANS = "loan-due-reminder"
    private const val BUDGETS = "budget-limit-warning"

    fun scheduleAll(context: Context) {
        val settings = ReminderPreferences(context).settings.value
        scheduleDaily(context, DAILY, ReminderWorker.TYPE_DAILY, settings.dailyEnabled, settings.dailyHour, settings.dailyMinute)
        scheduleDaily(context, LOANS, ReminderWorker.TYPE_LOAN, settings.loanEnabled, settings.loanHour, settings.loanMinute)
        val manager = WorkManager.getInstance(context)
        if (!settings.budgetEnabled) manager.cancelUniqueWork(BUDGETS)
        else manager.enqueueUniquePeriodicWork(
            BUDGETS,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf(ReminderWorker.KEY_TYPE to ReminderWorker.TYPE_BUDGET))
                .build(),
        )
    }

    private fun scheduleDaily(context: Context, name: String, type: String, enabled: Boolean, hour: Int, minute: Int) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(name)
            return
        }
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        manager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next))
                .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type))
                .build(),
        )
    }
}
