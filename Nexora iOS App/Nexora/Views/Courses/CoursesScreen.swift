//
//  CoursesScreen.swift
//  Nexora
//
//  Courses tab. Each level (Beginner / Intermediate / Advanced) shows its
//  courses as a horizontally scrolling row of square cards, so the row
//  can hold five, seven, however many courses without the page growing
//  tall — the next card peeking in at the edge is the scroll affordance.
//  Tapping a course pushes into its chapter list. A thin divider line
//  separates each level section from the next so the three groups read
//  as distinct rather than blurring together.
//
//  CourseRoute (the nav stack's destinations) and CourseSquareCard (the
//  card shown per course) live in this same file since both only exist
//  to serve this screen.
//
//  Progress (completed chapters, quiz scores) is owned by CoursesViewModel
//  and threaded down to every screen in the flow that needs it — the
//  course cards' progress bars, the chapter list's checkmarks, and the
//  completion screen's write-back when a chapter finishes. Mirrors
//  Android's CoursesViewModel.progressByCourseId 1:1.
//

import SwiftUI

// MARK: - Navigation route

/// Push destinations for the Courses tab's NavigationStack. Order mirrors
/// the actual flow:
/// chapters -> chapterIntro -> exerciseIntro -> dialogue -> quiz -> completion
enum CourseRoute: Hashable {
    case chapters(Course)
    case chapterIntro(Course, Chapter)
    case exerciseIntro(Course, Chapter)
    case exercise(Course, Chapter)   // was `.dialogue` — now routes by exercise kind
    case quiz(Course, Chapter)
    /// score is nil for picture-vocab chapters (no quiz to score), set
    /// for dialogue+quiz chapters.
    case completion(Course, Chapter, QuizScore?)

    /// Convenience for call sites that finish a chapter with no quiz
    /// score (picture-vocab chapters) — keeps `.completion(course,
    /// chapter)` working unchanged wherever it's already called, e.g.
    /// PictureExerciseScreen, without needing to touch that file.
    static func completion(_ course: Course, _ chapter: Chapter) -> CourseRoute {
        .completion(course, chapter, nil)
    }
}

// MARK: - Level presentation

private extension CourseLevel {
    /// Small glyph shown in the section header's icon bubble — cycles
    /// through a "growing skill" motif from level to level.
    var icon: String {
        switch self {
        case .beginner: return "leaf.fill"
        case .intermediate: return "flame.fill"
        case .advanced: return "trophy.fill"
        }
    }
}

// MARK: - Screen

struct CoursesScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = CoursesViewModel()

    // Owns its own path by default so pushes actually navigate even when
    // no one's wired up an external binding yet. Pass an external binding
    // (e.g. from MainTabView) so it can see the stack depth and hide the
    // floating tab bar while someone's deep in a course.
    @State private var internalPath: [CourseRoute] = []
    private let externalPath: Binding<[CourseRoute]>?

    init(path: Binding<[CourseRoute]>? = nil) {
        self.externalPath = path
    }

    private var path: Binding<[CourseRoute]> {
        externalPath ?? $internalPath
    }

    /// Levels that actually have courses to show — computed once so both
    /// the ForEach and the "is this the last section" divider check use
    /// the same list.
    private var populatedLevels: [CourseLevel] {
        CourseLevel.allCases.filter { !viewModel.courses(for: $0).isEmpty }
    }

    var body: some View {
        NavigationStack(path: path) {
            ZStack {
                AppBackground()
                DecorativeBlobs(sizeClass: sizeClass)

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: AppSizing.sectionSpacing(sizeClass)) {
                        header

                        ForEach(Array(populatedLevels.enumerated()), id: \.element) { index, level in
                            levelSection(level: level, courses: viewModel.courses(for: level))

                            // Divider between this section and the next —
                            // skipped after the last level so nothing
                            // dangles below "Advanced".
                            if index < populatedLevels.count - 1 {
                                levelDivider
                            }
                        }

                        Spacer(minLength: AppSizing.tabBarClearance(sizeClass))
                    }
                    .padding(.top, AppSizing.spacing(4, for: sizeClass))
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(for: CourseRoute.self) { route in
                destination(for: route)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Courses")
                .font(.system(size: AppSizing.font(.logo, for: sizeClass), weight: .heavy))
                .foregroundColor(.white)
            Text("Pick a level and start learning")
                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                .foregroundColor(.white.opacity(0.8))
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
    }

    /// Thin translucent white rule that separates one level's row of
    /// courses from the next level's — a soft line rather than a hard
    /// black divider, so it sits quietly on the gradient background.
    private var levelDivider: some View {
        Rectangle()
            .fill(.white.opacity(0.14))
            .frame(height: 1)
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
    }

    private func levelSection(level: CourseLevel, courses: [Course]) -> some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
            HStack(spacing: 10) {
                ZStack {
                    Circle()
                        .fill(.white.opacity(0.16))
                        .frame(width: 34, height: 34)
                    Image(systemName: level.icon)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                }

                Text(level.rawValue)
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)

                Spacer()

                Text("\(courses.count) \(courses.count == 1 ? "course" : "courses")")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                    .foregroundColor(.white.opacity(0.75))
                    .padding(.vertical, 5)
                    .padding(.horizontal, 10)
                    .background(.white.opacity(0.12))
                    .clipShape(Capsule())
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: AppSizing.spacing(4, for: sizeClass)) {
                    ForEach(courses) { course in
                        Button {
                            path.wrappedValue.append(.chapters(course))
                        } label: {
                            CourseSquareCard(
                                course: course,
                                progress: viewModel.progressByCourseId[course.id],
                                sizeClass: sizeClass
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                // Same horizontal inset as everything else, so the row
                // lines up with the header above it — the *last* card
                // still gets cut off by the screen edge as long as the
                // row is wider than the screen, which is what gives the
                // "there's more, keep scrolling" peek.
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                .padding(.vertical, 4)
            }
        }
    }

    @ViewBuilder
    private func destination(for route: CourseRoute) -> some View {
        switch route {
        case .chapters(let course):
            ChapterListScreen(
                course: course,
                progress: viewModel.progressByCourseId[course.id],
                path: path
            )
        case .chapterIntro(let course, let chapter):
            ChapterIntroScreen(course: course, chapter: chapter, path: path)
        case .exerciseIntro(let course, let chapter):
            ExerciseIntroScreen(course: course, chapter: chapter, path: path)
        case .exercise(let course, let chapter):
            switch chapter.exercise {
            case .dialogue:
                DialogueScreen(course: course, chapter: chapter, path: path)
            case .picture:
                PictureExerciseScreen(course: course, chapter: chapter, path: path)
            }
        case .quiz(let course, let chapter):
            // DialogueScreen already gates its "Start Quiz" button on
            // authViewModel.isAnonymous, so this branch is a safety net
            // rather than the primary gate — but if `.quiz` is ever
            // pushed from somewhere else later, it stays covered too.
            if authViewModel.isAnonymous {
                SignInRequiredScreen(message: "Create an account to take this quiz and save your score.")
            } else {
                ExerciseQuizScreen(course: course, chapter: chapter, path: path)
            }
        case .completion(let course, let chapter, let score):
            ExerciseCompletionScreen(
                course: course,
                chapter: chapter,
                score: score,
                path: path,
                coursesViewModel: viewModel
            )
        }
    }
}

// MARK: - Course square card (shown in the horizontal row per level)

/// Whole card now sits on the same solid white panel as the chapter rows
/// on the chapter list screen (`AppColors.cardBackground`), so the title
/// and chapter count are dark text popping off white instead of white
/// text over a translucent glass tint. The accent-colored icon square is
/// still the one colorful element, same as `ChapterBadgeIcon` is on the
/// white chapter rows.
struct CourseSquareCard: View {
    let course: Course
    /// nil means the course hasn't been started yet — no progress bar shown.
    let progress: CourseProgress?
    let sizeClass: AppSizeClass

    private var cardSize: CGFloat { AppSizing.courseCardSize(sizeClass) }
    private var panelCornerRadius: CGFloat { AppSizing.cardCornerRadius(sizeClass) + 6 }

    private var completedCount: Int {
        Set(progress?.completedChapterIds ?? []).count
    }
    private var totalChapters: Int { course.chapters.count }
    private var hasStarted: Bool { completedCount > 0 }
    private var fraction: CGFloat {
        guard totalChapters > 0 else { return 0 }
        return min(max(CGFloat(completedCount) / CGFloat(totalChapters), 0), 1)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2.5, for: sizeClass)) {
            // Icon square — same accent gradient as before, this is the
            // one piece that stays colorful against the white panel.
            ZStack {
                RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass), style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [course.accent.tint, course.accent.tint.opacity(0.68)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass), style: .continuous)
                            .stroke(.white.opacity(0.45), lineWidth: 1.5)
                    )
                Image(systemName: "text.book.closed.fill")
                    .font(.system(size: cardSize * 0.32, weight: .regular))
                    .foregroundColor(.white)
            }
            .frame(width: cardSize, height: cardSize)
            .shadow(color: course.accent.tint.opacity(0.4), radius: 10, y: 6)

            // Title — dark text on a light chip, same pairing as
            // AppColors.primaryText over cardBackground on the chapter rows.
            Text(course.title)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.primaryText)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(height: AppSizing.font(.callout, for: sizeClass) * 2.5, alignment: .top)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
                .background(course.accent.tint.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(course.accent.tint.opacity(0.2), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            // Progress bar — only once the course has at least one
            // completed chapter, same "hasStarted" gate Android uses.
            if hasStarted {
                CourseProgressBar(
                    fraction: fraction,
                    completedCount: completedCount,
                    totalChapters: totalChapters,
                    accentTint: course.accent.tint,
                    sizeClass: sizeClass
                )
            }

            // Chapter count — same muted placeholder-text tone the
            // chapter rows use for their "Chapter N" label.
            HStack(spacing: 4) {
                Image(systemName: "text.book.closed.fill")
                    .font(.system(size: 9, weight: .semibold))
                Text("\(course.chapters.count) \(course.chapters.count == 1 ? "Chapter" : "Chapters")")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
            }
            .foregroundColor(AppColors.placeholderText)
            .padding(.vertical, 5)
            .padding(.horizontal, 9)
            .background(AppColors.placeholderText.opacity(0.08))
            .overlay(
                Capsule().stroke(AppColors.placeholderText.opacity(0.2), lineWidth: 1)
            )
            .clipShape(Capsule())
        }
        .padding(AppSizing.spacing(2.5, for: sizeClass))
        .frame(width: cardSize + AppSizing.spacing(5, for: sizeClass), alignment: .leading)
        // Solid white panel — the same background the chapter list rows
        // use — so the title and chip read as dark text popping off white.
        .background(AppColors.cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: panelCornerRadius, style: .continuous)
                .stroke(.black.opacity(0.06), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: panelCornerRadius, style: .continuous))
        .shadow(color: .black.opacity(0.15), radius: 12, y: 6)
    }
}

/// "Progress   3/3" label over a filled capsule track, tinted to the
/// course's accent — matches the Android screenshot 1:1.
private struct CourseProgressBar: View {
    let fraction: CGFloat
    let completedCount: Int
    let totalChapters: Int
    let accentTint: Color
    let sizeClass: AppSizeClass

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text("Progress")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
                Spacer()
                Text("\(completedCount)/\(totalChapters)")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                    .foregroundColor(accentTint)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(accentTint.opacity(0.15))
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [accentTint, accentTint.opacity(0.75)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: geo.size.width * fraction)
                }
            }
            .frame(height: 5)
        }
    }
}

#Preview {
    CoursesScreen()
}
