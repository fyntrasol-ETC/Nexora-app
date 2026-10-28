package com.nexora_app.app.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.ui.components.FrostedCard
import com.nexora_app.app.ui.components.GradientScreenHeader
import com.nexora_app.app.ui.components.GradientScreenScaffold
import com.nexora_app.app.ui.components.PasswordRequirementItem
import com.nexora_app.app.ui.components.PremiumButton
import com.nexora_app.app.ui.components.PremiumButtonStyle
import com.nexora_app.app.ui.components.PremiumPasswordField
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.AuthViewModel

/**
 * Change Password screen.
 *
 * Wired to AuthViewModel.changePassword, which reauthenticates with
 * currentPassword before calling updatePassword — Firebase requires a
 * recent login for that call, so a wrong currentPassword surfaces as an
 * error here rather than as a silent failure.
 */
@Composable
fun ChangePasswordScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    val uiState by authViewModel.uiState.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val hasMinLength = newPassword.length >= 8
    val hasMix = newPassword.any { it.isLetter() } &&
            newPassword.any { it.isDigit() || !it.isLetterOrDigit() }
    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val canSubmit = currentPassword.isNotBlank() && hasMinLength && hasMix && passwordsMatch && !uiState.isLoading

    GradientScreenScaffold {

        GradientScreenHeader(
            title = "Change Password",
            subtitle = "Protect your account by creating a strong, unique password for Ellie.",
            onBack = { navController.popBackStack() }
        )

        Spacer(Modifier.height(dimens.spaceXL))

        FrostedCard(accent = AppColors.primary) {
            Column(modifier = Modifier.fillMaxWidth().padding(dimens.spaceL)) {

                PremiumPasswordField(
                    label = "Current Password",
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = "Enter your current password"
                )

                Spacer(Modifier.height(dimens.spaceL))

                PremiumPasswordField(
                    label = "New Password",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "Create a new password"
                )

                Spacer(Modifier.height(dimens.spaceL))

                PremiumPasswordField(
                    label = "Confirm New Password",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Re-type your new password"
                )

                Spacer(Modifier.height(dimens.spaceL))

                Text(
                    text = "Password must contain",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.textSecondary
                )

                Spacer(Modifier.height(dimens.spaceS))

                PasswordRequirementItem(
                    text = "At least 8 characters long",
                    satisfied = hasMinLength
                )

                Spacer(Modifier.height(dimens.spaceXS))

                PasswordRequirementItem(
                    text = "A mix of letters, numbers & symbols",
                    satisfied = hasMix
                )

                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Spacer(Modifier.height(dimens.spaceXS))
                    Text(
                        text = "Passwords don't match yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.error
                    )
                }

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(dimens.spaceS))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.error
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spaceXL))

        PremiumButton(
            text = "Update Password",
            icon = Icons.Default.Sync,
            style = PremiumButtonStyle.PRIMARY,
            isLoading = uiState.isLoading,
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onClick = {
                authViewModel.changePassword(currentPassword, newPassword) {
                    // Firebase invalidates other sessions' refresh tokens on
                    // password change, so this device is still fine — just
                    // head back to Settings.
                    navController.popBackStack()
                }
            }
        )

        Spacer(Modifier.height(dimens.spaceM))

        Text(
            text = "Cancel",
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.onGradient,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(dimens.spaceS)
                .then(
                    Modifier.clickable { navController.popBackStack() }
                )
        )

        Spacer(Modifier.height(dimens.spaceL))

        Text(
            text = "Changing your password will log you out from all other devices for security purposes.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.onGradient.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceM),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(dimens.spaceL))
    }
}