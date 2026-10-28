package com.nexora_app.app.screens.videos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.models.VideoCatalog
import com.nexora_app.app.models.VideoFilter
import com.nexora_app.app.models.VideoItem
import com.nexora_app.app.models.VideoTopic
import com.nexora_app.app.models.tint
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.ui.viewmodel.VideoThumbnailCache
import com.nexora_app.app.viewmodel.AuthViewModel

/**
 * Video Feed — parity with iOS VideoFeedScreen/VideoCard/VideoFilterSheet,
 * consolidated into one file since the state here is simple enough not to
 * need its own ViewModel: just a filter selection plus a read from
 * VideoFavoritesRepository. Background matches ProfileScreen's gradient.
 */
@Composable
fun VideoFeedScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current

    var activeFilter by remember { mutableStateOf<VideoFilter>(VideoFilter.All) }
    var sheetVisible by remember { mutableStateOf(false) }

    val favoriteIds by VideoFavoritesRepository.favoriteIds.collectAsState()

    // Pulls this user's favorited videos from Firestore — same spot iOS
    // calls this from VideoFeedScreen's .task. VideoFavoritesRepository is
    // a singleton, so this only needs to run once per sign-in, but Unit
    // keys it to "once per composition of this screen" which is
    // effectively the same thing in practice and matches the pattern used
    // on WordsScreen/ProfileScreen.
    LaunchedEffect(Unit) {
        authViewModel.currentUid?.let { uid -> VideoFavoritesRepository.configure(uid) }
    }

    val filteredVideos = remember(activeFilter, favoriteIds) {
        when (val filter = activeFilter) {
            is VideoFilter.All -> VideoCatalog.all
            is VideoFilter.Favorites -> VideoCatalog.all.filter { it.id in favoriteIds }
            is VideoFilter.Topic -> VideoCatalog.all.filter { it.topic == filter.topic }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.screenPaddingHorizontal,
                        vertical = dimens.spaceM
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Feed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.onGradient
                )
                FilterPill(onClick = { sheetVisible = true })
            }

            if (filteredVideos.isEmpty()) {
                EmptyState(
                    filter = activeFilter,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = dimens.screenPaddingHorizontal,
                        vertical = dimens.spaceS
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM)
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isFavorite = video.id in favoriteIds,
                            onToggleFavorite = { VideoFavoritesRepository.toggle(video.id) },
                            onPlay = { navController.navigate(Routes.VideoPlayer.createRoute(video.id)) }
                        )
                    }
                    item { Spacer(Modifier.height(dimens.spaceL)) }
                }
            }
        }
    }

    if (sheetVisible) {
        VideoFilterSheet(
            currentFilter = activeFilter,
            onDismiss = { sheetVisible = false },
            onApply = { newFilter ->
                activeFilter = newFilter
                sheetVisible = false
            }
        )
    }
}

@Composable
private fun FilterPill(onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(dimens.radiusPill))
            .background(AppColors.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXS)
    ) {
        Text(
            text = "Filter",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.primary
        )
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = "Filter",
            tint = AppColors.primary,
            modifier = Modifier.size(dimens.iconXS)
        )
    }
}

@Composable
private fun EmptyState(filter: VideoFilter, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val message = if (filter is VideoFilter.Favorites) "No favorites yet" else "No videos in this topic yet"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimens.spaceXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = AppColors.onGradient.copy(alpha = 0.85f),
            modifier = Modifier.size(dimens.iconXL)
        )
        Spacer(Modifier.height(dimens.spaceM))
        Text(
            text = message,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = AppColors.onGradient.copy(alpha = 0.9f)
        )
    }
}

// -----------------------------------------------------------------------
// FEED CARD — matches iOS VideoCard: tint placeholder, real thumbnail
// fades in once generated, duration badge, favorite heart.
// -----------------------------------------------------------------------

@Composable
private fun VideoCard(
    video: VideoItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val tint = video.topic.tint()

    var thumbnail by remember(video.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(video.id) {
        thumbnail = VideoThumbnailCache.thumbnail(context, video)
    }

    Card(
        shape = RoundedCornerShape(dimens.radiusLarge),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.featuredCardHeight)
                    .clip(RoundedCornerShape(topStart = dimens.radiusLarge, topEnd = dimens.radiusLarge))
                    .background(tint)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                thumbnail?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(dimens.iconXL)
                        .background(
                            Color.Black.copy(alpha = if (thumbnail != null) 0.15f else 0f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(
                    text = video.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimens.spaceS)
                        .clip(RoundedCornerShape(dimens.radiusSmall))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = dimens.spaceXS, vertical = dimens.spaceXXS)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.spaceM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                        maxLines = 2
                    )
                    Text(
                        text = video.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
                Spacer(Modifier.width(dimens.spaceS))
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites",
                    tint = if (isFavorite) AppColors.error else AppColors.primary,
                    modifier = Modifier
                        .size(dimens.iconM)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleFavorite
                        )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------
// FILTER BOTTOM SHEET — matches iOS VideoFilterSheet
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoFilterSheet(
    currentFilter: VideoFilter,
    onDismiss: () -> Unit,
    onApply: (VideoFilter) -> Unit
) {
    val dimens = LocalDimens.current
    var pendingFilter by remember { mutableStateOf(currentFilter) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Cap the sheet to ~85% of screen height (like iOS's
    // .presentationDetents([.fraction(0.85)])) so it never tries to grow
    // taller than the screen — that's what let the row list overflow and
    // push "Apply Filters" off-screen before.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            // ---- Header (fixed, never scrolls) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceL),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AppColors.textPrimary,
                    modifier = Modifier
                        .size(dimens.iconM)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                )
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.primary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { pendingFilter = VideoFilter.All }
                )
            }

            Spacer(Modifier.height(dimens.spaceM))

            // ---- Row list (the ONLY scrollable part — takes whatever
            // space is left between the fixed header above and the fixed
            // Apply button below) ----
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.spaceL)
            ) {
                FilterOptionRow(
                    label = "All",
                    selected = pendingFilter is VideoFilter.All,
                    onClick = { pendingFilter = VideoFilter.All }
                )
                FilterOptionRow(
                    label = "Favorites",
                    icon = Icons.Default.Favorite,
                    selected = pendingFilter is VideoFilter.Favorites,
                    onClick = { pendingFilter = VideoFilter.Favorites }
                )

                Spacer(Modifier.height(dimens.spaceS))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.borderThin)
                        .background(AppColors.divider)
                )
                Spacer(Modifier.height(dimens.spaceS))

                VideoTopic.values().forEach { topic ->
                    FilterOptionRow(
                        label = topic.displayName,
                        selected = (pendingFilter as? VideoFilter.Topic)?.topic == topic,
                        onClick = { pendingFilter = VideoFilter.Topic(topic) }
                    )
                }
            }

            // ---- Apply button (fixed, never scrolls, always visible) ----
            Button(
                onClick = { onApply(pendingFilter) },
                shape = RoundedCornerShape(dimens.radiusPill),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceL)
                    .height(dimens.buttonHeight)
            ) {
                Text(
                    text = "Apply Filters",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.onPrimary
                )
            }

            Spacer(Modifier.height(dimens.spaceM))
        }
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(if (selected) AppColors.primary else AppColors.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else AppColors.error,
                modifier = Modifier.size(dimens.iconS)
            )
            Spacer(Modifier.width(dimens.spaceS))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimens.iconS)
            )
        }
    }
    Spacer(Modifier.height(dimens.spaceXS))
}