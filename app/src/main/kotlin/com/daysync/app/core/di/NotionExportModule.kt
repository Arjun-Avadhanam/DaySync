package com.daysync.app.core.di

import com.daysync.app.BuildConfig
import com.daysync.app.core.notion.NotionExportClient
import com.daysync.app.core.notion.NotionFoodExporter
import com.daysync.app.core.notion.NotionJournalExporter
import com.daysync.app.core.notion.NotionMediaExporter
import com.daysync.app.core.notion.NotionSummaryReader
import com.daysync.app.feature.nutrition.di.NotionHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotionExportModule {

    @Provides
    @Singleton
    fun provideNotionExportClient(
        @NotionHttpClient httpClient: HttpClient,
    ): NotionExportClient {
        return NotionExportClient(httpClient, BuildConfig.NOTION_API_KEY)
    }

    @Provides
    @Singleton
    fun provideNotionFoodExporter(client: NotionExportClient): NotionFoodExporter {
        return NotionFoodExporter(client, BuildConfig.NOTION_MEAL_DATABASE_ID)
    }

    @Provides
    @Singleton
    fun provideNotionJournalExporter(client: NotionExportClient): NotionJournalExporter {
        return NotionJournalExporter(client, BuildConfig.NOTION_JOURNAL_DATABASE_ID)
    }

    @Provides
    @Singleton
    fun provideNotionMediaExporter(client: NotionExportClient): NotionMediaExporter {
        return NotionMediaExporter(client, BuildConfig.NOTION_MEDIA_DATABASE_ID)
    }

    @Provides
    @Singleton
    fun provideNotionSummaryReader(
        @NotionHttpClient httpClient: HttpClient,
    ): NotionSummaryReader {
        return NotionSummaryReader(httpClient, BuildConfig.NOTION_API_KEY)
    }
}
