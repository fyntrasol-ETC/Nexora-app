//
//  BadgesScreen.swift
//  Nexora
//
//  One badge per course (BadgeCatalog). progressByCourseId only feeds the
//  "3/6 chapters" line in the locked detail card — unlockedBadgeIds (from
//  users/{uid}/badges) is the source of truth for what's earned. Styled to
//  match ChapterListScreen / CoursesScreen: AppSizing spacing, translucent
//  circle icon buttons, AppColors.cardBackground panels.
//

import SwiftUI

struct BadgesScreen: View {
    @Environment(\.appSizeClass) private var sizeClass

    let unlockedBadgeIds: Set<String>
    let progressByCourseId: [String: CourseProgress]
    var onBack: () -> Void
    /// Caller pushes .chapters(course) onto whichever nav stack actually
    /// owns the Courses tab — MainTabView, in this app's case.
    var onGoToCourse: (Course) -> Void

    @State private var selectedBadge: CourseBadge?

    private let badges = BadgeCatalog.badges
    private var unlockedCount: Int { badges.filter { unlockedBadgeIds.contains($0.id) }.count }

    private let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                ScrollView {
                    LazyVGrid(columns: columns, spacing: AppSizing.spacing(5, for: sizeClass)) {
                        ForEach(badges) { badge in
                            BadgeTile(
                                badge: badge,
                                unlocked: unlockedBadgeIds.contains(badge.id),
                                sizeClass: sizeClass,
                                onTap: { selectedBadge = badge }
                            )
                        }
                    }
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                    .padding(.top, AppSizing.spacing(2, for: sizeClass))
                    .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
                }
            }

            if let badge = selectedBadge {
                let unlocked = unlockedBadgeIds.contains(badge.id)
                let completedChapters = Set(progressByCourseId[badge.courseId]?.completedChapterIds ?? []).count

                BadgeDetailOverlay(
                    badge: badge,
                    unlocked: unlocked,
                    completedChapters: completedChapters,
                    sizeClass: sizeClass,
                    onDismiss: { selectedBadge = nil },
                    onGoToCourse: {
                        selectedBadge = nil
                        if let course = CourseCatalog.courseById(badge.courseId) {
                            onGoToCourse(course)
                        }
                    }
                )
            }
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }

            Spacer()

            Text("Badges")
                .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                .foregroundColor(.white)

            Spacer()

            HStack(spacing: 4) {
                Text("\(unlockedCount)/\(badges.count)")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                Image(systemName: "trophy.fill")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.yellow)
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 10)
            .background(.white.opacity(0.16))
            .clipShape(Capsule())
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(3, for: sizeClass))
        .padding(.bottom, AppSizing.spacing(2, for: sizeClass))
    }
}

// MARK: - Badge tile

private struct BadgeTile: View {
    let badge: CourseBadge
    let unlocked: Bool
    let sizeClass: AppSizeClass
    var onTap: () -> Void

    private let circleSize: CGFloat = 76

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .fill(
                            unlocked
                                ? LinearGradient(colors: [badge.accent.tint, badge.accent.tint.opacity(0.7)],
                                                  startPoint: .topLeading, endPoint: .bottomTrailing)
                                : LinearGradient(colors: [Color.white.opacity(0.18), Color.white.opacity(0.10)],
                                                  startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .frame(width: circleSize, height: circleSize)
                        .overlay(
                            Circle().stroke(Color.white.opacity(unlocked ? 0.5 : 0.2), lineWidth: 1.5)
                        )
                        .shadow(color: badge.accent.tint.opacity(unlocked ? 0.4 : 0), radius: unlocked ? 10 : 0, y: 4)

                    Image(systemName: "text.book.closed.fill")
                        .font(.system(size: circleSize * 0.34, weight: .semibold))
                        .foregroundColor(unlocked ? .white : .white.opacity(0.5))
                        .frame(width: circleSize, height: circleSize)

                    if !unlocked {
                        ZStack {
                            Circle().fill(AppColors.cardBackground)
                            Image(systemName: "lock.fill")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundColor(AppColors.placeholderText)
                        }
                        .frame(width: 22, height: 22)
                        .overlay(Circle().stroke(AppColors.cardBackground, lineWidth: 2))
                    }
                }

                Text(badge.title)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
                    .foregroundColor(unlocked ? .white : .white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .frame(width: circleSize + 16)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Detail overlay

private struct BadgeDetailOverlay: View {
    let badge: CourseBadge
    let unlocked: Bool
    let completedChapters: Int
    let sizeClass: AppSizeClass
    var onDismiss: () -> Void
    var onGoToCourse: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                ZStack {
                    RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass), style: .continuous)
                        .fill(
                            unlocked
                                ? LinearGradient(colors: [badge.accent.tint, badge.accent.tint.opacity(0.7)],
                                                  startPoint: .topLeading, endPoint: .bottomTrailing)
                                : LinearGradient(colors: [AppColors.placeholderText.opacity(0.3), AppColors.placeholderText.opacity(0.18)],
                                                  startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .frame(height: 140)

                    Image(systemName: "text.book.closed.fill")
                        .font(.system(size: 44, weight: .semibold))
                        .foregroundColor(unlocked ? .white : AppColors.placeholderText)
                }

                Text(badge.title)
                    .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
                    .multilineTextAlignment(.center)

                Text(unlocked
                     ? "Badge earned — course completed!"
                     : "\(completedChapters)/\(badge.chapterCount) chapters complete")
                    .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)
                    .multilineTextAlignment(.center)

                GlowCTAButton(
                    title: "Go to course",
                    icon: "arrow.right",
                    tint: badge.accent.tint,
                    titleColor: .white,
                    action: onGoToCourse
                )
            }
            .padding(AppSizing.spacing(5, for: sizeClass))
            .background(AppColors.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.3, style: .continuous))
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        }
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.18), value: unlocked)
    }
}
