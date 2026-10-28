package com.nexora_app.app.screens.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.components.DiceBearAvatar
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.components.blendColors
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.DayState
import com.nexora_app.app.viewmodel.ProfileViewModel
import com.nexora_app.app.viewmodel.WeeklyCheckInViewModel
import com.nexora_app.app.viewmodel.WeeklyDayUi

// Card gradient + shadow tint — carried over 1:1 from iOS's WeeklyCheckInCard
// (Color(red: 0.42, green: 0.28, blue: 0.88) -> Color(red: 0.62, green: 0.38, blue: 0.95)).
private val CheckInGradientStart = Color(0xFF6B47E0)
private val CheckInGradientEnd = Color(0xFF9E61F2)
private val FlameOrange = Color(0xFFFFC740)
private val CheckInButtonTextPurple = Color(0xFF5933BF)
private val DayCompletedGreen = Color(0xFF4CCC75)
private val DayMissedRed = Color(0xFFF35961)

@Composable
fun ProfileScreen(
    navController: NavHostController,
    profileViewModel: ProfileViewModel = viewModel(),
    weeklyCheckInViewModel: WeeklyCheckInViewModel = viewModel()
) {

    val dimens = LocalDimens.current
    val uiState by profileViewModel.uiState.collectAsState()
    val weekState by weeklyCheckInViewModel.uiState.collectAsState()

    // Falls back to "there" while the doc is still loading or if
    // displayName somehow comes back blank, rather than flashing an
    // empty string.
    val userName = uiState.displayName.ifBlank { if (uiState.isLoading) "…" else "there" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = dimens.maxContentWidth)
                .padding(
                    horizontal = dimens.screenPaddingHorizontal,
                    vertical = dimens.screenPaddingVertical
                )
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(Modifier.width(dimens.spaceM))

                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.onGradient,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(dimens.iconXL * 0.6f)
                        .shadow(
                            elevation = dimens.cardElevation * 2,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .background(
                            AppColors.onGradient,
                            CircleShape
                        )
                        .clickable {
                            navController.navigate(Routes.Settings.route)
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AppColors.accentCourses,
                        modifier = Modifier.size(dimens.iconM)
                    )
                }
            }

            Spacer(Modifier.height(dimens.spaceXL))

            WeeklyCheckInCard(
                days = weekState.days,
                checkedInCount = weekState.checkedInCount,
                isTodayCheckedIn = weekState.isTodayCheckedIn,
                isCheckingIn = weekState.isCheckingIn,
                onCheckIn = weeklyCheckInViewModel::checkInToday
            )

            Spacer(Modifier.height(dimens.spaceXL))

            ProfileStatCard(
                icon = Icons.Default.EmojiEvents,
                label = "Badges",
                value = uiState.badgesCount,
                accent = AppColors.crownGold,
                showChevron = true,
                onClick = { navController.navigate(Routes.Badges.route) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(dimens.spaceM))

            ProfileStatCard(
                icon = Icons.Default.Star,
                label = "Words I know",
                value = uiState.wordsKnownCount,
                accent = AppColors.accentProfile,
                showChevron = true,
                onClick = { navController.navigate(Routes.Words.route) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(dimens.spaceXL))
        }
    }
}

// ---- Weekly check-in card: "THIS WEEK / N days checked in" + day pills + button ----

@Composable
private fun WeeklyCheckInCard(
    days: List<WeeklyDayUi>,
    checkedInCount: Int,
    isTodayCheckedIn: Boolean,
    isCheckingIn: Boolean,
    onCheckIn: () -> Unit
) {
    val dimens = LocalDimens.current
    val cardBrush = Brush.linearGradient(listOf(CheckInGradientStart, CheckInGradientEnd))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimens.cardElevation * 4,
                shape = RoundedCornerShape(dimens.radiusLarge),
                ambientColor = CheckInGradientStart.copy(alpha = 0.35f),
                spotColor = CheckInGradientStart.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(dimens.radiusLarge))
            .background(cardBrush)
            .padding(dimens.spaceXL),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceL)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "THIS WEEK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(dimens.spaceXXS))
                Text(
                    text = "$checkedInCount day${if (checkedInCount == 1) "" else "s"} checked in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = FlameOrange,
                modifier = Modifier.size(dimens.iconM)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                DayPill(day = day, modifier = Modifier.weight(1f))
            }
        }

        val buttonShape = RoundedCornerShape(dimens.radiusMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(buttonShape)
                .background(if (isTodayCheckedIn) Color.White.copy(alpha = 0.2f) else Color.White)
                .then(
                    if (isTodayCheckedIn) Modifier
                    else Modifier.clickable(enabled = !isCheckingIn, onClick = onCheckIn)
                )
                .padding(vertical = dimens.spaceM),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCheckingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconS),
                    color = CheckInButtonTextPurple,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isTodayCheckedIn) Icons.Default.Verified else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isTodayCheckedIn) Color.White else CheckInButtonTextPurple,
                    modifier = Modifier.size(dimens.iconS)
                )
                Spacer(Modifier.width(dimens.spaceXS))
                Text(
                    text = if (isTodayCheckedIn) "You're checked in for today!" else "Check in for today",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTodayCheckedIn) Color.White else CheckInButtonTextPurple
                )
            }
        }
    }
}

@Composable
private fun DayPill(day: WeeklyDayUi, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val pillSize = dimens.iconL * 0.85f

    val fillColor = when (day.state) {
        DayState.COMPLETED -> DayCompletedGreen
        DayState.MISSED -> DayMissedRed
        DayState.TODAY -> Color.White.copy(alpha = 0.22f)
        DayState.UPCOMING -> Color.White.copy(alpha = 0.12f)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(pillSize)
                .background(fillColor, CircleShape)
                .then(
                    if (day.state == DayState.TODAY)
                        Modifier.border(2.dp, Color.White, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = day.state, label = "dayPillIcon") { state ->
                when (state) {
                    DayState.COMPLETED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked in",
                        tint = Color.White,
                        modifier = Modifier.size(dimens.iconXS * 0.8f)
                    )
                    DayState.MISSED -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Missed",
                        tint = Color.White,
                        modifier = Modifier.size(dimens.iconXS * 0.7f)
                    )
                    DayState.TODAY -> Box(
                        modifier = Modifier
                            .size(dimens.spaceXS)
                            .background(Color.White, CircleShape)
                    )
                    DayState.UPCOMING -> Spacer(Modifier.size(1.dp))
                }
            }
        }
        Spacer(Modifier.height(dimens.spaceXXS))
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.state == DayState.TODAY) FontWeight.Bold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (day.state == DayState.UPCOMING) 0.45f else 0.9f)
        )
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    val dimens = LocalDimens.current

    val cardBackground = blendColors(
        accent,
        Color.White,
        0.88f
    )

    val iconBackground = accent.copy(alpha = 0.20f)

    Column(
        modifier = modifier
            .shadow(
                elevation = dimens.cardElevation * 3,
                shape = RoundedCornerShape(dimens.radiusLarge),
                ambientColor = accent.copy(alpha = 0.35f),
                spotColor = accent.copy(alpha = 0.35f)
            )
            .background(
                cardBackground,
                RoundedCornerShape(dimens.radiusLarge)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(dimens.spaceL)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary
            )

            if (showChevron) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(dimens.iconS)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(dimens.spaceM)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(dimens.iconXL * 0.55f)
                    .background(
                        iconBackground,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(dimens.iconM)
                )
            }

            Spacer(
                modifier = Modifier.width(dimens.spaceS)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary
            )
        }
    }
}