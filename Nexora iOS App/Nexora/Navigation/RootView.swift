import SwiftUI

struct RootView: View {
    @EnvironmentObject var authViewModel: AuthViewModel

    // Starts false on every fresh process launch and is never persisted
    // to disk on purpose — see the "no Firebase user yet" branch below
    // for why that's fine. It only matters for the very first ever
    // launch, before any session (anonymous or real) has been created.
    @State private var didFinishOnboarding = false

    // Guards against calling signInAnonymously() more than once while
    // the async call is in flight (e.g. body re-evaluating).
    @State private var isStartingGuestSession = false

    var body: some View {
        Group {
            if authViewModel.isInitializing {
                SplashScreen()
            } else if authViewModel.isAuthenticated {
                // Covers BOTH a real account and an anonymous guest
                // session — either way there's a uid to browse and
                // (for real accounts) write progress under. Screens
                // that need a real account gate themselves on
                // authViewModel.isAnonymous, not on this branch.
                MainTabView()
                    .id(authViewModel.userProfile?.id ?? "signed-out")
            } else if !didFinishOnboarding {
                OnboardingScreen {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        didFinishOnboarding = true
                    }
                }
            } else {
                // No Firebase user at all yet — either the very first
                // launch just finished onboarding, or a real account
                // just signed out. Either way, silently start a guest
                // session so the person lands straight on the home tab
                // with zero forms to fill out. This is what keeps
                // browsing (courses, lessons, word cards) free of any
                // forced registration wall — see App Review 5.1.1(v).
                //
                // Deliberately NOT replaying onboarding after a real
                // sign-out: onboarding is "here's what the app does,"
                // shown once ever, not a re-auth gate.
                Color.clear
                    .task {
                        guard !isStartingGuestSession else { return }
                        isStartingGuestSession = true
                        await authViewModel.signInAnonymously()
                        isStartingGuestSession = false
                    }
            }
        }
        .animation(.easeInOut(duration: 0.35), value: authViewModel.isInitializing)
        .animation(.easeInOut(duration: 0.35), value: authViewModel.isAuthenticated)
        .animation(.easeInOut(duration: 0.35), value: didFinishOnboarding)
    }
}
