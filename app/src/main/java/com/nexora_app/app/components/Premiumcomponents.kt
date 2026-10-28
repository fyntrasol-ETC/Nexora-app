package com.nexora_app.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

/**
 * Compatible color interpolation helper (mirrors the one in ProfileScreen.kt).
 * Kept here too so this file has no hard dependency on ProfileScreen.
 */
fun blendColors(start: Color, end: Color, fraction: Float): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = start.alpha + (end.alpha - start.alpha) * fraction
)

// -----------------------------------------------------------------------
// SCREEN SCAFFOLD — gradient background, scrollable, tablet-safe max width
// -----------------------------------------------------------------------

/**
 * Standard scaffold for every settings-family screen. Uses AppColors.brandGradient
 * (instead of a flat white background) and clamps content width via
 * dimens.maxContentWidth so it never stretches edge-to-edge on tablets while
 * still filling phones comfortably.
 */
@Composable
fun GradientScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(
            modifier = modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = dimens.maxContentWidth)
                .padding(
                    horizontal = dimens.screenPaddingHorizontal,
                    vertical = dimens.screenPaddingVertical
                ),
            content = content
        )
    }
}

/**
 * Back button + title (+ optional subtitle), sized off dimens so it scales
 * consistently between compact phones and expanded tablets.
 */
@Composable
fun GradientScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    val dimens = LocalDimens.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(dimens.iconXL * 0.55f)
                        .shadow(
                            elevation = dimens.cardElevation * 2,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.20f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        )
                        .clip(CircleShape)
                        .background(AppColors.onGradient)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.primary,
                        modifier = Modifier.size(dimens.iconM)
                    )
                }
                Spacer(Modifier.width(dimens.spaceM))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.onGradient
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(dimens.spaceXS))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.onGradient.copy(alpha = 0.85f)
            )
        }
    }
}

// -----------------------------------------------------------------------
// PREMIUM BUTTON — elevated / 3D, used for every primary action
// -----------------------------------------------------------------------

enum class PremiumButtonStyle { PRIMARY, DANGER, GHOST }

@Composable
fun PremiumButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PremiumButtonStyle = PremiumButtonStyle.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val baseColor = when (style) {
        PremiumButtonStyle.PRIMARY -> AppColors.primary
        PremiumButtonStyle.DANGER -> AppColors.error
        PremiumButtonStyle.GHOST -> AppColors.surface
    }

    val contentColor = if (style == PremiumButtonStyle.GHOST) AppColors.textPrimary else Color.White

    val fillBrush = if (style == PremiumButtonStyle.GHOST) {
        Brush.verticalGradient(listOf(AppColors.surface, AppColors.surface))
    } else {
        Brush.verticalGradient(
            colors = listOf(
                blendColors(baseColor, Color.White, 0.16f),
                blendColors(baseColor, Color.Black, 0.10f)
            )
        )
    }

    val scale = if (pressed) 0.98f else 1f
    val shadowElevation = if (pressed) dimens.cardElevation else dimens.cardElevation * 3

    Row(
        modifier = modifier
            .scale(scale)
            .height(dimens.buttonHeight)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(dimens.radiusPill),
                ambientColor = baseColor.copy(alpha = 0.45f),
                spotColor = baseColor.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(dimens.radiusPill))
            .background(fillBrush)
            .then(
                if (style == PremiumButtonStyle.GHOST)
                    Modifier.border(dimens.borderThin, AppColors.border, RoundedCornerShape(dimens.radiusPill))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !isLoading
            ) { onClick() }
            .padding(horizontal = dimens.spaceL),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = dimens.progressStrokeWidth,
                modifier = Modifier.size(dimens.iconS)
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(dimens.iconS)
                )
                Spacer(Modifier.width(dimens.spaceS))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

// -----------------------------------------------------------------------
// FROSTED CARD — the reusable "raised panel" every card sits on
// -----------------------------------------------------------------------

@Composable
fun FrostedCard(
    modifier: Modifier = Modifier,
    accent: Color = AppColors.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .shadow(
                elevation = dimens.cardElevation * 4,
                shape = RoundedCornerShape(dimens.radiusLarge),
                ambientColor = accent.copy(alpha = 0.30f),
                spotColor = accent.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(AppColors.surface),
        content = content
    )
}

// -----------------------------------------------------------------------
// SETTINGS ROW — one tappable line inside a FrostedCard
// -----------------------------------------------------------------------

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    external: Boolean = false,
    tint: Color = AppColors.primary,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val dimens = LocalDimens.current
    val effectiveTint = if (danger) AppColors.error else tint
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimens.iconXL * 0.5f)
                .clip(CircleShape)
                .background(effectiveTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = effectiveTint,
                modifier = Modifier.size(dimens.iconS)
            )
        }
        Spacer(Modifier.width(dimens.spaceM))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) AppColors.error else AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textHint,
                modifier = Modifier.padding(end = dimens.spaceS)
            )
        }
        Icon(
            imageVector = if (external) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.textHint,
            modifier = Modifier.size(dimens.iconS)
        )
    }
}

@Composable
fun RowDivider() {
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceL)
            .height(dimens.borderThin)
            .background(AppColors.divider)
    )
}

// -----------------------------------------------------------------------
// PASSWORD FIELD — frosted, elevated, with visibility toggle
// -----------------------------------------------------------------------

@Composable
fun PremiumPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    val dimens = LocalDimens.current
    var visible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.textSecondary
        )
        Spacer(Modifier.height(dimens.spaceXS))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.textFieldHeight)
                .shadow(
                    elevation = dimens.cardElevation,
                    shape = RoundedCornerShape(dimens.radiusMedium),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(dimens.radiusMedium))
                .background(AppColors.surfaceVariant)
                .border(dimens.borderThin, AppColors.border, RoundedCornerShape(dimens.radiusMedium))
                .padding(horizontal = dimens.spaceM),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textHint
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.textPrimary),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        cursorBrush = SolidColor(AppColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(dimens.spaceS))
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = AppColors.textHint,
                    modifier = Modifier
                        .size(dimens.iconS)
                        .clickable { visible = !visible }
                )
            }
        }
    }
}

@Composable
fun PasswordRequirementItem(text: String, satisfied: Boolean) {
    val dimens = LocalDimens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (satisfied) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (satisfied) AppColors.success else AppColors.textHint,
            modifier = Modifier.size(dimens.iconXS)
        )
        Spacer(Modifier.width(dimens.spaceS))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary
        )
    }
}