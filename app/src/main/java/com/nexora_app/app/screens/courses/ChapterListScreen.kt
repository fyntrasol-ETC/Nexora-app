package com.nexora_app.app.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.CourseLevel
import com.nexora_app.app.models.CourseProgress
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.Dimensions
import com.nexora_app.app.ui.theme.LocalDimens

@Composable
fun ChapterListScreen(
    course: Course,
    progress: CourseProgress?,
    onBack: () -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val dimens = LocalDimens.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(top = dimens.spaceM, bottom = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.spaceS)
        ) {
            item {
                CourseIntroBlock(course = course, dimens = dimens)
                Spacer(modifier = Modifier.height(dimens.spaceL))
            }
            items(course.chapters, key = { it.number }) { chapter ->
                ChapterRow(
                    chapter = chapter,
                    tint = course.accent.tint,
                    dimens = dimens,
                    isCompleted = progress?.completedChapterIds?.contains(chapter.number) == true,
                    score = progress?.exerciseScores?.get(chapter.number),
                    onClick = { onChapterClick(chapter) }
                )
                Spacer(modifier = Modifier.height(dimens.spaceS))
            }
            item { Spacer(modifier = Modifier.height(dimens.spaceXL)) }
        }
    }
}

@Composable
private fun CourseIntroBlock(course: Course, dimens: Dimensions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(course.accent.tint, course.accent.tint.copy(alpha = 0.65f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(dimens.spaceM))

        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelChip(level = course.level)
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val label = "${course.chapters.size} ${if (course.chapters.size == 1) "Chapter" else "Chapters"}"
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Spacer(modifier = Modifier.height(dimens.spaceM))

        Text(
            text = course.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center
        )

        if (course.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(dimens.spaceM))
            HighlightTagsRow(highlights = course.highlights)
        }
    }
}

@Composable
private fun LevelChip(level: CourseLevel) {
    Text(
        text = level.displayName.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = AppColors.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(vertical = 6.dp, horizontal = 10.dp)
    )
}

@Composable
private fun HighlightTagsRow(highlights: List<String>) {
    val rows = highlights.chunked(2)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(vertical = 7.dp, horizontal = 12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    tint: Color,
    dimens: Dimensions,
    isCompleted: Boolean,
    score: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(AppColors.surface)
            .clickable(onClick = onClick)
            .padding(dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChapterBadgeIcon(number = chapter.number, tint = tint, isCompleted = isCompleted)
        Spacer(modifier = Modifier.width(dimens.spaceS))
        Column(modifier = Modifier.weight(1f)) {
            Text("Chapter ${chapter.number}", style = MaterialTheme.typography.labelSmall, color = AppColors.textHint)
            Text(chapter.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
            if (isCompleted) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.success, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (score != null) "Completed • $score%" else "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.success
                    )
                }
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.textHint)
    }
}

/**
 * Star badge tinted to the course's accent, chapter number pinned bottom-end
 * like an achievement badge. When isCompleted, a small green check badge is
 * anchored top-end — same "anchored to the icon's own bounds" pattern used
 * for the notification bell badge elsewhere in the app, so it never drifts.
 */
@Composable
private fun ChapterBadgeIcon(number: Int, tint: Color, isCompleted: Boolean) {
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, AppColors.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.primary)
        }
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(AppColors.success)
                    .border(2.dp, AppColors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}