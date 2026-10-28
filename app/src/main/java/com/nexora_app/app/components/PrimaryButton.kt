package com.nexora_app.app.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    // Trailing icon (e.g. Icons.AutoMirrored.Filled.ArrowForward) instead of
    // embedding arrow characters like "\u2192" directly in the button text —
    // some devices' default font substitutes/breaks glyphs like that
    // (rendered as "'n" instead of an arrow) when they're baked into a
    // plain string. A real Icon composable renders reliably everywhere.
    trailingIcon: ImageVector? = null
) {
    val dimens = LocalDimens.current

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(dimens.radiusPill),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.primary,
            contentColor = AppColors.onPrimary,
            disabledContainerColor = AppColors.primary.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = AppColors.onPrimary,
                strokeWidth = dimens.progressStrokeWidth,
                modifier = Modifier.size(dimens.iconS)
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(dimens.spaceXS))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconS)
                )
            }
        }
    }
}