package com.floatmaster.di

import android.content.Context
import com.floatmaster.data.ClipboardRepository
import com.floatmaster.data.NotesRepository
import com.floatmaster.data.SettingsRepository
import com.floatmaster.manager.WindowHistoryManager
import com.floatmaster.data.BrowserHistoryRepository
import com.floatmaster.service.FloatingWindowManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFloatingWindowManager(@ApplicationContext ctx: Context): FloatingWindowManager =
        FloatingWindowManager(ctx)

    @Provides @Singleton
    fun provideNotesRepo(@ApplicationContext ctx: Context): NotesRepository = NotesRepository(ctx)

    @Provides @Singleton
    fun provideClipboardRepo(@ApplicationContext ctx: Context): ClipboardRepository = ClipboardRepository(ctx)

    @Provides @Singleton
    fun provideSettingsRepo(@ApplicationContext ctx: Context): SettingsRepository = SettingsRepository(ctx)

    @Provides @Singleton
    fun provideBrowserHistory(@ApplicationContext ctx: Context): BrowserHistoryRepository = BrowserHistoryRepository(ctx)

    @Provides @Singleton
    fun provideHistoryManager(@ApplicationContext ctx: Context): WindowHistoryManager = WindowHistoryManager(ctx)
}
