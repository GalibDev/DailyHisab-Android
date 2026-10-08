package com.dailyhisab.android.data.sync

import androidx.room.withTransaction
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.local.entity.BudgetEntity
import com.dailyhisab.android.data.local.entity.CategoryEntity
import com.dailyhisab.android.data.local.entity.LoanEntity
import com.dailyhisab.android.data.local.entity.TransactionEntity
import com.dailyhisab.android.data.local.entity.SavingsGoalEntity
import com.dailyhisab.android.domain.model.DefaultCategories
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FinanceSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val loans: List<LoanEntity>,
    val updatedAt: Long,
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
) {
    val hasData get() = categories.isNotEmpty() || transactions.isNotEmpty() || budgets.isNotEmpty() || loans.isNotEmpty() || savingsGoals.isNotEmpty()
}

/** Shares the exact Realtime Database user paths used by dailyhisab.xyz. */
class CloudSyncRepository(private val database: DailyHisabDatabase) {
    private val dao = database.financeDao()
    private val realtime = realtimeDatabase.reference

    suspend fun readLocal(updatedAt: Long = System.currentTimeMillis()) = FinanceSnapshot(
        categories = dao.observeCategories().first(),
        transactions = dao.observeTransactions().first(),
        budgets = dao.observeBudgets().first(),
        loans = dao.observeLoans().first(),
        updatedAt = updatedAt,
        savingsGoals = dao.observeSavingsGoals().first(),
    )

    suspend fun readRemote(uid: String): FinanceSnapshot? {
        val reference = realtime.child("users").child(uid).child("appData")
        reference.keepSynced(true)
        val appData = reference.get().await()
        if (!appData.exists()) return null
        return snapshotFromRealtime(appData)
    }

    fun observeRemote(uid: String): Flow<FinanceSnapshot?> = callbackFlow {
        val reference = realtime.child("users").child(uid).child("appData")
        reference.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(if (snapshot.exists()) snapshotFromRealtime(snapshot) else null)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    suspend fun upload(uid: String, snapshot: FinanceSnapshot) {
        val categoryById = snapshot.categories.associateBy { it.id }
        val now = snapshot.updatedAt
        val updates = mapOf<String, Any?>(
            "finance/categories" to snapshot.categories.sortedBy { it.position }.map { it.name },
            "finance/entries" to snapshot.transactions.map { it.toWebEntry(categoryById[it.categoryId]?.name.orEmpty()) },
            "finance/updatedAt" to now,
            "loans/loans" to snapshot.loans.map { it.toWebLoan() },
            "loans/updatedAt" to now,
            "nativeAndroid/categories" to snapshot.categories.map { it.toNativeCategory() },
            "nativeAndroid/budgets" to snapshot.budgets.map { it.toNativeBudget() },
            "nativeAndroid/savingsGoals" to snapshot.savingsGoals.map { it.toNativeSavingsGoal() },
            "nativeAndroid/updatedAt" to now,
        )
        // Preserve website-only reminders, recurring expenses and wallet data.
        realtime.child("users").child(uid).child("appData").updateChildren(updates).await()
    }

    suspend fun replaceLocal(snapshot: FinanceSnapshot) = database.withTransaction {
        dao.clearTransactions()
        dao.clearCategories()
        dao.clearBudgets()
        dao.clearLoans()
        dao.clearSavingsGoals()
        if (snapshot.categories.isNotEmpty()) dao.insertCategories(snapshot.categories)
        if (snapshot.transactions.isNotEmpty()) dao.insertTransactions(snapshot.transactions)
        if (snapshot.budgets.isNotEmpty()) dao.insertBudgets(snapshot.budgets)
        if (snapshot.loans.isNotEmpty()) dao.insertLoans(snapshot.loans)
        if (snapshot.savingsGoals.isNotEmpty()) dao.insertSavingsGoals(snapshot.savingsGoals)
    }

    suspend fun clearLocal() = database.withTransaction {
        dao.clearTransactions()
        dao.clearCategories()
        dao.clearBudgets()
        dao.clearLoans()
        dao.clearSavingsGoals()
        dao.insertCategories(DefaultCategories.map { CategoryEntity(name = it.name, iconKey = it.iconKey, colorArgb = it.colorArgb, position = it.position, isDefault = it.isDefault) })
    }

    companion object {
        private val realtimeDatabase: FirebaseDatabase by lazy {
            FirebaseDatabase.getInstance().also { database ->
                runCatching { database.setPersistenceEnabled(true) }
            }
        }
    }
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
        savingsGoals = mergeSavingsGoals(local.savingsGoals, remote.savingsGoals),
    )
}

private fun snapshotFromRealtime(appData: DataSnapshot): FinanceSnapshot {
    val finance = appData.child("finance")
    val loansNode = appData.child("loans")
    val native = appData.child("nativeAndroid")
    val metadata = native.child("categories").records().associateBy { it.string("name").lowercase() }
    val categoryNames = finance.child("categories").values().mapNotNull { it as? String }.filter { it.isNotBlank() }
    val namesFromEntries = finance.child("entries").records().map { it.string("category") }.filter { it.isNotBlank() }
    val names = (categoryNames + namesFromEntries).distinctBy { it.lowercase() }
    val categories = names.mapIndexed { index, name ->
        val stored = metadata[name.lowercase()]
        val fallback = DefaultCategories.firstOrNull { it.name.equals(name, true) }
        CategoryEntity(
            id = stored?.long("id")?.takeIf { it > 0 } ?: (index + 1).toLong(),
            name = name,
            iconKey = stored?.string("iconKey")?.takeIf { it.isNotBlank() } ?: fallback?.iconKey ?: "category",
            colorArgb = stored?.long("colorArgb")?.takeIf { it != 0L } ?: fallback?.colorArgb ?: 0xFF4F46E5,
            position = index,
            isDefault = stored?.boolean("isDefault") ?: (fallback?.isDefault ?: false),
        )
    }.ifEmpty {
        DefaultCategories.mapIndexed { index, item -> CategoryEntity((index + 1).toLong(), item.name, item.iconKey, item.colorArgb, index, item.isDefault) }
    }
    val categoryIds = categories.associate { it.name.lowercase() to it.id }
    val fallbackCategoryId = categories.first().id
    val transactions = finance.child("entries").records().mapNotNull { row ->
        val date = row.string("date").toEpochDayOrNull() ?: return@mapNotNull null
        val type = row.string("type").lowercase().takeIf { it == "expense" || it == "income" } ?: return@mapNotNull null
        val amount = row.number("amount") ?: return@mapNotNull null
        val id = row.long("id").takeIf { it > 0 } ?: return@mapNotNull null
        val timestamp = row.string("date").toEpochMillis(row.string("time"))
        TransactionEntity(
            id = id,
            amountMinor = amount.toMinorUnits(),
            type = type.replaceFirstChar(Char::uppercase),
            categoryId = categoryIds[row.string("category").lowercase()] ?: fallbackCategoryId,
            dateEpochDay = date,
            description = row.string("description"),
            paymentMethod = row.string("method").ifBlank { "Cash" },
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
        )
    }
    val budgets = native.child("budgets").records().mapNotNull { row ->
        row.string("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        BudgetEntity(row.long("id"), row.string("name"), row.long("amountMinor"), row.string("period"), row.long("startEpochDay"), row.long("endEpochDay"), row.long("updatedAtEpochMillis"))
    }
    val loans = loansNode.child("loans").records().mapNotNull { row ->
        val amount = row.number("amount") ?: return@mapNotNull null
        val person = row.string("person").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val payments = row.records("payments").mapNotNull { it.number("amount") }.sum()
        LoanEntity(
            id = row.long("id").takeIf { it > 0 } ?: return@mapNotNull null,
            personName = person,
            amountMinor = amount.toMinorUnits(),
            repaidMinor = payments.toMinorUnits(),
            direction = if (row.string("type") == "lent") "Lent" else "Borrowed",
            dueEpochDay = row.string("dueDate").toEpochDayOrNull() ?: LocalDate.now().toEpochDay(),
            note = row.string("note"),
            reminderEnabled = true,
            updatedAtEpochMillis = loansNode.child("updatedAt").getValue(Long::class.java) ?: 0L,
        )
    }
    val savingsGoals = native.child("savingsGoals").records().mapNotNull { row ->
        val title = row.string("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        SavingsGoalEntity(
            id = row.long("id"),
            title = title,
            targetMinor = row.long("targetMinor"),
            savedMinor = row.long("savedMinor"),
            deadlineEpochDay = row.long("deadlineEpochDay"),
            updatedAtEpochMillis = row.long("updatedAtEpochMillis"),
        )
    }
    return FinanceSnapshot(
        categories,
        transactions,
        budgets,
        loans,
        maxOf(finance.longValue("updatedAt"), loansNode.longValue("updatedAt"), native.longValue("updatedAt")),
        savingsGoals,
    )
}

private fun TransactionEntity.toWebEntry(categoryName: String) = mapOf(
    "id" to id,
    "date" to LocalDate.ofEpochDay(dateEpochDay).toString(),
    "category" to categoryName.ifBlank { "Other" },
    "description" to description,
    "amount" to amountMinor.toMajorUnits(),
    "time" to Instant.ofEpochMilli(createdAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
    "method" to paymentMethod,
    "type" to type.lowercase(),
)

private fun LoanEntity.toWebLoan(): Map<String, Any> {
    val updatedDate = Instant.ofEpochMilli(updatedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    val payments = if (repaidMinor > 0) listOf(mapOf("id" to updatedAtEpochMillis, "amount" to repaidMinor.toMajorUnits(), "date" to updatedDate)) else emptyList()
    return mapOf(
        "id" to id,
        "type" to direction.lowercase(),
        "person" to personName,
        "amount" to amountMinor.toMajorUnits(),
        "startDate" to updatedDate,
        "dueDate" to LocalDate.ofEpochDay(dueEpochDay).toString(),
        "note" to note,
        "payments" to payments,
    )
}

private fun CategoryEntity.toNativeCategory() = mapOf("id" to id, "name" to name, "iconKey" to iconKey, "colorArgb" to colorArgb, "position" to position, "isDefault" to isDefault)
private fun BudgetEntity.toNativeBudget() = mapOf("id" to id, "name" to name, "amountMinor" to amountMinor, "period" to period, "startEpochDay" to startEpochDay, "endEpochDay" to endEpochDay, "updatedAtEpochMillis" to updatedAtEpochMillis)
private fun SavingsGoalEntity.toNativeSavingsGoal() = mapOf("id" to id, "title" to title, "targetMinor" to targetMinor, "savedMinor" to savedMinor, "deadlineEpochDay" to deadlineEpochDay, "updatedAtEpochMillis" to updatedAtEpochMillis)
private fun TransactionEntity.uniqueKey() = listOf(amountMinor, type, categoryId, dateEpochDay, description.trim(), paymentMethod, createdAtEpochMillis).joinToString("|")
private fun Long.toMajorUnits() = BigDecimal.valueOf(this, 2).toDouble()
private fun Double.toMinorUnits() = BigDecimal.valueOf(this).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
private fun String.toEpochDayOrNull() = runCatching { LocalDate.parse(take(10)).toEpochDay() }.getOrNull()
private fun String.toEpochMillis(time: String): Long = runCatching {
    val safeTime = time.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "00:00"
    java.time.LocalDateTime.parse("${take(10)}T$safeTime").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrDefault(System.currentTimeMillis())

private fun DataSnapshot.values(): List<Any?> = when (val stored = value) {
    is List<*> -> stored
    is Map<*, *> -> stored.values.toList()
    else -> children.map { it.value }.toList()
}
private fun DataSnapshot.records(): List<Map<*, *>> = values().mapNotNull { it as? Map<*, *> }
private fun DataSnapshot.longValue(key: String) = (child(key).value as? Number)?.toLong() ?: 0L
private fun Map<*, *>.records(key: String): List<Map<*, *>> = when (val stored = this[key]) {
    is List<*> -> stored.mapNotNull { it as? Map<*, *> }
    is Map<*, *> -> stored.values.mapNotNull { it as? Map<*, *> }
    else -> emptyList()
}
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0L
private fun Map<*, *>.number(key: String) = (this[key] as? Number)?.toDouble()
private fun Map<*, *>.string(key: String) = this[key] as? String ?: ""
private fun Map<*, *>.boolean(key: String) = this[key] as? Boolean ?: false

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

private fun mergeSavingsGoals(local: List<SavingsGoalEntity>, remote: List<SavingsGoalEntity>): List<SavingsGoalEntity> {
    val result = local.toMutableList()
    var nextId = (result.maxOfOrNull { it.id } ?: 0L) + 1
    remote.forEach { incoming ->
        val index = result.indexOfFirst { it.title.equals(incoming.title, true) && it.deadlineEpochDay == incoming.deadlineEpochDay }
        if (index < 0) result += incoming.copy(id = nextId++)
        else if (incoming.updatedAtEpochMillis > result[index].updatedAtEpochMillis) result[index] = incoming.copy(id = result[index].id)
    }
    return result
}
