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
}
