package com.nexora_app.app.screens.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.components.AppBackground
import com.nexora_app.app.components.PrimaryButton
import com.nexora_app.app.models.Word
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.viewmodel.AuthViewModel
import com.nexora_app.app.viewmodel.WordsViewModel
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
private enum class Bucket { DONT_KNOW, KNOW }

@Composable
fun WordsScreen(
    navController: NavHostController,
    viewModel: WordsViewModel,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    val knownWords by viewModel.knownWords.collectAsStateWithLifecycle()
    val unknownWords by viewModel.unknownWords.collectAsStateWithLifecycle()
    val showSavedOnly by viewModel.showSavedOnly.collectAsStateWithLifecycle()

    // Pulls this user's saved/known/don't-know state from Firestore and
    // merges it onto the bundled catalog — same spot iOS calls this from
    // WordsScreen's .task. StartLearningScreen and FlashcardDeckScreen
    // read from this same shared WordsViewModel instance, so as long as
    // this fires before those screens are reached (it always does — this
    // is the graph's start destination), they don't need their own copy
    // of this call.
    LaunchedEffect(Unit) {
        authViewModel.currentUid?.let { viewModel.configure(it) }
    }

    var selectedBucket by remember { mutableStateOf(Bucket.KNOW) }
    val displayedWords = if (selectedBucket == Bucket.KNOW) knownWords else unknownWords

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            WordsHeader(
                showSavedOnly = showSavedOnly,
                onToggleFilter = { viewModel.toggleShowSavedOnly() },
                onToggleSort = { viewModel.toggleSortAlphabetically() }
            )

            BucketPills(
                dontKnowCount = unknownWords.size,
                knowCount = knownWords.size,
                selected = selectedBucket,
                onSelect = { selectedBucket = it }
            )

            Spacer(Modifier.height(dimens.spaceS))

            if (displayedWords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showSavedOnly) "No saved words here yet." else "Nothing in this list yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.onGradient.copy(alpha = 0.85f)
                    )
                }
            } else {
                WordList(
                    words = displayedWords,
                    bottomInset = dimens.floatingButtonClearance + dimens.buttonHeight,
                    onToggleSaved = { viewModel.toggleSaved(it) },
                    onDelete = { viewModel.deleteWord(it) },
                    onRowTap = {
                        val source = if (selectedBucket == Bucket.KNOW) {
                            Routes.Flashcards.SOURCE_KNOW
                        } else {
                            Routes.Flashcards.SOURCE_DONT_KNOW
                        }
                        navController.navigate(Routes.Flashcards.createRoute(source))
                    }
                )
            }
        }

        // Floating CTA pinned above the bottom nav — reachable regardless
        // of list length, matches iOS's fixed (not scrolling) placement.
        // Trailing icon (not an embedded "\u2192" arrow character) — some
        // devices' default font mangled that glyph into "'n".
        PrimaryButton(
            text = "Study words",
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { navController.navigate(Routes.StartLearning.route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    horizontal = dimens.screenPaddingHorizontal,
                    vertical = dimens.floatingButtonClearance
                )
        )
    }
}

@Composable
private fun WordsHeader(
    showSavedOnly: Boolean,
    onToggleFilter: () -> Unit,
    onToggleSort: () -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.screenPaddingHorizontal,
                vertical = dimens.spaceM
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My Words",
            style = MaterialTheme.typography.headlineLarge,
            color = AppColors.onGradient
        )

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onToggleFilter) {
            Icon(
                imageVector = Icons.Filled.FilterAlt,
                contentDescription = "Filter: show saved words only",
                tint = if (showSavedOnly) AppColors.onGradient else AppColors.onGradient.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onToggleSort) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = "Sort alphabetically",
                tint = AppColors.onGradient
            )
        }
    }
}

@Composable
private fun BucketPills(
    dontKnowCount: Int,
    knowCount: Int,
    selected: Bucket,
    onSelect: (Bucket) -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)
    ) {
        PillButton(
            title = "I don't know",
            count = dontKnowCount,
            icon = Icons.Filled.Close,
            isSelected = selected == Bucket.DONT_KNOW,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(Bucket.DONT_KNOW) }
        )
        PillButton(
            title = "I know",
            count = knowCount,
            icon = Icons.Filled.Check,
            isSelected = selected == Bucket.KNOW,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(Bucket.KNOW) }
        )
    }
}

@Composable
private fun PillButton(
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        modifier = modifier
            .height(dimens.buttonHeight * 0.8f)
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) AppColors.primary else AppColors.surface,
                shape = RoundedCornerShape(dimens.radiusPill)
            )
            .padding(horizontal = dimens.spaceM),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else AppColors.textSecondary,
            modifier = Modifier.size(dimens.iconS)
        )
        Spacer(Modifier.width(dimens.spaceXS))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isSelected) Color.White else AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(dimens.spaceXS))
        Box(
            modifier = Modifier
                .background(
                    color = if (isSelected) Color.White else AppColors.surfaceVariant,
                    shape = CircleShape
                )
                .padding(horizontal = dimens.spaceXS, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // maxLines + softWrap = false so a two-digit count (e.g. "10")
            // never wraps into a stacked "1" / "0" inside this tiny badge —
            // that was the "numbers written weird" bug.
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AppColors.primary else AppColors.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordList(
    words: List<Word>,
    bottomInset: Dp,
    onToggleSaved: (Word) -> Unit,
    onDelete: (Word) -> Unit,
    onRowTap: () -> Unit
) {
    val dimens = LocalDimens.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = dimens.screenPaddingHorizontal,
            vertical = dimens.spaceS
        ),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS)
    ) {
        items(words, key = { it.id }) { word ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(word)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.error, RoundedCornerShape(dimens.radiusMedium))
                            .padding(horizontal = dimens.spaceM),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            ) {
                WordRow(word = word, onToggleSaved = { onToggleSaved(word) }, onTap = onRowTap)
            }
        }

        item {
            Text(
                text = "You reached the end \uD83C\uDF89",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.onGradient.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceM),
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(Modifier.height(bottomInset))
        }
    }
}

@Composable
private fun WordRow(
    word: Word,
    onToggleSaved: () -> Unit,
    onTap: () -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(AppColors.surface)
            .clickable(onClick = onTap)
            .padding(horizontal = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word.text,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onToggleSaved) {
            Icon(
                imageVector = if (word.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (word.isSaved) "Unsave word" else "Save word",
                tint = if (word.isSaved) AppColors.primary else AppColors.textHint
            )
        }
    }
}