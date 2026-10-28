package com.nexora_app.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.nexora_app.app.ui.components.GradientScreenHeader
import com.nexora_app.app.components.DiceBearAvatarOption
import com.nexora_app.app.ui.components.PremiumButton
import com.nexora_app.app.models.AvatarOptions
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.ui.theme.LocalDimens
import com.nexora_app.app.viewmodel.EditProfileViewModel
import androidx.compose.ui.Modifier

@Composable
fun ChangeAvatarScreen(
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel
) {
    val dimens = LocalDimens.current
    val uiState by editProfileViewModel.uiState.collectAsState()

    // Local staging selection — only committed (via selectAvatar) when
    // "Select avatar" is tapped, so browsing the grid doesn't write on
    // every tap.
    var pendingSelection by remember(uiState.avatarSeed) {
        mutableStateOf(uiState.avatarSeed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimens.screenPaddingHorizontal,
                    vertical = dimens.screenPaddingVertical
                )
        ) {
            GradientScreenHeader(
                title = "Change Avatar",
                onBack = { navController.popBackStack() }
            )

            Spacer(Modifier.height(dimens.spaceL))

            // Curated DiceBear seeds — grid fills and scrolls on its own via
            // LazyVerticalGrid, matching the 4-column density of before.
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
                contentPadding = PaddingValues(bottom = dimens.spaceXS),
                modifier = Modifier.weight(1f)
            ) {
                items(AvatarOptions.curatedSeeds) { seed ->
                    DiceBearAvatarOption(
                        seed = seed,
                        selected = seed == pendingSelection,
                        onClick = { pendingSelection = seed }
                    )
                }
            }

            Spacer(Modifier.height(dimens.spaceL))

            PremiumButton(
                text = "Select avatar",
                onClick = {
                    editProfileViewModel.selectAvatar(pendingSelection)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(dimens.spaceM))
        }
    }
}