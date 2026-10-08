package com.dailyhisab.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetMinor: Long,
    val savedMinor: Long = 0,
    val deadlineEpochDay: Long,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
