package com.example.vaultbeat.data.local

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
    val createdAt: Long = System.currentTimeMillis()
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
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<PlaylistEntity>>
    @Insert suspend fun insert(playlist: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id = :playlistId") suspend fun delete(playlistId: Long)
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
    version = 1,
    exportSchema = false
)
abstract class VaultBeatDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun libraryStateDao(): LibraryStateDao
}
