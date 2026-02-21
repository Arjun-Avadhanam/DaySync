package com.daysync.app.feature.nutrition.di

import com.daysync.app.BuildConfig
import com.google.genai.Client
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGeminiClient(): Client =
        Client.builder().apiKey(BuildConfig.GEMINI_API_KEY).build()
}
