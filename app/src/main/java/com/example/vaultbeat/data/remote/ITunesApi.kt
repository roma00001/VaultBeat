package com.example.vaultbeat.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ITunesApi @Inject constructor() {
    
    companion object {
        private const val TAG = "ArtworkApi"
        private const val MUSICBRAINZ_SEARCH_URL = "https://musicbrainz.org/ws/2/recording"
        private const val COVERART_ARCHIVE_URL = "https://coverartarchive.org/release-group"
        private const val LASTFM_SEARCH_URL = "https://ws.audioscrobbler.com/2.0"
        private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    }
    
    suspend fun getArtworkUrl(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val uploaderArtist = cleanArtist(artist)
            var detectedArtist = uploaderArtist
            var detectedTitle = trackTitle
            
            // Try to detect "Artist - Title" format in the video name
            val separators = listOf(" - ", " – ", " — ", " : ", " | ")
            for (sep in separators) {
                if (trackTitle.contains(sep)) {
                    val parts = trackTitle.split(sep)
                    if (parts.size >= 2) {
                        val possibleArtist = parts[0].trim()
                        val possibleTitle = parts.subList(1, parts.size).joinToString(sep).trim()
                        
                        // Trust the split if uploader is generic or artist part looks valid
                        if (uploaderArtist.contains("Topic", ignoreCase = true) || 
                            uploaderArtist.isBlank() ||
                            uploaderArtist.contains("VEVO", ignoreCase = true) ||
                            possibleArtist.contains(uploaderArtist, ignoreCase = true) ||
                            uploaderArtist.contains(possibleArtist, ignoreCase = true) ||
                            possibleArtist.length < 40) {
                            detectedArtist = possibleArtist
                            detectedTitle = possibleTitle
                            break
                        }
                    }
                }
            }
            
            val finalArtist = cleanArtist(detectedArtist)
            val finalTitle = cleanTitle(detectedTitle, finalArtist)
            
            Log.d(TAG, "Searching artwork for: $finalArtist - $finalTitle")
            
            // 1. iTunes
            val itunesUrl = searchITunes(finalArtist, finalTitle)
            if (itunesUrl != null) return@withContext itunesUrl

            // 2. MusicBrainz
            val mbUrl = searchMusicBrainz(finalArtist, finalTitle)
            if (mbUrl != null) return@withContext mbUrl
            
            // 3. Last.fm
            searchLastfm(finalArtist, finalTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Artwork search error: ${e.message}")
            null
        }
    }

    private suspend fun searchITunes(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = "$artist $trackTitle"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$ITUNES_SEARCH_URL?term=$encodedQuery&entity=song&limit=1"
            
            Log.d(TAG, "Buscando en iTunes: $query")
            
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val jsonObject = JSONObject(response)
            val results = jsonObject.optJSONArray("results")
            
            if (results != null && results.length() > 0) {
                val artworkUrl = results.getJSONObject(0).optString("artworkUrl100", "")
                // Convertir miniatura de 100px a 600px de alta calidad
                if (artworkUrl.isNotBlank()) {
                    return@withContext artworkUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error en iTunes API: ${e.message}")
            null
        }
    }
    
    private fun cleanTitle(title: String, artist: String): String {
        var cleaned = title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?]"), "")
            
        val noiseRegex = Regex("(?i)\\b(official|video|audio|lyric|hd|4k|8k|hq|explicit|clean|remastered|live|envivo)\\b")
        cleaned = cleaned.replace(noiseRegex, "")
            
        val artistEscaped = Regex.escape(artist)
        cleaned = cleaned.replace(Regex("(?i)^$artistEscaped\\s*[-:–—|]\\s*"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*[-:–—|]\\s*$artistEscaped$"), "")
        
        cleaned = cleaned.replace(Regex("^[:\\-–—|\\s.]+"), "")
        cleaned = cleaned.replace(Regex("[:\\-–—|\\s.\\d{4}]+$"), "")
            
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace("- Topic", "")
            .replace(Regex("(?i)VEVO$"), "")
            .trim()
    }
    
    private suspend fun searchMusicBrainz(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = "$trackTitle artist:$artist"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$MUSICBRAINZ_SEARCH_URL?query=$encodedQuery&fmt=json&limit=1"
            
            Log.d(TAG, "Buscando en MusicBrainz: $query")
            
            val connection = URL(searchUrl).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "VaultBeat/1.0 (Music Player)")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val jsonObject = JSONObject(response)
            val recordings = jsonObject.optJSONArray("recordings")
            
            if (recordings != null && recordings.length() > 0) {
                val firstRecording = recordings.getJSONObject(0)
                val releaseGroups = firstRecording.optJSONArray("release-groups")
                
                if (releaseGroups != null && releaseGroups.length() > 0) {
                    val firstRelease = releaseGroups.getJSONObject(0)
                    val releaseGroupId = firstRelease.optString("id", "")
                    
                    if (releaseGroupId.isNotBlank()) {
                        return@withContext getCoverArtArchive(releaseGroupId)
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }
    
    private suspend fun getCoverArtArchive(releaseGroupId: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://coverartarchive.org/release-group/$releaseGroupId/front-250.jpg"
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
            }
            if (connection.responseCode == 200) url else null
        } catch (e: Exception) { null }
    }
    
    private suspend fun searchLastfm(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTrack = URLEncoder.encode(trackTitle, "UTF-8")
            val url = "$LASTFM_SEARCH_URL?method=track.getinfo&artist=$encodedArtist&track=$encodedTrack&format=json"
            
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val jsonObject = JSONObject(response)
            val image = jsonObject.optJSONObject("track")?.optJSONObject("album")?.optJSONArray("image")
            
            if (image != null && image.length() > 0) {
                var largeImage: String? = null
                for (i in image.length() - 1 downTo 0) {
                    val img = image.getJSONObject(i)
                    val size = img.optString("size", "")
                    val urlText = img.optString("#text", "")
                    if (urlText.isNotBlank() && (size == "extralarge" || size == "large" || largeImage == null)) {
                        largeImage = urlText
                        if (size == "extralarge" || size == "large") break
                    }
                }
                return@withContext largeImage
            }
            null
        } catch (e: Exception) { null }
    }
}
