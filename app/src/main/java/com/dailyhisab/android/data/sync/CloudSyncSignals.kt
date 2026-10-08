package com.dailyhisab.android.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CloudSyncSignals {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = mutableChanges.asSharedFlow()
    fun localDataChanged() { mutableChanges.tryEmit(Unit) }
}
