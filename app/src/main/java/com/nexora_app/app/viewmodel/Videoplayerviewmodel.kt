package com.nexora_app.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nexora_app.app.models.PlaybackSpeed
import com.nexora_app.app.models.VideoItem
import com.nexora_app.app.models.isMissingSource
import com.nexora_app.app.models.resolvedUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -----------------------------------------------------------------------
// PLAYER CONTROLLER
// -----------------------------------------------------------------------

/**
 * Owns the ExoPlayer instance, playback position, and the show/hide state
 * of VideoPlayerScreen's on-screen controls. Android equivalent of iOS's
 * VideoPlayerViewModel — same behavior spec:
 *
 *  - On play, the overlay is visible for 2 seconds, then fades out.
 *  - Tapping the video while the overlay is hidden just reveals it again.
 *  - Tapping while the overlay is visible toggles play/pause.
 *  - While paused, the overlay stays up indefinitely — it only starts the
 *    2s countdown again once playback resumes.
 *
 * NOT an androidx.lifecycle.ViewModel on purpose: it needs a `video` at
 * construction time, so it's created with `remember(videoId) { ... }` in
 * VideoPlayerScreen and torn down in a DisposableEffect — same lifetime
 * as iOS's @StateObject scoped to that one screen instance.
 *
 * When you move sources to Firestore/Storage later, nothing here needs to
 * change — `video.videoUrl` already accepts a remote URL.
 */
class VideoPlayerController(
    private val context: Context,
    val video: VideoItem
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var hideControlsJob: Job? = null
    private var positionPollJob: Job? = null
    private var isScrubbing = false

    // Compose-observable: PlayerView reads this in an AndroidView `update`
    // block, so it MUST be state-backed or the view never learns the
    // player exists once setUpPlayer() finishes.
    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set
    /** True if there's no bundled file AND no remote videoUrl to try — checked upfront, like iOS. */
    var fileMissing by mutableStateOf(false)
        private set
    /** True if we DID try to play something (local or remote) but it failed at runtime. */
    var playerError by mutableStateOf(false)
        private set
    var currentPositionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var showControls by mutableStateOf(true)
        private set

    private var _speed by mutableStateOf(PlaybackSpeed.NORMAL)
    var speed: PlaybackSpeed
        get() = _speed
        set(value) {
            _speed = value
            player?.playbackParameters = PlaybackParameters(value.value)
        }

    // MARK: Setup / teardown

    fun setUpPlayer() {
        if (video.isMissingSource(context)) {
            // No res/raw/<id>.mp4 and no remote videoUrl override — same
            // upfront guard as iOS's Bundle.main.url(forResource:) check.
            fileMissing = true
            return
        }

        val exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer.setMediaItem(MediaItem.fromUri(video.resolvedUri(context)))
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                this@VideoPlayerController.playerError = true
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                this@VideoPlayerController.isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    this@VideoPlayerController.durationMs = exoPlayer.duration.coerceAtLeast(0)
                }
            }
        })
        exoPlayer.playbackParameters = PlaybackParameters(speed.value)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        player = exoPlayer
        revealControls(autoHide = true)
        startPositionPolling()
    }

    fun tearDown() {
        hideControlsJob?.cancel()
        positionPollJob?.cancel()
        player?.release()
        player = null
        scope.cancel()
    }

    private fun startPositionPolling() {
        positionPollJob = scope.launch {
            while (isActive) {
                if (!isScrubbing) {
                    currentPositionMs = player?.currentPosition?.coerceAtLeast(0) ?: 0L
                }
                delay(250)
            }
        }
    }

    // MARK: Play / pause

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            revealControls(autoHide = false) // stay visible while paused
        } else {
            p.play()
            revealControls(autoHide = true)
        }
    }

    /** Tap anywhere on the video surface. */
    fun handleTap() {
        if (showControls) {
            togglePlayPause()
        } else {
            revealControls(autoHide = isPlaying)
        }
    }

    // MARK: Scrubbing (progress bar drag)

    fun beginScrubbing() {
        isScrubbing = true
        hideControlsJob?.cancel()
    }

    /** Called continuously while dragging — cheap, no seek yet. */
    fun scrub(positionMs: Long) {
        currentPositionMs = positionMs
    }

    /** Called when the drag ends — this is what actually seeks the player. */
    fun endScrubbing(positionMs: Long) {
        isScrubbing = false
        player?.seekTo(positionMs)
        currentPositionMs = positionMs
        if (isPlaying) revealControls(autoHide = true)
    }

    // MARK: Controls visibility

    private fun revealControls(autoHide: Boolean) {
        showControls = true
        hideControlsJob?.cancel()
        if (!autoHide) return
        hideControlsJob = scope.launch {
            delay(2_000)
            showControls = false
        }
    }
}

// -----------------------------------------------------------------------
// THUMBNAIL CACHE — used by VideoFeedScreen's cards
// -----------------------------------------------------------------------

/**
 * Generates and caches a frame from each video so feed cards can show a
 * real preview instead of a flat tint. Works for both bundled res/raw
 * videos and remote URLs — same `videoUrl` string either way. When you
 * move to Firestore/Storage, no change needed here.
 */
object VideoThumbnailCache {
    private val cache = mutableMapOf<String, ImageBitmap?>()

    suspend fun thumbnail(context: Context, video: VideoItem): ImageBitmap? {
        val key = video.id
        cache[key]?.let { return it }
        if (cache.containsKey(key)) return null // previously failed, don't retry every recomposition
        if (video.isMissingSource(context)) {
            cache[key] = null
            return null
        }

        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, video.resolvedUri(context))
                // A hair after zero avoids the occasional all-black first
                // frame some encoders produce right at time zero.
                val bitmap: Bitmap? = retriever.getFrameAtTime(100_000)
                val imageBitmap = bitmap?.asImageBitmap()
                cache[key] = imageBitmap
                imageBitmap
            } catch (e: Exception) {
                cache[key] = null
                null
            } finally {
                retriever.release()
            }
        }
    }
}

// -----------------------------------------------------------------------
// TIME FORMATTING — matches iOS's TimeInterval.formattedAsPlaybackTime
// -----------------------------------------------------------------------

/** Formats milliseconds as "m:ss", or "h:mm:ss" once past an hour. */
fun Long.formattedAsPlaybackTime(): String {
    if (this < 0) return "0:00"
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}