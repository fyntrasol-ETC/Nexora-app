import SwiftUI

struct SettingsScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    // Matches Android's PRIVACY_POLICY_URL / TERMS_OF_USE_URL constants —
    // same app, same doc-hosting links.
    private let privacyPolicyURL = URL(string: "https://doc-hosting.flycricket.io/nexora-privacy-policy/3aebc79a-6a96-4736-b711-764f0230dd5c/privacy")!
    private let termsOfUseURL = URL(string: "https://doc-hosting.flycricket.io/nexora-terms-of-use/9b0c38d6-b008-4b08-b015-6c6964a7e3ce/terms")!

    // Gates the actual sign-out behind a confirm step — matches Android's
    // LogoutConfirmationDialog, which doesn't log out immediately either.
    @State private var showingLogoutDialog = false

    private var userEmail: String {
        authViewModel.userProfile?.email ?? ""
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                ScreenHeader(title: "Settings", onBack: { dismiss() })
                    .padding(.bottom, 10)

                ScrollView {
                    VStack(spacing: 16) {

                        // Account group — matches Android's first FrostedCard
                        // (Edit profile / Email / Change Password).
                        SettingsCard {
                            NavigationLink(value: ProfileRoute.editProfile) {
                                SettingsRowContent(icon: "person.fill", title: "Edit profile")
                            }
                            .buttonStyle(.plain)

                            Divider().padding(.leading, 60)

                            SettingsRow(icon: "envelope.fill", title: "Email", value: userEmail) {
                                // Read-only on Android too (onClick = null) — no action.
                            }
                            Divider().padding(.leading, 60)

                            NavigationLink(value: ProfileRoute.changePassword) {
                                SettingsRowContent(icon: "key.fill", title: "Password")
                            }
                            .buttonStyle(.plain)
                        }

                        // Legal group — matches Android's second FrostedCard
                        // (Terms of use / Privacy policy / Log out). No
                        // "Talk to us" row on Android, so dropped here too.
                        SettingsCard {
                            SettingsRow(icon: "doc.text.fill", title: "Terms of use", trailingStyle: .external) {
                                UIApplication.shared.open(termsOfUseURL)
                            }
                            Divider().padding(.leading, 60)

                            SettingsRow(icon: "shield.fill", title: "Privacy policy", trailingStyle: .external) {
                                UIApplication.shared.open(privacyPolicyURL)
                            }
                            Divider().padding(.leading, 60)

                            SettingsRow(icon: "arrow.right.square.fill", title: "Log out") {
                                withAnimation(.easeOut(duration: 0.18)) {
                                    showingLogoutDialog = true
                                }
                            }
                        }

                        // Danger zone — its own card, unchanged.
                        NavigationLink(value: ProfileRoute.deleteAccount) {
                            SettingsCard {
                                SettingsRowContent(icon: "trash.fill", title: "Delete account", isDestructive: true)
                            }
                        }
                        .buttonStyle(.plain)

                        Text("Version 1.3.2 (33)")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.85))
                            .padding(.top, 4)

                        Spacer(minLength: 40)
                    }
                }
            }

            if showingLogoutDialog {
                LogoutConfirmationDialog(
                    onConfirm: {
                        withAnimation(.easeIn(duration: 0.15)) { showingLogoutDialog = false }
                        logOut()
                    },
                    onDismiss: {
                        withAnimation(.easeIn(duration: 0.15)) { showingLogoutDialog = false }
                    }
                )
            }
        }
        .navigationBarHidden(true)
    }

    private func logOut() {
        // No need to manually flip any navigation flag here — AuthViewModel
        // sets isAuthenticated = false, and RootView reacts to that
        // automatically, same as Android popping the back stack on logout.
        authViewModel.signOut()
    }
}

// MARK: - Logout confirmation dialog

/// SwiftUI counterpart to Android's `LogoutConfirmationDialog` in
/// SettingsScreen.kt — same icon badge + message + Cancel/Log out pill
/// buttons, styled with this app's own AppColors/AppTheme so it reads as
/// part of Nexora rather than a stock system alert.
private struct LogoutConfirmationDialog: View {
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 18) {
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color.red.opacity(0.85), Color.red.opacity(0.6)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 60, height: 60)
                    Image(systemName: "arrow.right.square.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                }

                Text("Log out?")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(AppColors.primaryText)

                Text("Are you sure you want to log out? You'll need to sign in again to continue.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.primaryText.opacity(0.7))
                    .multilineTextAlignment(.center)

                HStack(spacing: 12) {
                    Button(action: onDismiss) {
                        Text("Cancel")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppColors.primaryText)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(AppColors.fieldBackground)
                            .cornerRadius(24)
                    }

                    Button(action: onConfirm) {
                        Text("Log out")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color.red.opacity(0.85))
                            .cornerRadius(24)
                    }
                }
                .padding(.top, 4)
            }
            .padding(24)
            .frame(maxWidth: 320)
            .background(AppColors.cardBackground)
            .cornerRadius(20)
            .shadow(color: .black.opacity(0.35), radius: 24, y: 8)
        }
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }
}
