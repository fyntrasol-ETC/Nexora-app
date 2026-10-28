import SwiftUI

/// Destinations reachable from the Profile tab. Kept as one enum so both
/// Profile and Settings can push into the same NavigationStack.
enum ProfileRoute: Hashable {
    case settings
    case changePassword
    case deleteAccount
    case badges
    case editProfile
    case changeAvatar
}

struct ProfileScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var badgesViewModel = BadgesViewModel()
    // Read-only here — just used to surface the live "known words" count.
    // WordsScreen owns its own WordsViewModel instance for the actual
    // list/study flows; this one exists purely so the stat card doesn't
    // show a stale "0".
    @StateObject private var wordsViewModel = WordsViewModel()
    // Backs WeeklyCheckInCard with a live Firestore listener (see
    // WeeklyActivityViewModel). Replaces the old local-only @State that
    // used to live inside WeeklyCheckInCard.
    @StateObject private var weeklyActivityViewModel = WeeklyActivityViewModel()
    // Shared across EditProfileScreen and ChangeAvatarScreen so the staged
    // avatar pick and in-progress name edit survive the push/pop between
    // them — same "one view model backs both screens" shape as Android.
    @StateObject private var editProfileViewModel = EditProfileViewModel()

    // Bound up to MainTabView so it can hide the tab bar once this stack is
    // no longer at its root (i.e. Settings / Change Password / Delete
    // Account / Badges / Edit Profile / Change Avatar are pushed).
    @Binding var path: NavigationPath

    /// Called when a badge's "Go to course" is tapped — MainTabView owns
    /// the Courses tab's own path, so this bubbles up to it instead of
    /// trying to navigate a stack this screen doesn't own.
    var onGoToCourse: (Course) -> Void

    /// Called when the "Words I know" badge is tapped — MainTabView owns
    /// the Words tab's own NavigationStack, so this just asks it to switch
    /// the selected tab to Words rather than this screen trying to push
    /// into a stack it doesn't own.
    var onGoToWords: () -> Void

    private var userName: String {
        authViewModel.userProfile?.displayName ?? "..."
    }

    private var avatarSeed: String {
        AvatarOptions.resolveSeed(authViewModel.userProfile?.avatarSeed)
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                AppBackground()

                VStack(spacing: 0) {
                    header

                    ScrollView {
                        VStack(spacing: 18) {
                            // Checking in requires a real account — a guest
                            // has nowhere to keep a streak once the app is
                            // deleted or reinstalled. Swaps in the same
                            // sign-in prompt used elsewhere rather than
                            // showing a check-in button that silently does
                            // nothing (WeeklyActivityViewModel.checkInToday()
                            // already no-ops for a guest as a safety net).
                            if authViewModel.isRealAccount {
                                WeeklyCheckInCard(viewModel: weeklyActivityViewModel)
                            } else {
                                SignInPromptCard(
                                    title: "Sign in to check in",
                                    message: "Create an account to track your daily streak and check in.",
                                    icon: "flame.fill"
                                )
                            }

                            HStack(spacing: 14) {
                                NavigationLink(value: ProfileRoute.badges) {
                                    BadgeStatView(
                                        icon: "trophy.fill",
                                        value: "\(badgesViewModel.uiState.unlockedBadgeIds.count)",
                                        title: "Badges",
                                        tint: AppColors.badgeGold,
                                        cardBackground: Color(red: 0.99, green: 0.95, blue: 0.85)
                                    )
                                }
                                .buttonStyle(.plain)

                                Button {
                                    onGoToWords()
                                } label: {
                                    BadgeStatView(
                                        icon: "star.fill",
                                        value: "\(wordsViewModel.knownWords.count)",
                                        title: "Words I know",
                                        tint: AppColors.accentRed,
                                        cardBackground: Color(red: 0.99, green: 0.90, blue: 0.91)
                                    )
                                }
                                .buttonStyle(.plain)
                            }

                            Spacer(minLength: 100)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 18)
                    }
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(for: ProfileRoute.self) { route in
                switch route {
                case .settings:
                    SettingsScreen()
                case .changePassword:
                    ChangePasswordScreen()
                case .deleteAccount:
                    DeleteAccountScreen()
                case .badges:
                    BadgesScreen(
                        unlockedBadgeIds: badgesViewModel.uiState.unlockedBadgeIds,
                        progressByCourseId: badgesViewModel.uiState.progressByCourseId,
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                        },
                        onGoToCourse: onGoToCourse
                    )
                case .editProfile:
                    // Same gate as the check-in card above — editing name/
                    // avatar is account-based, so a guest sees the sign-in
                    // prompt instead of a form that (per
                    // EditProfileViewModel's guards) wouldn't actually save.
                    if authViewModel.isAnonymous {
                        SignInRequiredScreen(message: "Create an account to edit your profile.")
                    } else {
                        EditProfileScreen(
                            editProfileViewModel: editProfileViewModel,
                            path: $path
                        )
                    }
                case .changeAvatar:
                    // Only reachable from EditProfileScreen, which is
                    // already gated above — this is a safety net in case
                    // .changeAvatar is ever pushed some other way.
                    if authViewModel.isAnonymous {
                        SignInRequiredScreen(message: "Create an account to change your avatar.")
                    } else {
                        ChangeAvatarScreen(
                            editProfileViewModel: editProfileViewModel,
                            path: $path
                        )
                    }
                }
            }
        }
        .task {
            guard let uid = authViewModel.userProfile?.id else { return }
            await wordsViewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
            // Not async — just attaches a live Firestore listener. See the
            // note on WeeklyActivityViewModel.configure(uid:isRealAccount:).
            weeklyActivityViewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
        }
    }

    /// Centered "hero" header: big avatar, name underneath, settings gear
    /// pinned to the top-right corner so it doesn't disturb the centering.
    private var header: some View {
        ZStack(alignment: .topTrailing) {
            VStack(spacing: 12) {
                DiceBearAvatar(seed: avatarSeed, size: 116)
                    .shadow(color: .black.opacity(0.25), radius: 10, y: 4)

                Text(userName)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)

            NavigationLink(value: ProfileRoute.settings) {
                ZStack {
                    Circle().fill(Color.white).frame(width: 40, height: 40)
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 17))
                        .foregroundColor(AppColors.primaryButton)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 6)
    }
}

/// Duolingo-style weekly attendance card: a colored hero card with a row of
/// seven day pills and a "Check in" button for today. Driven entirely by
/// WeeklyActivityViewModel's live Firestore listener — no local-only state
/// here anymore, so this always agrees with Android and with other
/// sessions on the same account.
private struct WeeklyCheckInCard: View {
    @ObservedObject var viewModel: WeeklyActivityViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("THIS WEEK")
                        .font(.system(size: 12, weight: .bold))
                        .tracking(1.1)
                        .foregroundColor(.white.opacity(0.75))
                    Text("\(viewModel.checkedInCount) day\(viewModel.checkedInCount == 1 ? "" : "s") checked in")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(.white)
                }
                Spacer()
                Image(systemName: "flame.fill")
                    .font(.system(size: 24))
                    .foregroundColor(Color(red: 1.0, green: 0.78, blue: 0.25))
            }

            HStack(spacing: 8) {
                ForEach(viewModel.days) { day in
                    DayPill(day: day)
                }
            }

            Button {
                viewModel.checkInToday()
            } label: {
                HStack(spacing: 8) {
                    if viewModel.isCheckingIn {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .tint(Color(red: 0.35, green: 0.2, blue: 0.75))
                    } else {
                        Image(systemName: viewModel.isTodayCheckedIn ? "checkmark.seal.fill" : "checkmark.circle.fill")
                    }
                    Text(viewModel.isTodayCheckedIn ? "You're checked in for today!" : "Check in for today")
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(viewModel.isTodayCheckedIn ? .white : Color(red: 0.35, green: 0.2, blue: 0.75))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(viewModel.isTodayCheckedIn ? Color.white.opacity(0.2) : Color.white)
                )
            }
            .disabled(viewModel.isTodayCheckedIn || viewModel.isCheckingIn)
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color(red: 0.42, green: 0.28, blue: 0.88),
                            Color(red: 0.62, green: 0.38, blue: 0.95)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .shadow(color: Color(red: 0.42, green: 0.28, blue: 0.88).opacity(0.35), radius: 14, y: 8)
    }
}

private struct DayPill: View {
    let day: WeeklyDayModel

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(fillColor)
                    .frame(width: 32, height: 32)

                if day.state == .today {
                    Circle()
                        .stroke(Color.white, lineWidth: 2)
                        .frame(width: 32, height: 32)
                }

                iconView
            }
            Text(day.label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.white.opacity(day.state == .upcoming ? 0.45 : 0.9))
        }
        .frame(maxWidth: .infinity)
    }

    private var fillColor: Color {
        switch day.state {
        case .completed: return Color(red: 0.30, green: 0.80, blue: 0.46)
        case .missed: return Color(red: 0.95, green: 0.35, blue: 0.38)
        case .today: return Color.white.opacity(0.22)
        case .upcoming: return Color.white.opacity(0.12)
        }
    }

    @ViewBuilder
    private var iconView: some View {
        switch day.state {
        case .completed:
            Image(systemName: "checkmark")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.white)
        case .missed:
            Image(systemName: "xmark")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(.white)
        case .today:
            Circle()
                .fill(Color.white)
                .frame(width: 7, height: 7)
        case .upcoming:
            EmptyView()
        }
    }
}

// MARK: - Badge-shaped stat

/// A shield/medal silhouette used to present a stat "like an achievement
/// badge" — rounded shoulders tapering to a point at the bottom.
private struct BadgeShieldShape: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        let corner = w * 0.28

        var path = Path()
        path.move(to: CGPoint(x: 0, y: corner))
        path.addQuadCurve(to: CGPoint(x: corner, y: 0), control: CGPoint(x: 0, y: 0))
        path.addLine(to: CGPoint(x: w - corner, y: 0))
        path.addQuadCurve(to: CGPoint(x: w, y: corner), control: CGPoint(x: w, y: 0))
        path.addLine(to: CGPoint(x: w, y: h * 0.56))
        path.addQuadCurve(
            to: CGPoint(x: w * 0.5, y: h),
            control: CGPoint(x: w * 0.86, y: h * 0.86)
        )
        path.addQuadCurve(
            to: CGPoint(x: 0, y: h * 0.56),
            control: CGPoint(x: w * 0.14, y: h * 0.86)
        )
        path.addLine(to: CGPoint(x: 0, y: corner))
        path.closeSubpath()
        return path
    }
}

/// One stat rendered as a colored badge/medal with an icon + number inside
/// the shield, and its label underneath, all sitting on a soft card.
private struct BadgeStatView: View {
    let icon: String
    let value: String
    let title: String
    let tint: Color
    let cardBackground: Color

    var body: some View {
        VStack(spacing: 10) {
            ZStack {
                BadgeShieldShape()
                    .fill(
                        LinearGradient(
                            colors: [tint, tint.opacity(0.75)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 76, height: 90)
                    .shadow(color: tint.opacity(0.45), radius: 6, y: 4)

                // Subtle rim highlight so the badge reads as embossed metal
                // rather than a flat sticker.
                BadgeShieldShape()
                    .stroke(Color.white.opacity(0.35), lineWidth: 1.5)
                    .frame(width: 76, height: 90)

                VStack(spacing: 3) {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white.opacity(0.9))
                    Text(value)
                        .font(.system(size: 24, weight: .heavy))
                        .foregroundColor(.white)
                }
                .padding(.bottom, 8)
            }

            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.black.opacity(0.75))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(cardBackground)
        )
    }
}
