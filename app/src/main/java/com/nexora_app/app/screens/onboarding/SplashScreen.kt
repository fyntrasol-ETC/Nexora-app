package com.nexora_app.app.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.nexora_app.app.navigation.Routes
import com.nexora_app.app.ui.theme.AppColors
import com.nexora_app.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

// How long the splash stays on screen — the progress bar animation below
// is driven off this same value, so they always finish together.
private const val SPLASH_DURATION_MS = 3500

@Composable
fun SplashScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {

    // Navigate after splash animation — Home if a session already exists
    // (Firebase persists sign-in across app restarts by default), SignIn
    // otherwise. This is the only thing that decides where the user lands;
    // don't hardcode Routes.SignIn here again.
    LaunchedEffect(Unit) {

        delay(SPLASH_DURATION_MS.toLong())

        val destination = if (authViewModel.isSignedIn) Routes.Home.route else Routes.SignIn.route

        navController.navigate(destination) {
            popUpTo(Routes.Splash.route) {
                inclusive = true
            }
        }
    }



    // Fade animation

    val alpha =
        remember {
            Animatable(0f)
        }


    LaunchedEffect(Unit) {

        alpha.animateTo(
            1f,
            animationSpec = tween(
                1200
            )
        )
    }

    // Floating logo animation

    val transition =
        rememberInfiniteTransition(
            label = "logoAnimation"
        )


    val floating by transition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    1600,
                    easing = EaseInOut
                ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "floating"
    )



    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    1800,
                    easing = EaseInOut
                ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "pulse"
    )



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppColors.brandGradient
            )
    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {



            Box(
                modifier = Modifier
                    .offset(
                        y = floating.dp
                    )
                    .size(
                        160.dp
                    )
                    .shadow(
                        elevation = 25.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.White.copy(
                            alpha = 0.35f
                        ),
                        spotColor = Color.White.copy(
                            alpha = 0.35f
                        )
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ){


                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Nexora logo",
                    tint = Color.White,
                    modifier = Modifier
                        .size(
                            95.dp
                        )
                )
            }



            Spacer(
                modifier = Modifier.height(
                    25.dp
                )
            )



            // Wordmark — tight negative letter-spacing at this size reads as
            // a deliberate logotype rather than plain body text set large.
            Text(
                text = "Nexora",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )



            Spacer(
                modifier = Modifier.height(
                    12.dp
                )
            )



            Text(
                text = "Your English teacher workspace",
                fontSize = 16.sp,
                letterSpacing = 0.2.sp,
                color = Color.White.copy(
                    alpha = 0.85f
                )
            )



            Spacer(
                modifier = Modifier.height(
                    25.dp
                )
            )



            LoadingDots()



            Spacer(
                modifier = Modifier.height(
                    130.dp
                )
            )


            AnimatedProgressBar()



            Spacer(
                modifier = Modifier.height(
                    14.dp
                )
            )



            Text(
                text = "SYNCING CURRICULUM...",
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = Color.White.copy(
                    alpha = 0.85f
                )
            )
        }
    }
}
@Composable
private fun LoadingDots() {

    val transition =
        rememberInfiniteTransition(
            label = "dots"
        )


    val dot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )


    val dot2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 150,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )


    val dot3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 300,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )



    Row(
        horizontalArrangement = Arrangement.spacedBy(
            8.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {


        SplashDot(
            offset = dot1
        )


        SplashDot(
            offset = dot2
        )


        SplashDot(
            offset = dot3
        )
    }
}



@Composable
private fun SplashDot(
    offset: Float
) {

    Box(
        modifier = Modifier
            .offset(
                y = offset.dp
            )
            .size(
                8.dp
            )
            .background(
                Color.White.copy(
                    alpha = 0.9f
                ),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/**
 * A normal, fills-once loading bar: animates 0 -> 1 a single time, timed to
 * finish right as the splash delay ends, instead of the old infinite
 * grow-then-shrink "breathing" animation.
 */
@Composable
private fun AnimatedProgressBar() {

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SPLASH_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(4.dp)
            .background(
                Color.White.copy(
                    alpha = 0.25f
                ),
                RoundedCornerShape(
                    50
                )
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    progress.value
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(
                        50
                    )
                )
        )
    }
}