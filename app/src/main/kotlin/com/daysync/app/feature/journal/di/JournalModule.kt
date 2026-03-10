package com.daysync.app.feature.journal.di

import com.daysync.app.feature.journal.data.JournalRepository
import com.daysync.app.feature.journal.data.JournalRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class JournalModule {
    @Binds
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository
}
