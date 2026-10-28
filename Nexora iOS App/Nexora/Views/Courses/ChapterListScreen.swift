//
//  ChapterListScreen.swift
//  Nexora
//
//  Course detail screen — a centered intro to what the course covers
//  (level + chapter-count chips, description, "what you'll learn" tags),
//  then Chapter 1, 2, 3... listed below it. Each row now also shows a
//  green "Completed" check (plus quiz score, when there is one) once
//  that chapter's been finished — mirrors Android's ChapterRow, backed
//  by the same users/{uid}/courseProgress/{courseId} data via
//  CoursesViewModel.progressByCourseId. Tapping a chapter pushes its
//  intro page.
//

import SwiftUI

struct ChapterListScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    /// nil means the course hasn't been started yet — every chapter row
    /// renders as not-completed.
    let progress: CourseProgress?
    @Binding var path: [CourseRoute]

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                ScrollView {
                    VStack(alignment: .center, spacing: AppSizing.spacing(6, for: sizeClass)) {
                        courseIntro

                        VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                            ForEach(course.chapters) { chapter in
                                Button {
                                    path.append(.chapterIntro(course, chapter))
                                } label: {
                                    ChapterRow(
                                        chapter: chapter,
                                        accent: course.accent,
                                        sizeClass: sizeClass,
                                        isCompleted: progress?.completedChapterIds.contains(chapter.number) == true,
                                        score: progress?.exerciseScores[chapter.number]
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        Spacer(minLength: AppSizing.spacing(6, for: sizeClass))
                    }
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                    .padding(.top, AppSizing.spacing(3, for: sizeClass))
                }
            }
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button {
                path.removeLast()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.75, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(10)
                    .background(Color.white.opacity(0.15))
                    .clipShape(Circle())
            }
            Spacer()
            Text(course.title)
                .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                .foregroundColor(.white)
            Spacer()
            // Balances the back button so the title stays visually centered.
            Color.clear.frame(width: 40, height: 40)
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(3, for: sizeClass))
        .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
    }

    /// Centered intro block on what the course as a whole covers: a big
    /// badge icon, level + chapter-count chips, the fuller description,
    /// and a row of "what you'll learn" tags — sits above the chapter
    /// list so someone knows what they're about to start before diving in.
    private var courseIntro: some View {
        VStack(alignment: .center, spacing: AppSizing.spacing(4, for: sizeClass)) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [course.accent.tint, course.accent.tint.opacity(0.65)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 84, height: 84)
                    .shadow(color: course.accent.tint.opacity(0.4), radius: 14, y: 6)
                Image(systemName: "text.book.closed.fill")
                    .font(.system(size: 32, weight: .semibold))
                    .foregroundColor(.white)
            }

            HStack(spacing: 8) {
                LevelChip(level: course.level, sizeClass: sizeClass)
                HStack(spacing: 4) {
                    Image(systemName: "text.book.closed.fill")
                        .font(.system(size: 11, weight: .semibold))
                    Text("\(course.chapters.count) \(course.chapters.count == 1 ? "Chapter" : "Chapters")")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                }
                .foregroundColor(.white.opacity(0.85))
                .padding(.vertical, 6)
                .padding(.horizontal, 10)
                .background(.white.opacity(0.14))
                .clipShape(Capsule())
            }

            Text(course.description)
                .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                .foregroundColor(.white.opacity(0.92))
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: .infinity)

            if !course.highlights.isEmpty {
                HighlightTagsRow(highlights: course.highlights, sizeClass: sizeClass)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// Small capsule naming the course's level, tinted to match that level's
/// accent color so it visually echoes the square card it was tapped from
/// on the Courses tab.
private struct LevelChip: View {
    let level: CourseLevel
    let sizeClass: AppSizeClass

    var body: some View {
        Text(level.rawValue.uppercased())
            .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
            .tracking(0.5)
            .foregroundColor(AppColors.primaryButton)
            .padding(.vertical, 6)
            .padding(.horizontal, 10)
            .background(Color.white)
            .clipShape(Capsule())
    }
}

/// Centered, wrapping row of "what you'll learn" tags — a quick-glance
/// summary of the course's skills, styled like little achievement pills.
private struct HighlightTagsRow: View {
    let highlights: [String]
    let sizeClass: AppSizeClass

    // SwiftUI has no built-in wrapping HStack pre-iOS 16's Layout
    // protocol, and this app targets earlier availability elsewhere in
    // the codebase — so lay tags out two-per-row, which comfortably
    // fits the 2-3 short highlight strings every course has.
    private var rows: [[String]] {
        stride(from: 0, to: highlights.count, by: 2).map {
            Array(highlights[$0..<min($0 + 2, highlights.count)])
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row, id: \.self) { tag in
                        HStack(spacing: 5) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 11, weight: .semibold))
                            Text(tag)
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                        }
                        .foregroundColor(.white)
                        .padding(.vertical, 7)
                        .padding(.horizontal, 12)
                        .background(.white.opacity(0.16))
                        .overlay(Capsule().stroke(.white.opacity(0.25), lineWidth: 1))
                        .clipShape(Capsule())
                    }
                }
            }
        }
    }
}

private struct ChapterRow: View {
    let chapter: Chapter
    let accent: CourseAccent
    let sizeClass: AppSizeClass
    let isCompleted: Bool
    /// Quiz score percentage (0-100), when this chapter's exercise was a
    /// scored dialogue+quiz. nil for picture-vocab chapters, or if this
    /// chapter simply hasn't been completed yet.
    let score: Int?

    var body: some View {
        HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            ChapterBadgeIcon(number: chapter.number, tint: accent.tint, isCompleted: isCompleted, sizeClass: sizeClass)

            VStack(alignment: .leading, spacing: 2) {
                Text("Chapter \(chapter.number)")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
                Text(chapter.title)
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                    .foregroundColor(AppColors.primaryText)

                if isCompleted {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 11, weight: .semibold))
                        Text(score != nil ? "Completed • \(score!)%" : "Completed")
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                    }
                    .foregroundColor(AppColors.success)
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .foregroundColor(AppColors.placeholderText)
        }
        .padding(AppSizing.spacing(3, for: sizeClass))
        .background(AppColors.cardBackground)
        .cornerRadius(AppSizing.cardCornerRadius(sizeClass))
    }
}

/// The badge shown at the start of each chapter row — a rosette/star
/// shield tinted to the course's accent, with the chapter number pinned
/// in a small white circle at its corner like an achievement badge. When
/// isCompleted, a small green check badge is anchored at the opposite
/// corner — same "anchored to the icon's own bounds" pattern Android
/// uses for its ChapterBadgeIcon, so it never drifts.
private struct ChapterBadgeIcon: View {
    let number: Int
    let tint: Color
    let isCompleted: Bool
    let sizeClass: AppSizeClass

    private let size: CGFloat = 44

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [tint, tint.opacity(0.7)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: size, height: size)
                .shadow(color: tint.opacity(0.35), radius: 6, y: 3)

            Image(systemName: "star.fill")
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundColor(.white)

            // Chapter-number badge, anchored to the bottom-trailing
            // corner of the icon's own bounds.
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    ZStack {
                        Circle().fill(Color.white)
                        Text("\(number)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(AppColors.primaryButton)
                    }
                    .frame(width: 18, height: 18)
                    .overlay(Circle().stroke(AppColors.cardBackground, lineWidth: 2))
                }
            }
            .frame(width: size, height: size)

            // Completed check badge, anchored to the top-trailing corner.
            if isCompleted {
                VStack {
                    HStack {
                        Spacer()
                        ZStack {
                            Circle().fill(AppColors.success)
                            Image(systemName: "checkmark")
                                .font(.system(size: 8, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .frame(width: 16, height: 16)
                        .overlay(Circle().stroke(AppColors.cardBackground, lineWidth: 2))
                    }
                    Spacer()
                }
                .frame(width: size, height: size)
            }
        }
        .frame(width: size, height: size)
    }
}
