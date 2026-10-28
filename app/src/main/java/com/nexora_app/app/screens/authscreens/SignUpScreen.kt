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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.nexora_app.app.components.PrimaryButton
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val dimens = LocalDimens.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiState.collectAsState()

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
            Spacer(Modifier.height(dimens.spaceXL))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = AppColors.onGradient,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimens.spaceXXS))
            Text(
                text = "Join our global community and start your language journey today.",
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
                Text("Full Name", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
                Spacer(Modifier.height(dimens.spaceXXS))
                AuthTextField(value = fullName, onValueChange = { fullName = it }, placeholder = "Keiko Tanaka")

                Spacer(Modifier.height(dimens.spaceM))
                Text("Email Address", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
                Spacer(Modifier.height(dimens.spaceXXS))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "keiko@example.com",
                    keyboardType = KeyboardType.Email
                )

                Spacer(Modifier.height(dimens.spaceM))
                Text("Password", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
                Spacer(Modifier.height(dimens.spaceXXS))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    isPassword = true
                )

                Spacer(Modifier.height(dimens.spaceM))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppColors.primary)
                    )
                    Text(
                        text = "I agree to the Terms & Conditions and Privacy Policy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary
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

                Spacer(Modifier.height(dimens.spaceM))

                PrimaryButton(
                    text = "Create Account",
                    onClick = {
                        authViewModel.signUp(fullName, email, password) {
                            navController.navigate(Routes.Home.route) {
                                popUpTo(Routes.SignIn.route) { inclusive = true }
                            }
                        }
                    },
                    enabled = agreedToTerms &&
                            fullName.isNotBlank() &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            !uiState.isLoading,
                    isLoading = uiState.isLoading,
                )
            }

            Spacer(Modifier.height(dimens.spaceL))

            Row {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.onGradient
                )
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.onGradient,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
            }

            Spacer(Modifier.height(dimens.spaceL))
        }
    }
}