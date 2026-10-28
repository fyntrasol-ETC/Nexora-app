package com.nexora_app.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Provided by ThemeManager (see Theme.kt). Every screen reads colors through
 * the AppColors object below instead of hardcoding hex values, so flipping
 * this local instantly re-themes the whole app.
 */
val LocalIsDarkTheme = compositionLocalOf { false }

// ---------------------------------------------------------------------------
// RAW PALETTE — private, only referenced from the composable getters below
// ---------------------------------------------------------------------------

private val GradientBlueLight = Color(0xFF7B8FF0)
private val GradientPurpleLight = Color(0xFFA07FE0)
private val GradientPinkLight = Color(0xFFF993A8)

private val GradientBlueDark = Color(0xFF2B3570)
private val GradientPurpleDark = Color(0xFF4A3570)
private val GradientPinkDark = Color(0xFF7A3548)

private val NavyLight = Color(0xFF1B2A6B)
private val NavyDark = Color(0xFF3B4FA8)

private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF1C1C24)
private val BackgroundLight = Color(0xFFF4F6FB)
private val BackgroundDark = Color(0xFF121216)
private val SurfaceVariantLight = Color(0xFFF0F1F7)
private val SurfaceVariantDark = Color(0xFF26262E)

private val TextPrimaryLight = Color(0xFF1A1A2E)
private val TextPrimaryDark = Color(0xFFF2F2F5)
private val TextSecondaryLight = Color(0xFF6B7280)
private val TextSecondaryDark = Color(0xFFA0A0AC)
private val TextHintLight = Color(0xFF9CA3AF)
private val TextHintDark = Color(0xFF6E6E78)

private val BorderLight = Color(0xFFE5E7EB)
private val BorderDark = Color(0xFF34343E)

private val AccentPurpleLight = Color(0xFF7C6FE0)
private val AccentPurpleDark = Color(0xFF9A8DF0)
private val AccentOrangeLight = Color(0xFFF5B942)
private val AccentOrangeDark = Color(0xFFE0A939)
private val AccentPinkLight = Color(0xFFE8637F)
private val AccentPinkDark = Color(0xFFD9536E)
private val AccentBlueLight = Color(0xFF4A6CF0)
private val AccentBlueDark = Color(0xFF5C7CFA)
private val AccentCoralLight = Color(0xFFEE7A5F)
private val AccentCoralDark = Color(0xFFD96C50)

// Status colors (kept identical light/dark on purpose)
val SuccessGreen = Color(0xFF34C759)
val ErrorRed = Color(0xFFE0334E)
val LockedGray = Color(0xFFB9BCC6)
val CrownGold = Color(0xFFF5B942)
val DarkYellow = Color(0xFFD9A441)

// ---------------------------------------------------------------------------
// NOTE: the old AVATAR PALETTE section (AvatarNavy/AvatarBlue/etc + the
// avatarColor(role: AvatarColorRole) function) lived here. It's been
// removed as part of the DiceBear migration — MascotAvatarType.kt (which
// declared AvatarColorRole) is deleted, and avatars are now rendered as
// remote images via components/DiceBearAvatar.kt, so there's no local
// color palette to resolve anymore.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// APP COLORS — read these from screens, e.g. AppColors.textPrimary
// ---------------------------------------------------------------------------

object AppColors {

    val background: Color
        @Composable get() = if (LocalIsDarkTheme.current) BackgroundDark else BackgroundLight

    val surface: Color
        @Composable get() = if (LocalIsDarkTheme.current) SurfaceDark else SurfaceLight

    val surfaceVariant: Color
        @Composable get() = if (LocalIsDarkTheme.current) SurfaceVariantDark else SurfaceVariantLight

    val textPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) TextPrimaryDark else TextPrimaryLight

    val textSecondary: Color
        @Composable get() = if (LocalIsDarkTheme.current) TextSecondaryDark else TextSecondaryLight

    val textHint: Color
        @Composable get() = if (LocalIsDarkTheme.current) TextHintDark else TextHintLight

    val border: Color
        @Composable get() = if (LocalIsDarkTheme.current) BorderDark else BorderLight

    val divider: Color
        @Composable get() = if (LocalIsDarkTheme.current) BorderDark else BorderLight

    val primary: Color
        @Composable get() = if (LocalIsDarkTheme.current) NavyDark else NavyLight

    val onPrimary: Color
        @Composable get() = Color.White

    val accentCourses: Color
        @Composable get() = if (LocalIsDarkTheme.current) AccentPurpleDark else AccentPurpleLight

    val accentWords: Color
        @Composable get() = if (LocalIsDarkTheme.current) AccentOrangeDark else AccentOrangeLight

    val accentProfile: Color
        @Composable get() = if (LocalIsDarkTheme.current) AccentPinkDark else AccentPinkLight

    val accentVideos: Color
        @Composable get() = if (LocalIsDarkTheme.current) AccentBlueDark else AccentBlueLight

    val accentCoral: Color
        @Composable get() = if (LocalIsDarkTheme.current) AccentCoralDark else AccentCoralLight

    val success: Color = SuccessGreen
    val error: Color = ErrorRed
    val locked: Color = LockedGray
    val crownGold: Color = CrownGold

    // Flashcard swipe states: right/"I know" tints green, left/"I don't
    // know" tints dark yellow. Fixed across light/dark like other status colors.
    val knowAccent: Color = SuccessGreen
    val dontKnowAccent: Color = DarkYellow

    val onGradient: Color = Color.White

    val brandGradient: Brush
        @Composable get() = if (LocalIsDarkTheme.current) {
            Brush.verticalGradient(listOf(GradientBlueDark, GradientPurpleDark, GradientPinkDark))
        } else {
            Brush.verticalGradient(listOf(GradientBlueLight, GradientPurpleLight, GradientPinkLight))
        }

    // Horizontal gradient used behind the floating bottom nav bar — mirrors
    // the SwiftUI CustomTabBar's [gradientTop -> primaryButton(navy)] fill.
    val bottomNavGradient: Brush
        @Composable get() = if (LocalIsDarkTheme.current) {
            Brush.horizontalGradient(listOf(GradientBlueDark, NavyDark))
        } else {
            Brush.horizontalGradient(listOf(GradientBlueLight, NavyLight))
        }

    val cardBorderSubtle: Color
        @Composable get() = if (LocalIsDarkTheme.current) BorderDark else BorderLight
}

// ---------------------------------------------------------------------------
// DIMENSIONS
// ---------------------------------------------------------------------------

enum class DeviceSize { COMPACT, MEDIUM, EXPANDED }

data class Dimensions(
    val spaceXXS: Dp,
    val spaceXS: Dp,
    val spaceS: Dp,
    val spaceM: Dp,
    val spaceL: Dp,
    val spaceXL: Dp,
    val spaceXXL: Dp,

    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,

    val radiusSmall: Dp,
    val radiusMedium: Dp,
    val radiusLarge: Dp,
    val radiusPill: Dp,

    val buttonHeight: Dp,
    val textFieldHeight: Dp,
    val bottomNavHeight: Dp,
    val topBarHeight: Dp,

    val iconXS: Dp,
    val iconS: Dp,
    val iconM: Dp,
    val iconL: Dp,
    val iconXL: Dp,

    val avatarSmall: Dp,
    val avatarMedium: Dp,
    val avatarLarge: Dp,

    val courseCardWidth: Dp,
    val courseCardHeight: Dp,
    val featuredCardHeight: Dp,
    val cardElevation: Dp,

    val progressBarHeight: Dp,
    val weeklyChartBarWidth: Dp,
    val weeklyChartHeight: Dp,

    val gridColumns: Int,
    val maxContentWidth: Dp,

    val borderThin: Dp,
    val borderMedium: Dp,

    val progressStrokeWidth: Dp,

    // Words / Flashcard flow
    val flashcardSize: Dp,             // square flashcard in the study deck
    val floatingButtonClearance: Dp,   // space reserved above bottom nav for floating CTAs

    // Grid thumbnails (previously hardcoded per-screen — now dimens-driven
    // so they scale with device size like everything else)
    val courseGridThumbnailHeight: Dp, // CoursesScreen's small grid card image
    val videoGridThumbnailHeight: Dp,  // VideoFeedScreen's small grid card image

    val fontScale: Float
)

private val CompactDimensions = Dimensions(
    spaceXXS = 2.dp,
    spaceXS = 4.dp,
    spaceS = 8.dp,
    spaceM = 16.dp,
    spaceL = 28.dp,
    spaceXL = 32.dp,
    spaceXXL = 48.dp,

    screenPaddingHorizontal = 20.dp,
    screenPaddingVertical = 16.dp,

    radiusSmall = 8.dp,
    radiusMedium = 16.dp,
    radiusLarge = 24.dp,
    radiusPill = 28.dp,

    buttonHeight = 52.dp,
    textFieldHeight = 56.dp,
    bottomNavHeight = 80.dp,
    topBarHeight = 56.dp,

    iconXS = 16.dp,
    iconS = 20.dp,
    iconM = 28.dp,
    iconL = 40.dp,
    iconXL = 72.dp,

    avatarSmall = 32.dp,
    avatarMedium = 64.dp,
    avatarLarge = 88.dp,

    courseCardWidth = 156.dp,
    courseCardHeight = 140.dp,
    featuredCardHeight = 120.dp,
    cardElevation = 2.dp,

    progressBarHeight = 8.dp,
    weeklyChartBarWidth = 20.dp,
    weeklyChartHeight = 120.dp,

    gridColumns = 2,
    maxContentWidth = Dp.Unspecified,

    borderThin = 1.dp,
    borderMedium = 2.dp,

    progressStrokeWidth = 2.dp,

    flashcardSize = 320.dp,
    floatingButtonClearance = 96.dp, // bottomNavHeight(80) + spaceM(16)

    courseGridThumbnailHeight = 80.dp,
    videoGridThumbnailHeight = 70.dp,

    fontScale = 1.0f
)

private val MediumDimensions = CompactDimensions.copy(
    screenPaddingHorizontal = 32.dp,
    screenPaddingVertical = 20.dp,
    buttonHeight = 56.dp,
    textFieldHeight = 58.dp,
    bottomNavHeight = 68.dp,
    avatarLarge = 100.dp,
    courseCardWidth = 180.dp,
    courseCardHeight = 156.dp,
    featuredCardHeight = 140.dp,
    gridColumns = 3,
    maxContentWidth = 560.dp,
    flashcardSize = 380.dp,
    floatingButtonClearance = 84.dp, // bottomNavHeight(68) + spaceM(16, inherited)
    courseGridThumbnailHeight = 96.dp,
    videoGridThumbnailHeight = 84.dp,
    fontScale = 1.08f
)

private val ExpandedDimensions = CompactDimensions.copy(
    spaceM = 20.dp,
    spaceL = 32.dp,
    spaceXL = 40.dp,
    spaceXXL = 64.dp,

    screenPaddingHorizontal = 64.dp,
    screenPaddingVertical = 24.dp,

    radiusMedium = 20.dp,
    radiusLarge = 28.dp,

    buttonHeight = 60.dp,
    textFieldHeight = 60.dp,
    bottomNavHeight = 72.dp,
    topBarHeight = 64.dp,

    iconM = 28.dp,
    iconL = 40.dp,
    iconXL = 88.dp,

    avatarSmall = 40.dp,
    avatarMedium = 56.dp,
    avatarLarge = 120.dp,

    courseCardWidth = 220.dp,
    courseCardHeight = 180.dp,
    featuredCardHeight = 180.dp,
    cardElevation = 4.dp,

    progressBarHeight = 10.dp,
    weeklyChartBarWidth = 28.dp,
    weeklyChartHeight = 160.dp,

    gridColumns = 4,
    maxContentWidth = 640.dp,

    flashcardSize = 440.dp,
    floatingButtonClearance = 92.dp, // bottomNavHeight(72) + spaceM(20)

    courseGridThumbnailHeight = 120.dp,
    videoGridThumbnailHeight = 104.dp,

    fontScale = 1.15f
)

val LocalDimens = staticCompositionLocalOf { CompactDimensions }
val LocalDeviceSize = compositionLocalOf { DeviceSize.COMPACT }

@Composable
fun rememberDeviceSize(): DeviceSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> DeviceSize.COMPACT
        widthDp < 840 -> DeviceSize.MEDIUM
        else -> DeviceSize.EXPANDED
    }
}

fun dimensionsFor(deviceSize: DeviceSize): Dimensions = when (deviceSize) {
    DeviceSize.COMPACT -> CompactDimensions
    DeviceSize.MEDIUM -> MediumDimensions
    DeviceSize.EXPANDED -> ExpandedDimensions
}