package com.nexora_app.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexora_app.app.components.BackButton
import com.nexora_app.app.models.BadgeCatalog
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.CourseBadge
import com.nexora_app.app.models.CourseCatalog
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import androidx.compose.ui.draw.clip

/**
 * One badge per course (see BadgeCatalog). [progressByCourseId] maps
 * courseId -> completed chapter numbers, used only to show "3/6 chapters"
 * in the detail dialog for locked badges. [unlockedBadgeIds] is the
 * source of truth for what's actually earned (from users/{uid}/badges).
 */
@Composable
fun BadgesScreen(
    unlockedBadgeIds: Set<String>,
    progressByCourseId: Map<String, List<Int>>,
    onBack: () -> Unit,
    onGoToCourse: (Course) -> Unit
) {
    val dimens = LocalDimens.current
    var selectedBadge by remember { mutableStateOf<CourseBadge?>(null) }
    val badges = remember { BadgeCatalog.badges }
    val unlockedCount = badges.count { it.id in unlockedBadgeIds }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(top = dimens.screenPaddingVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(dimens.spaceM))
                Text(
                    "Badges",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.onGradient,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                ) {
                    Text(
                        "$unlockedCount/${badges.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = AppColors.crownGold,
                        modifier = Modifier.size(dimens.iconXS)
                    )
                }
            }

            Spacer(Modifier.height(dimens.spaceL))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    horizontal = dimens.screenPaddingHorizontal,
                    vertical = dimens.spaceS
                ),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceL),
                modifier = Modifier.weight(1f)
            ) {
                items(badges, key = { it.id }) { badge ->
                    BadgeTile(
                        badge = badge,
                        unlocked = badge.id in unlockedBadgeIds,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        selectedBadge?.let { badge ->
            val unlocked = badge.id in unlockedBadgeIds
            val completedChapters = progressByCourseId[badge.courseId]?.distinct()?.size ?: 0
            BadgeDetailDialog(
                badge = badge,
                unlocked = unlocked,
                completedChapters = completedChapters,
                onDismiss = { selectedBadge = null },
                onGoToCourse = {
                    selectedBadge = null
                    CourseCatalog.courseById(badge.courseId)?.let(onGoToCourse)
                }
            )
        }
    }
}

@Composable
private fun BadgeTile(badge: CourseBadge, unlocked: Boolean, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    val tint = badge.accent.tint
    val circleSize = dimens.iconXL

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Outer box is intentionally NOT clipped — it exists only so the
        // lock badge below can be aligned to the BottomEnd corner of this
        // square bounding area. A circle's corners fall outside its own
        // radius, so when the lock badge was a child of the circle-clipped
        // Box, that clip sliced the badge off right where it poked past
        // the circle's edge — that was the cropping bug.
        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .shadow(if (unlocked) dimens.cardElevation * 3 else 0.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) {
                            Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f)))
                        } else {
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.10f))
                            )
                        }
                    )
                    .border(
                        1.5.dp,
                        if (unlocked) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(dimens.iconM)
                )
            }
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(dimens.iconS)
                        .clip(CircleShape)
                        .background(AppColors.surface)
                        .border(dimens.borderThin, AppColors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = AppColors.textHint,
                        modifier = Modifier.size(dimens.iconXS)
                    )
                }
            }
        }
        Spacer(Modifier.height(dimens.spaceXXS))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (unlocked) AppColors.onGradient else AppColors.onGradient.copy(alpha = 0.6f),
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(circleSize + 16.dp)
        )
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: CourseBadge,
    unlocked: Boolean,
    completedChapters: Int,
    onDismiss: () -> Unit,
    onGoToCourse: () -> Unit
) {
    val dimens = LocalDimens.current
    val tint = badge.accent.tint

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(dimens.radiusLarge))
                .background(AppColors.surface)
                .padding(dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(dimens.radiusMedium))
                    .background(
                        if (unlocked) {
                            Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f)))
                        } else {
                            Brush.linearGradient(
                                listOf(AppColors.textHint.copy(alpha = 0.3f), AppColors.textHint.copy(alpha = 0.18f))
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else AppColors.textHint,
                    modifier = Modifier.size(dimens.iconXL)
                )
            }

            Spacer(Modifier.height(dimens.spaceM))

            Text(
                badge.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spaceXS))

            Text(
                text = if (unlocked) {
                    "Badge earned — course completed!"
                } else {
                    "$completedChapters/${badge.chapterCount} chapters complete"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spaceL))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.buttonHeight)
                    .clip(RoundedCornerShape(50))
                    .background(tint)
                    .clickable(onClick = onGoToCourse),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Go to course",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimens.iconS)
                )
            }
        }
    }
}