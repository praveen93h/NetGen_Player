package com.nextgen.player.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var showThemePicker by remember { mutableStateOf(false) }
    var showAccentPicker by remember { mutableStateOf(false) }
    var showDoubleTapPicker by remember { mutableStateOf(false) }

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
                ClickSetting(
                    title = stringResource(R.string.settings_theme_mode),
                    subtitle = themeDisplayName(settings.themeMode),
                    icon = Icons.Rounded.Palette,
                    onClick = { showThemePicker = true }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchSetting(
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_desc),
                        icon = Icons.Rounded.ColorLens,
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }

                if (!settings.dynamicColor) {
                    ClickSetting(
                        title = stringResource(R.string.settings_accent_color),
                        subtitle = if (settings.accentColorHex.isNotEmpty())
                            stringResource(R.string.settings_accent_custom)
                        else
                            stringResource(R.string.settings_accent_default),
                        icon = Icons.Rounded.Brush,
                        onClick = { showAccentPicker = true }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(stringResource(R.string.settings_section_gestures)) {
                ClickSetting(
                    title = stringResource(R.string.settings_double_tap_seek),
                    subtitle = "${settings.doubleTapSeekDuration}s",
                    icon = Icons.Rounded.TouchApp,
                    onClick = { showDoubleTapPicker = true }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_swipe_brightness),
                    subtitle = stringResource(R.string.settings_swipe_brightness_desc),
                    icon = Icons.Rounded.BrightnessHigh,
                    checked = settings.swipeBrightnessEnabled,
                    onCheckedChange = { viewModel.setSwipeBrightnessEnabled(it) }
                )

                SwitchSetting(
                    title = stringResource(R.string.settings_swipe_volume),
                    subtitle = stringResource(R.string.settings_swipe_volume_desc),
                    icon = Icons.Rounded.VolumeUp,
                    checked = settings.swipeVolumeEnabled,
                    onCheckedChange = { viewModel.setSwipeVolumeEnabled(it) }
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

    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = settings.themeMode,
            onModeSelected = {
                viewModel.setThemeMode(it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showAccentPicker) {
        AccentColorPickerDialog(
            currentHex = settings.accentColorHex,
            onColorSelected = {
                viewModel.setAccentColorHex(it)
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false }
        )
    }

    if (showDoubleTapPicker) {
        DoubleTapSeekPickerDialog(
            currentDuration = settings.doubleTapSeekDuration,
            onDurationSelected = {
                viewModel.setDoubleTapSeekDuration(it)
                showDoubleTapPicker = false
            },
            onDismiss = { showDoubleTapPicker = false }
        )
    }
}

@Composable
private fun themeDisplayName(mode: String): String = when (mode.lowercase()) {
    "dark" -> stringResource(R.string.theme_dark)
    "pure_black" -> stringResource(R.string.theme_pure_black)
    "midnight_blue" -> stringResource(R.string.theme_midnight_blue)
    "light" -> stringResource(R.string.theme_light)
    "system" -> stringResource(R.string.theme_system)
    else -> stringResource(R.string.theme_dark)
}

@Composable
private fun ThemePickerDialog(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf("dark", "pure_black", "midnight_blue", "light", "system")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_mode)) },
        text = {
            Column {
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            themeDisplayName(mode),
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentMode == mode)
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

private val accentColors = listOf(
    "FF6200EE" to "Purple",
    "FF03DAC5" to "Teal",
    "FFBB86FC" to "Lavender",
    "FFFF5722" to "Deep Orange",
    "FF4CAF50" to "Green",
    "FF2196F3" to "Blue",
    "FFE91E63" to "Pink",
    "FFFFEB3B" to "Yellow",
    "FF00BCD4" to "Cyan",
    "FF9C27B0" to "Violet",
    "FFFF9800" to "Orange",
    "FF607D8B" to "Blue Grey"
)

@Composable
private fun AccentColorPickerDialog(
    currentHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_accent_color)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onColorSelected("") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentHex.isEmpty(),
                        onClick = { onColorSelected("") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_accent_default),
                        fontWeight = if (currentHex.isEmpty()) FontWeight.Bold else FontWeight.Normal
                    )
                }
                @Suppress("DEPRECATION")
                val chunked = accentColors.chunked(4)
                chunked.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { (hex, name) ->
                            val color = Color(hex.toLong(16))
                            val isSelected = currentHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { onColorSelected(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
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
private fun DoubleTapSeekPickerDialog(
    currentDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val durations = listOf(5, 10, 15, 20, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_double_tap_seek)) },
        text = {
            Column {
                durations.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDurationSelected(seconds) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentDuration == seconds,
                            onClick = { onDurationSelected(seconds) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${seconds}s",
                            fontWeight = if (currentDuration == seconds) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentDuration == seconds)
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
