package com.vaultbeat.data.remote

import android.util.Log
import com.vaultbeat.core.utils.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepository @Inject constructor() {
    
    companion object {
        private const val TAG = "MetadataRepository"
        private const val MUSICBRAINZ_SEARCH_URL = "https://musicbrainz.org/ws/2/recording"
        private const val LASTFM_SEARCH_URL = "https://ws.audioscrobbler.com/2.0"
        private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    }
    
    suspend fun getArtworkUrl(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val uploaderArtist = MetadataUtils.cleanArtist(artist)
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
            
            val finalArtist = MetadataUtils.cleanArtist(detectedArtist)
            val finalTitle = MetadataUtils.cleanTitle(detectedTitle, finalArtist)
            
            Log.d(TAG, "Searching artwork for: $finalArtist - $finalTitle")
            
            // 1. iTunes (Best quality/coverage)
            val itunesUrl = searchITunes(finalArtist, finalTitle)
            if (itunesUrl != null) return@withContext itunesUrl

            // 2. MusicBrainz (Good for metadata)
            val mbUrl = searchMusicBrainz(finalArtist, finalTitle)
            if (mbUrl != null) return@withContext mbUrl
            
            // 3. Last.fm (Fallback)
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
            
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val jsonObject = JSONObject(response)
            val results = jsonObject.optJSONArray("results")
            
            if (results != null && results.length() > 0) {
                val artworkUrl = results.getJSONObject(0).optString("artworkUrl100", "")
                if (artworkUrl.isNotBlank()) {
                    return@withContext artworkUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun searchMusicBrainz(artist: String, trackTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = "$trackTitle artist:$artist"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$MUSICBRAINZ_SEARCH_URL?query=$encodedQuery&fmt=json&limit=1"
            
            val connection = URL(searchUrl).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "VaultBeat/1.0")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val recordings = JSONObject(response).optJSONArray("recordings")
            if (recordings != null && recordings.length() > 0) {
                val releaseGroupId = recordings.getJSONObject(0)
                    .optJSONArray("release-groups")?.optJSONObject(0)?.optString("id", "")
                
                if (!releaseGroupId.isNullOrBlank()) {
                    return@withContext getCoverArtArchive(releaseGroupId)
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
                connectTimeout = 5000
                readTimeout = 5000
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
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            val image = JSONObject(response).optJSONObject("track")
                ?.optJSONObject("album")?.optJSONArray("image")
            
            if (image != null && image.length() > 0) {
                for (i in image.length() - 1 downTo 0) {
                    val img = image.getJSONObject(i)
                    val urlText = img.optString("#text", "")
                    if (urlText.isNotBlank()) return@withContext urlText
                }
            }
            null
        } catch (e: Exception) { null }
    }
}
