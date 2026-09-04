package com.nextgen.player.subtitle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SubtitleDisplayConfig(
    val fontSize: Float = 18f,
    val fontFamily: String = "sans",
    val fontColor: Color = Color.White,
    val backgroundColor: Color = Color(0xAA000000),
    val outlineColor: Color = Color.Black,
    val shadowColor: Color = Color.Black,
    val outlineWidth: Float = 2f,
    val bottomPadding: Float = 48f,
    val yOffset: Float = 48f,
    val showBackground: Boolean = true,
    val shadowEnabled: Boolean = true,
    val position: SubtitleVerticalPosition = SubtitleVerticalPosition.BOTTOM
)

@Composable
fun SubtitleOverlay(
    currentPositionMs: Long,
    cues: List<SubtitleCue>,
    syncOffsetMs: Long = 0L,
    config: SubtitleDisplayConfig = SubtitleDisplayConfig(),
    position: SubtitleVerticalPosition = config.position,
    modifier: Modifier = Modifier
) {
    val activeCue = remember(currentPositionMs, syncOffsetMs) {
        val adjustedPosition = currentPositionMs - syncOffsetMs
        cues.firstOrNull { cue ->
            adjustedPosition >= cue.startTimeMs && adjustedPosition <= cue.endTimeMs
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = when (position) {
            SubtitleVerticalPosition.TOP -> Alignment.TopCenter
            SubtitleVerticalPosition.BOTTOM -> Alignment.BottomCenter
            SubtitleVerticalPosition.CUSTOM -> Alignment.TopCenter
        }
    ) {
        if (activeCue != null) {
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = when (position) {
                            SubtitleVerticalPosition.TOP -> config.yOffset.dp
                            SubtitleVerticalPosition.BOTTOM -> config.bottomPadding.dp
                            SubtitleVerticalPosition.CUSTOM -> config.yOffset.dp
                        }
                    )
                    .then(
                        if (config.showBackground) {
                            Modifier.background(
                                config.backgroundColor,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedSubtitleText(activeCue, config)
            }
        }
    }
}

@Composable
private fun OutlinedSubtitleText(
    cue: SubtitleCue,
    config: SubtitleDisplayConfig
) {
    val textColor = cue.style.primaryColor?.let { Color(it) } ?: config.fontColor
    val outlineColor = cue.style.outlineColor?.let { Color(it) } ?: config.outlineColor
    val shadowColor = cue.style.shadowColor?.let { Color(it) } ?: config.shadowColor
    val style = TextStyle(
        fontSize = (cue.style.fontSize ?: config.fontSize).sp,
        color = textColor,
        fontFamily = when (cue.style.fontName ?: config.fontFamily) {
            "serif" -> FontFamily.Serif
            "mono" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        },
        fontWeight = if (cue.style.bold) FontWeight.Bold else FontWeight.Medium,
        textDecoration = if (cue.style.underline) TextDecoration.Underline else TextDecoration.None,
        textAlign = TextAlign.Center,
        shadow = if (config.shadowEnabled || cue.style.shadowEnabled) {
            Shadow(color = shadowColor, offset = Offset(2f, 2f), blurRadius = 4f)
        } else {
            null
        }
    )

    if (config.outlineWidth > 0f || cue.style.outlineWidth > 0f) {
        Text(
            text = cue.text,
            style = style.copy(
                color = outlineColor,
                drawStyle = Stroke(width = cue.style.outlineWidth.takeIf { it > 0f } ?: config.outlineWidth)
            ),
            textAlign = TextAlign.Center
        )
    }

    Text(
        text = cue.text,
        style = style,
        textAlign = TextAlign.Center
    )
}
