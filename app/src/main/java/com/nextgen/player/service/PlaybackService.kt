package com.nextgen.player.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nextgen.player.player.PlayerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerEngine: PlayerEngine

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        playerEngine.initialize()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playerEngine.mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerEngine.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
