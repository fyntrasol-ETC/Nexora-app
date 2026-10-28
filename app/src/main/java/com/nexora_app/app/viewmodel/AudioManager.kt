package com.nexora_app.app.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import android.util.Log
import java.util.Locale

/**
 * Wraps Android's built-in TextToSpeech so the speaker button on the
 * flashcard reads the word aloud — Android equivalent of iOS's
 * AudioManager (AVSpeechSynthesizer). No audio files to bundle or download.
 *
 * Must be initialized once with an application Context before first use —
 * call AudioManager.init(applicationContext) from MainActivity.onCreate.
 * Uses applicationContext specifically to avoid leaking an Activity.
 */
object AudioManager {

    private const val TAG = "AudioManager"

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init(context: Context) {
        if (tts != null) return // already initialized
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                val result = tts?.setLanguage(Locale.US)
                Log.d(TAG, "TTS init SUCCESS, setLanguage(US) result = $result")
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Engine loaded, but en-US voice data isn't installed.
                    // pronounce() will otherwise fail silently on speak().
                    Log.e(TAG, "en-US language data missing or not supported on this device/emulator")
                    isReady = false
                } else {
                    tts?.setSpeechRate(0.95f)
                }
            } else {
                Log.e(TAG, "TTS init FAILED, status = $status (no TTS engine installed?)")
            }
        }
    }

    fun pronounce(text: String) {
        if (!isReady) {
            Log.w(TAG, "pronounce('$text') called but isReady = false — ignoring")
            return
        }
        // QUEUE_FLUSH stops anything mid-utterance so rapid taps (or
        // swiping right after tapping speaker) don't queue speech behind
        // a card that's already advanced.
        val result = tts?.speak(text, QUEUE_FLUSH, null, "word_pronunciation")
        Log.d(TAG, "speak('$text') result = $result")
    }

    /** Call from MainActivity.onDestroy() to release the TTS engine cleanly. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}