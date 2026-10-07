package com.dailyhisab.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amountMinor: Long,
    val repaidMinor: Long = 0,
    val direction: String,
    val dueEpochDay: Long,
    val note: String = "",
    val reminderEnabled: Boolean = true,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
