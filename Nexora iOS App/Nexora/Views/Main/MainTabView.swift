import SwiftUI

enum MainTab: String, CaseIterable {
    case home = "Home"
    case courses = "Courses"
    case words = "Words"
    case videos = "Videos"
    case profile = "Profile"

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .courses: return "doc.text.fill"
        case .words: return "character.book.closed.fill"
        case .videos: return "film.fill"
        case .profile: return "person.fill"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: MainTab = .home

    // Owned here (not inside ProfileScreen) so MainTabView can see how deep
    // the Profile tab's stack is and decide whether the tab bar should show.
    @State private var profilePath = NavigationPath()

    // Same pattern for Words: StartLearningScreen and FlashcardDeckScreen
    // are pushed onto WordsScreen's own NavigationStack, so MainTabView has
    // no visibility into that unless it owns the path too. Without this,
    // the floating tab bar rendered on top of those pushed screens.
    @State private var wordsPath: [WordsRoute] = []

    // Same pattern again for Courses: ChapterListScreen, ChapterIntro,
    // ExerciseIntro, Dialogue, Quiz, and Completion are all pushed onto
    // CoursesScreen's own NavigationStack. Without owning this path here,
    // MainTabView has no way to know the user is deep inside a course, so
    // the floating tab bar kept rendering on top of those pushed screens —
    // this is what was hiding the CTA buttons.
    @State private var coursesPath: [CourseRoute] = []

    var body: some View {
        ZStack(alignment: .bottom) {
            AppBackground()

            Group {
                switch selectedTab {
                case .home:
                    HomeScreen(
                        onGoToCourse: goToCourse,
                        onGoToCourses: goToCourses,
                        onGoToWords: goToWords,
                        onGoToVideos: goToVideos
                    )
                case .courses: CoursesScreen(path: $coursesPath)
                case .words: WordsScreen(path: $wordsPath)
                case .videos: VideoFeedScreen()
                case .profile: ProfileScreen(path: $profilePath, onGoToCourse: goToCourse, onGoToWords: goToWords)
                }
            }

            let showTabBar = (selectedTab != .profile || profilePath.isEmpty)
                && (selectedTab != .words || wordsPath.isEmpty)
                && (selectedTab != .courses || coursesPath.isEmpty)

            if showTabBar {
                CustomTabBar(selectedTab: $selectedTab)
            }
        }
        .ignoresSafeArea(edges: .bottom)
    }

    /// Called from a Badge's "Go to course" (Profile tab) or a course card
    /// on Home — jumps to the Courses tab and pushes straight into that
    /// course's chapter list. Also resets profilePath back to root so the
    /// Profile tab isn't left sitting on the Badges screen underneath when
    /// the person returns to it later.
    private func goToCourse(_ course: Course) {
        coursesPath = [.chapters(course)]
        profilePath = NavigationPath()
        withAnimation(.easeInOut(duration: 0.15)) {
            selectedTab = .courses
        }
    }

    /// Called from Home's "See all" buttons (Continue Learning / Discover)
    /// — switches to the Courses tab at its root, rather than pushed into
    /// any specific course.
    private func goToCourses() {
        coursesPath = []
        withAnimation(.easeInOut(duration: 0.15)) {
            selectedTab = .courses
        }
    }

    /// Called from Home's "Words I know" card, or Profile's "Words I know"
    /// badge — switches to the Words tab at its root. Also resets
    /// profilePath back to root (harmless no-op when called from Home) so
    /// the Profile tab isn't left mid-stack if the person navigates back
    /// to it later.
    private func goToWords() {
        wordsPath = []
        profilePath = NavigationPath()
        withAnimation(.easeInOut(duration: 0.15)) {
            selectedTab = .words
        }
    }

    /// Called from Home's "Favorite videos" card — switches to the Videos
    /// tab. Videos has no external path binding (it doesn't push any
    /// screens onto its own stack the way Courses/Words/Profile do), so
    /// there's no path to reset here.
    private func goToVideos() {
        withAnimation(.easeInOut(duration: 0.15)) {
            selectedTab = .videos
        }
    }
}

/// Flat, full-width tab bar matching the Android reference UI — a plain
/// white bar (not a floating capsule), icon-over-label per tab, with the
/// selected tab tinted red and the rest gray.
struct CustomTabBar: View {
    @Binding var selectedTab: MainTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(MainTab.allCases, id: \.self) { tab in
                tabButton(for: tab)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 28)
                .fill(
                    LinearGradient(
                        colors: [AppColors.gradientTop, AppColors.primaryButton],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .shadow(color: .black.opacity(0.20), radius: 16, y: 8)
        )
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
    }

    private func tabButton(for tab: MainTab) -> some View {
        let isSelected = tab == selectedTab
        return Button {
            withAnimation(.easeInOut(duration: 0.15)) {
                selectedTab = tab
            }
        } label: {
            VStack(spacing: 4) {
                Image(systemName: tab.icon)
                    .font(.system(size: 19))
                Text(tab.rawValue)
                    .font(.system(size: 10, weight: isSelected ? .semibold : .regular))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(isSelected ? Color.white.opacity(0.15) : Color.clear)
            )
        }
    }
}
