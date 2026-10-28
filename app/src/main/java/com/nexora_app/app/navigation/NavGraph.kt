package com.nexora_app.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.nexora_app.app.components.BottomNavBar
import com.nexora_app.app.models.CourseCatalog
import com.nexora_app.app.screens.onboarding.SplashScreen
import com.nexora_app.app.screens.authscreens.SignInScreen
import com.nexora_app.app.screens.authscreens.SignUpScreen
import com.nexora_app.app.screens.authscreens.ResetPasswordScreen
import com.nexora_app.app.screens.words.FlashcardDeckScreen
import com.nexora_app.app.screens.words.StartLearningScreen
import com.nexora_app.app.screens.words.WordsScreen
import com.nexora_app.app.screens.home.HomeScreen
import com.nexora_app.app.screens.courses.CoursesScreen
import com.nexora_app.app.screens.videos.VideoFeedScreen
import com.nexora_app.app.screens.videos.VideoPlayerScreen
import com.nexora_app.app.screens.profile.ProfileScreen
import com.nexora_app.app.screens.profile.BadgesScreen
import com.nexora_app.app.screens.profile.SettingsScreen
import com.nexora_app.app.screens.profile.EditProfileScreen
import com.nexora_app.app.screens.profile.ChangeAvatarScreen
import com.nexora_app.app.viewmodel.WordsViewModel
import com.nexora_app.app.viewmodel.BadgesViewModel
import com.nexora_app.app.viewmodel.EditProfileViewModel
import com.nexora_app.app.screens.settings.DeleteAccountScreen
import com.nexora_app.app.screens.settings.ChangePasswordScreen

/**
 * Root composable: hosts the NavHost and shows/hides the bottom nav bar
 * depending on the current route. Call this from MainActivity inside EllieTheme.
 */
@Composable
fun EllieApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Splash.route) {
                SplashScreen(navController)
            }

            // ---- Auth ----
            composable(Routes.SignIn.route) {
                SignInScreen(navController)
            }
            composable(Routes.SignUp.route) {
                SignUpScreen(navController)
            }
            composable(Routes.ResetPassword.route) {
                ResetPasswordScreen(navController)
            }
            composable(Routes.ChangePassword.route) {
                ChangePasswordScreen(navController)
            }

            // ---- Main tabs ----
            composable(Routes.Home.route) {
                HomeScreen(navController)
            }
            composable(Routes.Courses.route) {
                CoursesScreen(navController)
            }

            // Deep link into a specific course's chapter list (e.g. from
            // BadgesScreen's "Go to course" button). Separate from the
            // plain Courses tab route above so the bottom-nav tab itself
            // still opens on the level list as before.
            composable(
                route = Routes.CourseDetail.route,
                arguments = listOf(navArgument(Routes.CourseDetail.ARG_COURSE_ID) {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString(Routes.CourseDetail.ARG_COURSE_ID)
                val startCourse = remember(courseId) { courseId?.let { CourseCatalog.courseById(it) } }
                CoursesScreen(navController, startCourse = startCourse)
            }

            // ---- Words flow (nested graph — one WordsViewModel instance
            // shared across all three screens, scoped to this graph's own
            // back stack entry so it survives navigating between them but
            // is cleared once the whole graph is popped) ----
            navigation(
                startDestination = Routes.Words.route,
                route = Routes.WORDS_GRAPH_ROUTE
            ) {
                composable(Routes.Words.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.WORDS_GRAPH_ROUTE)
                    }
                    val wordsViewModel: WordsViewModel = viewModel(parentEntry)
                    WordsScreen(navController, wordsViewModel)
                }
                composable(Routes.StartLearning.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.WORDS_GRAPH_ROUTE)
                    }
                    val wordsViewModel: WordsViewModel = viewModel(parentEntry)
                    StartLearningScreen(navController, wordsViewModel)
                }
                composable(Routes.Flashcards.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.WORDS_GRAPH_ROUTE)
                    }
                    val wordsViewModel: WordsViewModel = viewModel(parentEntry)
                    val source = backStackEntry.arguments
                        ?.getString(Routes.Flashcards.ARG_SOURCE)
                        ?: Routes.Flashcards.SOURCE_KNOW
                    FlashcardDeckScreen(navController, wordsViewModel, source)
                }
            }

            composable(Routes.Videos.route) {
                VideoFeedScreen(navController)
            }

            // Full-screen player, opened from VideoFeedScreen's play button.
            // Deliberately NOT in Routes.bottomNavRoutes, so the bottom nav
            // bar is hidden automatically while a video is playing.
            composable(
                route = Routes.VideoPlayer.route,
                arguments = listOf(navArgument(Routes.VideoPlayer.ARG_VIDEO_ID) {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString(Routes.VideoPlayer.ARG_VIDEO_ID)
                VideoPlayerScreen(navController, videoId)
            }

            composable(Routes.Profile.route) {
                ProfileScreen(navController)
            }

            composable(Routes.Badges.route) {
                val badgesViewModel: BadgesViewModel = viewModel()
                val uiState by badgesViewModel.uiState.collectAsState()
                BadgesScreen(
                    unlockedBadgeIds = uiState.unlockedBadgeIds,
                    progressByCourseId = uiState.progressByCourseId,
                    onBack = { navController.popBackStack() },
                    onGoToCourse = { course ->
                        navController.navigate(Routes.CourseDetail.createRoute(course.id)) {
                            popUpTo(Routes.Home.route)
                        }
                    }
                )
            }

            // ---- Settings ----
            composable(Routes.Settings.route) {
                SettingsScreen(navController)
            }

            // ---- Edit profile flow (nested graph — one EditProfileViewModel
            // instance shared across EditProfile and ChangeAvatar, same
            // pattern as the Words graph above) ----
            navigation(
                startDestination = Routes.EditProfile.route,
                route = Routes.EDIT_PROFILE_GRAPH_ROUTE
            ) {
                composable(Routes.EditProfile.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.EDIT_PROFILE_GRAPH_ROUTE)
                    }
                    val editProfileViewModel: EditProfileViewModel = viewModel(parentEntry)
                    EditProfileScreen(navController, editProfileViewModel)
                }
                composable(Routes.ChangeAvatar.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.EDIT_PROFILE_GRAPH_ROUTE)
                    }
                    val editProfileViewModel: EditProfileViewModel = viewModel(parentEntry)
                    ChangeAvatarScreen(navController, editProfileViewModel)
                }
            }

            // ---- Detail screens ----
            composable(Routes.LessonDetail.route) { backStackEntry ->
                val lessonId = backStackEntry.arguments
                    ?.getString(Routes.LessonDetail.ARG_LESSON_ID)
                // TODO: LessonDetailScreen(navController, lessonId)
            }
            composable(Routes.VocabularyPreview.route) { backStackEntry ->
                val wordListId = backStackEntry.arguments
                    ?.getString(Routes.VocabularyPreview.ARG_WORD_LIST_ID)
                // TODO: VocabularyPreviewScreen(navController, wordListId)
            }
            composable(Routes.DeleteAccount.route) {
                DeleteAccountScreen(navController)
            }
        }
    }
}