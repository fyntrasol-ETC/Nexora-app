package com.nexora_app.app.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.ui.theme.LocalIsDarkTheme
import com.nexora_app.app.viewmodel.AuthViewModel

/**
 * Delete Account screen.
 *
 * Wired to AuthViewModel.deleteAccount, which does:
 * reauth -> wipe all 5 subcollections -> delete users/{uid} doc -> Auth
 * deleteUser(). On success we clear the whole back stack and drop the
 * user on SignIn, since there's no account left to navigate "back" to.
 */
@Composable
fun DeleteAccountScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    val isDark = LocalIsDarkTheme.current
    val uiState by authViewModel.uiState.collectAsState()

    val onNavigateBack: () -> Unit = { navController.popBackStack() }
    val onKeepAccount: () -> Unit = {
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.Home.route) { inclusive = true }
        }
    }

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }

    val canDelete = password.isNotBlank() && confirmText == "DELETE" && !uiState.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingHorizontal)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.onGradient
                    )
                }
                Spacer(modifier = Modifier.width(dimens.spaceS))
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.onGradient
                )
            }

            Spacer(modifier = Modifier.height(dimens.spaceXL))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(dimens.avatarSmall + dimens.spaceM)
                    .clip(CircleShape)
                    .background(AppColors.onGradient.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.onGradient,
                    modifier = Modifier.size(dimens.iconL)
                )
            }

            Spacer(modifier = Modifier.height(dimens.spaceM))

            Text(
                text = "Delete Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.onGradient,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimens.spaceS))

            Text(
                text = "This action is permanent. All your progress, courses, and data will be erased and cannot be recovered.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.onGradient.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceM)
            )

            Spacer(modifier = Modifier.height(dimens.spaceXL))

            Card(
                shape = RoundedCornerShape(dimens.radiusLarge),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceS)
                ) {
                    Text(
                        text = "Confirm Password",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.textPrimary
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.textFieldHeight),
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = AppColors.textSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(dimens.radiusSmall),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AppColors.surfaceVariant,
                            focusedContainerColor = AppColors.surfaceVariant,
                            unfocusedBorderColor = AppColors.border,
                            focusedBorderColor = AppColors.primary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedTextColor = AppColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(dimens.spaceXS))

                    Text(
                        text = "Type DELETE to confirm",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.textPrimary
                    )
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.textFieldHeight),
                        placeholder = { Text("DELETE", color = AppColors.textHint) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(dimens.radiusSmall),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AppColors.surfaceVariant,
                            focusedContainerColor = AppColors.surfaceVariant,
                            unfocusedBorderColor = AppColors.border,
                            focusedBorderColor = AppColors.primary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedTextColor = AppColors.textPrimary
                        )
                    )

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.error
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.spaceS))

                    Button(
                        onClick = {
                            authViewModel.deleteAccount(password) {
                                navController.navigate(Routes.SignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        enabled = canDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.buttonHeight),
                        shape = RoundedCornerShape(dimens.radiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.error,
                            disabledContainerColor = AppColors.error.copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimens.iconS),
                                color = Color.White,
                                strokeWidth = dimens.progressStrokeWidth
                            )
                        } else {
                            Text(
                                text = "Delete My Account",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceM))

            Text(
                text = "or",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.onGradient.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(dimens.spaceM))

            Button(
                onClick = onKeepAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.buttonHeight),
                shape = RoundedCornerShape(dimens.radiusPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimens.iconS)
                )
                Spacer(modifier = Modifier.width(dimens.spaceXS))
                Text(
                    text = "Keep My Account",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(dimens.spaceL))
        }
    }
}