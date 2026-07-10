package com.example.vaultbeat.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.vaultbeat.data.preferences.PlaybackPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VaultBeatPlaybackService : MediaSessionService() {
    @Inject lateinit var preferences: PlaybackPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                        events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                        events.contains(Player.EVENT_IS_PLAYING_CHANGED)
                    ) {
                        // Player methods must be read on Media3's application (main) thread.
                        val mediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
                        val positionMs = player.currentPosition
                        mediaId?.let { id ->
                            serviceScope.launch { preferences.save(id, positionMs) }
                        }
                    }
                }
            })
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        val mediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val positionMs = player.currentPosition
        mediaId?.let { id ->
            serviceScope.launch { preferences.save(id, positionMs) }
        }
        mediaSession?.run {
            release()
        }
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
