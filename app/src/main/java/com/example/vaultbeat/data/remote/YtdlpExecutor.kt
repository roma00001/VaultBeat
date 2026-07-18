package com.example.vaultbeat.data.remote

import com.example.vaultbeat.core.network.model.DownloadProgress
import com.example.vaultbeat.core.network.model.SearchResult
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class YtdlpExecutor @Inject constructor(
    private val iTunesApi: ITunesApi
) {
    
    companion object {
        private const val TAG = "YtdlpExecutor"
        private const val SEARCH_TIMEOUT_MS = 30000L // 30 segundos de timeout
    }
    
    suspend fun getBestThumbnail(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val query = "$artist - $title"
        try {
            val request = YoutubeDLRequest("ytsearch1:$query").apply {
                addOption("--dump-json")
                addOption("--no-download")
                addOption("--skip-download")
                addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                addOption("--force-ipv4")
            }
            
            val result = withTimeoutOrNull(15000L) {
                YoutubeDL.getInstance().execute(request)
            }
            
            if (result != null && !result.out.isNullOrBlank()) {
                val json = JSONObject(result.out)
                val thumb = json.optString("thumbnail", "")
                if (thumb.isNotBlank() && thumb != "null") return@withContext thumb
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching thumbnail with yt-dlp for $query: ${e.message}")
        }
        null
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {

        val isUrl = query.startsWith("http") && (query.contains("youtube.com") || query.contains("youtu.be"))
        val searchPrefix = if (isUrl) "" else "ytsearch25:" // Aumentado a 25 resultados

        try {
            val searchResult = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                val request = YoutubeDLRequest("$searchPrefix$query").apply {
                    addOption("--dump-json")
                    addOption("--no-download")
                    addOption("--skip-download")
                    addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    addOption("--socket-timeout", "20")
                    addOption("--no-warnings")
                    addOption("--force-ipv4")
                    addOption("--ignore-errors") // CRÍTICO: No abortar si un video no está disponible
                    addOption("--no-check-certificates")
                    addOption("--rm-cache-dir")
                    addOption("--referer", "https://www.google.com/")
                    addOption("--extractor-args", "youtube:player-client=web,web_embedded,tv")
                    
                    if (isUrl) {
                        addOption("--yes-playlist")
                        addOption("--flat-playlist") // Obtener lista rápido sin entrar en cada video inicialmente
                    } else {
                        addOption("--flat-playlist")
                        addOption("--playlist-end", "25")
                    }
                }
                
                Log.d(TAG, "Iniciando búsqueda robusta para: $query")
                YoutubeDL.getInstance().execute(request)
            }
            
            if (searchResult == null || searchResult.out.isNullOrBlank()) {
                return@withContext emptyList()
            }
            
            val lines = searchResult.out.lines().filter { it.isNotBlank() }
            
            coroutineScope {
                lines.map { line ->
                    async {
                        try {
                            val json = JSONObject(line)
                            val id = json.optString("id", "").takeIf { it.isNotBlank() } ?: return@async null
                            
                            // En modo flat-playlist, algunos campos pueden variar
                            val title = json.optString("track", "").takeIf { it.isNotBlank() } 
                                ?: json.optString("title", "").takeIf { it.isNotBlank() } 
                                ?: return@async null
                                
                            val artist = json.optString("artist", "").takeIf { it.isNotBlank() }
                                ?: json.optString("uploader", "Unknown Artist")
                            
                            // Timeout para carátula de alta calidad
                            val realArtwork = withTimeoutOrNull(3000L) {
                                iTunesApi.getArtworkUrl(artist, title)
                            }
                            
                            val thumb = json.optString("thumbnail", "").takeIf { 
                                (it.isNotBlank() && it != "null")
                            } ?: "https://img.youtube.com/vi/$id/hqdefault.jpg"

                            SearchResult(
                                id = id,
                                title = title,
                                artist = artist,
                                durationMs = json.optLong("duration", 0) * 1000,
                                thumbnailUrl = realArtwork ?: thumb,
                                url = "https://www.youtube.com/watch?v=$id"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fatal en búsqueda: ${e.message}")
            emptyList()
        }
    }

    fun download(url: String, outputPath: String): Flow<DownloadProgress> = callbackFlow {
        val request = YoutubeDLRequest(url).apply {
            addOption("-f", "bestaudio/best") // Seleccionar el mejor audio directamente
            addOption("-x")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", "5") // Calidad estándar para una conversión mucho más rápida
            addOption("--embed-metadata")
            addOption("--embed-thumbnail")
            addOption("--convert-thumbnails", "jpg")
            addOption("-o", "$outputPath/%(title)s.%(ext)s")
            addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            addOption("--referer", "https://www.google.com/")
            addOption("--extractor-args", "youtube:player-client=web,web_embedded,tv")
            addOption("--rm-cache-dir")
            addOption("--force-ipv4")
            addOption("--no-check-certificates")
            addOption("--no-mtime")
            addOption("--buffer-size", "1M") // Buffer de red optimizado para conexiones modernas
            addOption("--external-downloader", "aria2c")
            // Optimización agresiva de aria2c: 16 hilos, 16 conexiones, tamaño de split pequeño para paralizar más rápido
            addOption("--external-downloader-args", "aria2c:-x 16 -s 16 -j 16 -k 1M --min-split-size=1M --check-certificate=false --file-allocation=none")
        }
        
        try {
            YoutubeDL.getInstance().execute(request) { progress, eta, speed ->
                trySend(DownloadProgress.Downloading(progress, speed))
            }
            trySend(DownloadProgress.Completed)
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error en descarga: ${e.message}", e)
            trySend(DownloadProgress.Error(e.message ?: "Unknown error"))
            close()
        }
        awaitClose { /* No-op, YoutubeDL library handles process termination */ }
    }.flowOn(Dispatchers.IO)
}
