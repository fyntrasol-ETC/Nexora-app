package com.nexora_app.app.screens.words

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nexora_app.app.components.AppBackground
import com.nexora_app.app.models.Word
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.AudioManager
import com.nexora_app.app.viewmodel.FlashcardWordCache
import com.nexora_app.app.viewmodel.WordsViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * `source` is one of: "know", "dont_know", or a Word.Difficulty name
 * lowercased ("beginner"/"intermediate"/"advanced"). Resolves the actual
 * word list + a display title from that key, reading through the shared
 * WordsViewModel (bucket sources) or FlashcardWordCache (difficulty
 * sources, stashed by StartLearningScreen).
 */
@Composable
fun FlashcardDeckScreen(
    navController: NavHostController,
    viewModel: WordsViewModel,
    source: String
) {
    val dimens = LocalDimens.current

    val (title, initialWords) = remember(source) {
        when (source) {
            Routes.Flashcards.SOURCE_KNOW -> "Words I know" to viewModel.allKnownWords()
            Routes.Flashcards.SOURCE_DONT_KNOW -> "Words I don't know" to viewModel.allDontKnowWords()
            else -> {
                val difficulty = Word.Difficulty.entries.firstOrNull { it.name.lowercase() == source }
                val words = FlashcardWordCache.get(source).orEmpty()
                val label = difficulty?.let { "${it.displayName} words" } ?: "Words"
                label to words
            }
        }
    }

    var words by remember(source) { mutableStateOf(initialWords) }
    var currentIndex by remember(source) { mutableStateOf(0) }
    var showDefinition by remember { mutableStateOf(false) }

    val dragOffsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val currentWord = words.getOrNull(currentIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(
                title = title,
                onBack = { navController.popBackStack() }
            )

            Spacer(Modifier.height(dimens.spaceM))

            Box(
                modifier = Modifier
                    .background(AppColors.surfaceVariant, RoundedCornerShape(dimens.radiusPill))
                    .padding(horizontal = dimens.spaceM, vertical = dimens.spaceXS)
            ) {
                Text(
                    text = "Do you know this word?",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.textPrimary
                )
            }

            Spacer(Modifier.weight(1f))

            if (currentWord != null) {
                FlashcardView(
                    word = currentWord,
                    dragOffsetX = dragOffsetX.value,
                    showDefinition = showDefinition,
                    onToggleSaved = {
                        val updated = currentWord.copy(isSaved = !currentWord.isSaved)
                        words = words.map { if (it.id == updated.id) updated else it }
                        viewModel.toggleSaved(updated)
                    },
                    onPronounce = { AudioManager.pronounce(currentWord.text) },
                    onToggleInfo = { showDefinition = !showDefinition },
                    onDragEnd = { finalOffset ->
                        scope.launch {
                            when {
                                finalOffset > 110f -> swipe(
                                    right = true, word = currentWord, dragOffsetX = dragOffsetX,
                                    viewModel = viewModel,
                                    advance = { currentIndex++; showDefinition = false }
                                )
                                finalOffset < -110f -> swipe(
                                    right = false, word = currentWord, dragOffsetX = dragOffsetX,
                                    viewModel = viewModel,
                                    advance = { currentIndex++; showDefinition = false }
                                )
                                else -> dragOffsetX.animateTo(0f, tween(200))
                            }
                        }
                    },
                    onDragChange = { delta ->
                        scope.launch { dragOffsetX.snapTo(dragOffsetX.value + delta) }
                    },
                    onSwipeKnow = {
                        scope.launch {
                            swipe(
                                right = true, word = currentWord, dragOffsetX = dragOffsetX,
                                viewModel = viewModel,
                                advance = { currentIndex++; showDefinition = false }
                            )
                        }
                    },
                    onSwipeDontKnow = {
                        scope.launch {
                            swipe(
                                right = false, word = currentWord, dragOffsetX = dragOffsetX,
                                viewModel = viewModel,
                                advance = { currentIndex++; showDefinition = false }
                            )
                        }
                    }
                )
            } else {
                CompletionState(onDone = { navController.popBackStack() })
            }

            Spacer(Modifier.weight(2f))
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.onGradient)
            Spacer(Modifier.width(4.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = AppColors.onGradient, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FlashcardView(
    word: Word,
    dragOffsetX: Float,
    showDefinition: Boolean,
    onToggleSaved: () -> Unit,
    onPronounce: () -> Unit,
    onToggleInfo: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onDragChange: (Float) -> Unit,
    onSwipeKnow: () -> Unit,
    onSwipeDontKnow: () -> Unit
) {
    val dimens = LocalDimens.current
    val side = dimens.flashcardSize
    val swipeProgress = (abs(dragOffsetX) / 150f).coerceIn(0f, 1f)
    val swipeTint = if (dragOffsetX >= 0) AppColors.knowAccent else AppColors.dontKnowAccent

    Box(
        modifier = Modifier
            .size(side)
            .rotate((dragOffsetX / 20f))
            .pointerInput(word.id) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = { onDragEnd(accumulated) },
                    onDragCancel = { onDragEnd(0f) },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount
                        onDragChange(dragAmount)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.surface, RoundedCornerShape(dimens.radiusLarge))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM)
            ) {
                IconButton(onClick = onPronounce) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Pronounce", tint = AppColors.textPrimary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleSaved) {
                    Icon(
                        imageVector = if (word.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (word.isSaved) "Unsave" else "Save",
                        tint = if (word.isSaved) AppColors.primary else AppColors.textPrimary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceM),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = word.text,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = word.partOfSpeech,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textHint
                )

                AnimatedVisibility(
                    visible = showDefinition && word.definition.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = dimens.spaceS)
                    ) {
                        Text(
                            text = word.definition,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (word.example.isNotEmpty()) {
                            Text(
                                text = word.example,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.textHint,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(icon = Icons.Filled.Close, tint = AppColors.dontKnowAccent, filled = true, onClick = onSwipeDontKnow)
                RoundIconButton(icon = Icons.Filled.Info, tint = AppColors.primary, filled = false, onClick = onToggleInfo)
                RoundIconButton(icon = Icons.Filled.Check, tint = AppColors.knowAccent, filled = true, onClick = onSwipeKnow)
            }
        }

        // Green/dark-yellow tint overlay that intensifies with drag distance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(swipeTint.copy(alpha = swipeProgress * 0.5f), RoundedCornerShape(dimens.radiusLarge))
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    filled: Boolean,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val buttonSize = dimens.buttonHeight * 0.65f
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(buttonSize)
            .background(
                color = if (filled) AppColors.surfaceVariant else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun CompletionState(onDone: () -> Unit) {
    val dimens = LocalDimens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM)
    ) {
        Text(
            text = "You reached the end \uD83C\uDF89",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.onGradient
        )
        androidx.compose.material3.Button(
            onClick = onDone,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AppColors.primary
            ),
            shape = RoundedCornerShape(dimens.radiusPill),
            modifier = Modifier.height(dimens.buttonHeight)
        ) {
            Text("Done", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private suspend fun swipe(
    right: Boolean,
    word: Word,
    dragOffsetX: Animatable<Float, AnimationVector1D>,
    viewModel: WordsViewModel,
    advance: () -> Unit
) {
    dragOffsetX.animateTo(if (right) 900f else -900f, tween(220))
    if (right) viewModel.markKnown(word) else viewModel.markDontKnow(word)
    advance()
    dragOffsetX.snapTo(0f)
}