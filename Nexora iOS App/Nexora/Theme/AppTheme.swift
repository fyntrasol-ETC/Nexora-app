

import SwiftUI

enum AppColors {
    // Brand gradient: indigo-blue -> muted violet -> peach/pink
    static let gradientTop    = Color(red: 0.40, green: 0.46, blue: 0.90)
    static let gradientMid    = Color(red: 0.62, green: 0.53, blue: 0.80)
    static let gradientBottom = Color(red: 0.96, green: 0.64, blue: 0.57)

    static let primaryButton   = Color(red: 0.16, green: 0.19, blue: 0.48) // navy "Sign In" pill
    static let link            = Color(red: 0.29, green: 0.35, blue: 0.87) // "Forgot Password?", "Sign Up"
    static let cardBackground  = Color.white
    static let fieldBackground = Color(red: 0.95, green: 0.95, blue: 0.97)
    static let fieldBorder     = Color(red: 0.87, green: 0.87, blue: 0.90)
    static let placeholderText = Color(red: 0.55, green: 0.55, blue: 0.58)
    static let primaryText     = Color(red: 0.12, green: 0.14, blue: 0.29)
    static let topBarLight     = Color(red: 0.95, green: 0.96, blue: 0.98)

    // Added for the Android-parity Profile / Settings / Tab bar screens
    static let accentRed  = Color(red: 0.86, green: 0.20, blue: 0.34) // selected tab, "Words I know" accent
    static let avatarGold = Color(red: 0.82, green: 0.62, blue: 0.20) // profile avatar background
    static let badgeGold  = Color(red: 0.85, green: 0.62, blue: 0.15) // trophy icon tint

    // Added for course-progress parity with Android (AppColors.success) —
    // "Completed" checks/labels on ChapterListScreen's chapter rows.
    static let success = Color(red: 0.20, green: 0.66, blue: 0.40)
}

/// Drop this behind any screen that should match the Splash/Login/Register look.
struct AppBackground: View {
    var body: some View {
        LinearGradient(
            colors: [AppColors.gradientTop, AppColors.gradientMid, AppColors.gradientBottom],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}
