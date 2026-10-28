package com.nexora_app.app.screens.authscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.components.AuthTextField
import com.nexora_app.app.components.BackButton
import com.nexora_app.app.components.PrimaryButton
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    var email by remember { mutableStateOf("") }
    val uiState by authViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.screenPaddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = { navController.popBackStack() })
            }

            Spacer(Modifier.height(dimens.spaceXL))

            Box(
                modifier = Modifier
                    .size(dimens.iconXL)
                    .background(AppColors.onGradient.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = AppColors.onGradient,
                    modifier = Modifier.size(dimens.iconL)
                )
            }

            Spacer(Modifier.height(dimens.spaceM))

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineLarge,
                color = AppColors.onGradient,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimens.spaceXXS))
            Text(
                text = "Enter your email address below and we'll send you a secure link to reset your account password.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.onGradient.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spaceXL))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = dimens.maxContentWidth)
                    .background(AppColors.surface, RoundedCornerShape(dimens.radiusLarge))
                    .padding(dimens.spaceL)
            ) {
                Text("Email Address", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
                Spacer(Modifier.height(dimens.spaceXXS))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "name@example.com",
                    keyboardType = KeyboardType.Email
                )

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(dimens.spaceS))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.error
                    )
                }

                Spacer(Modifier.height(dimens.spaceM))

                PrimaryButton(
                    text = if (uiState.resetLinkSent) "Link Sent" else "Send Link",
                    onClick = { authViewModel.sendPasswordReset(email) },
                    enabled = email.isNotBlank() && !uiState.resetLinkSent && !uiState.isLoading,
                    isLoading = uiState.isLoading,
                )
            }

            Spacer(Modifier.height(dimens.spaceL))

            Text(
                text = "Back to Login",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.onGradient,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }
    }
}