package com.example.vaultbeat.library

import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.data.library.MediaStoreMusicDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val mediaStore: MediaStoreMusicDataSource
) {
    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) { mediaStore.loadSongs() }
}
