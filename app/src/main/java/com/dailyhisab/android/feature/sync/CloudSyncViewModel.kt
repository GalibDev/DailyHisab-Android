package com.dailyhisab.android.feature.sync

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.sync.CloudSyncRepository
import com.dailyhisab.android.data.sync.CloudSyncSignals
import com.dailyhisab.android.data.sync.FinanceSnapshot
import com.dailyhisab.android.data.sync.mergeGuestWithRemote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncPhase { Guest, Syncing, Synced, Offline, Error }

data class SyncUiState(
    val phase: SyncPhase = SyncPhase.Guest,
    val lastSyncEpochMillis: Long = 0,
    val message: String = "Sign in to enable cloud backup",
)

@OptIn(FlowPreview::class)
class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CloudSyncRepository(DailyHisabDatabase.getInstance(application))
    private val preferences = SyncPreferences(application)
    private val auth = FirebaseAuth.getInstance()
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(
        SyncUiState(lastSyncEpochMillis = preferences.lastSync),
    )
    val state: StateFlow<SyncUiState> = mutableState.asStateFlow()
    private var currentUid: String? = null
    private val authListener = FirebaseAuth.AuthStateListener { firebase ->
        viewModelScope.launch { switchUser(firebase.currentUser?.uid) }
    }

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            CloudSyncSignals.changes.debounce(900).collect {
                preferences.localModified = System.currentTimeMillis()
                currentUid?.let { backup(it, "Changes backed up") }
            }
        }
    }

    fun backupNow() = viewModelScope.launch {
        val uid = auth.currentUser?.uid
        if (uid == null) mutableState.value = SyncUiState(SyncPhase.Guest, message = "Sign in to back up data")
        else backup(uid, "Backup complete")
    }

    fun restoreNow() = viewModelScope.launch {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            mutableState.value = SyncUiState(SyncPhase.Guest, message = "Sign in to restore data")
            return@launch
        }
        mutex.withLock {
            runCatching {
                mutableState.value = mutableState.value.copy(phase = SyncPhase.Syncing, message = "Restoring from cloud…")
                val remote = repository.readRemote(uid) ?: error("No cloud backup found")
                repository.replaceLocal(remote)
                recordSuccess("Cloud backup restored")
            }.onFailure(::recordFailure)
        }
    }

    private suspend fun switchUser(uid: String?) {
        mutex.withLock {
            if (uid == currentUid) return@withLock
            val previousOwner = preferences.activeUid
            currentUid = uid
            if (uid == null) {
                if (previousOwner != null) repository.clearLocal()
                preferences.activeUid = null
                mutableState.value = SyncUiState(SyncPhase.Guest, preferences.lastSync, "Signed out — local account data cleared")
                return@withLock
            }
            runCatching {
                mutableState.value = mutableState.value.copy(phase = SyncPhase.Syncing, message = "Checking cloud data…")
                if (previousOwner != null && previousOwner != uid) repository.clearLocal()
                val local = repository.readLocal(preferences.localModified)
                val remote = repository.readRemote(uid)
                val firstMigration = !preferences.wasMigrated(uid)
                val resolved: FinanceSnapshot = when {
                    remote == null -> local.copy(updatedAt = System.currentTimeMillis())
                    firstMigration && local.hasData -> mergeGuestWithRemote(local, remote)
                    remote.updatedAt > preferences.localModified -> remote
                    else -> local.copy(updatedAt = System.currentTimeMillis())
                }
                if (resolved !== local) repository.replaceLocal(resolved)
                repository.upload(uid, resolved.copy(updatedAt = System.currentTimeMillis()))
                preferences.activeUid = uid
                preferences.markMigrated(uid)
                recordSuccess(if (firstMigration) "Local data migrated and synced" else "Cloud data synchronized")
            }.onFailure(::recordFailure)
        }
    }

    private suspend fun backup(uid: String, successMessage: String) = mutex.withLock {
        runCatching {
            mutableState.value = mutableState.value.copy(phase = SyncPhase.Syncing, message = "Backing up…")
            val snapshot = repository.readLocal(System.currentTimeMillis())
            repository.upload(uid, snapshot)
            recordSuccess(successMessage)
        }.onFailure(::recordFailure)
    }

    private fun recordSuccess(message: String) {
        val now = System.currentTimeMillis()
        preferences.lastSync = now
        preferences.localModified = now
        mutableState.value = SyncUiState(SyncPhase.Synced, now, message)
    }

    private fun recordFailure(error: Throwable) {
        val offline = error.message?.contains("network", ignoreCase = true) == true || error.message?.contains("offline", ignoreCase = true) == true
        mutableState.value = SyncUiState(
            if (offline) SyncPhase.Offline else SyncPhase.Error,
            preferences.lastSync,
            error.localizedMessage ?: "Cloud sync failed",
        )
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}

private class SyncPreferences(context: Context) {
    private val values = context.getSharedPreferences("daily_hisab_sync", Context.MODE_PRIVATE)
    var activeUid: String?
        get() = values.getString("activeUid", null)
        set(value) { values.edit().putString("activeUid", value).apply() }
    var lastSync: Long
        get() = values.getLong("lastSync", 0)
        set(value) { values.edit().putLong("lastSync", value).apply() }
    var localModified: Long
        get() = values.getLong("localModified", 0)
        set(value) { values.edit().putLong("localModified", value).apply() }
    fun wasMigrated(uid: String) = values.getStringSet("migrated", emptySet()).orEmpty().contains(uid)
    fun markMigrated(uid: String) {
        val migrated = values.getStringSet("migrated", emptySet()).orEmpty().toMutableSet().apply { add(uid) }
        values.edit().putStringSet("migrated", migrated).apply()
    }
}
