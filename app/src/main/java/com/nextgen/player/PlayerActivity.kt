package com.nextgen.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nextgen.player.data.SettingsRepository
import com.nextgen.player.player.PlayerEngine
import com.nextgen.player.ui.PlayerScreen
import com.nextgen.player.ui.theme.NextGenPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_MEDIA_PATH = "media_path"
        const val EXTRA_FOLDER_PATH = "folder_path"
        private const val ACTION_PIP_CONTROL = "com.nextgen.player.PIP_CONTROL"
        private const val EXTRA_PIP_ACTION = "pip_action"
        private const val PIP_PLAY_PAUSE = 1
        private const val PIP_REWIND = 2
        private const val PIP_FORWARD = 3
    }

    @Inject lateinit var playerEngine: PlayerEngine
    @Inject lateinit var settingsRepository: SettingsRepository

    private var mediaId: Long = -1L
    private var mediaPath: String = ""
    private var folderPath: String? = null
    private var externalUri: Uri? = null
    private var isInPiPMode by mutableStateOf(false)
    private var autoPiP = true

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                when (intent.getIntExtra(EXTRA_PIP_ACTION, 0)) {
                    PIP_PLAY_PAUSE -> playerEngine.togglePlayPause()
                    PIP_REWIND -> playerEngine.seekBackward()
                    PIP_FORWARD -> playerEngine.seekForward()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updatePiPParams()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        resolveIntent(intent)

        // Load auto-PiP preference
        try {
            autoPiP = runBlocking { settingsRepository.settings.first().autoPiP }
        } catch (_: Exception) { }

        // Register PiP control receiver
        ContextCompat.registerReceiver(
            this, pipReceiver, IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            NextGenPlayerTheme(darkTheme = true) {
                PlayerScreen(
                    mediaId = mediaId,
                    mediaPath = mediaPath,
                    onBackPressed = { finish() },
                    onEnterPiP = { enterPiPMode() },
                    isInPiPMode = isInPiPMode,
                    folderPath = folderPath
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resolveIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
        mediaPath = intent.getStringExtra(EXTRA_MEDIA_PATH) ?: ""
        folderPath = intent.getStringExtra(EXTRA_FOLDER_PATH)

        if (mediaPath.isEmpty() && intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                externalUri = uri
                mediaPath = uri.toString()
            }
        }
    }

    private fun applyImmersiveMode() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyImmersiveMode()
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePiPParams()
            enterPictureInPictureMode(buildPiPParams())
        }
    }

    private fun buildPiPParams(): PictureInPictureParams {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(buildPiPActions())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(autoPiP)
                builder.setSeamlessResizeEnabled(true)
            }
            return builder.build()
        }
        throw IllegalStateException("PiP not supported")
    }

    private fun updatePiPParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPiPParams())
        }
    }

    private fun buildPiPActions(): List<RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        val actions = mutableListOf<RemoteAction>()

        // Rewind
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                "Rewind", "Rewind 10 seconds",
                PendingIntent.getBroadcast(
                    this, PIP_REWIND,
                    Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_REWIND),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Play/Pause
        val isPlaying = playerEngine.player?.isPlaying == true
        actions.add(
            RemoteAction(
                Icon.createWithResource(
                    this,
                    if (isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                ),
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "Pause playback" else "Resume playback",
                PendingIntent.getBroadcast(
                    this, PIP_PLAY_PAUSE,
                    Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_PLAY_PAUSE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Forward
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                "Forward", "Forward 10 seconds",
                PendingIntent.getBroadcast(
                    this, PIP_FORWARD,
                    Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_FORWARD),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        return actions
    }

    override fun onPictureInPictureModeChanged(isInPiP: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig)
        isInPiPMode = isInPiP
        if (!isInPiP) {
            applyImmersiveMode()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (autoPiP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // On pre-S, use manual PiP entry (S+ uses setAutoEnterEnabled)
                enterPiPMode()
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(pipReceiver) } catch (_: Exception) { }
        super.onDestroy()
    }
}
