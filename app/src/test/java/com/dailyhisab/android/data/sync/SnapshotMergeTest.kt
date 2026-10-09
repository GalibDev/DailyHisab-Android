package com.dailyhisab.android.data.sync

import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotMergeTest {
    @Test fun `guest and cloud categories merge by name and transactions keep valid category`() {
        val localCategory = CategoryEntity(1, "Food", "restaurant", 1, 0, false)
        val remoteCategory = CategoryEntity(7, "Food", "restaurant", 1, 0, false)
        val remoteTransaction = TransactionEntity(9, 5000, "Expense", 7, 10, "Lunch", "Cash", 100, 100)
        val local = FinanceSnapshot(listOf(localCategory), emptyList(), emptyList(), emptyList(), 10)
        val remote = FinanceSnapshot(listOf(remoteCategory), listOf(remoteTransaction), emptyList(), emptyList(), 20)

        val merged = mergeGuestWithRemote(local, remote)

        assertEquals(1, merged.categories.size)
        assertEquals(1, merged.transactions.size)
        assertEquals(1, merged.transactions.single().categoryId)
    }

    @Test fun `same offline transaction is not duplicated during conflict merge`() {
        val category = CategoryEntity(1, "Food", "restaurant", 1, 0, false)
        val row = TransactionEntity(1, 25000, "Expense", 1, 20735, "Dinner", "Cash", 100, 100)
        val local = FinanceSnapshot(listOf(category), listOf(row), emptyList(), emptyList(), 100)
        val remote = FinanceSnapshot(listOf(category.copy(id = 9)), listOf(row.copy(id = 99, categoryId = 9)), emptyList(), emptyList(), 200)

        val merged = mergeGuestWithRemote(local, remote)

        assertEquals(1, merged.transactions.size)
        assertEquals(1, merged.categories.size)
    }

    @Test fun `offline-only and cloud-only transactions both survive merge`() {
        val category = CategoryEntity(1, "Food", "restaurant", 1, 0, false)
        val localRow = TransactionEntity(1, 10000, "Expense", 1, 20735, "Lunch", "Cash", 100, 100)
        val remoteRow = TransactionEntity(2, 20000, "Expense", 7, 20736, "Dinner", "Card", 200, 200)
        val merged = mergeGuestWithRemote(
            FinanceSnapshot(listOf(category), listOf(localRow), emptyList(), emptyList(), 100),
            FinanceSnapshot(listOf(category.copy(id = 7)), listOf(remoteRow), emptyList(), emptyList(), 200),
        )

        assertEquals(listOf(10000L, 20000L), merged.transactions.map { it.amountMinor }.sorted())
    }
}
