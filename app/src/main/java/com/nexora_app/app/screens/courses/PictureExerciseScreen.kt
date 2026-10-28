package com.nexora_app.app.screens.courses

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.ChapterExerciseContent
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.PictureCard
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

/**
 * Mirrors iOS's PictureExerciseViewModel — owns progression through a
 * chapter's picture-vocabulary deck: current card, the typed guess,
 * whether the card is flipped, and the running correct count.
 */
class PictureExerciseController(val cards: List<PictureCard>) {
    var currentIndex by mutableStateOf(0)
        private set
    var userInput by mutableStateOf("")
    var isRevealed by mutableStateOf(false)
        private set
    var correctCount by mutableStateOf(0)
        private set

    val currentCard: PictureCard? get() = cards.getOrNull(currentIndex)
    val isLastCard: Boolean get() = currentIndex == cards.lastIndex
    val progressLabel: String get() = "Word ${currentIndex + 1} of ${cards.size}"

    val isGuessCorrect: Boolean
        get() = currentCard?.let { userInput.trim().equals(it.answer, ignoreCase = true) } ?: false

    fun reveal() {
        if (isRevealed || currentCard == null) return
        if (isGuessCorrect) correctCount += 1
        isRevealed = true
    }

    /** Advances to the next card. Returns true once the deck is finished. */
    fun advanceOrFinish(): Boolean {
        if (isLastCard) return true
        currentIndex += 1
        userInput = ""
        isRevealed = false
        return false
    }
}

@Composable
fun PictureExerciseScreen(
    course: Course,
    chapter: Chapter,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onFinish: () -> Unit
) {
    val dimens = LocalDimens.current
    val pictureExercise = (chapter.exercise as? ChapterExerciseContent.Picture)?.exercise
    val controller = remember(chapter) { PictureExerciseController(pictureExercise?.cards ?: emptyList()) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(top = dimens.spaceM, bottom = dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PictureRoundIconButton(Icons.Filled.ArrowBack, "Back", onBack)
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Picture Vocabulary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    if (controller.cards.isNotEmpty()) {
                        Text(controller.progressLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                PictureRoundIconButton(Icons.Filled.Close, "Close", onClose)
            }

            val card = controller.currentCard
            if (card != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimens.screenPaddingHorizontal)
                        .padding(top = dimens.spaceM, bottom = dimens.floatingButtonClearance),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlipCard(
                        card = card,
                        isRevealed = controller.isRevealed,
                        isCorrect = controller.isGuessCorrect,
                        onTap = {
                            if (!controller.isRevealed) {
                                focusManager.clearFocus()
                                controller.reveal()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(dimens.spaceM))
                    OutlinedTextField(
                        value = controller.userInput,
                        onValueChange = { controller.userInput = it },
                        enabled = !controller.isRevealed,
                        placeholder = { Text("Type what you see...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(dimens.radiusMedium))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the card or type your guess, then reveal the answer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No cards in this exercise yet.", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        if (controller.currentCard != null) {
            GlowCTAButton(
                title = if (controller.isRevealed) { if (controller.isLastCard) "Finish" else "Next Word" } else "Reveal Answer",
                icon = if (controller.isRevealed) {
                    if (controller.isLastCard) Icons.Filled.Check else Icons.Filled.ArrowForward
                } else Icons.Filled.RemoveRedEye,
                enabled = controller.isRevealed || controller.userInput.trim().isNotEmpty(),
                onClick = {
                    if (controller.isRevealed) {
                        if (controller.advanceOrFinish()) onFinish()
                    } else {
                        focusManager.clearFocus()
                        controller.reveal()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(bottom = dimens.spaceM)
            )
        }
    }
}

@Composable
private fun FlipCard(card: PictureCard, isRevealed: Boolean, isCorrect: Boolean, onTap: () -> Unit) {
    val rotation by animateFloatAsState(if (isRevealed) 180f else 0f, tween(450), label = "flip")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(enabled = !isRevealed, onClick = onTap)
            .graphicsLayer { rotationY = rotation; cameraDistance = 16 * density },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            PictureFace(card)
        } else {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                RevealFace(card, isCorrect)
            }
        }
    }
}

@Composable
private fun PictureFace(card: PictureCard) {
    val context = LocalContext.current
    val resId = remember(card.imageName) {
        context.resources.getIdentifier(card.imageName, "drawable", context.packageName)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(130.dp).clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .height(130.dp).fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.textHint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = AppColors.textHint, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("What is this?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.textHint)
    }
}

@Composable
private fun RevealFace(card: PictureCard, isCorrect: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
        Icon(
            imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (isCorrect) Color(0xFF34C759) else Color(0xFFFF3B30),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(card.answer.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(card.exampleSentence, style = MaterialTheme.typography.bodyMedium, color = AppColors.textHint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PictureRoundIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}