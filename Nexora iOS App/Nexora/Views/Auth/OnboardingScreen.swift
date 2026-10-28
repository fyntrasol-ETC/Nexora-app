import SwiftUI

// MARK: - Page data

private struct OnboardingPageData {
    let eyebrow: String
    let title: String
    let subtitle: String
}

private let onboardingPages: [OnboardingPageData] = [
    .init(
        eyebrow: "LEARN",
        title: "Structured Courses",
        subtitle: "Work through Beginner, Intermediate and Advanced courses — chapter by chapter, dialogues and quizzes included."
    ),
    .init(
        eyebrow: "PRACTICE",
        title: "Build Your Vocabulary",
        subtitle: "Swipe through flashcards, bookmark tricky words, and see everything you already know in one place."
    ),
    .init(
        eyebrow: "GROW",
        title: "Watch, Track, Repeat",
        subtitle: "Learn from bite-sized videos, keep your streak alive, and collect badges as you go."
    )
]

// MARK: - Root onboarding screen

struct OnboardingScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @State private var currentPage = 0
    var onFinish: () -> Void

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {

                // MARK: Skip
                HStack {
                    Spacer()
                    Button {
                        onFinish()
                    } label: {
                        Text("Skip")
                            .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                            .foregroundColor(.white.opacity(0.85))
                    }
                }
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                .padding(.top, AppSizing.spacing(3, for: sizeClass))
                .opacity(currentPage == onboardingPages.count - 1 ? 0 : 1)
                .animation(.easeInOut(duration: 0.2), value: currentPage)

                // MARK: Pages
                TabView(selection: $currentPage) {
                    ForEach(0..<onboardingPages.count, id: \.self) { index in
                        OnboardingPageView(page: onboardingPages[index], index: index, isActive: currentPage == index)
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))
                .animation(.easeInOut(duration: 0.3), value: currentPage)

                // MARK: Page dots
                HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                    ForEach(0..<onboardingPages.count, id: \.self) { index in
                        Capsule()
                            .fill(index == currentPage ? Color.white : Color.white.opacity(0.35))
                            .frame(
                                width: (index == currentPage ? 22 : 8) * sizeClass.scale,
                                height: 8 * sizeClass.scale
                            )
                            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: currentPage)
                    }
                }
                .padding(.bottom, AppSizing.spacing(5, for: sizeClass))

                // MARK: CTA
                PrimaryButton(
                    title: currentPage == onboardingPages.count - 1 ? "Get Started" : "Next",
                    icon: "arrow.right"
                ) {
                    if currentPage < onboardingPages.count - 1 {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentPage += 1
                        }
                    } else {
                        onFinish()
                    }
                }
                .frame(maxWidth: AppSizing.maxContentWidth(sizeClass) * 0.72)
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                .scaleEffect(currentPage == onboardingPages.count - 1 ? 1.03 : 1.0)
                .animation(.spring(response: 0.4, dampingFraction: 0.6), value: currentPage)

                if currentPage < onboardingPages.count - 1 {
                    Button {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentPage = onboardingPages.count - 1
                        }
                    } label: {
                        Text("Skip to end")
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .padding(.top, AppSizing.spacing(2.5, for: sizeClass))
                    .transition(.opacity)
                } else {
                    Color.clear
                        .frame(height: AppSizing.font(.caption, for: sizeClass) + AppSizing.spacing(2.5, for: sizeClass))
                }

                Spacer(minLength: AppSizing.spacing(4, for: sizeClass))
            }
        }
        .navigationBarHidden(true)
    }
}

// MARK: - Single page

private struct OnboardingPageView: View {
    let page: OnboardingPageData
    let index: Int
    let isActive: Bool
    @Environment(\.appSizeClass) private var sizeClass

    @State private var showMockup = false
    @State private var showText = false

    var body: some View {
        VStack(spacing: AppSizing.spacing(5, for: sizeClass)) {
            Spacer(minLength: AppSizing.spacing(2, for: sizeClass))

            // Mockup illustration matching the real screen for this feature
            Group {
                switch index {
                case 0: CoursesMockup(isActive: isActive)
                case 1: FlashcardMockup(isActive: isActive)
                default: VideoAndStreakMockup(isActive: isActive)
                }
            }
            .frame(maxWidth: AppSizing.maxContentWidth(sizeClass) * 0.68)
            .opacity(showMockup ? 1 : 0)
            .scaleEffect(showMockup ? 1 : 0.85)
            .offset(y: showMockup ? 0 : 24)

            VStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                Text(page.eyebrow)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .tracking(2)
                    .foregroundColor(.white.opacity(0.75))

                Text(page.title)
                    .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)

                Text(page.subtitle)
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                    .foregroundColor(.white.opacity(0.9))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            }
            .opacity(showText ? 1 : 0)
            .offset(y: showText ? 0 : 16)

            Spacer(minLength: AppSizing.spacing(2, for: sizeClass))
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .onAppear { animateIn() }
        .onChange(of: isActive) { active in
            if active {
                animateIn()
            }
        }
    }

    private func animateIn() {
        showMockup = false
        showText = false
        withAnimation(.spring(response: 0.55, dampingFraction: 0.75).delay(0.05)) {
            showMockup = true
        }
        withAnimation(.easeOut(duration: 0.45).delay(0.22)) {
            showText = true
        }
    }
}

// MARK: - Mockup 1: Courses + progress ring + level pills + badges strip
// (matches Courses tab cards, Home's "Your Progress", and the Badges grid)

private struct CoursesMockup: View {
    let isActive: Bool
    @Environment(\.appSizeClass) private var sizeClass

    @State private var progressValue: CGFloat = 0
    @State private var cardFloat = false
    @State private var badgePulse = false

    private let badgeGlyphs: [(icon: String, unlocked: Bool)] = [
        ("book.closed.fill", true),
        ("book.closed.fill", true),
        ("book.closed.fill", false),
        ("book.closed.fill", false)
    ]

    var body: some View {
        VStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {

            // MARK: Your Progress ring — echoes Home's "Your Progress" card
            HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                ZStack {
                    Circle()
                        .stroke(AppColors.fieldBorder, lineWidth: 5 * sizeClass.scale)
                    Circle()
                        .trim(from: 0, to: progressValue)
                        .stroke(
                            AppColors.link,
                            style: StrokeStyle(lineWidth: 5 * sizeClass.scale, lineCap: .round)
                        )
                        .rotationEffect(.degrees(-90))
                    Text("38%")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                        .foregroundColor(AppColors.primaryText)
                }
                .frame(width: 42 * sizeClass.scale, height: 42 * sizeClass.scale)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Your Progress")
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                        .foregroundColor(AppColors.primaryText)
                    Text("Across 6 enrolled courses")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                        .foregroundColor(AppColors.placeholderText)
                }
                Spacer()
            }
            .padding(AppSizing.spacing(3, for: sizeClass))
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
            .shadow(color: .black.opacity(0.12), radius: 10, y: 5)

            // MARK: Level pills — echoes Beginner / Intermediate / Advanced rows
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                levelPill(icon: "leaf.fill", label: "Beginner", selected: true)
                levelPill(icon: "flame.fill", label: "Intermediate", selected: false)
                levelPill(icon: "bolt.fill", label: "Advanced", selected: false)
            }

            // MARK: Course cards, stacked/peeking like the horizontal carousel
            ZStack {
                courseCard(title: "Making\nSmall Talk", progress: 1.0, tag: "1 Chapter")
                    .frame(width: AppSizing.courseCardSize(sizeClass) * 1.5)
                    .rotationEffect(.degrees(6))
                    .offset(x: 32 * sizeClass.scale, y: (cardFloat ? 6 : 10) * sizeClass.scale)
                    .opacity(0.55)

                courseCard(title: "Everyday\nGreetings", progress: 1.0, tag: "3 Chapters")
                    .frame(width: AppSizing.courseCardSize(sizeClass) * 1.6)
                    .offset(y: cardFloat ? -4 : 0)
                    .shadow(color: .black.opacity(0.18), radius: 14, y: 8)
            }

            // MARK: Badges strip — small unlocked/locked badge circles,
            // echoing the Badges grid on the Profile tab.
            HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                ForEach(0..<badgeGlyphs.count, id: \.self) { i in
                    badgeCircle(
                        icon: badgeGlyphs[i].icon,
                        unlocked: badgeGlyphs[i].unlocked
                    )
                    .scaleEffect(badgeGlyphs[i].unlocked && badgePulse ? 1.12 : 1.0)
                    .animation(
                        badgeGlyphs[i].unlocked
                            ? .easeInOut(duration: 0.9).repeatForever(autoreverses: true).delay(Double(i) * 0.15)
                            : .default,
                        value: badgePulse
                    )
                }
            }
            .padding(.horizontal, AppSizing.spacing(3, for: sizeClass))
            .padding(.vertical, AppSizing.spacing(2.5, for: sizeClass))
            .background(Color.white.opacity(0.16))
            .clipShape(Capsule())
        }
        .onAppear { startAnimations() }
        .onChange(of: isActive) { active in
            if active { startAnimations() }
        }
    }

    private func startAnimations() {
        progressValue = 0
        withAnimation(.easeOut(duration: 1.0).delay(0.3)) {
            progressValue = 0.38
        }
        withAnimation(.easeInOut(duration: 1.6).repeatForever(autoreverses: true)) {
            cardFloat = true
        }
        badgePulse = false
        withAnimation { badgePulse = true }
    }

    private func levelPill(icon: String, label: String, selected: Bool) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.85))
            Text(label)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
        }
        .foregroundColor(selected ? AppColors.primaryButton : .white)
        .padding(.horizontal, AppSizing.spacing(2.5, for: sizeClass))
        .padding(.vertical, AppSizing.spacing(1.5, for: sizeClass))
        .background(selected ? Color.white : Color.white.opacity(0.18))
        .clipShape(Capsule())
    }

    private func badgeCircle(icon: String, unlocked: Bool) -> some View {
        ZStack {
            Circle()
                .fill(unlocked ? AppColors.link : Color.white.opacity(0.2))
                .frame(width: 34 * sizeClass.scale, height: 34 * sizeClass.scale)
            Image(systemName: icon)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                .foregroundColor(unlocked ? .white : .white.opacity(0.5))
            if !unlocked {
                ZStack {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 14 * sizeClass.scale, height: 14 * sizeClass.scale)
                    Image(systemName: "lock.fill")
                        .font(.system(size: 7 * sizeClass.scale, weight: .bold))
                        .foregroundColor(AppColors.placeholderText)
                }
                .offset(x: 12 * sizeClass.scale, y: 12 * sizeClass.scale)
            }
        }
    }

    private func courseCard(title: String, progress: Double, tag: String) -> some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
            RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass))
                .fill(
                    LinearGradient(
                        colors: [AppColors.gradientTop, AppColors.link],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .frame(height: 60 * sizeClass.scale)
                .overlay(
                    Image(systemName: "book.closed.fill")
                        .font(.system(size: AppSizing.font(.title, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.9))
                )

            Text(title)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                .foregroundColor(AppColors.primaryText)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)

            Capsule()
                .fill(AppColors.fieldBorder)
                .frame(height: 5 * sizeClass.scale)
                .overlay(
                    GeometryReader { geo in
                        Capsule()
                            .fill(AppColors.link)
                            .frame(width: geo.size.width * progress)
                    }
                )

            HStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                Image(systemName: "book")
                Text(tag)
            }
            .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
            .foregroundColor(AppColors.placeholderText)
        }
        .padding(AppSizing.spacing(3, for: sizeClass))
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
    }
}

// MARK: - Mockup 2: Flashcard + know/don't-know strip
// (matches the Words flashcard screen + My Words summary)

private struct FlashcardMockup: View {
    let isActive: Bool
    @Environment(\.appSizeClass) private var sizeClass

    @State private var tilt: Double = -6
    @State private var checkPulse = false
    @State private var barsIn = false

    var body: some View {
        VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            Text("Do you know this word?")
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .medium))
                .foregroundColor(AppColors.primaryText)
                .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
                .padding(.vertical, AppSizing.spacing(2, for: sizeClass))
                .background(Color.white.opacity(0.95))
                .clipShape(Capsule())

            VStack(spacing: AppSizing.spacing(5, for: sizeClass)) {
                HStack {
                    Image(systemName: "speaker.wave.2.fill")
                    Spacer()
                    Image(systemName: "bookmark.fill")
                }
                .foregroundColor(AppColors.primaryButton)
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))

                VStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                    Text("essential")
                        .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                        .foregroundColor(AppColors.primaryText)
                    Text("adjective")
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                        .foregroundColor(AppColors.placeholderText)
                }
                .frame(maxHeight: .infinity)

                HStack {
                    circleGlyph("xmark", tint: AppColors.link)
                    Spacer()
                    circleGlyph("info", tint: AppColors.link, filled: false)
                    Spacer()
                    circleGlyph("checkmark", tint: AppColors.accentRed, pulse: checkPulse)
                }
            }
            .padding(AppSizing.spacing(5, for: sizeClass))
            .frame(height: 200 * sizeClass.scale)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
            .shadow(color: .black.opacity(0.18), radius: 14, y: 8)
            .rotationEffect(.degrees(tilt))

            // MARK: I don't know / I know strip — echoes My Words screen
            HStack(spacing: AppSizing.spacing(2.5, for: sizeClass)) {
                wordCountPill(icon: "xmark", label: "Don't know", count: "4", filled: true)
                wordCountPill(icon: "checkmark", label: "I know", count: "11", filled: false)
            }
            .opacity(barsIn ? 1 : 0)
            .offset(y: barsIn ? 0 : 10)
        }
        .onAppear { startAnimations() }
        .onChange(of: isActive) { active in
            if active { startAnimations() }
        }
    }

    private func startAnimations() {
        tilt = -6
        withAnimation(.spring(response: 0.6, dampingFraction: 0.55).delay(0.1)) {
            tilt = 0
        }
        checkPulse = false
        withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true).delay(0.6)) {
            checkPulse = true
        }
        barsIn = false
        withAnimation(.easeOut(duration: 0.4).delay(0.35)) {
            barsIn = true
        }
    }

    private func wordCountPill(icon: String, label: String, count: String, filled: Bool) -> some View {
        HStack(spacing: AppSizing.spacing(1.5, for: sizeClass)) {
            Image(systemName: icon)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
            Text(label)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
            Text(count)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                .padding(.horizontal, AppSizing.spacing(1.5, for: sizeClass))
                .padding(.vertical, 2)
                .background(filled ? Color.white.opacity(0.25) : AppColors.fieldBackground)
                .clipShape(Capsule())
        }
        .foregroundColor(filled ? .white : AppColors.primaryText)
        .padding(.horizontal, AppSizing.spacing(3, for: sizeClass))
        .padding(.vertical, AppSizing.spacing(2, for: sizeClass))
        .frame(maxWidth: .infinity)
        .background(filled ? AppColors.primaryButton : Color.white)
        .clipShape(Capsule())
    }

    private func circleGlyph(_ icon: String, tint: Color, filled: Bool = true, pulse: Bool = false) -> some View {
        ZStack {
            Circle()
                .fill(filled ? AppColors.fieldBackground : Color.clear)
                .overlay(Circle().stroke(tint.opacity(filled ? 0 : 0.5), lineWidth: 1))
                .frame(width: 38 * sizeClass.scale, height: 38 * sizeClass.scale)
            Image(systemName: icon)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                .foregroundColor(tint)
        }
        .scaleEffect(pulse ? 1.15 : 1.0)
    }
}

// MARK: - Mockup 3: Video card + weekly streak + mini stat cards
// (matches Videos tab, Profile weekly strip, and Profile stat tiles)

// MARK: - Mockup 3: Video card + weekly streak + mini stat cards
// (matches Videos tab, Profile weekly strip, and Profile stat tiles)

private struct VideoAndStreakMockup: View {
    let isActive: Bool
    @Environment(\.appSizeClass) private var sizeClass

    private let letters = ["M", "T", "W", "T", "F", "S", "S"]
    private let filled: Set<Int> = [0, 1]

    @State private var playPulse = false
    @State private var heartBeat = false
    @State private var flameFlicker = false
    @State private var dayReveal = 0
    @State private var statsIn = false

    var body: some View {
        VStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {
            // Video card
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .bottomTrailing) {
                    Rectangle()
                        .fill(Color.black.opacity(0.85))
                        .frame(height: 92 * sizeClass.scale)
                        .overlay(
                            ZStack {
                                Circle()
                                    .fill(Color.white)
                                    .frame(width: 38 * sizeClass.scale, height: 38 * sizeClass.scale)
                                    .scaleEffect(playPulse ? 1.12 : 1.0)
                                Image(systemName: "play.fill")
                                    .foregroundColor(AppColors.primaryButton)
                            }
                        )

                    Text("04:32")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, AppSizing.spacing(1.5, for: sizeClass))
                        .padding(.vertical, AppSizing.spacing(0.5, for: sizeClass))
                        .background(Color.black.opacity(0.6))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                        .padding(6)
                }

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Pronunciation: Getting Started")
                            .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                            .foregroundColor(AppColors.primaryText)
                            .lineLimit(1)
                        Text("Pronunciation")
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                            .foregroundColor(AppColors.placeholderText)
                    }
                    Spacer()
                    Image(systemName: "heart.fill")
                        .foregroundColor(AppColors.accentRed)
                        .scaleEffect(heartBeat ? 1.25 : 1.0)
                }
                .padding(AppSizing.spacing(3, for: sizeClass))
            }
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
            .shadow(color: .black.opacity(0.18), radius: 14, y: 8)

            // Weekly streak strip
            VStack(alignment: .leading, spacing: AppSizing.spacing(2.5, for: sizeClass)) {
                HStack {
                    Text("This week")
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                        .foregroundColor(.white)
                    Spacer()
                    Image(systemName: "flame.fill")
                        .foregroundColor(AppColors.badgeGold)
                        .rotationEffect(.degrees(flameFlicker ? -6 : 6))
                        .scaleEffect(flameFlicker ? 1.1 : 0.95)
                }
                HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                    ForEach(0..<letters.count, id: \.self) { i in
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(filled.contains(i) ? Color.white : Color.white.opacity(0.25))
                                    .frame(width: 20 * sizeClass.scale, height: 20 * sizeClass.scale)
                                if filled.contains(i) {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.75, weight: .bold))
                                        .foregroundColor(AppColors.primaryButton)
                                }
                            }
                            .scaleEffect(dayReveal > i ? 1.0 : 0.4)
                            .opacity(dayReveal > i ? 1.0 : 0.0)
                            Text(letters[i])
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.8))
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(AppSizing.spacing(3.5, for: sizeClass))
            .background(Color.white.opacity(0.16))
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))

            // MARK: Mini stat cards — echoes Profile's Badges / Words I know tiles
            HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                miniStatCard(
                    title: "Badges",
                    icon: "trophy.fill",
                    tint: AppColors.badgeGold,
                    value: "4",
                    background: Color(red: 0.98, green: 0.93, blue: 0.78)
                )
                miniStatCard(
                    title: "Words I know",
                    icon: "star.fill",
                    tint: AppColors.accentRed,
                    value: "11",
                    background: Color(red: 0.99, green: 0.90, blue: 0.92)
                )
            }
            .opacity(statsIn ? 1 : 0)
            .offset(y: statsIn ? 0 : 12)
        }
        .onAppear { startAnimations() }
        .onChange(of: isActive) { active in
            if active { startAnimations() }
        }
    }

    private func startAnimations() {
        withAnimation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true)) {
            playPulse = true
        }
        withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true).delay(0.4)) {
            heartBeat = true
        }
        withAnimation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true)) {
            flameFlicker = true
        }

        dayReveal = 0
        for i in 0..<letters.count {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.6).delay(0.15 + Double(i) * 0.06)) {
                dayReveal = i + 1
            }
        }

        statsIn = false
        withAnimation(.easeOut(duration: 0.4).delay(0.5)) {
            statsIn = true
        }
    }

    // MARK: - Mini stat card (must stay inside this struct — it references sizeClass)
    private func miniStatCard(title: String, icon: String, tint: Color, value: String, background: Color) -> some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(1.5, for: sizeClass)) {
            Text(title)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
                .foregroundColor(AppColors.primaryText.opacity(0.7))
            HStack(spacing: AppSizing.spacing(1.5, for: sizeClass)) {
                ZStack {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 22 * sizeClass.scale, height: 22 * sizeClass.scale)
                    Image(systemName: icon)
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.85))
                        .foregroundColor(tint)
                }
                Text(value)
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
            }
        }
        .padding(AppSizing.spacing(2.5, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass)))
    }
}
#if DEBUG
#Preview {
    OnboardingScreen(onFinish: {})
        .previewSizeClass()
}
#endif
