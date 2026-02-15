package com.daysync.app.core.sync

interface SyncEngine {
    suspend fun syncAll(): Result<Unit>
}
