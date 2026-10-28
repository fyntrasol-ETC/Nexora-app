package com.nexora_app.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexora_app.app.ui.theme.AppColors

/**
 * The brand gradient background used on Splash, Sign In, Sign Up, Reset
 * Password, and now Words/Flashcards — Android equivalent of iOS's
 * AppBackground() view. Fills the whole screen behind content.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.brandGradient)
    )
}