package com.nexora_app.app.screens.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.models.Chapter
import com.nexora_app.app.models.ChapterExerciseContent
import com.nexora_app.app.models.Course
import com.nexora_app.app.models.CourseCatalog
import com.nexora_app.app.models.CourseLevel
import com.nexora_app.app.models.CourseProgress
import com.nexora_app.app.models.QuizScore
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.Dimensions
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.CoursesViewModel

// -----------------------------------------------------------------------
// Navigation route
//
// Mirrors iOS's CourseRoute enum (chapters -> chapterIntro ->
// exerciseIntro -> exercise -> quiz -> completion). Completion now carries
// an optional QuizScore — present for dialogue+quiz chapters, null for
// picture-vocab chapters (no scored "quiz" there).
// -----------------------------------------------------------------------

sealed class CourseRoute {
    data class Chapters(val course: Course) : CourseRoute()
    data class ChapterIntro(val course: Course, val chapter: Chapter) : CourseRoute()
    data class ExerciseIntro(val course: Course, val chapter: Chapter) : CourseRoute()
    data class Exercise(val course: Course, val chapter: Chapter) : CourseRoute()
    data class Quiz(val course: Course, val chapter: Chapter) : CourseRoute()
    data class Completion(val course: Course, val chapter: Chapter, val score: QuizScore? = null) : CourseRoute()
}

/** Small glyph shown in each level section's icon bubble. */
private val CourseLevel.icon
    get() = when (this) {
        CourseLevel.BEGINNER -> Icons.Filled.Eco
        CourseLevel.INTERMEDIATE -> Icons.Filled.LocalFireDepartment
        CourseLevel.ADVANCED -> Icons.Filled.EmojiEvents
    }

@Composable
fun CoursesScreen(
    navController: NavHostController,
    startCourse: Course? = null,
    coursesViewModel: CoursesViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    // Own back-stack for the chapters -> ... -> completion sub-flow.
    // Empty path = showing the level list (this screen's "root").
    // If a startCourse is supplied (deep link from Badges), seed the
    // stack straight to that course's chapter list instead of root.
    val path = remember(startCourse) {
        mutableStateListOf<CourseRoute>().apply {
            if (startCourse != null) add(CourseRoute.Chapters(startCourse))
        }
    }

    val progressByCourseId by coursesViewModel.progressByCourseId.collectAsState()

    BackHandler(enabled = path.isNotEmpty()) { path.removeAt(path.lastIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        if (path.isEmpty()) {
            CoursesRoot(
                dimens = dimens,
                progressByCourseId = progressByCourseId,
                onCourseClick = { course -> path.add(CourseRoute.Chapters(course)) }
            )
        } else {
            when (val route = path.last()) {
                is CourseRoute.Chapters -> ChapterListScreen(
                    course = route.course,
                    progress = progressByCourseId[route.course.id],
                    onBack = { path.removeAt(path.lastIndex) },
                    onChapterClick = { chapter -> path.add(CourseRoute.ChapterIntro(route.course, chapter)) }
                )
                is CourseRoute.ChapterIntro -> ChapterIntroScreen(
                    course = route.course,
                    chapter = route.chapter,
                    onClose = { path.clear() },
                    onContinue = { path.add(CourseRoute.ExerciseIntro(route.course, route.chapter)) }
                )

                is CourseRoute.ExerciseIntro -> ExerciseIntroScreen(
                    course = route.course,
                    chapter = route.chapter,
                    onClose = { path.clear() },
                    onStart = { path.add(CourseRoute.Exercise(route.course, route.chapter)) }
                )

                is CourseRoute.Exercise -> when (route.chapter.exercise) {
                    is ChapterExerciseContent.Dialogue -> DialogueScreen(
                        course = route.course,
                        chapter = route.chapter,
                        onBack = { path.removeAt(path.lastIndex) },
                        onClose = { path.clear() },
                        onStartQuiz = { path.add(CourseRoute.Quiz(route.course, route.chapter)) }
                    )
                    is ChapterExerciseContent.Picture -> PictureExerciseScreen(
                        course = route.course,
                        chapter = route.chapter,
                        onBack = { path.removeAt(path.lastIndex) },
                        onClose = { path.clear() },
                        // Picture chapters have no quiz score — completion
                        // is reached with score = null.
                        onFinish = { path.add(CourseRoute.Completion(route.course, route.chapter)) }
                    )
                }

                is CourseRoute.Quiz -> ExerciseQuizScreen(
                    chapter = route.chapter,
                    onClose = { path.clear() },
                    onFinish = { score -> path.add(CourseRoute.Completion(route.course, route.chapter, score)) }
                )

                is CourseRoute.Completion -> {
                    // Fire-and-forget write to users/{uid}/courseProgress/{courseId}.
                    // Keyed on course.id + chapter.number so it only fires once
                    // per time this route is actually reached (not on every
                    // recomposition) — arrayUnion on the repo side makes a
                    // second write here harmless anyway (e.g. re-entering the
                    // same completion screen), it's just unnecessary network
                    // traffic we skip by keying the effect this way. Retaking
                    // a chapter re-fires this with a fresh score, which just
                    // overwrites that one chapter's exerciseScores entry —
                    // completedChapterIds is untouched by arrayUnion either way.
                    LaunchedEffect(route.course.id, route.chapter.number, route.score) {
                        coursesViewModel.markChapterComplete(route.course.id, route.chapter.number)
                    }

                    ExerciseCompletionScreen(
                        course = route.course,
                        chapter = route.chapter,
                        score = route.score,
                        onGoBackToChapters = {
                            // path is [chapters, chapterIntro, exerciseIntro,
                            // exercise, (quiz,) completion] — drop everything
                            // after the first entry, landing back on the
                            // chapters list. Same trick as the iOS version.
                            while (path.size > 1) path.removeAt(path.lastIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoursesRoot(
    dimens: Dimensions,
    progressByCourseId: Map<String, CourseProgress>,
    onCourseClick: (Course) -> Unit
) {
    val populatedLevels = remember {
        CourseLevel.values().filter { CourseCatalog.coursesFor(it).isNotEmpty() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimens.spaceM, bottom = dimens.floatingButtonClearance)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = dimens.screenPaddingHorizontal)) {
                Text(
                    text = "Courses",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Pick a level and start learning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(dimens.spaceL))
        }

        populatedLevels.forEachIndexed { index, level ->
            item {
                LevelSection(
                    level = level,
                    courses = CourseCatalog.coursesFor(level),
                    progressByCourseId = progressByCourseId,
                    dimens = dimens,
                    onCourseClick = onCourseClick
                )
            }
            if (index < populatedLevels.size - 1) {
                item {
                    Spacer(modifier = Modifier.height(dimens.spaceM))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = dimens.screenPaddingHorizontal)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Spacer(modifier = Modifier.height(dimens.spaceM))
                }
            }
        }
    }
}

@Composable
private fun LevelSection(
    level: CourseLevel,
    courses: List<Course>,
    progressByCourseId: Map<String, CourseProgress>,
    dimens: Dimensions,
    onCourseClick: (Course) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(level.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(dimens.iconS))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = level.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            val countLabel = "${courses.size} ${if (courses.size == 1) "course" else "courses"}"
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(vertical = 5.dp, horizontal = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.spaceS))

        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPaddingHorizontal, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)
        ) {
            items(courses, key = { it.id }) { course ->
                CourseSquareCard(
                    course = course,
                    progress = progressByCourseId[course.id],
                    dimens = dimens,
                    onClick = { onCourseClick(course) }
                )
            }
        }
    }
}

@Composable
private fun CourseSquareCard(
    course: Course,
    progress: CourseProgress?,
    dimens: Dimensions,
    onClick: () -> Unit
) {
    val accentTint = course.accent.tint
    val panelCornerRadius = dimens.radiusMedium + 6.dp
    val iconSize = dimens.iconXL

    val completedCount = progress?.completedChapterIds?.distinct()?.size ?: 0
    val totalChapters = course.chapters.size
    val hasStarted = completedCount > 0
    val fraction = if (totalChapters > 0) {
        (completedCount.toFloat() / totalChapters.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .width(dimens.courseCardWidth)
            .shadow(dimens.cardElevation * 4, RoundedCornerShape(panelCornerRadius))
            .clip(RoundedCornerShape(panelCornerRadius))
            .background(AppColors.surface)
            .border(dimens.borderThin, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(panelCornerRadius))
            .clickable(onClick = onClick)
            .padding(dimens.spaceS)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(10.dp, RoundedCornerShape(dimens.radiusMedium))
                .clip(RoundedCornerShape(dimens.radiusMedium))
                .background(Brush.linearGradient(listOf(accentTint, accentTint.copy(alpha = 0.68f))))
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(dimens.radiusMedium)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize * 0.4f))
        }

        Spacer(modifier = Modifier.height(dimens.spaceXS))

        Text(
            text = course.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(accentTint.copy(alpha = 0.08f))
                .border(1.dp, accentTint.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

        if (hasStarted) {
            Spacer(modifier = Modifier.height(dimens.spaceXS))
            CourseProgressBar(
                fraction = fraction,
                completedCount = completedCount,
                totalChapters = totalChapters,
                accentTint = accentTint,
                dimens = dimens
            )
        }

        Spacer(modifier = Modifier.height(dimens.spaceXS))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.textHint.copy(alpha = 0.08f))
                .border(1.dp, AppColors.textHint.copy(alpha = 0.2f), RoundedCornerShape(50))
                .padding(vertical = 5.dp, horizontal = 9.dp)
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = AppColors.textHint, modifier = Modifier.size(9.dp))
            Spacer(modifier = Modifier.width(4.dp))
            val chapterLabel = "${course.chapters.size} ${if (course.chapters.size == 1) "Chapter" else "Chapters"}"
            Text(chapterLabel, style = MaterialTheme.typography.labelSmall, color = AppColors.textHint)
        }
    }
}

@Composable
private fun CourseProgressBar(
    fraction: Float,
    completedCount: Int,
    totalChapters: Int,
    accentTint: Color,
    dimens: Dimensions
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.textHint
            )
            Text(
                text = "$completedCount/$totalChapters",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentTint
            )
        }

        Spacer(modifier = Modifier.height(dimens.spaceXXS))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.progressBarHeight)
                .clip(RoundedCornerShape(50))
                .background(accentTint.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(accentTint, accentTint.copy(alpha = 0.75f))))
            )
        }
    }
}

