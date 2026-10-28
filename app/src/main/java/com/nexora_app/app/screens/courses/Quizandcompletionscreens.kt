package com.nexora_app.app.screens.courses

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.ChapterExerciseContent
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.QuizQuestionType
import com.nexora_app.app.models.QuizScore
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

// -----------------------------------------------------------------------
// The last two steps of the exercise flow: the quiz itself, and the
// completion screen it lands on. Kept together since one only exists to
// follow the other — mirrors iOS's QuizAndCompletionScreens.swift.
//
// Note: the quiz only ever follows a Dialogue-kind chapter (a Picture
// chapter finishes straight to Completion — see CoursesScreen), so
// ExerciseQuizScreen safely casts chapter.exercise to Dialogue.
//
// ExerciseQuizController (progression state for the quiz) now lives in
// its own file, ExerciseQuizController.kt, in this same package.
// -----------------------------------------------------------------------

// MARK: - Quiz

/**
 * One question at a time (MCQ or fill-in-the-blank), answered from
 * memory of the dialogue just read. "Next" is disabled until an option
 * is picked; on the last question it becomes "Finish" and moves to the
 * completion screen.
 */
@Composable
fun ExerciseQuizScreen(
    chapter: Chapter,
    onClose: () -> Unit,
    onFinish: (QuizScore) -> Unit
) {
    val dimens = LocalDimens.current
    val questions = remember(chapter) {
        (chapter.exercise as? ChapterExerciseContent.Dialogue)?.exercise?.questions ?: emptyList()
    }
    val controller = remember(chapter) { ExerciseQuizController(questions) }

    if (questions.isEmpty()) {
        // Defensive fallback — shouldn't happen since Quiz only follows
        // a Dialogue chapter, but avoids a crash if routing ever changes.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions in this exercise yet.", color = Color.White.copy(alpha = 0.8f))
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            QuizHeader(
                dimens = dimens,
                progressLabel = controller.progressLabel,
                total = controller.questions.size,
                currentIndex = controller.currentIndex,
                onClose = onClose
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(top = dimens.spaceM, bottom = dimens.floatingButtonClearance),
                horizontalAlignment = Alignment.Start
            ) {
                QuestionKindTag(isFillInBlank = controller.currentQuestion.type == QuizQuestionType.FILL_IN_BLANK)
                Spacer(modifier = Modifier.height(dimens.spaceS))
                Text(
                    text = controller.currentQuestion.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(dimens.spaceL))

                controller.currentQuestion.options.forEachIndexed { index, option ->
                    OptionButton(
                        index = index,
                        option = option,
                        isSelected = controller.selectedOptionIndex == index,
                        onClick = { controller.select(index) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        GlowCTAButton(
            title = if (controller.isLastQuestion) "Finish" else "Next",
            icon = if (controller.isLastQuestion) Icons.Filled.Check else Icons.Filled.ArrowForward,
            enabled = controller.hasAnswered,
            onClick = {
                if (controller.advanceOrFinish()) {
                    onFinish(QuizScore(controller.correctCount, controller.questions.size))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(bottom = dimens.spaceM)
        )
    }
}

@Composable
private fun QuizHeader(
    dimens: com.nexora_app.app.ui.theme.Dimensions,
    progressLabel: String,
    total: Int,
    currentIndex: Int,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = dimens.screenPaddingHorizontal)
            .padding(top = dimens.spaceM)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(progressLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            // Balances the close button so the label stays centered.
            Spacer(modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(dimens.spaceS))
        QuizProgressBar(total = total, currentIndex = currentIndex)
    }
}

/** Small caption pill flagging whether this question is fill-in-the-blank or multiple choice. */
@Composable
private fun QuestionKindTag(isFillInBlank: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        Icon(
            imageVector = if (isFillInBlank) Icons.Filled.Edit else Icons.Filled.Checklist,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isFillInBlank) "FILL IN THE BLANK" else "CHOOSE THE CORRECT ANSWER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/** Segmented capsule progress bar — one segment per question, filled up through the current one. */
@Composable
private fun QuizProgressBar(total: Int, currentIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index <= currentIndex) Color.White else Color.White.copy(alpha = 0.22f))
            )
        }
    }
}

@Composable
private fun OptionButton(index: Int, option: String, isSelected: Boolean, onClick: () -> Unit) {
    val letter = ('A' + index)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.14f))
            .then(
                if (!isSelected)
                    Modifier
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isSelected) AppColors.primary else Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(letter.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = option,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) AppColors.primary else Color.White,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.primary)
        }
    }
}

// MARK: - Completion

/** What to show for "how much" was completed — questions for a dialogue chapter, words for a picture chapter. */
private val Chapter.exerciseItemCountLabel: String
    get() = when (val ex = exercise) {
        is ChapterExerciseContent.Dialogue -> "${ex.exercise.questions.size} Questions"
        is ChapterExerciseContent.Picture -> "${ex.exercise.cards.size} Words"
    }

/**
 * Shown once the exercise (quiz, or picture deck) is finished. "Go Back"
 * pops the whole chapterIntro -> exerciseIntro -> exercise -> (quiz ->)
 * completion run in one go, landing back on the chapters list.
 *
 * [score] is non-null only when this completion follows a quiz (Dialogue
 * chapters); Picture chapters finish straight here with no score to show.
 */
@Composable
fun ExerciseCompletionScreen(
    course: Course,
    chapter: Chapter,
    score: QuizScore? = null,
    onGoBackToChapters: () -> Unit
) {
    val dimens = LocalDimens.current
    var badgeAppeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (badgeAppeared) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "badgeScale"
    )

    LaunchedEffect(Unit) { badgeAppeared = true }

    Box(modifier = Modifier.fillMaxSize()) {
        DecorativeBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(scale).alpha(scale)) {
                Box(modifier = Modifier.size(210.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)))
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .shadow(20.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(50.dp))
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceL))

            Text(
                text = "Exercise Completed!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Great work finishing \u201C${chapter.title}\u201D.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )

            // Score pill — only shown when this completion came from a quiz.
            if (score != null) {
                Spacer(modifier = Modifier.height(dimens.spaceS))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(vertical = 8.dp, horizontal = 14.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Score: ${score.correct}/${score.total} (${score.percentage}%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceM))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompletionStatChip(icon = Icons.Filled.MenuBook, label = chapter.exerciseItemCountLabel)
                CompletionStatChip(icon = Icons.Filled.Bookmark, label = course.title)
            }

            Spacer(modifier = Modifier.weight(2f))
        }

        GlowCTAButton(
            title = "Go Back to Chapters",
            icon = Icons.Filled.Undo,
            onClick = onGoBackToChapters,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .padding(bottom = dimens.spaceM)
        )
    }
}

@Composable
private fun CompletionStatChip(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
    }
}