package com.nexora_app.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/**
 * Base sizes tuned for COMPACT (phone). Multiplied by dimens.fontScale
 * (1.0 / 1.08 / 1.15) for medium/expanded devices.
 */
@Composable
fun appTypography(dimens: Dimensions): Typography {
    val scale = dimens.fontScale
    val family = FontFamily.Default

    return Typography(
        displayLarge = TextStyle(family, FontWeight.Bold, (30 * scale).sp, lineHeight = (36 * scale).sp),
        headlineLarge = TextStyle(family, FontWeight.Bold, (26 * scale).sp, lineHeight = (32 * scale).sp),
        headlineMedium = TextStyle(family, FontWeight.SemiBold, (22 * scale).sp, lineHeight = (28 * scale).sp),
        titleLarge = TextStyle(family, FontWeight.SemiBold, (20 * scale).sp, lineHeight = (26 * scale).sp),
        titleMedium = TextStyle(family, FontWeight.SemiBold, (18 * scale).sp, lineHeight = (24 * scale).sp),
        titleSmall = TextStyle(family, FontWeight.Medium, (16 * scale).sp, lineHeight = (22 * scale).sp),
        bodyLarge = TextStyle(family, FontWeight.Normal, (18 * scale).sp, lineHeight = (24 * scale).sp),
        bodyMedium = TextStyle(family, FontWeight.Normal, (16 * scale).sp, lineHeight = (22 * scale).sp),
        bodySmall = TextStyle(family, FontWeight.Normal, (14 * scale).sp, lineHeight = (18 * scale).sp),
        labelLarge = TextStyle(family, FontWeight.SemiBold, (18 * scale).sp, lineHeight = (22 * scale).sp),
        labelMedium = TextStyle(family, FontWeight.Medium, (14 * scale).sp, lineHeight = (18 * scale).sp),
        labelSmall = TextStyle(family, FontWeight.Medium, (12 * scale).sp, lineHeight = (16 * scale).sp)
    )
}

// Small helper so TextStyle calls above stay one-line readable
private fun TextStyle(
    family: FontFamily,
    weight: FontWeight,
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) = TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, lineHeight = lineHeight)

/**
 * Single entry point for theming the whole app.
 *
 * isDarkTheme comes from ThemeManager's StateFlow (collected in MainActivity),
 * NOT isSystemInDarkTheme() — it's a per-uid Firestore/DataStore preference.
 */
@Composable
fun EllieTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val deviceSize = rememberDeviceSize()
    val dimens = dimensionsFor(deviceSize)
    val typography = appTypography(dimens)

    // Real devices commonly run with the user's accessibility "Font size"
    // setting above 100% (110%–130% is common), while emulators are almost
    // always left at the default 100%. Our layout containers (buttonHeight,
    // textFieldHeight, bottomNavHeight, etc.) are fixed Dp values that do
    // NOT grow with font scale, but Text using .sp DOES grow automatically.
    // On an untouched device this mismatch causes text to overflow/wrap and
    // visually collide with neighboring elements — exactly the "works in
    // the emulator, breaks on my phone" symptom. Clamping to a 1.0–1.2x
    // range still respects the user's preference for slightly larger text
    // without letting it blow out our fixed-height buttons/fields/nav bar.
    val baseDensity = LocalDensity.current
    val clampedDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale.coerceIn(1f, 1.2f)
    )

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = AppColorsStatic.NavyDark,
            background = AppColorsStatic.BackgroundDark,
            surface = AppColorsStatic.SurfaceDark,
            onPrimary = Color.White,
            onBackground = AppColorsStatic.TextPrimaryDark,
            onSurface = AppColorsStatic.TextPrimaryDark,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = AppColorsStatic.NavyLight,
            background = AppColorsStatic.BackgroundLight,
            surface = AppColorsStatic.SurfaceLight,
            onPrimary = Color.White,
            onBackground = AppColorsStatic.TextPrimaryLight,
            onSurface = AppColorsStatic.TextPrimaryLight,
            error = ErrorRed
        )
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDarkTheme,
        LocalDeviceSize provides deviceSize,
        LocalDimens provides dimens,
        LocalDensity provides clampedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Mirrors the private hex values in Tokens.kt, needed only to build the
 * MaterialTheme ColorScheme (wants raw Color, not @Composable getters).
 * Screens should NEVER touch this — always go through AppColors.xxx.
 */
private object AppColorsStatic {
    val NavyLight = Color(0xFF1B2A6B)
    val NavyDark = Color(0xFF3B4FA8)
    val BackgroundLight = Color(0xFFF4F6FB)
    val BackgroundDark = Color(0xFF121216)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1C1C24)
    val TextPrimaryLight = Color(0xFF1A1A2E)
    val TextPrimaryDark = Color(0xFFF2F2F5)
}