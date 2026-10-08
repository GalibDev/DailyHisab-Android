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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    private var remoteObserver: Job? = null
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
                preferences.lastRemoteApplied = remote.updatedAt
                recordSuccess("Cloud backup restored")
            }.onFailure(::recordFailure)
        }
    }

    private suspend fun switchUser(uid: String?) {
        mutex.withLock {
            if (uid == currentUid) return@withLock
            val previousOwner = preferences.activeUid
            currentUid = uid
            remoteObserver?.cancel()
            remoteObserver = null
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
                val localHasUserData = local.transactions.isNotEmpty() || local.budgets.isNotEmpty() || local.loans.isNotEmpty()
                val resolved: FinanceSnapshot = when {
                    remote == null -> local.copy(updatedAt = System.currentTimeMillis())
                    firstMigration && localHasUserData -> mergeGuestWithRemote(local, remote)
                    !localHasUserData || remote.updatedAt >= preferences.localModified -> remote
                    else -> mergeGuestWithRemote(local, remote)
                }
                if (resolved !== local) repository.replaceLocal(resolved)
                val needsUpload = remote == null || (firstMigration && localHasUserData) ||
                    (localHasUserData && preferences.localModified > remote.updatedAt)
                if (needsUpload) {
                    val outgoing = resolved.copy(updatedAt = System.currentTimeMillis())
                    repository.upload(uid, outgoing)
                    preferences.lastRemoteApplied = outgoing.updatedAt
                } else {
                    preferences.lastRemoteApplied = remote.updatedAt
                }
                preferences.activeUid = uid
                preferences.markMigrated(uid)
                recordSuccess(if (firstMigration) "Local data migrated and synced" else "Cloud data synchronized")
                observeRemote(uid)
            }.onFailure(::recordFailure)
        }
    }

    private fun observeRemote(uid: String) {
        remoteObserver?.cancel()
        remoteObserver = viewModelScope.launch {
            repository.observeRemote(uid).collect { remote ->
                if (remote == null || uid != currentUid || remote.updatedAt <= preferences.lastRemoteApplied) return@collect
                mutex.withLock {
                    if (uid != currentUid || remote.updatedAt <= preferences.lastRemoteApplied) return@withLock
                    runCatching {
                        mutableState.value = mutableState.value.copy(phase = SyncPhase.Syncing, message = "Receiving website changes…")
                        repository.replaceLocal(remote)
                        preferences.lastRemoteApplied = remote.updatedAt
                        recordSuccess("Website changes synchronized")
                    }.onFailure(::recordFailure)
                }
            }
        }
    }

    private suspend fun backup(uid: String, successMessage: String) = mutex.withLock {
        runCatching {
            mutableState.value = mutableState.value.copy(phase = SyncPhase.Syncing, message = "Backing up…")
            val snapshot = repository.readLocal(System.currentTimeMillis())
            var lastError: Throwable? = null
            for (attempt in 0..2) {
                try {
                    repository.upload(uid, snapshot)
                    lastError = null
                    break
                } catch (error: Throwable) {
                    lastError = error
                    if (attempt < 2) delay((attempt + 1) * 1_000L)
                }
            }
            lastError?.let { throw it }
            preferences.lastRemoteApplied = snapshot.updatedAt
            recordSuccess(successMessage)
        }.onFailure(::recordFailure)
    }

    private fun recordSuccess(message: String) {
        val now = System.currentTimeMillis()
        preferences.lastSync = now
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
        remoteObserver?.cancel()
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
    var lastRemoteApplied: Long
        get() = values.getLong("lastRemoteApplied", 0)
        set(value) { values.edit().putLong("lastRemoteApplied", value).apply() }
    fun wasMigrated(uid: String) = values.getStringSet("migrated", emptySet()).orEmpty().contains(uid)
    fun markMigrated(uid: String) {
        val migrated = values.getStringSet("migrated", emptySet()).orEmpty().toMutableSet().apply { add(uid) }
        values.edit().putStringSet("migrated", migrated).apply()
    }
}
