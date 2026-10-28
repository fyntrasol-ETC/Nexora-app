package com.nexora_app.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.components.DiceBearAvatar
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.UserRepository
import com.nexora_app.app.data.VideoActivityRepository
import com.nexora_app.app.models.AvatarOptions
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.CourseCatalog
import com.nexora_app.app.models.CourseLevel
import com.nexora_app.app.models.CourseProgress
import com.nexora_app.app.models.User
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.Dimensions
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.CoursesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// -----------------------------------------------------------------------
// Mirrors iOS HomeScreen.swift section-for-section:
//   1. Header (avatar + time-of-day greeting + name)
//   2. Your Progress (sum of completedChapterIds / sum of chapters, catalog-wide)
//   3. Continue Learning (enrolled courses = have a courseProgress doc)
//   4. Weekly Progress (Mon...Sun bars, "today" computed from LocalDate)
//   5. Practice More (Words known / Favorite videos stat cards)
//   6. Discover (one random course per populated level)
// -----------------------------------------------------------------------

// ---- Weekly progress model (mirrors WeekDayProgress / DayProgressState) ----

private enum class WeekDayState { COMPLETED, TODAY, UPCOMING }

private data class WeekDay(
    val id: String,
    val label: String,
    val state: WeekDayState
)

// TODO: replace with a real WeeklyActivityViewModel backed by
// users/{uid}/weeklyActivity/{dateId} (see firestore.rules) — same TODO
// already flagged on the iOS side and in ProfileScreen.kt's weekly card.
// Day labels + "today" are real (java.time-driven); only which days are
// completed/missed is still a placeholder.
private fun currentWeekPlaceholder(): List<WeekDay> {
    val today = LocalDate.now()
    // Monday-start week, matching iOS's calendar.firstWeekday = 2.
    val monday = today.minusDays(((today.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7L) % 7L)
    return (0..6).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val isToday = date == today
        val isPast = date.isBefore(today)
        WeekDay(
            id = date.toString(),
            label = date.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()).uppercase(),
            state = when {
                isToday -> WeekDayState.TODAY
                isPast -> WeekDayState.COMPLETED
                else -> WeekDayState.UPCOMING
            }
        )
    }
}

// ---- HomeViewModel: combines user profile + course progress + stat counts ----

data class HomeUiState(
    val displayName: String = "",
    val avatarSeed: String = AvatarOptions.DEFAULT_SEED,
    val wordsKnownCount: Int = 0,
    val favoriteVideoCount: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val videoActivityRepository: VideoActivityRepository = VideoActivityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                userRepository.observeUser(uid).collect { user: User? ->
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            displayName = user.displayName,
                            avatarSeed = AvatarOptions.resolveSeed(user.avatarSeed),
                            isLoading = false
                        )
                    }
                }
            }
            viewModelScope.launch {
                val known = userRepository.knownWordsCount(uid)
                _uiState.value = _uiState.value.copy(wordsKnownCount = known)
            }
            // Mirrors iOS's VideoFeedViewModel.filteredVideos.filter(isFavorited)
            // count, but sourced from VideoActivityRepository directly since
            // that's a one-shot fetch rather than a live listener — same
            // trade-off knownWordsCount above already makes.
            viewModelScope.launch {
                val favorites = videoActivityRepository.fetchFavoriteIds(uid).size
                _uiState.value = _uiState.value.copy(favoriteVideoCount = favorites)
            }
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    coursesViewModel: CoursesViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    val progressByCourseId by coursesViewModel.progressByCourseId.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()

    val allCourses = remember { CourseCatalog.courses }
    val enrolledCourses = remember(progressByCourseId) {
        allCourses.filter { progressByCourseId.containsKey(it.id) }
    }

    val overallProgressPercent = remember(progressByCourseId) {
        val totalChapters = allCourses.sumOf { it.chapters.size }
        if (totalChapters == 0) 0
        else {
            val completed = allCourses.sumOf { course ->
                progressByCourseId[course.id]?.completedChapterIds?.distinct()?.size ?: 0
            }
            ((completed.toFloat() / totalChapters.toFloat()) * 100).toInt()
        }
    }

    // Frozen once per screen "session" so Discover doesn't reshuffle on
    // every recomposition — mirrors iOS's @State discoverCourses.
    var discoverCourses by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.listSaver(
        save = { it.map { c -> c.id } },
        restore = { ids -> ids.mapNotNull { CourseCatalog.courseById(it) } }
    )) { mutableStateOf(emptyList<Course>()) }

    LaunchedEffect(Unit) {
        if (discoverCourses.isEmpty()) {
            discoverCourses = CourseLevel.values()
                .mapNotNull { level -> CourseCatalog.coursesFor(level).randomOrNull() }
        }
    }

    val weekProgress = remember { currentWeekPlaceholder() }
    val daysCompletedThisWeek = weekProgress.count { it.state == WeekDayState.COMPLETED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPaddingHorizontal)
            .padding(top = dimens.spaceM, bottom = dimens.floatingButtonClearance)
    ) {
        HomeHeader(dimens = dimens, displayName = homeState.displayName, avatarSeed = homeState.avatarSeed)

        Spacer(Modifier.height(dimens.spaceL))

        ProgressCard(
            dimens = dimens,
            percent = overallProgressPercent,
            enrolledCount = enrolledCourses.size
        )

        Spacer(Modifier.height(dimens.spaceL))

        SectionHeader(
            title = "Continue Learning",
            dimens = dimens,
            onSeeAll = if (enrolledCourses.isEmpty()) null else { { navController.navigate(Routes.Courses.route) } }
        )
        Spacer(Modifier.height(dimens.spaceS))
        if (enrolledCourses.isEmpty()) {
            EmptyEnrolledCard(dimens = dimens, onBrowse = { navController.navigate(Routes.Courses.route) })
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                items(enrolledCourses, key = { it.id }) { course ->
                    ResumeCourseCard(
                        course = course,
                        progress = progressByCourseId[course.id],
                        dimens = dimens,
                        onClick = { navController.navigate(Routes.CourseDetail.createRoute(course.id)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spaceL))

        WeeklyProgressCard(dimens = dimens, week = weekProgress, daysCompleted = daysCompletedThisWeek)

        Spacer(Modifier.height(dimens.spaceL))

        SectionHeader(title = "Practice More", dimens = dimens, onSeeAll = null, white = false)
        Spacer(Modifier.height(dimens.spaceS))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            HomeStatCard(
                icon = Icons.Filled.Star,
                label = "Words I know",
                value = homeState.wordsKnownCount.toString(),
                accent = AppColors.accentWords,
                dimens = dimens,
                onClick = { navController.navigate(Routes.Words.route) },
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                icon = Icons.Filled.Favorite,
                label = "Favorite videos",
                value = homeState.favoriteVideoCount.toString(),
                accent = AppColors.accentVideos,
                dimens = dimens,
                onClick = { navController.navigate(Routes.Videos.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(dimens.spaceL))

        SectionHeader(
            title = "Discover",
            dimens = dimens,
            onSeeAll = { navController.navigate(Routes.Courses.route) }
        )
        Spacer(Modifier.height(dimens.spaceS))
        discoverCourses.firstOrNull()?.let { featured ->
            FeaturedDiscoverCard(
                course = featured,
                dimens = dimens,
                onClick = { navController.navigate(Routes.CourseDetail.createRoute(featured.id)) }
            )
        }
        if (discoverCourses.size > 1) {
            Spacer(Modifier.height(dimens.spaceS))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                discoverCourses.drop(1).forEach { course ->
                    CompactDiscoverCard(
                        course = course,
                        dimens = dimens,
                        onClick = { navController.navigate(Routes.CourseDetail.createRoute(course.id)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---- Header ----

private fun timeOfDayGreeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Still up?"
}

private fun timeOfDayIcon() = when (java.time.LocalTime.now().hour) {
    in 5..11 -> Icons.Filled.WbTwilight
    in 12..16 -> Icons.Filled.WbSunny
    in 17..21 -> Icons.Filled.Brightness3
    else -> Icons.Filled.NightsStay
}

@Composable
private fun HomeHeader(dimens: Dimensions, displayName: String, avatarSeed: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .padding(dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = timeOfDayIcon(),
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(dimens.iconXS)
                )
                Spacer(Modifier.width(dimens.spaceXXS))
                Text(
                    text = timeOfDayGreeting().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary
                )
            }
            Spacer(Modifier.height(dimens.spaceXXS))
            Text(
                text = displayName.ifBlank { "there" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = AppColors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "Let's keep improving your English",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary
            )
        }

        Spacer(Modifier.width(dimens.spaceM))

        Box(
            modifier = Modifier
                .size(dimens.avatarMedium)
                .background(AppColors.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            DiceBearAvatar(seed = avatarSeed, size = dimens.avatarMedium * 0.8f)
        }
    }
}

// ---- Your Progress card ----

@Composable
private fun ProgressCard(dimens: Dimensions, percent: Int, enrolledCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .padding(dimens.spaceM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppColors.primary)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(dimens.spaceS))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.progressBarHeight)
                .clip(RoundedCornerShape(dimens.radiusPill)),
            color = AppColors.primary,
            trackColor = AppColors.primary.copy(alpha = 0.15f)
        )
        Spacer(Modifier.height(dimens.spaceXS))
        Text(
            text = if (enrolledCount == 0)
                "You haven't started a course yet — pick one below to begin."
            else
                "Across $enrolledCount ${if (enrolledCount == 1) "enrolled course" else "enrolled courses"}.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary
        )
    }
}

// ---- Shared section header (white-on-gradient like CoursesScreen's level headers) ----

@Composable
private fun SectionHeader(title: String, dimens: Dimensions, onSeeAll: (() -> Unit)?, white: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (white) Color.White else AppColors.textPrimary
        )
        if (onSeeAll != null) {
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "See all",
                    color = if (white) Color.White.copy(alpha = 0.9f) else AppColors.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (white) Color.White.copy(alpha = 0.9f) else AppColors.primary,
                    modifier = Modifier.size(dimens.iconXS)
                )
            }
        }
    }
}

// ---- Continue Learning: resume card + empty state ----

@Composable
private fun ResumeCourseCard(course: Course, progress: CourseProgress?, dimens: Dimensions, onClick: () -> Unit) {
    val accent = course.accent.tint
    val completed = progress?.completedChapterIds?.distinct()?.size ?: 0
    val total = course.chapters.size
    val remaining = (total - completed).coerceAtLeast(0)
    val fraction = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val lessonsLeftText = if (remaining == 0) "All chapters complete!"
    else "$remaining ${if (remaining == 1) "chapter" else "chapters"} left"

    Column(
        modifier = Modifier
            .width(dimens.courseCardWidth)
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusMedium))
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(AppColors.surface)
            .border(dimens.borderThin, accent.copy(alpha = 0.14f), RoundedCornerShape(dimens.radiusMedium))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.68f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
        }
        Column(modifier = Modifier.padding(dimens.spaceS)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Text(lessonsLeftText, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
            Spacer(Modifier.height(dimens.spaceXXS))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.progressBarHeight)
                    .clip(RoundedCornerShape(dimens.radiusPill)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun EmptyEnrolledCard(dimens: Dimensions, onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimens.cardElevation * 2, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .padding(dimens.spaceM)
    ) {
        Text(
            "Nothing in progress yet",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary
        )
        Spacer(Modifier.height(dimens.spaceXXS))
        Text(
            "Start a course below and it'll show up here so you can pick up right where you left off.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary
        )
        Spacer(Modifier.height(dimens.spaceM))
        Button(
            onClick = onBrowse,
            shape = RoundedCornerShape(dimens.radiusPill),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
        ) {
            Text("Browse courses", color = Color.White)
        }
    }
}

// ---- Weekly Progress ----

@Composable
private fun WeeklyProgressCard(dimens: Dimensions, week: List<WeekDay>, daysCompleted: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .padding(dimens.spaceM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Weekly Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Text(
                "$daysCompleted/7 days",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondary
            )
        }
        Spacer(Modifier.height(dimens.spaceM))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.weeklyChartHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEach { day ->
                val barColor = when (day.state) {
                    WeekDayState.COMPLETED -> AppColors.success
                    WeekDayState.TODAY -> AppColors.primary
                    WeekDayState.UPCOMING -> AppColors.surfaceVariant
                }
                val heightFraction = when (day.state) {
                    WeekDayState.COMPLETED -> 0.6f
                    WeekDayState.TODAY -> 0.85f
                    WeekDayState.UPCOMING -> 0.12f
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (day.state == WeekDayState.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(AppColors.success, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    } else if (day.state == WeekDayState.TODAY) {
                        Box(modifier = Modifier.size(8.dp).background(AppColors.primary, CircleShape))
                        Spacer(Modifier.height(9.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(dimens.weeklyChartBarWidth)
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(dimens.radiusSmall))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(dimens.spaceXXS))
                    Text(
                        day.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.state == WeekDayState.TODAY) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.state == WeekDayState.TODAY) AppColors.primary else AppColors.textSecondary
                    )
                }
            }
        }
    }
}

// ---- Practice More stat card ----

@Composable
private fun HomeStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    dimens: Dimensions,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .clickable(onClick = onClick)
            .padding(dimens.spaceM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(dimens.iconXS)
            )
        }
        Spacer(Modifier.height(dimens.spaceS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimens.iconXL * 0.55f)
                    .background(accent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(dimens.iconM))
            }
            Spacer(Modifier.width(dimens.spaceS))
            Text(value, style = MaterialTheme.typography.titleLarge, color = AppColors.textPrimary)
        }
    }
}

// ---- Discover cards ----

@Composable
private fun FeaturedDiscoverCard(course: Course, dimens: Dimensions, onClick: () -> Unit) {
    val accent = course.accent.tint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimens.cardElevation * 3, RoundedCornerShape(dimens.radiusLarge))
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface)
            .clickable(onClick = onClick)
            .padding(dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(dimens.radiusMedium))
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.68f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(dimens.spaceM))
        Column(modifier = Modifier.weight(1f)) {
            LevelChip(text = course.level.displayName, tint = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                course.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                maxLines = 1
            )
            Text(
                course.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 2
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accent)
    }
}

@Composable
private fun CompactDiscoverCard(course: Course, dimens: Dimensions, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = course.accent.tint
    Column(
        modifier = modifier
            .shadow(dimens.cardElevation * 2, RoundedCornerShape(dimens.radiusMedium))
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(AppColors.surface)
            .clickable(onClick = onClick)
            .padding(dimens.spaceS)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(dimens.radiusSmall))
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.68f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(dimens.spaceXS))
        LevelChip(text = course.level.displayName, tint = accent)
        Spacer(Modifier.height(4.dp))
        Text(
            course.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            maxLines = 2
        )
    }
}

@Composable
private fun LevelChip(text: String, tint: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = tint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}