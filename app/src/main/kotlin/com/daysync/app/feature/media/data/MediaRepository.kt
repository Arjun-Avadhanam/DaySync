package com.daysync.app.feature.media.data

import com.daysync.app.feature.media.domain.MediaItem
import com.daysync.app.feature.media.domain.MediaStatus
import com.daysync.app.feature.media.domain.MediaType
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllItems(): Flow<List<MediaItem>>
    fun getItemsByTypes(types: List<MediaType>): Flow<List<MediaItem>>
    fun getItemsByStatus(status: MediaStatus): Flow<List<MediaItem>>
    fun getFinishedItems(): Flow<List<MediaItem>>
    fun searchItems(query: String): Flow<List<MediaItem>>
    suspend fun getItemById(id: String): MediaItem?
    suspend fun addItem(item: MediaItem)
    suspend fun updateItem(item: MediaItem)
    suspend fun deleteItem(id: String)
}
