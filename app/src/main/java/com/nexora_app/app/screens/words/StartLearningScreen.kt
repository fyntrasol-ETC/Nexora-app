package com.nexora_app.app.screens.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.nexora_app.app.components.AppBackground
import com.nexora_app.app.models.Word
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.FlashcardWordCache
import com.nexora_app.app.viewmodel.WordsViewModel

@Composable
fun StartLearningScreen(
    navController: NavHostController,
    viewModel: WordsViewModel
) {
    val dimens = LocalDimens.current

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.screenPaddingHorizontal,
                        vertical = dimens.spaceM
                    )
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = AppColors.onGradient
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.screenPaddingHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Start Learning",
                    style = MaterialTheme.typography.displayLarge,
                    color = AppColors.onGradient,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimens.spaceXS))
                Text(
                    text = "Choose a level to begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.onGradient.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceM)
            ) {
                Word.Difficulty.entries.forEach { level ->
                    DifficultyRow(level = level) {
                        val words = viewModel.randomWords(level)
                        val source = level.name.lowercase()
                        // Stash the freshly-picked words so FlashcardDeckScreen
                        // can read them by difficulty key without re-rolling
                        // the random selection on recomposition.
                        FlashcardWordCache.put(source, words)
                        navController.navigate(Routes.Flashcards.createRoute(source))
                    }
                }
            }

            Spacer(Modifier.weight(2f))
        }
    }
}

@Composable
private fun DifficultyRow(level: Word.Difficulty, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clickable(onClick = onClick)
            .background(AppColors.surface, RoundedCornerShape(dimens.radiusPill))
            .padding(horizontal = dimens.spaceL),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = level.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = AppColors.primary
        )
    }
}