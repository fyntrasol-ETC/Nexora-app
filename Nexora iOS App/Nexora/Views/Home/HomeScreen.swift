//
//  HomeScreen.swift
//  Elli
//
//  Home tab: same AppBackground() + DecorativeBlobs() treatment as the
//  Courses tab (CoursesScreen.swift) so Home no longer looks like a
//  different app once you swipe over from Courses/Words/Videos/Profile —
//  those four all sit on the full brand gradient, Home was the only tab
//  still using a flat light-gray page background.
//
//  ── DATA CONTRACT (for Android parity) ──────────────────────────────
//  Course content (titles, chapters, levels) comes straight from the
//  static CourseCatalog.courses (CourseContent.swift) — same bundled
//  catalog both platforms ship, not a Firestore read. Progress comes from
//  CoursesViewModel.progressByCourseId — the SAME view model class the
//  Courses tab uses (CoursesScreen.swift) — so "enrolled" here means
//  exactly the same thing it means on the Courses tab: the course has a
//  document at users/{uid}/courseProgress/{courseId}.
//
//  1. Header
//     - DiceBearAvatar + name, same avatar/name pairing ProfileScreen uses
//       (AvatarOptions.resolveSeed(authViewModel.userProfile?.avatarSeed)).
//
//  2. "Your Progress" card
//     - sum(completedChapterIds across ALL courses) / sum(chapters
//       across ALL courses) in the catalog, shown as a real filled
//       progress bar + percentage — match this formula on Android.
//
//  3. Continue Learning (enrolled courses)
//     - Source: courses where progressByCourseId[course.id] != nil.
//     - Tapping a card calls onGoToCourse(course); "See all" calls
//       onGoToCourses() — both bubble up to MainTabView, same pattern
//       ProfileScreen already uses for "onGoToCourse".
//
//  4. Weekly Progress
//     - Per-day bars for the CURRENT calendar week (Mon...Sun), with
//       "today" computed from Calendar.current — not hardcoded.
//     - Which days are "completed" is still a placeholder (see TODO
//       below) until a WeeklyActivityViewModel backed by
//       users/{uid}/weeklyActivity/{dateId} exists — same TODO already
//       flagged in ProfileView.swift for its own weekly activity card.
//
//  5. Practice More (Words + Videos shortcuts)
//     - Reuses Components.swift's StatCard, same component ProfileScreen
//       uses for "Badges" / "Words I know".
//     - Words count: WordsViewModel.knownWords.count (own listener,
//       configured the same way WordsScreen.swift configures its own).
//     - Videos count: favorited videos in VideoFeedViewModel.filteredVideos
//       (own listener, configured the same way VideoFeedScreen.swift
//       does). NOTE: this assumes VideoFeedViewModel's default
//       activeFilter is `.all` on a fresh instance — if that default
//       differs, swap this for whatever "all videos" accessor exists.
//     - Tapping either calls onGoToWords() / onGoToVideos() — bubbles up
//       to MainTabView to switch tabs, same pattern as onGoToCourses().
//
//  6. Discover (mixed courses)
//     - One random course per populated CourseLevel → 2-3 cards. Picked
//       once per screen session (@State), not re-randomized on re-render.
//     - "See all" calls onGoToCourses().
//

import SwiftUI

// MARK: - Weekly progress model

struct WeekDayProgress: Identifiable {
    let id: String       // e.g. "2026-08-24"
    let label: String    // single-letter day label, e.g. "M"
    let state: DayProgressState
}

enum DayProgressState {
    case completed
    case today
    case upcoming
}

// MARK: - Home Screen

struct HomeScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @EnvironmentObject var authViewModel: AuthViewModel

    // Same view model class the Courses tab uses, so "enrolled" and
    // "completed" mean identically the same thing on both screens.
    @StateObject private var coursesViewModel = CoursesViewModel()
    // Own instances of the Words / Videos view models, configured the
    // same way their own tabs configure them — see .task below. This
    // means Home runs its own Firestore listeners alongside Words/Videos'
    // listeners when those tabs are also open, same trade-off the app
    // already makes for CoursesViewModel between Home and Courses.
    @StateObject private var wordsViewModel = WordsViewModel()
    @StateObject private var videoFeedViewModel = VideoFeedViewModel()

    /// Called when a course card (Continue Learning or Discover) is
    /// tapped — bubbles up to MainTabView so it can switch to the
    /// Courses tab and push that course's chapter list, same pattern as
    /// ProfileScreen.onGoToCourse.
    var onGoToCourse: (Course) -> Void = { _ in }
    /// Switches to the Courses tab (either "See all" button).
    var onGoToCourses: () -> Void = {}
    /// Switches to the Words tab.
    var onGoToWords: () -> Void = {}
    /// Switches to the Videos tab.
    var onGoToVideos: () -> Void = {}

    // Frozen once per appearance so the Discover row doesn't re-shuffle
    // on every state change elsewhere on the screen.
    @State private var discoverCourses: [Course] = []

    // TODO: replace with a real WeeklyActivityViewModel backed by
    // users/{uid}/weeklyActivity/{dateId} (see firestore.rules). Until
    // then the day *labels* and *today* marker are real (Calendar-driven);
    // only the completed/missed state per day is still a placeholder.
    @State private var weekProgress: [WeekDayProgress] = []

    private var userName: String {
        authViewModel.userProfile?.displayName ?? "there"
    }

    private var avatarSeed: String {
        AvatarOptions.resolveSeed(authViewModel.userProfile?.avatarSeed)
    }

    /// Real time-of-day greeting (not a fixed "Hello") — recomputed each
    /// time the header renders, so it's accurate whenever the person
    /// actually opens the app rather than a static string.
    private var timeOfDayGreeting: String {
        switch Calendar.current.component(.hour, from: Date()) {
        case 5..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        case 17..<22: return "Good evening"
        default: return "Still up?"
        }
    }

    private var timeOfDayIcon: String {
        switch Calendar.current.component(.hour, from: Date()) {
        case 5..<12: return "sunrise.fill"
        case 12..<17: return "sun.max.fill"
        case 17..<22: return "sunset.fill"
        default: return "moon.stars.fill"
        }
    }

    /// Every course in the catalog, across all three levels. Reads
    /// straight from CourseCatalog.courses (CourseContent.swift) rather
    /// than through CoursesViewModel, since the catalog itself is static
    /// bundled content, not something that needs a Firestore listener.
    private var allCourses: [Course] {
        CourseCatalog.courses
    }

    /// Courses the user has actually started — i.e. has a
    /// courseProgress document for. This is the "resume" list.
    private var enrolledCourses: [Course] {
        allCourses.filter { coursesViewModel.progressByCourseId[$0.id] != nil }
    }

    /// completed chapters / total chapters across the WHOLE catalog —
    /// the number shown in the "Your Progress" card.
    private var overallProgressPercent: Int {
        let totalChapters = allCourses.reduce(0) { $0 + $1.chapters.count }
        guard totalChapters > 0 else { return 0 }
        let completed = allCourses.reduce(0) { partial, course in
            partial + Set(coursesViewModel.progressByCourseId[course.id]?.completedChapterIds ?? []).count
        }
        return Int((Double(completed) / Double(totalChapters) * 100).rounded())
    }

    private var daysCompletedThisWeek: Int {
        weekProgress.filter { $0.state == .completed }.count
    }

    private var favoriteVideoCount: Int {
        videoFeedViewModel.filteredVideos.filter(videoFeedViewModel.isFavorited).count
    }

    var body: some View {
        ZStack {
            AppBackground()
            DecorativeBlobs(sizeClass: sizeClass)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: AppSizing.sectionSpacing(sizeClass)) {
                    header
                    progressCard
                    continueLearningSection
                    weeklyProgressSection
                    practiceMoreSection
                    discoverSection

                    Spacer(minLength: AppSizing.tabBarClearance(sizeClass))
                }
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                .padding(.top, AppSizing.spacing(4, for: sizeClass))
            }
        }
        .task {
            if weekProgress.isEmpty { weekProgress = Self.currentWeekPlaceholder() }
            guard let uid = authViewModel.userProfile?.id else { return }
            videoFeedViewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
            await wordsViewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
        }
        .onChange(of: coursesViewModel.progressByCourseId) { _ in
            if discoverCourses.isEmpty { discoverCourses = pickDiscoverCourses() }
        }
        .onAppear {
            if discoverCourses.isEmpty { discoverCourses = pickDiscoverCourses() }
        }
    }

    // MARK: Header (avatar + name)

    private var header: some View {
        HStack(alignment: .center, spacing: AppSizing.spacing(3.5, for: sizeClass)) {
            VStack(alignment: .leading, spacing: 4) {
                // Eyebrow: a real time-of-day read, not a static "Hello" —
                // small, quiet, and sets up the name below as the payoff.
                HStack(spacing: 5) {
                    Image(systemName: timeOfDayIcon)
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.9, weight: .semibold))
                    Text(timeOfDayGreeting.uppercased())
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                        .tracking(1.4)
                }
                .foregroundColor(AppColors.link)

                // The name is the hero here — bigger and heavier than the
                // old "Hello, X!" line, so it reads as a headline rather
                // than a label.
                Text(userName)
                    .font(.system(size: AppSizing.font(.title, for: sizeClass) * 1.2, weight: .heavy))
                    .foregroundColor(AppColors.primaryText)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                Text("Let's keep improving your English")
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
            }

            Spacer(minLength: AppSizing.spacing(2, for: sizeClass))

            // Avatar sits in a soft purple halo (was a white halo, which
            // only worked against the gradient — now that the header
            // itself is a white card, the halo needs its own tint to
            // stay visible) with a slight tilt.
            ZStack {
                Circle()
                    .fill(AppColors.link.opacity(0.12))
                    .frame(
                        width: AppSizing.avatarSize(sizeClass) * 0.64 + 16,
                        height: AppSizing.avatarSize(sizeClass) * 0.64 + 16
                    )
                DiceBearAvatar(seed: avatarSeed, size: AppSizing.avatarSize(sizeClass) * 0.64)
            }
            .rotationEffect(.degrees(-6))
            .shadow(color: .black.opacity(0.12), radius: 6, y: 3)
        }
        .padding(AppSizing.spacing(4, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .fill(AppColors.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(AppColors.link.opacity(0.10), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.06), radius: 10, y: 5)
    }

    // MARK: Your Progress card

    /// Progress itself is real for anonymous sessions too (it's written
    /// under whatever uid is currently signed in, guest or not) — but we
    /// don't surface it back to a guest, since "your progress" implies
    /// an account that will still be there tomorrow. Swaps in a sign-in
    /// prompt instead until the session is a real account.
    @ViewBuilder
    private var progressCard: some View {
        if authViewModel.isAnonymous {
            SignInPromptCard(
                title: "Sign in to view your progress",
                message: "Create an account to save and track your progress across every course.",
                icon: "chart.bar.fill"
            )
        } else {
            progressCardContent
        }
    }

    private var progressCardContent: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2.5, for: sizeClass)) {
            HStack {
                Text("Your Progress")
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                    .foregroundColor(AppColors.primaryText)
                Spacer()
                Text("\(overallProgressPercent)%")
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(AppColors.primaryButton))
            }

            MiniProgressBar(fraction: CGFloat(overallProgressPercent) / 100, tint: AppColors.primaryButton)
                .frame(height: 7)

            Text(enrolledCourses.isEmpty
                 ? "You haven't started a course yet — pick one below to begin."
                 : "Across \(enrolledCourses.count) enrolled \(enrolledCourses.count == 1 ? "course" : "courses").")
                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                .foregroundColor(AppColors.placeholderText)
        }
        .padding(AppSizing.spacing(4, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .fill(AppColors.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(AppColors.primaryButton.opacity(0.10), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.06), radius: 10, y: 5)
    }

    // MARK: Continue Learning (enrolled courses)

    private var continueLearningSection: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
            sectionHeader(title: "Continue Learning", onSeeAll: enrolledCourses.isEmpty ? nil : onGoToCourses)

            if enrolledCourses.isEmpty {
                EmptyEnrolledCard(sizeClass: sizeClass, onBrowse: onGoToCourses)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {
                        ForEach(enrolledCourses) { course in
                            Button {
                                onGoToCourse(course)
                            } label: {
                                ResumeCourseCard(
                                    course: course,
                                    progress: coursesViewModel.progressByCourseId[course.id],
                                    sizeClass: sizeClass
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    // MARK: Weekly Progress

    private var weeklyProgressSection: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(4.5, for: sizeClass)) {
            HStack {
                Text("Weekly Progress")
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
                Spacer()
                Text("\(daysCompletedThisWeek)/7 days")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                    .foregroundColor(AppColors.placeholderText)
            }

            MiniProgressBar(fraction: CGFloat(daysCompletedThisWeek) / 7, tint: AppColors.accentRed)
                .frame(height: 7)

            HStack(alignment: .bottom, spacing: AppSizing.spacing(3.5, for: sizeClass)) {
                ForEach(weekProgress) { day in
                    WeekDayBarView(day: day, sizeClass: sizeClass)
                }
            }
            .frame(height: 140 * sizeClass.scale)
        }
        .padding(AppSizing.spacing(5, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .fill(AppColors.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(AppColors.accentRed.opacity(0.08), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.06), radius: 10, y: 5)
    }

    // MARK: Practice More (Words + Videos)

    private var practiceMoreSection: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
            sectionHeader(title: "Practice More", onSeeAll: nil)

            HStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {
                Button(action: onGoToWords) {
                    StatCard(
                        title: "Words I know",
                        icon: "star.fill",
                        iconTint: AppColors.accentRed,
                        value: "\(wordsViewModel.knownWords.count)",
                        showChevron: true,
                        backgroundColor: Color(red: 0.99, green: 0.90, blue: 0.91)
                    )
                }
                .buttonStyle(.plain)

                Button(action: onGoToVideos) {
                    StatCard(
                        title: "Favorite videos",
                        icon: "heart.fill",
                        iconTint: AppColors.link,
                        value: "\(favoriteVideoCount)",
                        showChevron: true,
                        backgroundColor: Color(red: 0.90, green: 0.93, blue: 0.99)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: Discover (random mix across levels)

    private var discoverSection: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
            sectionHeader(title: "Discover", onSeeAll: onGoToCourses)

            if let featured = discoverCourses.first {
                Button { onGoToCourse(featured) } label: {
                    FeaturedDiscoverCard(
                        course: featured,
                        progress: coursesViewModel.progressByCourseId[featured.id],
                        sizeClass: sizeClass
                    )
                }
                .buttonStyle(.plain)
            }

            if discoverCourses.count > 1 {
                HStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {
                    ForEach(discoverCourses.dropFirst()) { course in
                        Button { onGoToCourse(course) } label: {
                            CompactDiscoverCard(
                                course: course,
                                progress: coursesViewModel.progressByCourseId[course.id],
                                sizeClass: sizeClass
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    /// White-on-gradient section header — these sections sit directly on
    /// AppBackground/DecorativeBlobs (not inside a white card), same as
    /// CoursesScreen's level headers, so the title needs to be white here.
    private func sectionHeader(title: String, onSeeAll: (() -> Void)?) -> some View {
        HStack {
            Text(title)
                .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                .foregroundColor(.white)
            Spacer()
            if let onSeeAll {
                Button(action: onSeeAll) {
                    HStack(spacing: 2) {
                        Text("See all")
                        Image(systemName: "chevron.right")
                    }
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                    .foregroundColor(.white.opacity(0.9))
                }
            }
        }
    }

    // MARK: Helpers

    /// One random course per populated level → 2-3 cards depending on
    /// how many levels currently have courses.
    private func pickDiscoverCourses() -> [Course] {
        CourseLevel.allCases.compactMap { level in
            CourseCatalog.courses.filter { $0.level == level }.randomElement()
        }
    }

    /// Builds Mon...Sun labels for the CURRENT calendar week and marks
    /// which one is "today" — real, not hardcoded. completed/missed
    /// state is a placeholder until real weeklyActivity data exists (see
    /// TODO at the top of this file).
    private static func currentWeekPlaceholder() -> [WeekDayProgress] {
        var calendar = Calendar.current
        calendar.firstWeekday = 2 // Monday

        let today = Date()
        guard let weekInterval = calendar.dateInterval(of: .weekOfYear, for: today) else { return [] }

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let letterFormatter = DateFormatter()
        letterFormatter.dateFormat = "EEEEE" // single-letter day symbol, locale-aware

        return (0..<7).compactMap { offset -> WeekDayProgress? in
            guard let date = calendar.date(byAdding: .day, value: offset, to: weekInterval.start) else { return nil }
            let isToday = calendar.isDate(date, inSameDayAs: today)
            let isPast = date < calendar.startOfDay(for: today)
            return WeekDayProgress(
                id: formatter.string(from: date),
                label: letterFormatter.string(from: date).uppercased(),
                // Placeholder: treats every past day as "completed" until
                // real weeklyActivity data replaces this.
                state: isToday ? .today : (isPast ? .completed : .upcoming)
            )
        }
    }
}

// MARK: - Shared mini progress bar

/// Small capsule progress bar reused for both the progress card's overall
/// percentage and the weekly-days capsule.
private struct MiniProgressBar: View {
    let fraction: CGFloat
    let tint: Color

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(tint.opacity(0.15))
                Capsule()
                    .fill(tint)
                    .frame(width: geo.size.width * min(max(fraction, 0), 1))
            }
        }
    }
}

// MARK: - Shared level chip

/// Small rounded badge for a course's level, reused across the Continue
/// Learning and Discover cards so every level label reads consistently
/// as a "chip" rather than plain colored text.
private struct LevelChip: View {
    let text: String
    let tint: Color
    let sizeClass: AppSizeClass

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: AppSizing.font(.caption, for: sizeClass) * 0.82, weight: .bold))
            .tracking(0.4)
            .foregroundColor(tint)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Capsule().fill(tint.opacity(0.12)))
    }
}

// MARK: - Continue Learning card

private struct ResumeCourseCard: View {
    let course: Course
    let progress: CourseProgress?
    let sizeClass: AppSizeClass

    private var completedCount: Int { Set(progress?.completedChapterIds ?? []).count }
    private var totalChapters: Int { course.chapters.count }
    private var remaining: Int { max(totalChapters - completedCount, 0) }
    private var fraction: CGFloat {
        guard totalChapters > 0 else { return 0 }
        return min(max(CGFloat(completedCount) / CGFloat(totalChapters), 0), 1)
    }
    private var lessonsLeftText: String {
        remaining == 0 ? "All chapters complete!" : "\(remaining) \(remaining == 1 ? "chapter" : "chapters") left"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                LinearGradient(
                    colors: [course.accent.tint, course.accent.tint.opacity(0.68)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Image(systemName: "text.book.closed.fill")
                    .font(.system(size: 30, weight: .regular))
                    .foregroundColor(.white.opacity(0.9))
            }
            .frame(height: 90)

            VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
                HStack(alignment: .top, spacing: 6) {
                    Text(course.title)
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                        .foregroundColor(AppColors.primaryText)
                        .lineLimit(2)
                    Spacer(minLength: 4)
                    Text("\(Int(fraction * 100))%")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                        .foregroundColor(course.accent.tint)
                }

                Text(lessonsLeftText)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)

                MiniProgressBar(fraction: fraction, tint: course.accent.tint)
                    .frame(height: 5)
            }
            .padding(AppSizing.spacing(3, for: sizeClass))
        }
        .frame(width: 200 * sizeClass.scale)
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(course.accent.tint.opacity(0.14), lineWidth: 1)
        )
        .shadow(color: course.accent.tint.opacity(0.20), radius: 10, y: 6)
    }
}

private struct EmptyEnrolledCard: View {
    let sizeClass: AppSizeClass
    let onBrowse: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2.5, for: sizeClass)) {
            ZStack {
                Circle()
                    .fill(AppColors.primaryButton.opacity(0.12))
                    .frame(width: 44, height: 44)
                Image(systemName: "sparkles")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.primaryButton)
            }

            Text("Nothing in progress yet")
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryText)
            Text("Start a course below and it'll show up here so you can pick up right where you left off.")
                .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                .foregroundColor(AppColors.placeholderText)
            Button(action: onBrowse) {
                HStack(spacing: 6) {
                    Text("Browse courses")
                    Image(systemName: "arrow.right")
                }
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
                .padding(.vertical, AppSizing.spacing(2.5, for: sizeClass))
                .background(AppColors.primaryButton)
                .clipShape(Capsule())
            }
        }
        .padding(AppSizing.spacing(4, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(AppColors.primaryButton.opacity(0.10), lineWidth: 1)
        )
    }
}

// MARK: - Discover cards (varied sizes, same theme)

/// The large "hero" pick in Discover — one per screen, full width,
/// horizontal layout with a bigger icon tile.
private struct FeaturedDiscoverCard: View {
    let course: Course
    let progress: CourseProgress?
    let sizeClass: AppSizeClass

    var body: some View {
        HStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
            ZStack {
                RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass), style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [course.accent.tint, course.accent.tint.opacity(0.68)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                Image(systemName: "sparkles")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(width: 76, height: 76)
            .shadow(color: course.accent.tint.opacity(0.35), radius: 10, y: 5)

            VStack(alignment: .leading, spacing: 4) {
                LevelChip(text: course.level.rawValue, tint: course.accent.tint, sizeClass: sizeClass)
                Text(course.title)
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
                    .lineLimit(1)
                Text(course.description)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
                    .lineLimit(2)
            }
            Spacer()

            ZStack {
                Circle().fill(course.accent.tint.opacity(0.10)).frame(width: 30, height: 30)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(course.accent.tint)
            }
        }
        .padding(AppSizing.spacing(3.5, for: sizeClass))
        .frame(maxWidth: .infinity)
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(course.accent.tint.opacity(0.12), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.08), radius: 10, y: 5)
    }
}

/// The smaller square picks that sit below the featured card.
private struct CompactDiscoverCard: View {
    let course: Course
    let progress: CourseProgress?
    let sizeClass: AppSizeClass

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
            ZStack {
                RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass), style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [course.accent.tint, course.accent.tint.opacity(0.68)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                Image(systemName: "text.book.closed.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(height: 64)
            .shadow(color: course.accent.tint.opacity(0.25), radius: 6, y: 3)

            LevelChip(text: course.level.rawValue, tint: course.accent.tint, sizeClass: sizeClass)

            Text(course.title)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryText)
                .lineLimit(2)
        }
        .padding(AppSizing.spacing(2.5, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass)))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .stroke(course.accent.tint.opacity(0.10), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.06), radius: 6, y: 3)
    }
}

// MARK: - Weekly day bar

private struct WeekDayBarView: View {
    let day: WeekDayProgress
    let sizeClass: AppSizeClass

    private var barColor: Color {
        switch day.state {
        case .completed: return AppColors.success
        case .today: return AppColors.primaryButton
        case .upcoming: return AppColors.fieldBackground
        }
    }

    private var barHeight: CGFloat {
        switch day.state {
        case .completed: return 70 * sizeClass.scale
        case .today: return 84 * sizeClass.scale
        case .upcoming: return 14 * sizeClass.scale
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            Spacer(minLength: 0)

            // Small marker above the bar: a checkmark for a day that's
            // actually done, a dot for today, nothing for upcoming days —
            // gives the eye a faster read than bar height alone.
            Group {
                if day.state == .completed {
                    Image(systemName: "checkmark")
                        .font(.system(size: 9, weight: .black))
                        .foregroundColor(.white)
                        .frame(width: 18, height: 18)
                        .background(Circle().fill(AppColors.success))
                } else if day.state == .today {
                    Circle()
                        .fill(AppColors.primaryButton)
                        .frame(width: 8, height: 8)
                        .padding(5)
                } else {
                    Color.clear.frame(width: 18, height: 18)
                }
            }

            Capsule()
                .fill(barColor)
                .frame(width: 14 * sizeClass.scale, height: barHeight)

            Text(day.label)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                .foregroundColor(day.state == .today ? AppColors.primaryButton : AppColors.placeholderText)
                .fontWeight(day.state == .today ? .bold : .regular)
        }
    }
}

#if DEBUG
#Preview {
    HomeScreen()
        .environmentObject(AuthViewModel())
        .previewSizeClass()
}
#endif
