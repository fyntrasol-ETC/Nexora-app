import SwiftUI

// MARK: - Text fields

struct AppTextField: View {
    let placeholder: String
    @Binding var text: String
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        TextField(placeholder, text: $text)
            .font(.system(size: AppSizing.font(.body, for: sizeClass)))
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            .frame(height: AppSizing.fieldHeight(sizeClass))
            .background(AppColors.fieldBackground)
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass))
                    .stroke(AppColors.fieldBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass)))
    }
}

struct AppSecureField: View {
    let placeholder: String
    @Binding var text: String
    @Binding var isVisible: Bool
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        HStack {
            Group {
                if isVisible {
                    TextField(placeholder, text: $text)
                } else {
                    SecureField(placeholder, text: $text)
                }
            }
            .font(.system(size: AppSizing.font(.body, for: sizeClass)))

            Button {
                isVisible.toggle()
            } label: {
                Image(systemName: isVisible ? "eye.slash" : "eye")
                    .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
            }
        }
        .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
        .frame(height: AppSizing.fieldHeight(sizeClass))
        .background(AppColors.fieldBackground)
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass))
                .stroke(AppColors.fieldBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass)))
    }
}

// MARK: - Labeled field wrapper (title + any input control)

struct LabeledField<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(1.5, for: sizeClass)) {
            Text(title)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryText)
            content
        }
    }
}

// MARK: - Buttons

struct SocialButton: View {
    let icon: String
    let label: String
    var isSystemImage: Bool = true
    var action: () -> Void = {}
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                if isSystemImage {
                    Image(systemName: icon)
                        .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                } else {
                    Image(icon)
                        .resizable()
                        .scaledToFit()
                        .frame(
                            width: AppSizing.font(.body, for: sizeClass),
                            height: AppSizing.font(.body, for: sizeClass)
                        )
                }
                Text(label)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .medium))
            }
            .foregroundColor(AppColors.primaryText)
            .frame(maxWidth: .infinity)
            .frame(height: AppSizing.buttonHeight(sizeClass))
            .background(AppColors.fieldBackground)
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass))
                    .stroke(AppColors.fieldBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cornerRadius(sizeClass)))
        }
    }
}

/// The navy capsule pill used for every primary call-to-action
/// (Sign In, Create Account, Send Link, Update Password, ...).
struct PrimaryButton: View {
    let title: String
    var icon: String? = nil
    var tint: Color = AppColors.primaryButton
    let action: () -> Void
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                if let icon {
                    Image(systemName: icon)
                }
                Text(title)
            }
            .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: AppSizing.buttonHeight(sizeClass))
            .background(tint)
            .clipShape(Capsule())
        }
    }
}

// MARK: - Back button

/// The circular white "back" button used on every screen that supports
/// back navigation — pushed screens, and sheets that should read like a
/// full screen rather than a modal. Drop this into any header instead of
/// declaring a bare chevron locally, so back nav stays visually identical
/// everywhere and only needs to change in one place.
struct BackButton: View {
    var action: () -> Void
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryButton)
                .frame(width: 40 * sizeClass.scale, height: 40 * sizeClass.scale)
                .background(Color.white)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.12), radius: 6, y: 2)
        }
    }
}

// MARK: - Top bars / headers

/// Centered header used at the top of every auth screen (Login, Register...).
/// Set `showBackButton: false` on screens that already offer their own way
/// back (e.g. a "Back to Login" link lower on the page).
struct AuthTopBar: View {
    var title: String = ""
    var showBackButton: Bool = true
    var onBack: () -> Void = {}
    @Environment(\.appSizeClass) private var sizeClass

    private var barButtonSize: CGFloat { 40 * sizeClass.scale }

    var body: some View {
        HStack {
            if showBackButton {
                BackButton(action: onBack)
                Spacer()
                Text(title)
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                // Invisible spacer sized to match BackButton so the title stays centered.
                Color.clear.frame(width: barButtonSize, height: barButtonSize)
            } else {
                Spacer()
                Text(title)
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                Spacer()
            }
        }
        .padding(.horizontal, AppSizing.spacing(5, for: sizeClass))
        .padding(.top, AppSizing.spacing(3.5, for: sizeClass))
    }
}

/// Left-aligned header (back button + title, optional subtitle) used on
/// screens like Change Password, Delete Account, and Settings, where the
/// title sits next to a top-left back button rather than centered.
struct ScreenHeader: View {
    let title: String
    var subtitle: String? = nil
    var onBack: (() -> Void)? = nil
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
            HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                if let onBack {
                    BackButton(action: onBack)
                }
                Text(title)
                    .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass) * 0.92, weight: .bold))
                    .foregroundColor(.white)
            }
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                    .foregroundColor(.white.opacity(0.9))
            }
        }
        // Without this, the VStack only takes up as much width as its
        // content needs (back button + short title), so a short title like
        // "Settings" hugs tight and the *parent* container's default
        // center alignment shoves the whole block — back button included —
        // toward the middle of the screen instead of the left edge.
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, AppSizing.spacing(5, for: sizeClass))
        .padding(.top, AppSizing.spacing(3.5, for: sizeClass))
    }
}

// MARK: - Card containers

/// The rounded white card every auth form sits inside.
struct AuthCard<Content: View>: View {
    var spacing: CGFloat? = nil
    @ViewBuilder var content: Content
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        VStack(spacing: spacing ?? AppSizing.spacing(4, for: sizeClass)) {
            content
        }
        .padding(AppSizing.spacing(6, for: sizeClass))
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 28 * sizeClass.scale))
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass) * 0.83) // ~20pt on iPhone, scales with the card cap on iPad
    }
}

/// A tighter white card for stacked rows (Settings, Profile menus) where
/// each row manages its own internal padding — unlike `AuthCard`, this
/// doesn't add extra padding around the row list.
struct SettingsCard<Content: View>: View {
    @ViewBuilder var content: Content
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        VStack(spacing: 0) {
            content
        }
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.33))
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass) * 0.83)
    }
}

// MARK: - Settings / menu rows

/// The plain content of a settings-style row: icon bubble, title, optional
/// trailing value, and a trailing chevron / external-link glyph. Use this
/// directly as a `NavigationLink` label; use `SettingsRow` when you just
/// need a tap action instead of a navigation push.
struct SettingsRowContent: View {
    enum TrailingStyle {
        case chevron
        case external
        case none
    }

    let icon: String
    let title: String
    var value: String? = nil
    var isDestructive: Bool = false
    var trailingStyle: TrailingStyle = .chevron
    @Environment(\.appSizeClass) private var sizeClass

    private var titleColor: Color { isDestructive ? .red : AppColors.primaryText }
    private var iconTint: Color { isDestructive ? .red : AppColors.primaryButton }
    private var iconBackground: Color { isDestructive ? Color.red.opacity(0.12) : AppColors.fieldBackground }
    private var iconBubbleSize: CGFloat { 36 * sizeClass.scale }

    var body: some View {
        HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            ZStack {
                Circle()
                    .fill(iconBackground)
                    .frame(width: iconBubbleSize, height: iconBubbleSize)
                Image(systemName: icon)
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                    .foregroundColor(iconTint)
            }
            Text(title)
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .medium))
                .foregroundColor(titleColor)
            Spacer()
            if let value {
                Text(value)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
            }
            switch trailingStyle {
            case .chevron:
                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
            case .external:
                Image(systemName: "arrow.up.forward.square")
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
            case .none:
                EmptyView()
            }
        }
        .padding(.vertical, AppSizing.spacing(3.5, for: sizeClass))
        .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
        .contentShape(Rectangle())
    }
}

/// A tappable settings row that runs a closure (open a link, log out, etc.)
/// rather than pushing a screen. For navigation pushes, use
/// `NavigationLink(value:) { SettingsRowContent(...) }` instead.
struct SettingsRow: View {
    let icon: String
    let title: String
    var value: String? = nil
    var isDestructive: Bool = false
    var trailingStyle: SettingsRowContent.TrailingStyle = .chevron
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SettingsRowContent(
                icon: icon,
                title: title,
                value: value,
                isDestructive: isDestructive,
                trailingStyle: trailingStyle
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Profile stat cards

/// One of the small colored stat tiles on the Profile screen
/// ("Time spent", "Badges", "Words I know").
struct StatCard: View {
    let title: String
    let icon: String
    let iconTint: Color
    let value: String
    var showChevron: Bool = false
    var backgroundColor: Color
    @Environment(\.appSizeClass) private var sizeClass

    private var innerCircleSize: CGFloat { 32 * sizeClass.scale }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
            HStack {
                Text(title)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .medium))
                    .foregroundColor(AppColors.primaryText.opacity(0.7))
                Spacer()
                if showChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                        .foregroundColor(iconTint)
                }
            }
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                ZStack {
                    Circle().fill(Color.white).frame(width: innerCircleSize, height: innerCircleSize)
                    Image(systemName: icon)
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                        .foregroundColor(iconTint)
                }
                Text(value)
                    .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
            }
        }
        .padding(AppSizing.spacing(4, for: sizeClass))
        .background(backgroundColor)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.11))
    }
}

/// A single day's state inside the Weekly activity strip on Profile.
struct DayActivityStatus: Identifiable {
    enum State {
        case missed
        case today
        case completed
        case upcoming
    }

    let id = UUID()
    let label: String
    let state: State
}

/// The "Weekly activity" card on the Profile screen: a translucent panel
/// with one circle + day-letter per day of the week.
struct WeeklyActivityCard: View {
    let days: [DayActivityStatus]
    @Environment(\.appSizeClass) private var sizeClass

    private var dayCircleSize: CGFloat { 36 * sizeClass.scale }
    private var dotSize: CGFloat { 10 * sizeClass.scale }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(4, for: sizeClass)) {
            Text("Weekly activity")
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                .foregroundColor(.white)
            HStack(spacing: AppSizing.spacing(2.5, for: sizeClass)) {
                ForEach(days) { day in
                    VStack(spacing: AppSizing.spacing(1.5, for: sizeClass)) {
                        dayCircle(for: day.state)
                        Text(day.label)
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
                            .foregroundColor(.white.opacity(0.85))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(AppSizing.spacing(5, for: sizeClass))
        .background(Color.white.opacity(0.16))
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.33))
    }

    @ViewBuilder
    private func dayCircle(for state: DayActivityStatus.State) -> some View {
        ZStack {
            Circle()
                .fill(state == .today ? Color.white : Color.white.opacity(0.25))
                .frame(width: dayCircleSize, height: dayCircleSize)
            switch state {
            case .missed:
                Image(systemName: "xmark")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .foregroundColor(.white.opacity(0.85))
            case .today:
                Circle()
                    .fill(AppColors.primaryButton)
                    .frame(width: dotSize, height: dotSize)
            case .completed:
                Image(systemName: "checkmark")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryButton)
            case .upcoming:
                EmptyView()
            }
        }
    }
}

// MARK: - Sign-in gate (anonymous-session upsell)
//
// The app runs everyone — including people who haven't registered —
// on an anonymous Firebase session so browsing (courses, lessons, word
// cards) never hits a forced sign-up wall (App Review 5.1.1(v)). These
// two views are the one place every "this needs a real account" prompt
// is built, so wording/behavior stays consistent across Quiz, Words,
// Videos, Profile, and Home. Both present the existing
// Login/Register flow (AuthNavigationView) as a sheet; because
// AuthViewModel's sign-in/sign-up methods call `link(with:)` on an
// anonymous session, whatever the person does in that sheet upgrades
// the SAME uid rather than starting over.

/// Inline card that replaces a piece of account-based content (e.g. the
/// Home progress card, or the end of a word-swipe session) while the
/// current session is anonymous.
struct SignInPromptCard: View {
    var title: String = "Sign in to continue"
    var message: String
    var icon: String = "lock.fill"
    @Environment(\.appSizeClass) private var sizeClass
    @State private var showingAuth = false

    var body: some View {
        VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            ZStack {
                Circle()
                    .fill(AppColors.primaryButton.opacity(0.12))
                    .frame(width: 56 * sizeClass.scale, height: 56 * sizeClass.scale)
                Image(systemName: icon)
                    .font(.system(size: AppSizing.font(.title, for: sizeClass)))
                    .foregroundColor(AppColors.primaryButton)
            }

            Text(title)
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryText)
                .multilineTextAlignment(.center)

            Text(message)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                .foregroundColor(AppColors.placeholderText)
                .multilineTextAlignment(.center)

            PrimaryButton(title: "Sign In", icon: "arrow.right") {
                showingAuth = true
            }
            .frame(maxWidth: 220 * sizeClass.scale)
        }
        .padding(AppSizing.spacing(5, for: sizeClass))
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .fill(AppColors.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(AppColors.primaryButton.opacity(0.10), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.06), radius: 10, y: 5)
        .sheet(isPresented: $showingAuth) {
            AuthNavigationView()
        }
    }
}

/// Full-screen variant for a destination that shouldn't render its real
/// content at all until the person has a real account — e.g. what a
/// quiz's "Start Quiz" button pushes to instead of the quiz itself, or
/// what tapping a video pushes to instead of the player, while
/// `authViewModel.isAnonymous` is true.
struct SignInRequiredScreen: View {
    var title: String = "Sign in to continue"
    var message: String
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appSizeClass) private var sizeClass

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
                Spacer()

                SignInPromptCard(title: title, message: message)
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))

                Button("Not now") { dismiss() }
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .medium))
                    .foregroundColor(.white.opacity(0.85))

                Spacer()
            }
        }
        .navigationBarHidden(true)
    }
}
