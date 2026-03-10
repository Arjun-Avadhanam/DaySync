package com.daysync.app.core.sync

import kotlinx.coroutines.flow.StateFlow

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val currentTable: String, val progress: Int, val total: Int) : SyncState
    data class Completed(val successCount: Int, val failureCount: Int) : SyncState
    data class Failed(val error: String) : SyncState
}

interface SyncEngine {
    val syncState: StateFlow<SyncState>
    suspend fun syncAll(): Result<Unit>
}
