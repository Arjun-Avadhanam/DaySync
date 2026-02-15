package com.daysync.app.core.sync

import kotlin.time.Instant

interface SyncableEntity {
    val syncStatus: SyncStatus
    val lastModified: Instant
    val isDeleted: Boolean
}
