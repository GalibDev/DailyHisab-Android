package com.dailyhisab.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dailyhisab.android.data.local.dao.FinanceDao
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import com.dailyhisab.android.data.local.entity.BudgetEntity
import com.dailyhisab.android.data.local.entity.LoanEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, LoanEntity::class],
    version = 3,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `period` TEXT NOT NULL, `startEpochDay` INTEGER NOT NULL, `endEpochDay` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `loans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personName` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `repaidMinor` INTEGER NOT NULL, `direction` TEXT NOT NULL, `dueEpochDay` INTEGER NOT NULL, `note` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL)",
                )
            }
        }
    }
}
