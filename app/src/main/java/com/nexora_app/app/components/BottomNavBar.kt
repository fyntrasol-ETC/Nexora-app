package com.nexora_app.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.Dimensions
import com.nexora_app.app.ui.theme.LocalDimens

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem(Routes.Home.route, "Home", Icons.Filled.Home),
    NavItem(Routes.Courses.route, "Courses", Icons.Filled.Description),
    NavItem(Routes.Words.route, "Words", Icons.Filled.TextFields),
    NavItem(Routes.Videos.route, "Videos", Icons.Filled.VideoLibrary),
    NavItem(Routes.Profile.route, "Profile", Icons.Filled.Person)
)

/**
 * Floating gradient tab bar — direct port of the SwiftUI CustomTabBar.
 *
 * Key structural detail carried over from the SwiftUI version: each tab
 * item has NO extra horizontal padding of its own (only vertical). The
 * previous Compose version added horizontal padding per item on top of an
 * already-tight `weight(1f)` slot, which is what caused labels like
 * "Courses"/"Videos" to run out of room and get clipped ("Wor_", "Vide_").
 * Keeping items horizontal-padding-free and letting `fillMaxWidth()` +
 * `weight(1f)` do the sizing matches the original exactly.
 *
 * Navigation note: popUpTo uses navController.graph.findStartDestination().id
 * rather than a hardcoded Routes.Home.route string. This is the standard
 * Google-recommended bottom-nav pattern — it resolves correctly no matter
 * which destination is currently on top of the back stack (including detail
 * screens pushed from Home, like CourseDetail/VideoDetail), so tapping any
 * tab — including Home itself — reliably pops back and restores saved state.
 */
@Composable
fun BottomNavBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = dimens.spaceS)
            .shadow(
                elevation = dimens.cardElevation * 4,
                shape = RoundedCornerShape(dimens.radiusPill),
                ambientColor = Color.Black.copy(alpha = 0.20f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(dimens.radiusPill))
            .background(AppColors.bottomNavGradient)
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceS),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavBarItem(
                item = item,
                selected = selected,
                dimens = dimens,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    dimens: Dimensions,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusMedium))
            .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = dimens.spaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXS)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = Color.White,
            modifier = Modifier.size(dimens.iconS)
        )
        // Fixed 10.sp (not scaled by dimens.fontScale / system font size) —
        // matches the SwiftUI reference exactly and keeps every label on a
        // single line regardless of device font settings.
        Text(
            text = item.label,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}