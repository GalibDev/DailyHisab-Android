package com.dailyhisab.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val period: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
