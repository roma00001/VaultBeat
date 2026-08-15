package com.vaultbeat.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val customCoverSongId: Long? = null,
    val sortOrder: String = "DATE_ADDED"
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "hidden_songs")
data class HiddenSongEntity(@PrimaryKey val songId: Long)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(@PrimaryKey val songId: Long, val playedAt: Long = System.currentTimeMillis())

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC") fun observeAll(): Flow<List<PlaylistEntity>>
    @Insert suspend fun insert(playlist: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id = :playlistId") suspend fun delete(playlistId: Long)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId") suspend fun deleteSongsByPlaylist(playlistId: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSong(song: PlaylistSongEntity)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId") suspend fun deleteSong(playlistId: Long, songId: Long)
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC") fun observePlaylistSongIds(playlistId: Long): Flow<List<Long>>
    @Query("UPDATE playlist_songs SET addedAt = :addedAt WHERE playlistId = :playlistId AND songId = :songId") suspend fun updateSongOrder(playlistId: Long, songId: Long, addedAt: Long)
    @Query("UPDATE playlists SET createdAt = :createdAt WHERE id = :playlistId") suspend fun updatePlaylistOrder(playlistId: Long, createdAt: Long)
    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId") suspend fun updatePlaylistName(playlistId: Long, name: String)
    @Query("UPDATE playlists SET customCoverSongId = :songId WHERE id = :playlistId") suspend fun updatePlaylistCover(playlistId: Long, songId: Long?)
    @Query("UPDATE playlists SET sortOrder = :sortOrder WHERE id = :playlistId") suspend fun updatePlaylistSortOrder(playlistId: Long, sortOrder: String)
}

@Dao
interface LibraryStateDao {
    @Query("SELECT * FROM favorites") fun observeFavorites(): Flow<List<FavoriteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun favorite(favorite: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE songId = :songId") suspend fun removeFavorite(songId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun hide(hidden: HiddenSongEntity)
    @Query("DELETE FROM hidden_songs WHERE songId = :songId") suspend fun restore(songId: Long)
}

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, FavoriteEntity::class, HiddenSongEntity::class, PlayHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VaultBeatDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun libraryStateDao(): LibraryStateDao
}

