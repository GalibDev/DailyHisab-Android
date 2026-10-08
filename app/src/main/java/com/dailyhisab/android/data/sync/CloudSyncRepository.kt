package com.dailyhisab.android.data.sync

import androidx.room.withTransaction
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.BudgetEntity
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.LoanEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import com.dailyhisab.android.domain.model.DefaultCategories

data class FinanceSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val loans: List<LoanEntity>,
    val updatedAt: Long,
) {
    val hasData get() = categories.isNotEmpty() || transactions.isNotEmpty() || budgets.isNotEmpty() || loans.isNotEmpty()
}

class CloudSyncRepository(private val database: DailyHisabDatabase) {
    private val dao = database.financeDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun readLocal(updatedAt: Long = System.currentTimeMillis()) = FinanceSnapshot(
        categories = dao.observeCategories().first(),
        transactions = dao.observeTransactions().first(),
        budgets = dao.observeBudgets().first(),
        loans = dao.observeLoans().first(),
        updatedAt = updatedAt,
    )

    suspend fun readRemote(uid: String): FinanceSnapshot? {
        val document = document(uid).get().await()
        return if (!document.exists()) null else snapshotFromMap(document.data.orEmpty())
    }

    suspend fun upload(uid: String, snapshot: FinanceSnapshot) {
        document(uid).set(snapshot.toMap()).await()
    }

    suspend fun replaceLocal(snapshot: FinanceSnapshot) = database.withTransaction {
        dao.clearTransactions()
        dao.clearCategories()
        dao.clearBudgets()
        dao.clearLoans()
        if (snapshot.categories.isNotEmpty()) dao.insertCategories(snapshot.categories)
        if (snapshot.transactions.isNotEmpty()) dao.insertTransactions(snapshot.transactions)
        if (snapshot.budgets.isNotEmpty()) dao.insertBudgets(snapshot.budgets)
        if (snapshot.loans.isNotEmpty()) dao.insertLoans(snapshot.loans)
    }

    suspend fun clearLocal() = database.withTransaction {
        dao.clearTransactions()
        dao.clearCategories()
        dao.clearBudgets()
        dao.clearLoans()
        dao.insertCategories(DefaultCategories.map { CategoryEntity(name = it.name, iconKey = it.iconKey, colorArgb = it.colorArgb, position = it.position, isDefault = it.isDefault) })
    }

    private fun document(uid: String) = firestore.collection("users").document(uid)
        .collection("backups").document("current")
}

internal fun mergeGuestWithRemote(local: FinanceSnapshot, remote: FinanceSnapshot): FinanceSnapshot {
    val categories = local.categories.toMutableList()
    var nextCategoryId = (categories.maxOfOrNull { it.id } ?: 0L) + 1
    val remoteCategoryMap = mutableMapOf<Long, Long>()
    remote.categories.forEach { incoming ->
        val existing = categories.firstOrNull { it.name.equals(incoming.name, ignoreCase = true) }
        if (existing != null) remoteCategoryMap[incoming.id] = existing.id
        else {
            val added = incoming.copy(id = nextCategoryId++, position = categories.size)
            categories += added
            remoteCategoryMap[incoming.id] = added.id
        }
    }

    val transactions = local.transactions.toMutableList()
    val transactionKeys = transactions.mapTo(mutableSetOf()) { it.uniqueKey() }
    var nextTransactionId = (transactions.maxOfOrNull { it.id } ?: 0L) + 1
    remote.transactions.forEach { incoming ->
        val mapped = incoming.copy(
            id = nextTransactionId,
            categoryId = remoteCategoryMap[incoming.categoryId] ?: categories.firstOrNull()?.id ?: incoming.categoryId,
        )
        if (transactionKeys.add(mapped.uniqueKey())) {
            transactions += mapped
            nextTransactionId++
        }
    }

    return FinanceSnapshot(
        categories = categories,
        transactions = transactions,
        budgets = mergeBudgets(local.budgets, remote.budgets),
        loans = mergeLoans(local.loans, remote.loans),
        updatedAt = maxOf(local.updatedAt, remote.updatedAt, System.currentTimeMillis()),
    )
}

private fun mergeBudgets(local: List<BudgetEntity>, remote: List<BudgetEntity>): List<BudgetEntity> {
    val result = local.toMutableList()
    var nextId = (result.maxOfOrNull { it.id } ?: 0L) + 1
    remote.forEach { incoming ->
        val index = result.indexOfFirst { it.name.equals(incoming.name, true) && it.period == incoming.period && it.startEpochDay == incoming.startEpochDay && it.endEpochDay == incoming.endEpochDay }
        if (index < 0) result += incoming.copy(id = nextId++)
        else if (incoming.updatedAtEpochMillis > result[index].updatedAtEpochMillis) result[index] = incoming.copy(id = result[index].id)
    }
    return result
}

private fun mergeLoans(local: List<LoanEntity>, remote: List<LoanEntity>): List<LoanEntity> {
    val result = local.toMutableList()
    var nextId = (result.maxOfOrNull { it.id } ?: 0L) + 1
    remote.forEach { incoming ->
        val index = result.indexOfFirst { it.personName.equals(incoming.personName, true) && it.direction == incoming.direction && it.dueEpochDay == incoming.dueEpochDay }
        if (index < 0) result += incoming.copy(id = nextId++)
        else if (incoming.updatedAtEpochMillis > result[index].updatedAtEpochMillis) result[index] = incoming.copy(id = result[index].id)
    }
    return result
}

private fun TransactionEntity.uniqueKey() = listOf(amountMinor, type, categoryId, dateEpochDay, description.trim(), paymentMethod, createdAtEpochMillis).joinToString("|")

private fun FinanceSnapshot.toMap(): Map<String, Any> = mapOf(
    "schemaVersion" to 1L,
    "updatedAt" to updatedAt,
    "categories" to categories.map { mapOf("id" to it.id, "name" to it.name, "iconKey" to it.iconKey, "colorArgb" to it.colorArgb, "position" to it.position.toLong(), "isDefault" to it.isDefault) },
    "transactions" to transactions.map { mapOf("id" to it.id, "amountMinor" to it.amountMinor, "type" to it.type, "categoryId" to it.categoryId, "dateEpochDay" to it.dateEpochDay, "description" to it.description, "paymentMethod" to it.paymentMethod, "createdAtEpochMillis" to it.createdAtEpochMillis, "updatedAtEpochMillis" to it.updatedAtEpochMillis) },
    "budgets" to budgets.map { mapOf("id" to it.id, "name" to it.name, "amountMinor" to it.amountMinor, "period" to it.period, "startEpochDay" to it.startEpochDay, "endEpochDay" to it.endEpochDay, "updatedAtEpochMillis" to it.updatedAtEpochMillis) },
    "loans" to loans.map { mapOf("id" to it.id, "personName" to it.personName, "amountMinor" to it.amountMinor, "repaidMinor" to it.repaidMinor, "direction" to it.direction, "dueEpochDay" to it.dueEpochDay, "note" to it.note, "reminderEnabled" to it.reminderEnabled, "updatedAtEpochMillis" to it.updatedAtEpochMillis) },
)

private fun snapshotFromMap(data: Map<String, Any>): FinanceSnapshot {
    fun maps(key: String) = (data[key] as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()
    return FinanceSnapshot(
        categories = maps("categories").mapNotNull { row ->
            CategoryEntity(row.long("id"), row.string("name"), row.string("iconKey"), row.long("colorArgb"), row.long("position").toInt(), row.boolean("isDefault"))
        },
        transactions = maps("transactions").mapNotNull { row ->
            TransactionEntity(row.long("id"), row.long("amountMinor"), row.string("type"), row.long("categoryId"), row.long("dateEpochDay"), row.string("description"), row.string("paymentMethod"), row.long("createdAtEpochMillis"), row.long("updatedAtEpochMillis"))
        },
        budgets = maps("budgets").mapNotNull { row ->
            BudgetEntity(row.long("id"), row.string("name"), row.long("amountMinor"), row.string("period"), row.long("startEpochDay"), row.long("endEpochDay"), row.long("updatedAtEpochMillis"))
        },
        loans = maps("loans").mapNotNull { row ->
            LoanEntity(row.long("id"), row.string("personName"), row.long("amountMinor"), row.long("repaidMinor"), row.string("direction"), row.long("dueEpochDay"), row.string("note"), row.boolean("reminderEnabled"), row.long("updatedAtEpochMillis"))
        },
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
    )
}

private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0L
private fun Map<*, *>.string(key: String) = this[key] as? String ?: ""
private fun Map<*, *>.boolean(key: String) = this[key] as? Boolean ?: false
