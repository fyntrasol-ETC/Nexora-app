package com.nexora_app.app.screens.videos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.nexora_app.app.models.PlaybackSpeed
import com.nexora_app.app.models.VideoCatalog
import com.nexora_app.app.models.tint
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.ui.viewmodel.VideoPlayerController
import com.nexora_app.app.ui.viewmodel.formattedAsPlaybackTime

/**
 * Full-screen player, opened from VideoFeedScreen's play button. Matches
 * iOS VideoPlayerScreen: tap surface to reveal/toggle controls, center
 * play/pause icon while showing, top X to close, bottom overlay with
 * title + scrub bar + speed menu.
 *
 * Needs androidx.media3:media3-exoplayer + media3-ui in build.gradle.
 */
@Composable
fun VideoPlayerScreen(navController: NavHostController, videoId: String?) {
    val dimens = LocalDimens.current
    val context = LocalContext.current

    val video = remember(videoId) { VideoCatalog.all.firstOrNull { it.id == videoId } }

    if (video == null) {
        // Shouldn't happen from normal navigation, but fail safely.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val controller = remember(video.id) { VideoPlayerController(context, video) }

    DisposableEffect(video.id) {
        controller.setUpPlayer()
        onDispose { controller.tearDown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controller.handleTap() }
    ) {
        // Player surface / fallback tint if missing or errored
        if (!controller.fileMissing && !controller.playerError) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        useController = false // we draw our own controls
                    }
                },
                update = { view ->
                    // Re-runs whenever controller.player (Compose state)
                    // changes — this is what actually attaches the
                    // ExoPlayer once setUpPlayer() finishes creating it.
                    // A plain `factory`-only assignment would run before
                    // the player exists and PlayerView would stay blank.
                    view.player = controller.player
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(video.topic.tint()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (controller.fileMissing) {
                            "${video.id}.mp4 not found in res/raw"
                        } else {
                            "Couldn't load this video"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Center play/pause icon — no click handling of its own, so taps
        // fall through to the Box's clickable above (same as iOS's
        // .allowsHitTesting(false)).
        AnimatedVisibility(
            visible = controller.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimens.iconXL)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = controller.showControls, enter = fadeIn(), exit = fadeOut()) {
                TopBar(onClose = { navController.popBackStack() })
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = controller.showControls, enter = fadeIn(), exit = fadeOut()) {
                BottomOverlay(
                    title = video.title,
                    subtitle = video.subtitle,
                    controller = controller
                )
            }
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.spaceM),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(dimens.iconXL * 0.5f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(dimens.iconS)
            )
        }
    }
}

@Composable
private fun BottomOverlay(
    title: String,
    subtitle: String,
    controller: VideoPlayerController
) {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.65f))
                )
            )
            .padding(horizontal = dimens.screenPaddingHorizontal)
            .padding(bottom = dimens.spaceXL, top = dimens.spaceL)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(dimens.spaceS))

        val duration = controller.durationMs.coerceAtLeast(100L)
        Slider(
            value = controller.currentPositionMs.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { controller.scrub(it.toLong()) },
            onValueChangeFinished = { controller.endScrubbing(controller.currentPositionMs) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = controller.currentPositionMs.formattedAsPlaybackTime(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = controller.durationMs.formattedAsPlaybackTime(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        Spacer(Modifier.height(dimens.spaceS))

        SpeedMenu(
            current = controller.speed,
            onSelect = { controller.speed = it }
        )
    }
}

@Composable
private fun SpeedMenu(current: PlaybackSpeed, onSelect: (PlaybackSpeed) -> Unit) {
    val dimens = LocalDimens.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(dimens.radiusPill))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = true }
                .padding(horizontal = dimens.spaceM, vertical = dimens.spaceXS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXS)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimens.iconXS)
            )
            Text(
                text = current.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColors.surface
        ) {
            PlaybackSpeed.values().forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (option == current) AppColors.primary else AppColors.textPrimary,
                            fontWeight = if (option == current) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}