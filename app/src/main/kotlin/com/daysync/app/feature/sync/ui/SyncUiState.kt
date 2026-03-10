package com.daysync.app.feature.sync.ui

import com.daysync.app.core.database.entity.SyncLogEntity
import com.daysync.app.core.sync.SyncState

data class SyncUiState(
    val syncState: SyncState = SyncState.Idle,
    val recentLogs: List<SyncLogEntity> = emptyList(),
    val lastSyncLog: SyncLogEntity? = null,
)
