package com.nextgen.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextgen.player.navigation.NavGraph
import com.nextgen.player.ui.SettingsViewModel
import com.nextgen.player.ui.theme.NextGenPlayerTheme
import com.nextgen.player.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val themeMode = try {
                ThemeMode.valueOf(settings.themeMode.uppercase())
            } catch (_: Exception) {
                ThemeMode.DARK
            }
            val accentColor = if (settings.accentColorHex.isNotEmpty()) {
                try {
                    androidx.compose.ui.graphics.Color(settings.accentColorHex.toLong(16))
                } catch (_: Exception) { null }
            } else null

            NextGenPlayerTheme(
                themeMode = themeMode,
                dynamicColor = settings.dynamicColor,
                accentColor = accentColor
            ) {
                var hasPermission by remember { mutableStateOf(checkMediaPermission()) }
                var showRationale by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                    if (!isGranted) {
                        showRationale = true
                    }
                }

                if (hasPermission) {
                    NavGraph(
                        onPlayMedia = { mediaId, path ->
                            val intent = Intent(this, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_MEDIA_ID, mediaId)
                                putExtra(PlayerActivity.EXTRA_MEDIA_PATH, path)
                            }
                            startActivity(intent)
                        },
                        onPlayMediaFromFolder = { mediaId, path, folderPath ->
                            val intent = Intent(this, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_MEDIA_ID, mediaId)
                                putExtra(PlayerActivity.EXTRA_MEDIA_PATH, path)
                                putExtra(PlayerActivity.EXTRA_FOLDER_PATH, folderPath)
                            }
                            startActivity(intent)
                        },
                        onPlayUrl = { url ->
                            val intent = Intent(this, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_MEDIA_PATH, url)
                            }
                            startActivity(intent)
                        }
                    )
                } else {
                    PermissionScreen(
                        showRationale = showRationale,
                        onRequestPermission = {
                            val permission = getMediaPermission()
                            permissionLauncher.launch(permission)
                        },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    private fun checkMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getMediaPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

@Composable
private fun PermissionScreen(
    showRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "📹",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (showRationale)
                    stringResource(R.string.permission_rationale)
                else
                    stringResource(R.string.permission_prompt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = if (showRationale) onOpenSettings else onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(if (showRationale) stringResource(R.string.permission_open_settings) else stringResource(R.string.permission_grant))
            }
        }
    }
}
