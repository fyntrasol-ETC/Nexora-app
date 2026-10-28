package com.nexora_app.app.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.ChapterExerciseContent
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.DialogueLine
import com.nexora_app.app.models.DialogueSpeaker
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

@Composable
fun DialogueScreen(
    course: Course,
    chapter: Chapter,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val dimens = LocalDimens.current
    // DialogueScreen is only ever pushed for a Dialogue-kind chapter (see
    // CoursesScreen's `when` on CourseRoute.Exercise), so this cast is safe.
    val dialogueLines = (chapter.exercise as ChapterExerciseContent.Dialogue).exercise.dialogue

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(top = dimens.spaceM, bottom = dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dialogue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(chapter.title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.Close, contentDescription = "Close", onClick = onClose)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.spaceM),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceM)
            ) {
                items(dialogueLines) { line ->
                    DialogueBubble(line = line)
                }
                item { Spacer(modifier = Modifier.height(dimens.floatingButtonClearance)) }
            }
        }

        GlowCTAButton(
            title = "Start Quiz",
            icon = Icons.Filled.ArrowForward,
            onClick = onStartQuiz,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(bottom = dimens.spaceM)
        )
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun DialogueBubble(line: DialogueLine) {
    val isMia = line.speaker == DialogueSpeaker.MIA

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMia) Arrangement.Start else Arrangement.End
    ) {
        if (isMia) SpeakerAvatar(line.speaker, isMia = true)

        Column(
            horizontalAlignment = if (isMia) Alignment.Start else Alignment.End,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = line.speaker.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isMia) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMia) AppColors.textPrimary else Color.White,
                modifier = Modifier
                    .shadow(4.dp, bubbleShape(isMia))
                    .clip(bubbleShape(isMia))
                    .background(if (isMia) Color.White else AppColors.primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        if (!isMia) SpeakerAvatar(line.speaker, isMia = false)
    }
}

/** Small initial-letter avatar tinted to match its speaker's bubble color. */
@Composable
private fun SpeakerAvatar(speaker: DialogueSpeaker, isMia: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (isMia) Color.White.copy(alpha = 0.9f) else AppColors.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = speaker.displayName.take(1),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isMia) AppColors.primary else Color.White
        )
    }
}

/**
 * A rounded shape with one bottom corner squared off toward its speaker's
 * avatar — a subtle "speech tail" without an actual pointer.
 */
private fun bubbleShape(isLeading: Boolean): Shape {
    val radius = 16.dp
    return RoundedCornerShape(
        topStart = radius,
        topEnd = radius,
        bottomStart = if (isLeading) 0.dp else radius,
        bottomEnd = if (isLeading) radius else 0.dp
    )
}