package com.dailyhisab.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dailyhisab.android.data.local.dao.FinanceDao
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DailyHisabDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile private var instance: DailyHisabDatabase? = null

        fun getInstance(context: Context): DailyHisabDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DailyHisabDatabase::class.java,
                "daily-hisab.db",
            ).build().also { instance = it }
        }
    }
}

