package com.vaultbeat.core.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri?
) {
    val displayArtist: String
        get() = artist.split(',').firstOrNull()?.trim() ?: artist
}

