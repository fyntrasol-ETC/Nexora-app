//
//  IntroScreens.swift
//  Elli
//
//  The two "title page" style screens in the course flow — a chapter's
//  own intro (number/title/description -> Continue) and the exercise's
//  intro (e.g. "Dialogue Exercise" heading/instructions -> Start). Kept
//  in one file since they're structurally the same screen with different
//  content and sit back-to-back in the flow.
//
//  Both CTAs use `.safeAreaInset(edge: .bottom)` instead of sitting
//  inside the scrolling VStack — that pins them just above the home
//  indicator no matter what's layered underneath (e.g. MainTabView's
//  floating tab bar, until that's wired to hide on this stack).
//

import SwiftUI

// MARK: - Chapter intro

/// The chapter's own title page — number, title, short description, and
/// a Continue button that moves forward into the exercise intro.
struct ChapterIntroScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    let chapter: Chapter
    @Binding var path: [CourseRoute]

    var body: some View {
        ZStack {
            AppBackground()
            DecorativeBlobs(sizeClass: sizeClass)

            VStack(alignment: .leading, spacing: 0) {
                IntroCloseButton(sizeClass: sizeClass) { path.removeAll() }
                    .padding(.top, AppSizing.spacing(2, for: sizeClass))

                Spacer()

                VStack(alignment: .leading, spacing: AppSizing.spacing(3, for: sizeClass)) {
                    ChapterBadge(number: chapter.number, sizeClass: sizeClass)

                    Text(chapter.title)
                        .font(.system(size: AppSizing.font(.logo, for: sizeClass), weight: .heavy))
                        .foregroundColor(.white)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(chapter.description)
                        .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.88))
                        .lineSpacing(3)
                }
                .padding(AppSizing.spacing(5, for: sizeClass))
                .background(.white.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.3, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.3, style: .continuous)
                        .stroke(.white.opacity(0.22), lineWidth: 1)
                )

                Spacer()
                Spacer()
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            GlowCTAButton(title: "Continue", icon: "arrow.right") {
                path.append(.exerciseIntro(course, chapter))
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        }
    }
}

// MARK: - Exercise intro

/// The exercise's own title page — e.g. heading "Dialogue Exercise" with
/// the 1-2 line explanation of what's about to happen, and a Start
/// button that opens the dialogue.
struct ExerciseIntroScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    let chapter: Chapter
    @Binding var path: [CourseRoute]

    private var exercise: ChapterExerciseContent { chapter.exercise }

    var body: some View {
        ZStack {
            AppBackground()
            DecorativeBlobs(sizeClass: sizeClass)

            VStack(alignment: .leading, spacing: 0) {
                IntroCloseButton(sizeClass: sizeClass) { path.removeAll() }
                    .padding(.top, AppSizing.spacing(2, for: sizeClass))

                Spacer()

                VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
                    ZStack {
                        Circle()
                            .fill(.white.opacity(0.16))
                            .frame(width: AppSizing.avatarSize(sizeClass) * 1.15, height: AppSizing.avatarSize(sizeClass) * 1.15)
                        Circle()
                            .fill(.white.opacity(0.22))
                            .frame(width: AppSizing.avatarSize(sizeClass) * 0.82, height: AppSizing.avatarSize(sizeClass) * 0.82)
                        Image(systemName: "bubble.left.and.bubble.right.fill")
                            .font(.system(size: AppSizing.avatarSize(sizeClass) * 0.34, weight: .semibold))
                            .foregroundColor(.white)
                    }

                    VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
                        Text(exercise.title)
                            .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass), weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Text(exercise.instructions)
                            .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                            .foregroundColor(.white.opacity(0.88))
                            .lineSpacing(3)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }

                Spacer()
                Spacer()
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            GlowCTAButton(title: "Start", icon: "play.fill") {
                path.append(.exercise(course, chapter))
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        }
    }
}

// MARK: - Shared close button

private struct IntroCloseButton: View {
    let sizeClass: AppSizeClass
    let action: () -> Void

    var body: some View {
        HStack {
            Button(action: action) {
                Image(systemName: "xmark")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }
            Spacer()
        }
    }
}

// MARK: - Chapter number badge

/// Small pill that reads "Chapter N" — reused wherever a chapter needs a
/// quick visual tag rather than just plain caption text.
struct ChapterBadge: View {
    let number: Int
    let sizeClass: AppSizeClass

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(.white)
                .frame(width: 20, height: 20)
                .overlay(
                    Text("\(number)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(AppColors.primaryButton)
                )
            Text("CHAPTER \(number)")
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                .tracking(0.6)
                .foregroundColor(.white.opacity(0.85))
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 10)
        .background(.white.opacity(0.14))
        .clipShape(Capsule())
    }
}

// MARK: - Glow CTA button

/// The wide capsule primary action button used at the bottom of every
/// course-flow screen. "Glow" refers to the soft drop shadow beneath it —
/// a small lift that keeps the white pill from reading flat against the
/// gradient.
struct GlowCTAButton: View {
    let title: String
    var icon: String? = nil
    var tint: Color = .white
    var titleColor: Color = AppColors.primaryButton
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(title)
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .font(.system(size: 17, weight: .bold))
            .foregroundColor(titleColor)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(tint)
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.22), radius: 16, y: 8)
        }
    }
}

// MARK: - Decorative background blobs

/// Soft, out-of-focus circles layered behind content on the course-flow
/// screens so the gradient doesn't read as an empty flat wash. Purely
/// decorative — ignores hit testing.
struct DecorativeBlobs: View {
    let sizeClass: AppSizeClass

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Circle()
                    .fill(.white.opacity(0.10))
                    .frame(width: geo.size.width * 0.75)
                    .offset(x: geo.size.width * 0.55, y: -geo.size.height * 0.06)
                Circle()
                    .fill(.white.opacity(0.08))
                    .frame(width: geo.size.width * 0.55)
                    .offset(x: -geo.size.width * 0.35, y: geo.size.height * 0.42)
            }
            .blur(radius: 2)
        }
        .allowsHitTesting(false)
        .ignoresSafeArea()
    }
}
