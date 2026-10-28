package com.nexora_app.app.navigation

/**
 * Every navigable destination in the app. Keep route strings here only —
 * never inline "screens/home" style strings inside a screen or NavHost.
 */
sealed class Routes(val route: String) {

    // ---- Onboarding ----
    data object Splash : Routes("splash")

    // ---- Auth ----
    data object SignIn : Routes("sign_in")
    data object SignUp : Routes("sign_up")
    data object ResetPassword : Routes("reset_password")
    data object ChangePassword : Routes("change_password")

    // ---- Main (bottom nav) ----
    data object Home : Routes("home")
    data object Courses : Routes("courses")
    data object Words : Routes("words")
    data object Videos : Routes("videos")
    data object Profile : Routes("profile")

    data object Settings : Routes("Settings")
    data object Badges : Routes("badges")

    // ---- Course detail (deep link into a specific course's chapter list,
    // e.g. from BadgesScreen's "Go to course" button). Separate from the
    // plain Courses tab route so the bottom-nav tab itself still opens on
    // the level list as before. ----
    data object CourseDetail : Routes("courses/{courseId}") {
        fun createRoute(courseId: String) = "courses/$courseId"
        const val ARG_COURSE_ID = "courseId"
    }

    // ---- Detail / secondary screens ----
    data object LessonDetail : Routes("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/$lessonId"
        const val ARG_LESSON_ID = "lessonId"
    }


    data object VocabularyPreview : Routes("vocabulary_preview/{wordListId}") {
        fun createRoute(wordListId: String) = "vocabulary_preview/$wordListId"
        const val ARG_WORD_LIST_ID = "wordListId"
    }

    // ---- Video player (opened from VideoFeedScreen's play button) ----
    data object VideoPlayer : Routes("video_player/{videoId}") {
        fun createRoute(videoId: String) = "video_player/$videoId"
        const val ARG_VIDEO_ID = "videoId"
    }

    // ---- Words flow (nested graph — shares one WordsViewModel instance
    // across Words, StartLearning, and Flashcards, same as iOS's shared
    // @StateObject + path array pattern) ----

    data object StartLearning : Routes("start_learning")

    /**
     * `source` identifies which word set to show: "know", "dont_know", or
     * a Word.Difficulty name lowercased ("beginner"/"intermediate"/"advanced").
     * FlashcardDeckScreen resolves the actual word list from the shared
     * WordsViewModel using this key, rather than serializing a word list
     * through the nav arg.
     */
    data object Flashcards : Routes("flashcards/{source}") {
        fun createRoute(source: String) = "flashcards/$source"
        const val ARG_SOURCE = "source"

        const val SOURCE_KNOW = "know"
        const val SOURCE_DONT_KNOW = "dont_know"
    }

    data object DeleteAccount : Routes("delete_account")

    // ---- Edit profile flow (nested graph — shares one EditProfileViewModel
    // instance across EditProfile and ChangeAvatar, same pattern as the
    // Words graph above, so a change made on ChangeAvatar is immediately
    // reflected back on EditProfile without a re-fetch) ----
    data object EditProfile : Routes("edit_profile")
    data object ChangeAvatar : Routes("change_avatar")

    companion object {
        /** Routes that should show the bottom nav bar. */
        const val WORDS_GRAPH_ROUTE = "words_graph"
        const val EDIT_PROFILE_GRAPH_ROUTE = "edit_profile_graph"
        val bottomNavRoutes = setOf(
            Home.route, Courses.route, CourseDetail.route, Words.route, Videos.route, Profile.route
        )
    }

}