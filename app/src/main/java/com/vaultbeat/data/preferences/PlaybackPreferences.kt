package com.vaultbeat.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.playbackDataStore by preferencesDataStore(name = "playback")

data class LastPlayback(val mediaId: Long?, val positionMs: Long)

class PlaybackPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val mediaId = longPreferencesKey("last_media_id")
    private val position = longPreferencesKey("last_position_ms")

    val lastPlayback: Flow<LastPlayback> = context.playbackDataStore.data.map { preferences ->
        LastPlayback(preferences[mediaId], preferences[position] ?: 0L)
    }

    suspend fun save(songId: Long, positionMs: Long) {
        context.playbackDataStore.edit { preferences ->
            preferences[mediaId] = songId
            preferences[position] = positionMs
        }
    }
}

