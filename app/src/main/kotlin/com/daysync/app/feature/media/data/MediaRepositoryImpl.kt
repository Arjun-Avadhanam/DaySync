package com.daysync.app.feature.media.data

import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.entity.MediaItemEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.media.domain.MediaItem
import com.daysync.app.feature.media.domain.MediaStatus
import com.daysync.app.feature.media.domain.MediaType
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val dao: MediaItemDao,
) : MediaRepository {

    override fun getAllItems(): Flow<List<MediaItem>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getItemsByTypes(types: List<MediaType>): Flow<List<MediaItem>> =
        dao.getByTypes(types.map { it.name }).map { entities -> entities.map { it.toDomain() } }

    override fun getItemsByStatus(status: MediaStatus): Flow<List<MediaItem>> =
        dao.getByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override fun getFinishedItems(): Flow<List<MediaItem>> =
        dao.getFinished().map { entities -> entities.map { it.toDomain() } }

    override fun searchItems(query: String): Flow<List<MediaItem>> =
        dao.searchByTitle(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getItemById(id: String): MediaItem? =
        dao.getById(id)?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addItem(item: MediaItem) {
        val entity = item.toEntity().copy(
            id = if (item.id.isBlank()) Uuid.random().toString() else item.id,
        )
        dao.insert(entity)
    }

    override suspend fun updateItem(item: MediaItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun deleteItem(id: String) {
        dao.softDelete(id, Clock.System.now().toEpochMilliseconds())
    }

    private fun MediaItemEntity.toDomain() = MediaItem(
        id = id,
        title = title,
        mediaType = MediaType.fromString(mediaType),
        status = MediaStatus.fromString(status),
        score = score,
        creators = creators,
        completedDate = completedDate,
        notes = notes,
        coverImageUrl = coverImageUrl,
    )

    private fun MediaItem.toEntity() = MediaItemEntity(
        id = id,
        title = title,
        mediaType = mediaType.name,
        status = status.name,
        score = score,
        creators = creators,
        completedDate = completedDate,
        notes = notes,
        coverImageUrl = coverImageUrl,
        syncStatus = SyncStatus.PENDING,
        lastModified = Clock.System.now(),
    )
}
