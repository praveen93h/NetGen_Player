package com.nextgen.player.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextgen.player.ui.theme.Orange500
import kotlinx.coroutines.delay

enum class GuidedDemoSurface {
    HOME,
    PLAYER
}

data class GuidedDemoStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetLabel: String
)

@Composable
fun GuidedDemoPromptDialog(
    title: String,
    message: String,
    startText: String,
    dismissText: String,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.TipsAndUpdates, null, tint = Orange500) },
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Orange500)
            ) {
                Text(startText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun GuidedDemoDialog(
    title: String,
    steps: List<GuidedDemoStep>,
    surface: GuidedDemoSurface,
    doneText: String,
    previousText: String,
    nextText: String,
    onDismiss: () -> Unit
) {
    if (steps.isEmpty()) return

    var currentStep by remember(steps) { mutableIntStateOf(0) }
    val safeStep = steps[currentStep.coerceIn(0, steps.lastIndex)]

    LaunchedEffect(currentStep, steps.size) {
        delay(3000)
        currentStep = (currentStep + 1) % steps.size
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DemoPreview(
                    surface = surface,
                    stepIndex = currentStep,
                    stepCount = steps.size
                )

                Crossfade(targetState = safeStep, label = "guided-demo-copy") { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(step.icon, null, tint = Orange500, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step.targetLabel,
                                    color = Orange500,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = step.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                StepDots(
                    currentStep = currentStep,
                    stepCount = steps.size,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStep == steps.lastIndex) onDismiss()
                    else currentStep += 1
                },
                colors = ButtonDefaults.buttonColors(containerColor = Orange500)
            ) {
                Text(if (currentStep == steps.lastIndex) doneText else nextText, color = Color.White)
            }
        },
        dismissButton = {
            Row {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep -= 1 }) {
                        Text(previousText)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(doneText)
                }
            }
        }
    )
}

@Composable
private fun DemoPreview(
    surface: GuidedDemoSurface,
    stepIndex: Int,
    stepCount: Int
) {
    val targets = if (surface == GuidedDemoSurface.HOME) homeTargets else playerTargets
    val target = targets[stepIndex % targets.size]
    val markerX by animateDpAsState(targetValue = target.x, animationSpec = tween(520), label = "demo-marker-x")
    val markerY by animateDpAsState(targetValue = target.y, animationSpec = tween(520), label = "demo-marker-y")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF090909)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF151515), Color.Black)
                    )
                )
                .padding(12.dp)
        ) {
            if (surface == GuidedDemoSurface.HOME) {
                HomeDemoMock()
            } else {
                PlayerDemoMock()
            }
            PulseMarker(x = markerX, y = markerY)
        }
    }
}

@Composable
private fun HomeDemoMock() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .width(112.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(62.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                )
            }
            listOf(Icons.Rounded.Search, Icons.Rounded.GridView, Icons.Rounded.Refresh, Icons.Rounded.Lan, Icons.Rounded.Settings).forEach { icon ->
                DemoIcon(icon)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Folders", "Recent", "Favorites").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (index == 0) Orange500 else Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (index == 1) Color(0xFF202020) else Color(0xFF151515)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Orange500.copy(alpha = 0.9f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoPill(Icons.Rounded.Folder, "Folders")
            DemoPill(Icons.Rounded.TipsAndUpdates, "Guide")
        }
    }
}

@Composable
private fun PlayerDemoMock() {
    Box(Modifier.fillMaxWidth().height(190.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111))
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.32f))
            )
            listOf(Icons.Rounded.Subtitles, Icons.Rounded.Tune, Icons.Rounded.Pause).forEach { icon ->
                DemoIcon(icon)
            }
        }
        Icon(
            Icons.Rounded.PlayArrow,
            null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp)
                .clip(CircleShape)
                .background(Orange500)
                .padding(11.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.42f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Orange500)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoPill(Icons.Rounded.Speed, "1x")
                DemoPill(Icons.Rounded.AspectRatio, "Fit")
                DemoPill(Icons.Rounded.PictureInPictureAlt, "PiP")
                DemoPill(Icons.Rounded.Equalizer, "EQ")
                DemoPill(Icons.Rounded.Tune, "FX")
            }
        }
    }
}

@Composable
private fun DemoIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .padding(start = 5.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun DemoPill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Orange500, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun PulseMarker(x: Dp, y: Dp) {
    val transition = rememberInfiniteTransition(label = "demo-pulse")
    val scale by transition.animateFloat(
        initialValue = 0.76f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "demo-pulse-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "demo-pulse-alpha"
    )

    Box(
        modifier = Modifier
            .offset(x, y)
            .size(38.dp * scale)
            .clip(CircleShape)
            .background(Orange500.copy(alpha = 0.18f))
            .border(2.dp, Orange500.copy(alpha = alpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Orange500)
        )
    }
}

@Composable
private fun StepDots(
    currentStep: Int,
    stepCount: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(stepCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 18.dp else 7.dp, 7.dp)
                    .clip(CircleShape)
                    .background(if (index == currentStep) Orange500 else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

private data class DemoTarget(val x: Dp, val y: Dp)

private val homeTargets = listOf(
    DemoTarget(142.dp, 10.dp),
    DemoTarget(226.dp, 10.dp),
    DemoTarget(36.dp, 48.dp),
    DemoTarget(118.dp, 118.dp),
    DemoTarget(12.dp, 168.dp),
    DemoTarget(252.dp, 10.dp)
)

private val playerTargets = listOf(
    DemoTarget(128.dp, 75.dp),
    DemoTarget(186.dp, 8.dp),
    DemoTarget(38.dp, 88.dp),
    DemoTarget(36.dp, 140.dp),
    DemoTarget(152.dp, 164.dp),
    DemoTarget(236.dp, 164.dp)
)
