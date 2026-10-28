package com.nexora_app.app.models

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nexora_app.app.ui.theme.AppColors

// -----------------------------------------------------------------------
// TOPICS
// -----------------------------------------------------------------------

/**
 * 10 topics to start — mirrors the iOS VideoTopic exactly (including the
 * "Kids & Beginners" 10th case) so the catalog stays in sync across
 * platforms. Add a case + two VideoCatalog entries to extend.
 */
enum class VideoTopic(val displayName: String) {
    PRONUNCIATION("Pronunciation"),
    GRAMMAR("Grammar"),
    VOCABULARY("Vocabulary"),
    LISTENING_SPEAKING("Listening & Speaking"),
    BUSINESS_ENGLISH("Business English"),
    IDIOMS_PHRASES("Idioms & Phrases"),
    TENSES("Tenses"),
    EVERYDAY_CONVERSATION("Everyday Conversation"),
    TRAVEL_ENGLISH("Travel English"),
    KIDS_BEGINNERS("Kids & Beginners")
}

/**
 * Android equivalent of iOS's `VideoTopic.tint` — cycles through the
 * brand gradient stops so every topic still reads as "our app" instead of
 * borrowing arbitrary per-topic colors.
 */
@Composable
fun VideoTopic.tint(): Color = when (this) {
    VideoTopic.PRONUNCIATION, VideoTopic.BUSINESS_ENGLISH, VideoTopic.TENSES ->
        AppColors.accentVideos
    VideoTopic.GRAMMAR, VideoTopic.IDIOMS_PHRASES, VideoTopic.TRAVEL_ENGLISH ->
        AppColors.accentCourses
    VideoTopic.VOCABULARY, VideoTopic.LISTENING_SPEAKING,
    VideoTopic.EVERYDAY_CONVERSATION, VideoTopic.KIDS_BEGINNERS ->
        AppColors.accentProfile
}

// -----------------------------------------------------------------------
// FILTER
// -----------------------------------------------------------------------

/** What the feed is currently filtered to. Matches iOS's VideoFilter. */
sealed class VideoFilter {
    data object All : VideoFilter()
    data object Favorites : VideoFilter()
    data class Topic(val topic: VideoTopic) : VideoFilter()
}

// -----------------------------------------------------------------------
// VIDEO ITEM
// -----------------------------------------------------------------------

data class VideoItem(
    val id: String,
    val title: String,
    val topic: VideoTopic,
    val duration: String,
    /**
     * Leave null (the default) to look up a bundled res/raw file whose
     * name matches `id` — e.g. id "pronunciation_1" looks for
     * app/src/main/res/raw/pronunciation_1.mp4. This is the Android
     * equivalent of iOS's `videoFileName` + `Bundle.main.url(forResource:)`.
     *
     * Set this to a real "https://..." URL instead once a video is hosted
     * remotely (Firebase Storage, CDN, etc) — VideoPlayerController and
     * VideoThumbnailCache both read it the same way either way.
     */
    val videoUrl: String? = null
) {
    val subtitle: String get() = topic.displayName
}

/**
 * Resolves this video's playable/thumbnail-able Uri: the explicit
 * `videoUrl` if set, otherwise a `res/raw/<id>` lookup using the app's
 * OWN package name at runtime (never hardcode it — it must match
 * whatever `applicationId` your build.gradle actually uses).
 */
fun VideoItem.resolvedUri(context: Context): Uri =
    if (videoUrl != null) {
        Uri.parse(videoUrl)
    } else {
        Uri.parse("android.resource://${context.packageName}/raw/$id")
    }

/**
 * True if this video has no bundled res/raw file matching its id AND no
 * remote videoUrl override — i.e. nothing to actually play yet. Mirrors
 * iOS's upfront `Bundle.main.url(forResource:) == nil` check instead of
 * waiting for a runtime player error.
 */
fun VideoItem.isMissingSource(context: Context): Boolean {
    if (videoUrl != null) return false
    val resId = context.resources.getIdentifier(id, "raw", context.packageName)
    return resId == 0
}

/**
 * Hand-edit this list to add your real videos for now. Swap for a
 * Firestore-backed repository later — VideoFeedScreen and
 * VideoPlayerViewModel only ever read `VideoCatalog.all`, so nothing else
 * has to change.
 */
object VideoCatalog {
    val all: List<VideoItem> = listOf(
        VideoItem("pronunciation_1", "Pronunciation: Getting Started", VideoTopic.PRONUNCIATION, "04:32"),
        VideoItem("pronunciation_2", "Pronunciation: Common Mistakes", VideoTopic.PRONUNCIATION, "07:15"),
        VideoItem("grammar_1", "Grammar: Getting Started", VideoTopic.GRAMMAR, "04:32"),
        VideoItem("grammar_2", "Grammar: Common Mistakes", VideoTopic.GRAMMAR, "07:15"),
        VideoItem("vocabulary_1", "Vocabulary: Getting Started", VideoTopic.VOCABULARY, "04:32"),
        VideoItem("vocabulary_2", "Vocabulary: Common Mistakes", VideoTopic.VOCABULARY, "07:15"),
        VideoItem("listening_speaking_1", "Listening & Speaking: Getting Started", VideoTopic.LISTENING_SPEAKING, "04:32"),
        VideoItem("listening_speaking_2", "Listening & Speaking: Common Mistakes", VideoTopic.LISTENING_SPEAKING, "07:15"),
        VideoItem("business_english_1", "Business English: Getting Started", VideoTopic.BUSINESS_ENGLISH, "04:32"),
        VideoItem("business_english_2", "Business English: Common Mistakes", VideoTopic.BUSINESS_ENGLISH, "07:15"),
        VideoItem("idioms_phrases_1", "Idioms & Phrases: Getting Started", VideoTopic.IDIOMS_PHRASES, "04:32"),
        VideoItem("idioms_phrases_2", "Idioms & Phrases: Common Mistakes", VideoTopic.IDIOMS_PHRASES, "07:15"),
        VideoItem("tenses_1", "Tenses: Getting Started", VideoTopic.TENSES, "04:32"),
        VideoItem("tenses_2", "Tenses: Common Mistakes", VideoTopic.TENSES, "07:15"),
        VideoItem("everyday_conversation_1", "Everyday Conversation: Getting Started", VideoTopic.EVERYDAY_CONVERSATION, "04:32"),
        VideoItem("everyday_conversation_2", "Everyday Conversation: Common Mistakes", VideoTopic.EVERYDAY_CONVERSATION, "07:15"),
        VideoItem("travel_english_1", "Travel English: Getting Started", VideoTopic.TRAVEL_ENGLISH, "04:32"),
        VideoItem("travel_english_2", "Travel English: Common Mistakes", VideoTopic.TRAVEL_ENGLISH, "07:15"),
        VideoItem("kids_beginners_1", "Kids & Beginners: Getting Started", VideoTopic.KIDS_BEGINNERS, "04:32"),
        VideoItem("kids_beginners_2", "Kids & Beginners: Common Mistakes", VideoTopic.KIDS_BEGINNERS, "07:15")
    )
}

// -----------------------------------------------------------------------
// PLAYBACK SPEED
// -----------------------------------------------------------------------

/** Matches iOS's PlaybackSpeed exactly: 1x / 1.5x / 2x. */
enum class PlaybackSpeed(val value: Float, val label: String) {
    NORMAL(1f, "1x"),
    ONE_AND_HALF(1.5f, "1.5x"),
    FAST(2f, "2x")
}