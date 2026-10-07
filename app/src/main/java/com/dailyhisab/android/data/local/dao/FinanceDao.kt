package com.dailyhisab.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class SummaryRow(val expenseMinor: Long, val incomeMinor: Long)

@Dao
interface FinanceDao {
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeTransactionsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'Expense' THEN amountMinor ELSE 0 END), 0) AS expenseMinor, COALESCE(SUM(CASE WHEN type = 'Income' THEN amountMinor ELSE 0 END), 0) AS incomeMinor FROM transactions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeSummaryBetween(startEpochDay: Long, endEpochDay: Long): Flow<SummaryRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM categories ORDER BY position, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE id IN (:ids)")
    suspend fun categoriesByIds(ids: List<Long>): List<CategoryEntity>
}
