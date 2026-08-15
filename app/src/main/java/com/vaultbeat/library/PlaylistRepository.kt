package com.vaultbeat.library

import com.vaultbeat.data.local.PlaylistDao
import com.vaultbeat.data.local.PlaylistEntity
import com.vaultbeat.data.local.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    fun observePlaylistSongIds(playlistId: Long): Flow<List<Long>> = playlistDao.observePlaylistSongIds(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(PlaylistEntity(name = name.trim()))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deleteSongsByPlaylist(playlistId)
        playlistDao.delete(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, addedAt = System.currentTimeMillis()))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deleteSong(playlistId, songId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            playlistDao.updateSongOrder(playlistId, songId, index.toLong())
        }
    }

    suspend fun movePlaylist(orderedPlaylistIds: List<Long>) {
        orderedPlaylistIds.forEachIndexed { index, playlistId ->
            playlistDao.updatePlaylistOrder(playlistId, index.toLong())
        }
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        playlistDao.updatePlaylistName(playlistId, name.trim())
    }

    suspend fun updatePlaylistCover(playlistId: Long, songId: Long?) {
        playlistDao.updatePlaylistCover(playlistId, songId)
    }

    suspend fun updatePlaylistSortOrder(playlistId: Long, sortOrder: String) {
        playlistDao.updatePlaylistSortOrder(playlistId, sortOrder)
    }
}

