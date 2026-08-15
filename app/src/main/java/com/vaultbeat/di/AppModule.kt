package com.vaultbeat.di

import android.content.Context
import androidx.room.Room
import com.vaultbeat.data.local.LibraryStateDao
import com.vaultbeat.data.local.PlaylistDao
import com.vaultbeat.data.local.VaultBeatDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): VaultBeatDatabase =
        Room.databaseBuilder(context, VaultBeatDatabase::class.java, "vaultbeat.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun providePlaylistDao(database: VaultBeatDatabase): PlaylistDao = database.playlistDao()
    @Provides fun provideLibraryStateDao(database: VaultBeatDatabase): LibraryStateDao = database.libraryStateDao()
}

