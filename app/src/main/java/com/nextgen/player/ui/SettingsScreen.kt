package com.nextgen.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextgen.player.R
import com.nextgen.player.player.PlayerEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSpeedPicker by remember { mutableStateOf(false) }
    var showFontSizePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(stringResource(R.string.settings_section_player)) {
                SwitchSetting(
                    title = stringResource(R.string.settings_resume_playback),
                    subtitle = stringResource(R.string.settings_resume_subtitle),
                    icon = Icons.Rounded.PlayCircle,
                    checked = settings.resumePlayback,
                    onCheckedChange = { viewModel.setResumePlayback(it) }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_hw_decoding),
                    subtitle = stringResource(R.string.settings_hw_subtitle),
                    icon = Icons.Rounded.Memory,
                    checked = settings.hardwareDecoding,
                    onCheckedChange = { viewModel.setHardwareDecoding(it) }
                )

                ClickSetting(
                    title = stringResource(R.string.settings_default_speed),
                    subtitle = "${settings.defaultSpeed}x",
                    icon = Icons.Rounded.Speed,
                    onClick = { showSpeedPicker = true }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_auto_pip),
                    subtitle = stringResource(R.string.settings_auto_pip_desc),
                    icon = Icons.Rounded.PictureInPictureAlt,
                    checked = settings.autoPiP,
                    onCheckedChange = { viewModel.setAutoPiP(it) }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_auto_play_next),
                    subtitle = stringResource(R.string.settings_auto_play_next_desc),
                    icon = Icons.Rounded.SkipNext,
                    checked = settings.autoPlayNext,
                    onCheckedChange = { viewModel.setAutoPlayNext(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(stringResource(R.string.settings_section_subtitles)) {
                ClickSetting(
                    title = stringResource(R.string.settings_font_size),
                    subtitle = "${settings.subtitleFontSize.toInt()}sp",
                    icon = Icons.Rounded.TextFields,
                    onClick = { showFontSizePicker = true }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_subtitle_bg),
                    subtitle = stringResource(R.string.settings_subtitle_bg_desc),
                    icon = Icons.Rounded.FormatColorFill,
                    checked = settings.subtitleBackground,
                    onCheckedChange = { viewModel.setSubtitleBackground(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(stringResource(R.string.settings_section_appearance)) {
                SwitchSetting(
                    title = stringResource(R.string.settings_dark_theme),
                    subtitle = stringResource(R.string.settings_dark_theme_desc),
                    icon = Icons.Rounded.DarkMode,
                    checked = settings.darkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(stringResource(R.string.settings_section_about)) {
                ClickSetting(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_version),
                    icon = Icons.Rounded.Info,
                    onClick = { }
                )

                ClickSetting(
                    title = stringResource(R.string.settings_privacy_title),
                    subtitle = stringResource(R.string.settings_privacy_desc),
                    icon = Icons.Rounded.PrivacyTip,
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSpeedPicker) {
        SpeedPickerDialog(
            currentSpeed = settings.defaultSpeed,
            onSpeedSelected = {
                viewModel.setDefaultSpeed(it)
                showSpeedPicker = false
            },
            onDismiss = { showSpeedPicker = false }
        )
    }

    if (showFontSizePicker) {
        FontSizePickerDialog(
            currentSize = settings.subtitleFontSize,
            onSizeSelected = {
                viewModel.setSubtitleFontSize(it)
                showFontSizePicker = false
            },
            onDismiss = { showFontSizePicker = false }
        )
    }
}

@Composable
private fun SpeedPickerDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_speed_title)) },
        text = {
            Column {
                PlayerEngine.PLAYBACK_SPEEDS.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${speed}x",
                            fontWeight = if (currentSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentSpeed == speed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun FontSizePickerDialog(
    currentSize: Float,
    onSizeSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = listOf(12f, 14f, 16f, 18f, 20f, 22f, 24f, 28f, 32f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_subtitle_font_size)) },
        text = {
            Column {
                sizes.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelected(size) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSize == size,
                            onClick = { onSizeSelected(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${size.toInt()}sp",
                            fontWeight = if (currentSize == size) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentSize == size)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    )
}

@Composable
private fun ClickSetting(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}
