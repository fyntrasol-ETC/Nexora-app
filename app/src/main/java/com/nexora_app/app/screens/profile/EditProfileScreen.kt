package com.nexora_app.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.navigation.NavHostController
import com.nexora_app.app.ui.components.GradientScreenHeader
import com.nexora_app.app.ui.components.GradientScreenScaffold
import com.nexora_app.app.components.DiceBearAvatar
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.EditProfileViewModel
import com.nexora_app.app.ui.components.PremiumButton

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel
) {
    val dimens = LocalDimens.current
    val uiState by editProfileViewModel.uiState.collectAsState()

    GradientScreenScaffold {

        GradientScreenHeader(
            title = "Edit profile",
            onBack = { navController.popBackStack() }
        )

        Spacer(Modifier.height(dimens.spaceXL))

        // Current avatar + Change avatar entry point
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.avatarLarge)
                    .shadow(
                        elevation = dimens.cardElevation * 3,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                DiceBearAvatar(seed = uiState.avatarSeed, size = dimens.avatarLarge)
            }

            Spacer(Modifier.height(dimens.spaceM))

            Box(
                modifier = Modifier
                    .height(dimens.buttonHeight * 0.72f)
                    .shadow(
                        elevation = dimens.cardElevation,
                        shape = RoundedCornerShape(dimens.radiusPill),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(dimens.radiusPill))
                    .background(AppColors.surface)
                    .clickable { navController.navigate(Routes.ChangeAvatar.route) }
                    .padding(horizontal = dimens.spaceL),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Change avatar",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.primary
                )
            }
        }

        Spacer(Modifier.height(dimens.spaceXXL))

        // Name field
        Text(
            text = "Name",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.onGradient.copy(alpha = 0.85f)
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
                .background(AppColors.surface)
                .padding(horizontal = dimens.spaceM),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = uiState.nameInput,
                onValueChange = editProfileViewModel::onNameChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.textPrimary),
                cursorBrush = SolidColor(AppColors.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.error != null) {
            Spacer(Modifier.height(dimens.spaceS))
            Text(
                text = uiState.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.error
            )
        }

        Spacer(Modifier.height(dimens.spaceXXL))

        PremiumButton(
            text = "Save",
            enabled = uiState.isNameChanged && !uiState.isSaving,
            isLoading = uiState.isSaving,
            onClick = { editProfileViewModel.save() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(dimens.spaceL))
    }
}