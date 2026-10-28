package com.nexora_app.app.models

/**
 * Replaces the old Canvas-drawn MascotAvatarType system. Avatars are now
 * generated remotely by DiceBear (https://www.dicebear.com) from a "seed"
 * string — same seed always produces the same image, so we don't need to
 * store or ship any image assets ourselves.
 *
 * Firestore field: users/{uid}.avatarSeed (String), replacing the old
 * avatarId (String) field from MascotAvatarType.
 *
 * MIGRATION NOTE: a user with a legacy avatarId already stored in Firestore
 * (e.g. "bear_gold") will simply fall back to DEFAULT_SEED the next time
 * avatarUrlFor() resolves it — no crash, avatar just resets to default once.
 */
object AvatarOptions {

    // DiceBear style — "notionists" gives friendly, hand-drawn-looking
    // people avatars. Full style catalog: https://www.dicebear.com/styles
    private const val STYLE = "notionists"

    // PNG (not SVG) endpoint deliberately — both Coil (Android) and
    // AsyncImage (iOS/SwiftUI) can load a PNG URL directly with zero extra
    // decoder setup, unlike SVG which needs a special image loader on each
    // platform.
    private const val BASE_URL = "https://api.dicebear.com/9.x/$STYLE/png"

    const val DEFAULT_SEED = "nexora-01"

    /**
     * 24 curated seeds for the "Change avatar" picker grid. These are just
     * arbitrary stable strings — swap in whatever you like, order doesn't
     * matter, and you can add more any time without touching any other code.
     */
    val curatedSeeds: List<String> = listOf(
        "nexora-01", "nexora-02", "nexora-03", "nexora-04",
        "nexora-05", "nexora-06", "nexora-07", "nexora-08",
        "nexora-09", "nexora-10", "nexora-11", "nexora-12",
        "nexora-13", "nexora-14", "nexora-15", "nexora-16",
        "nexora-17", "nexora-18", "nexora-19", "nexora-20",
        "nexora-21", "nexora-22", "nexora-23", "nexora-24"
    )

    /**
     * Builds the image URL for a given seed. Pass a fixed pixel size so
     * DiceBear returns an appropriately-sized PNG rather than a huge default
     * — cheaper to download and decode. 256 covers our largest avatar slot
     * (avatarLarge) comfortably even on the EXPANDED dimension set.
     */
    fun urlFor(seed: String, size: Int = 256): String =
        "$BASE_URL?seed=$seed&size=$size&backgroundType=gradientLinear"

    /** Falls back to DEFAULT_SEED for null/blank/legacy ids. */
    fun resolveSeed(stored: String?): String =
        stored?.takeIf { it.isNotBlank() && it in curatedSeeds } ?: DEFAULT_SEED
}