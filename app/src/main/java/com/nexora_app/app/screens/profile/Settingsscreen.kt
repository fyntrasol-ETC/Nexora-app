package com.nexora_app.app.screens.profile

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.components.FrostedCard
import com.nexora_app.app.ui.components.GradientScreenHeader
import com.nexora_app.app.ui.components.GradientScreenScaffold
import com.nexora_app.app.ui.components.RowDivider
import com.nexora_app.app.ui.components.SettingsRow
import com.nexora_app.app.ui.components.blendColors
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData as AndroidClipData

private const val PRIVACY_POLICY_URL =
    "https://doc-hosting.flycricket.io/nexora-privacy-policy/0d0dbf8d-90e2-4c70-b4d0-7895cd004546/privacy"
private const val TERMS_OF_USE_URL =
    "https://doc-hosting.flycricket.io/nexora-terms-of-use/9b0c38d6-b008-4b08-b015-6c6964a7e3ce/terms"

/**
 * Settings screen.
 *
 * NOTE: assumes the following routes exist on Routes (add them if missing):
 * Routes.EditProfile, Routes.ChangePassword, Routes.RecoverSubscription,
 * Routes.Subscribe, Routes.DeleteAccount.
 *
 * NOTE: userEmail defaults to whatever ProfileViewModel resolves from
 * users/{uid}.email in Firestore. The param is kept so this screen still
 * stays overridable/previewable, but in real usage you don't need to pass
 * anything — just don't pass userEmail and it'll show the signed-in user's
 * actual email.
 *
 * NOTE: the Email row truncates with a single-line ellipsis (handled inside
 * SettingsRow's value Text — make sure that composable sets
 * maxLines = 1, overflow = TextOverflow.Ellipsis on the value text) and is
 * tappable: tapping it copies the full email to the clipboard and shows a
 * confirmation toast, so long emails are never cut off from the user.
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    profileViewModel: ProfileViewModel = viewModel(),
    userEmail: String? = null,
    isSubscribed: Boolean = false,
    appVersion: String = "1.3.2 (33)",
    onLogOut: () -> Unit = {}
) {
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val profileUiState by profileViewModel.uiState.collectAsState()

    // Falls back to the live Firestore-backed email; userEmail param only
    // wins if the caller explicitly passes one (e.g. for previews).
    val displayEmail = userEmail ?: profileUiState.email.ifBlank { "…" }

    // Controls visibility of the "Are you sure you want to log out" dialog.
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun copyEmailToClipboard() {
        // Guard against copying the placeholder while the email is still loading.
        if (displayEmail.isBlank() || displayEmail == "…") return

        coroutineScope.launch {
            clipboard.setClipEntry(
                ClipEntry(AndroidClipData.newPlainText("Email", displayEmail))
            )
            Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
        }
    }

    GradientScreenScaffold {

        GradientScreenHeader(
            title = "Settings",
            onBack = { navController.popBackStack() }
        )

        Spacer(Modifier.height(dimens.spaceXL))

        // Subscribe banner — only shown to non-subscribers
        if (!isSubscribed) {
            //SubscribeBanner(
            //    onClick = { navController.navigate(Routes.Subscribe.route) }
            // )
            Spacer(Modifier.height(dimens.spaceL))
        }

        // Account group
        FrostedCard(accent = AppColors.primary) {
            SettingsRow(
                icon = Icons.Default.Person,
                title = "Edit profile",
                onClick = { navController.navigate(Routes.EditProfile.route) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Default.Email,
                title = "Email",
                value = displayEmail,
                // Tapping the row copies the full email — needed since long
                // emails are truncated with an ellipsis in the row itself.
                onClick = { copyEmailToClipboard() }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Default.Key,
                title = "Change Password",
                onClick = { navController.navigate(Routes.ChangePassword.route) }
            )
        }

        Spacer(Modifier.height(dimens.spaceL))

        // Legal group
        FrostedCard(accent = AppColors.accentCourses) {
            SettingsRow(
                icon = Icons.Default.Description,
                title = "Terms of use",
                external = true,
                onClick = { openUrl(TERMS_OF_USE_URL) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Default.Shield,
                title = "Privacy policy",
                external = true,
                onClick = { openUrl(PRIVACY_POLICY_URL) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log out",
                // Instead of logging out immediately, ask for confirmation first.
                onClick = { showLogoutDialog = true }
            )
        }

        Spacer(Modifier.height(dimens.spaceL))

        // Danger zone — its own elevated card so it reads as a distinct,
        // deliberate action rather than another list row.
        FrostedCard(accent = AppColors.error) {
            SettingsRow(
                icon = Icons.Default.DeleteForever,
                title = "Delete account",
                danger = true,
                onClick = { navController.navigate(Routes.DeleteAccount.route) }
            )
        }

        Spacer(Modifier.height(dimens.spaceXL))

        Text(
            text = "Version $appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.onGradient.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(dimens.spaceL))
    }

    // Premium logout confirmation dialog. Lives outside GradientScreenScaffold's
    // content flow so it overlays the whole screen.
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogOut()
                navController.navigate(Routes.SignIn.route) {
                    // Clear the entire back stack so the user can't swipe/back
                    // their way back into the app after logging out.
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

/**
 * A premium, brand-consistent confirmation dialog for the logout action.
 *
 * Built on top of [Dialog] rather than the stock Material [AlertDialog] so we
 * can fully control the visual language (frosted surface, accent-tinted icon
 * badge, pill buttons) to match the rest of the app.
 */
@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = LocalDimens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.9f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .shadow(
                        elevation = dimens.cardElevation * 6,
                        shape = RoundedCornerShape(dimens.radiusLarge),
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(dimens.radiusLarge))
                    .background(AppColors.surface)
                    .padding(dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Icon badge — soft-tinted circle with the logout glyph.
                Box(
                    modifier = Modifier
                        .size(dimens.iconXL)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    blendColors(AppColors.error, Color.White, 0.15f),
                                    blendColors(AppColors.error, Color.Black, 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(dimens.iconM)
                    )
                }

                Spacer(Modifier.height(dimens.spaceM))

                Text(
                    text = "Log out?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(dimens.spaceXS))

                Text(
                    text = "Are you sure you want to log out? You'll need to sign in again to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(dimens.spaceL))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)
                ) {
                    // No — dismiss, stay on Settings.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight)
                            .clip(RoundedCornerShape(dimens.radiusPill))
                            .background(AppColors.surfaceVariant)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.textPrimary
                        )
                    }

                    // Yes — confirm logout.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight)
                            .shadow(
                                elevation = dimens.cardElevation * 2,
                                shape = RoundedCornerShape(dimens.radiusPill),
                                ambientColor = AppColors.error.copy(alpha = 0.4f),
                                spotColor = AppColors.error.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(dimens.radiusPill))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        blendColors(AppColors.error, Color.White, 0.05f),
                                        blendColors(AppColors.error, Color.Black, 0.10f)
                                    )
                                )
                            )
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscribeBanner(onClick: () -> Unit) {
    val dimens = LocalDimens.current
    val accent = AppColors.accentCourses

    val bannerBrush = Brush.linearGradient(
        colors = listOf(
            blendColors(accent, Color.White, 0.10f),
            blendColors(accent, Color.Black, 0.18f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimens.cardElevation * 4,
                shape = RoundedCornerShape(dimens.radiusLarge),
                ambientColor = accent.copy(alpha = 0.5f),
                spotColor = accent.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(bannerBrush)
            .padding(dimens.spaceL)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimens.iconM)
            )
            Spacer(Modifier.width(dimens.spaceS))
            Text(
                text = "Subscribe to EnglishApp",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Spacer(Modifier.height(dimens.spaceXS))

        Text(
            text = "Unlock access to all courses to meet your English goals faster.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )

        Spacer(Modifier.height(dimens.spaceM))

        Box(
            modifier = Modifier
                .align(Alignment.Start)
                .height(dimens.buttonHeight * 0.8f)
                .shadow(
                    elevation = dimens.cardElevation * 3,
                    shape = RoundedCornerShape(dimens.radiusPill),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(dimens.radiusPill))
                .background(AppColors.crownGold)
                .clickable { onClick() }
                .padding(horizontal = dimens.spaceL),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Subscribe now",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF3A2A00)
            )
        }
    }
}