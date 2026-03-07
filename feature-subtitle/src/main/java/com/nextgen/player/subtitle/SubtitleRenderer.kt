package com.nextgen.player.subtitle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SubtitleDisplayConfig(
    val fontSize: Float = 18f,
    val fontColor: Color = Color.White,
    val backgroundColor: Color = Color(0xAA000000),
    val outlineColor: Color = Color.Black,
    val bottomPadding: Float = 48f,
    val showBackground: Boolean = true
)

@Composable
fun SubtitleOverlay(
    currentPositionMs: Long,
    cues: List<SubtitleCue>,
    syncOffsetMs: Long = 0L,
    config: SubtitleDisplayConfig = SubtitleDisplayConfig(),
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
        contentAlignment = Alignment.BottomCenter
    ) {
        if (activeCue != null) {
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = config.bottomPadding.dp
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
                Text(
                    text = activeCue.text,
                    style = TextStyle(
                        fontSize = config.fontSize.sp,
                        color = config.fontColor,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
