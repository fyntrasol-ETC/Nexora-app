package com.nexora_app.app.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.Course
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

// -----------------------------------------------------------------------
// The two "title page" screens in the course flow — a chapter's own
// intro (number/title/description -> Continue) and the exercise's intro
// (e.g. "Dialogue Exercise" heading/instructions -> Start). Both sit on
// the gradient background already applied by CoursesScreen, with the CTA
// pinned to the bottom via Box + align(BottomCenter).
// -----------------------------------------------------------------------

// MARK: - Chapter intro

@Composable
fun ChapterIntroScreen(
    course: Course,
    chapter: Chapter,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    val dimens = LocalDimens.current

    Box(modifier = Modifier.fillMaxSize()) {
        DecorativeBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(dimens.spaceM))
            IntroCloseButton(onClick = onClose)
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.radiusLarge))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(dimens.radiusLarge))
                    .padding(dimens.spaceL)
            ) {
                ChapterBadge(number = chapter.number)
                Spacer(modifier = Modifier.height(dimens.spaceS))
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(dimens.spaceS))
                Text(
                    text = chapter.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            Spacer(modifier = Modifier.weight(2f))
        }

        GlowCTAButton(
            title = "Continue",
            icon = Icons.Filled.ArrowForward,
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(bottom = dimens.spaceM)
        )
    }
}

// MARK: - Exercise intro

@Composable
fun ExerciseIntroScreen(
    course: Course,
    chapter: Chapter,
    onClose: () -> Unit,
    onStart: () -> Unit
) {
    val dimens = LocalDimens.current
    val exercise = chapter.exercise

    Box(modifier = Modifier.fillMaxSize()) {
        DecorativeBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(dimens.spaceM))
            IntroCloseButton(onClick = onClose)
            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.Start) {
                Box(modifier = Modifier.size(dimens.avatarLarge), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(dimens.avatarLarge)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(dimens.avatarLarge * 0.7f)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                    Icon(
                        Icons.Filled.Forum,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(dimens.avatarLarge * 0.34f)
                    )
                }

                Spacer(modifier = Modifier.height(dimens.spaceM))

                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(dimens.spaceXS))
                Text(
                    text = exercise.instructions,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            Spacer(modifier = Modifier.weight(2f))
        }

        GlowCTAButton(
            title = "Start",
            icon = Icons.Filled.PlayArrow,
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(bottom = dimens.spaceM)
        )
    }
}

// MARK: - Shared close button

@Composable
private fun IntroCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
    }
}

// MARK: - Chapter number badge

/** Small pill reading "CHAPTER N" — reused wherever a quick chapter tag is needed. */
@Composable
fun ChapterBadge(number: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.primary)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "CHAPTER $number",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// MARK: - Glow CTA button

/**
 * The wide capsule primary action button used at the bottom of every
 * course-flow screen. "Glow" = the soft shadow underneath, keeping the
 * white pill from reading flat against the gradient.
 */
@Composable
fun GlowCTAButton(
    title: String,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    titleColor: Color = AppColors.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(16.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(tint)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
        if (icon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(15.dp))
        }
    }
}

// MARK: - Decorative background blobs

/**
 * Soft, translucent circles layered behind content on the course-flow
 * screens so the gradient doesn't read as an empty flat wash — purely
 * decorative, no hit testing. (Skips iOS's actual gaussian blur since
 * that needs API 31+; the low opacity alone reads soft enough here.)
 */
@Composable
fun DecorativeBlobs() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-70).dp, y = 120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
    }
}