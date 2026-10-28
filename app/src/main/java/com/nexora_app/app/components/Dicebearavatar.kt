package com.nexora_app.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.nexora_app.app.models.AvatarOptions
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

/**
 * Renders a single DiceBear avatar for the given seed. Replaces the old
 * Canvas-drawn MascotAvatar — same call-site shape (seed + size in, circular
 * image out) so swapping it into existing screens is a near drop-in.
 *
 * Uses SubcomposeAsyncImage (Coil) so we can show a small spinner while the
 * network image loads instead of a blank circle — avatars are small enough
 * that a loading flash would look broken otherwise.
 */
@Composable
fun DiceBearAvatar(
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = AvatarOptions.urlFor(seed),
        contentDescription = "Avatar",
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(AppColors.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.3f),
                    strokeWidth = 2.dp,
                    color = AppColors.primary
                )
            }
        },
        error = {
            // Network hiccup / bad seed — flat colored circle so the layout
            // never collapses to nothing.
            Box(
                modifier = Modifier
                    .size(size)
                    .background(AppColors.surfaceVariant, CircleShape)
            )
        },
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

@Composable
fun DiceBearAvatarOption(
    seed: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val tileSize = dimens.avatarMedium + dimens.spaceXS * 2

    Box(
        modifier = modifier
            .size(tileSize)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(AppColors.primary.copy(alpha = 0.14f))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiceBearAvatar(seed = seed, size = dimens.avatarMedium)

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dimens.iconS)
                    .clip(CircleShape)
                    .background(AppColors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(dimens.iconXS)
                )
            }
        }
    }
}