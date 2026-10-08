package com.dailyhisab.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailyhisab.android.R
import com.dailyhisab.android.data.local.DailyHisabDatabase
import java.math.BigDecimal
import java.time.LocalDate

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val dao = DailyHisabDatabase.getInstance(context).financeDao()
    private val settings = ReminderPreferences(context).settings.value

    override suspend fun doWork(): Result = runCatching {
        createChannel()
        when (inputData.getString(KEY_TYPE)) {
            TYPE_DAILY -> dailyReminder()
            TYPE_LOAN -> loanReminders()
            TYPE_BUDGET -> { budgetWarnings(); savingsGoalAlerts() }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    private suspend fun dailyReminder() {
        if (!settings.dailyEnabled) return
        val today = LocalDate.now().toEpochDay()
        if (dao.transactions().none { it.dateEpochDay == today }) {
            notify(1001, "আজকের হিসাব যোগ করুন", "আজকের আয়-ব্যয় এখনো যোগ করা হয়নি।")
        }
    }

    private suspend fun loanReminders() {
        if (!settings.loanEnabled) return
        val today = LocalDate.now()
        dao.loans().filter { it.reminderEnabled && it.repaidMinor < it.amountMinor }.forEach { loan ->
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(loan.dueEpochDay))
            if (days <= 1) {
                val whenText = when {
                    days < 0 -> "${-days} দিন overdue"
                    days == 0L -> "আজ due"
                    else -> "আগামীকাল due"
                }
                notify((2000L + loan.id).toInt(), "${loan.personName}–এর loan reminder", "$whenText • বাকি ${money(loan.amountMinor - loan.repaidMinor)}")
            }
        }
    }

    private suspend fun budgetWarnings() {
        if (!settings.budgetEnabled) return
        val expenses = dao.transactions().filter { it.type == "Expense" }
        dao.budgets().forEach { budget ->
            if (budget.amountMinor <= 0) return@forEach
            val spent = expenses.filter { it.dateEpochDay in budget.startEpochDay..budget.endEpochDay }.sumOf { it.amountMinor }
            val percent = (spent * 100 / budget.amountMinor).toInt()
            if (percent >= settings.budgetThreshold) {
                notify((3000L + budget.id).toInt(), "${budget.name} budget warning", "$percent% ব্যবহার হয়েছে • ${money(spent)} / ${money(budget.amountMinor)}")
            }
        }
    }

    private suspend fun savingsGoalAlerts() {
        val today = LocalDate.now().toEpochDay()
        dao.savingsGoals().forEach { goal ->
            when {
                goal.savedMinor >= goal.targetMinor -> notify((4000L + goal.id).toInt(), "Savings goal completed", "${goal.title} লক্ষ্য পূরণ হয়েছে!")
                goal.deadlineEpochDay <= today -> notify((4000L + goal.id).toInt(), "Savings goal deadline", "${goal.title}–এর deadline এসে গেছে।")
            }
        }
    }

    private fun notify(id: Int, title: String, body: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Daily Hisab reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val KEY_TYPE = "reminder_type"
        const val TYPE_DAILY = "daily"
        const val TYPE_LOAN = "loan"
        const val TYPE_BUDGET = "budget"
        private const val CHANNEL_ID = "daily_hisab_reminders"
        private fun money(minor: Long) = "৳" + BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
    }
}
