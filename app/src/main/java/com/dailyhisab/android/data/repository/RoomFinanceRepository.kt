package com.dailyhisab.android.data.repository

import androidx.room.withTransaction
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceSummary
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import com.dailyhisab.android.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import com.dailyhisab.android.data.sync.CloudSyncSignals

class RoomFinanceRepository(private val database: DailyHisabDatabase) : FinanceRepository {
    private val dao = database.financeDao()

    override fun observeTransactions(): Flow<List<FinanceTransaction>> =
        dao.observeTransactions().map { rows -> rows.map(TransactionEntity::toDomain) }

    override fun observeTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<FinanceTransaction>> =
        dao.observeTransactionsBetween(startDate.toEpochDay(), endDate.toEpochDay())
            .map { rows -> rows.map(TransactionEntity::toDomain) }

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map(CategoryEntity::toDomain) }

    override fun observeSummaryBetween(startDate: LocalDate, endDate: LocalDate): Flow<FinanceSummary> =
        dao.observeSummaryBetween(startDate.toEpochDay(), endDate.toEpochDay())
            .map { FinanceSummary(it.expenseMinor, it.incomeMinor) }

    override suspend fun saveTransaction(transaction: FinanceTransaction): Long =
        dao.insertTransaction(transaction.toEntity()).also { CloudSyncSignals.localDataChanged() }

    override suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id).also { CloudSyncSignals.localDataChanged() }

    override suspend fun saveCategory(category: Category): Long = dao.insertCategory(category.toEntity()).also { CloudSyncSignals.localDataChanged() }

    override suspend fun deleteCategory(id: Long) = dao.deleteCategory(id).also { CloudSyncSignals.localDataChanged() }

    override suspend fun updateCategoryOrder(categoryIds: List<Long>) {
        database.withTransaction {
            val byId = dao.categoriesByIds(categoryIds).associateBy(CategoryEntity::id)
            dao.updateCategories(categoryIds.mapIndexedNotNull { index, id -> byId[id]?.copy(position = index) })
        }
        CloudSyncSignals.localDataChanged()
    }
}

internal fun TransactionEntity.toDomain() = FinanceTransaction(
    id, amountMinor, TransactionType.valueOf(type), categoryId, LocalDate.ofEpochDay(dateEpochDay),
    description, paymentMethod, createdAtEpochMillis, updatedAtEpochMillis,
)

internal fun FinanceTransaction.toEntity() = TransactionEntity(
    id, amountMinor, type.name, categoryId, date.toEpochDay(), description, paymentMethod,
    createdAtEpochMillis, updatedAtEpochMillis,
)

internal fun CategoryEntity.toDomain() = Category(id, name, iconKey, colorArgb, position, isDefault)
internal fun Category.toEntity() = CategoryEntity(id, name, iconKey, colorArgb, position, isDefault)
