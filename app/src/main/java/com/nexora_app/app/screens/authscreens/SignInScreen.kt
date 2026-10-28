package com.nexora_app.app.screens.authscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nexora_app.app.R
import com.nexora_app.app.components.AuthTextField
import com.nexora_app.app.components.PrimaryButton
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    val uiState by authViewModel.uiState.collectAsState()

    fun launchGoogleSignIn() {
        coroutineScope.launch {
            isGoogleLoading = true
            googleError = null
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    authViewModel.signInWithGoogle(googleIdTokenCredential.idToken) {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.SignIn.route) { inclusive = true }
                        }
                    }
                } else {
                    googleError = "Unexpected credential type returned"
                }
            } catch (e: GetCredentialException) {
                googleError = e.message ?: "Google sign-in was cancelled or failed"
            } finally {
                isGoogleLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.screenPaddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimens.spaceXXL))

            Box(
                modifier = Modifier
                    .size(dimens.iconXL)
                    .background(AppColors.onGradient, RoundedCornerShape(dimens.radiusLarge)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = "Logo",
                    tint = AppColors.primary,
                    modifier = Modifier.size(dimens.iconL)
                )
            }

            Spacer(Modifier.height(dimens.spaceM))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = AppColors.onGradient,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimens.spaceXXS))
            Text(
                text = "Master the world's most spoken language with ease.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.onGradient.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spaceXL))

            // ---- Form card ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = dimens.maxContentWidth)
                    .background(AppColors.surface, RoundedCornerShape(dimens.radiusLarge))
                    .padding(dimens.spaceL)
            ) {
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email Address",
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(dimens.spaceM))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = true
                )

                Spacer(Modifier.height(dimens.spaceS))

                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { navController.navigate(Routes.ResetPassword.route) }
                )

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(dimens.spaceS))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.error
                    )
                }

                if (googleError != null) {
                    Spacer(Modifier.height(dimens.spaceS))
                    Text(
                        text = googleError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.error
                    )
                }

                Spacer(Modifier.height(dimens.spaceL))

                PrimaryButton(
                    text = "Sign In",
                    onClick = {
                        authViewModel.signIn(email, password) {
                            navController.navigate(Routes.Home.route) {
                                popUpTo(Routes.SignIn.route) { inclusive = true }
                            }
                        }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                    isLoading = uiState.isLoading,
                )

                Spacer(Modifier.height(dimens.spaceM))
                Text(
                    text = "OR CONTINUE WITH",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textHint,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(dimens.spaceM))

                SocialButton(
                    text = if (isGoogleLoading) "Signing in…" else "Google",
                    enabled = !isGoogleLoading && !uiState.isLoading,
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(dimens.spaceL))

            Row {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.onGradient
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.onGradient,
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.SignUp.route)
                    }
                )
            }

            Spacer(Modifier.height(dimens.spaceL))
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dimens = LocalDimens.current
    Row(
        modifier = modifier
            .height(dimens.buttonHeight)
            .background(AppColors.surfaceVariant, RoundedCornerShape(dimens.radiusMedium))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = dimens.spaceS),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = AppColors.textPrimary)
    }
}