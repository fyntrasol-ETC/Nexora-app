//
//  QuizAndCompletionScreens.swift
//  Nexora
//
//  The last two steps of the exercise flow: the quiz itself, and the
//  completion screen it lands on. Kept together since one only exists to
//  follow the other.
//
//  Both CTAs sit in `.safeAreaInset(edge: .bottom)` so they're always
//  pinned above the home indicator, independent of anything layered
//  underneath.
//
//  Completion now also writes progress back to Firestore via
//  CoursesViewModel — completedChapterIds + (for quiz chapters)
//  exerciseScores — mirroring Android's LaunchedEffect-on-arrival write.
//

import SwiftUI

// MARK: - Quiz

/// One question at a time (MCQ or fill-in-the-blank), answered from
/// memory of the dialogue just read. "Next" is disabled until an option
/// is picked; on the last question it becomes "Finish", tallies the
/// final score, and moves to the completion screen with it.
struct ExerciseQuizScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    let chapter: Chapter
    @Binding var path: [CourseRoute]
    @StateObject private var viewModel: ExerciseQuizViewModel

    init(course: Course, chapter: Chapter, path: Binding<[CourseRoute]>) {
        self.course = course
        self.chapter = chapter
        self._path = path
        _viewModel = StateObject(wrappedValue: ExerciseQuizViewModel(questions: Self.quizQuestions(for: chapter)))
    }

    private static func quizQuestions(for chapter: Chapter) -> [QuizQuestion] {
        if case .dialogue(let exercise) = chapter.exercise {
            return exercise.questions
        }
        return []
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                ScrollView {
                    VStack(alignment: .leading, spacing: AppSizing.spacing(5, for: sizeClass)) {
                        VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
                            QuestionKindTag(
                                isFillInBlank: viewModel.currentQuestion.type == .fillInBlank,
                                sizeClass: sizeClass
                            )

                            Text(viewModel.currentQuestion.prompt)
                                .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                                .foregroundColor(.white)
                                .lineSpacing(2)
                        }

                        VStack(spacing: 12) {
                            ForEach(Array(viewModel.currentQuestion.options.enumerated()), id: \.offset) { index, option in
                                optionButton(index: index, option: option)
                            }
                        }
                    }
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                    .padding(.top, AppSizing.spacing(4, for: sizeClass))
                    .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
                }
            }
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            nextButton
        }
        // Reset the picked-answer highlight the instant the question
        // changes, otherwise SwiftUI can briefly show the previous
        // question's selected state on the new one.
        .animation(.easeInOut(duration: 0.2), value: viewModel.currentIndex)
    }

    private var header: some View {
        VStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
            HStack {
                Button {
                    path.removeAll()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                        .background(.white.opacity(0.15))
                        .clipShape(Circle())
                }
                Spacer()
                Text(viewModel.progressLabel)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                // Balances the close button so the label stays centered.
                Color.clear.frame(width: 40, height: 40)
            }

            QuizProgressBar(
                total: viewModel.questions.count,
                currentIndex: viewModel.currentIndex
            )
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(2, for: sizeClass))
    }

    private func optionButton(index: Int, option: String) -> some View {
        let isSelected = viewModel.selectedOptionIndex == index
        let letter = Character(UnicodeScalar(65 + index)!)

        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                viewModel.select(index)
            }
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(isSelected ? AppColors.primaryButton : Color.white.opacity(0.18))
                        .frame(width: 30, height: 30)
                    Text(String(letter))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.white)
                }

                Text(option)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? AppColors.primaryButton : .white)
                    .multilineTextAlignment(.leading)

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(AppColors.primaryButton)
                }
            }
            .padding(AppSizing.spacing(3, for: sizeClass))
            .background(isSelected ? Color.white : Color.white.opacity(0.14))
            .clipShape(RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass) * 1.3, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass) * 1.3, style: .continuous)
                    .stroke(isSelected ? Color.white : Color.white.opacity(0.22), lineWidth: isSelected ? 0 : 1)
            )
        }
    }

    private var nextButton: some View {
        GlowCTAButton(
            title: viewModel.isLastQuestion ? "Finish" : "Next",
            icon: viewModel.isLastQuestion ? "checkmark" : "arrow.right"
        ) {
            let finished = viewModel.advanceOrFinish()
            if finished {
                let score = QuizScore(correct: viewModel.correctCount, total: viewModel.questions.count)
                path.append(.completion(course, chapter, score))
            }
        }
        .disabled(!viewModel.hasAnswered)
        .opacity(viewModel.hasAnswered ? 1 : 0.45)
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
    }
}

/// Small caption pill above the question prompt — flags whether this one
/// is fill-in-the-blank or multiple choice, since the two need slightly
/// different reading modes.
private struct QuestionKindTag: View {
    let isFillInBlank: Bool
    let sizeClass: AppSizeClass

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: isFillInBlank ? "square.and.pencil" : "checklist")
                .font(.system(size: 11, weight: .bold))
            Text(isFillInBlank ? "FILL IN THE BLANK" : "CHOOSE THE CORRECT ANSWER")
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                .tracking(0.5)
        }
        .foregroundColor(.white.opacity(0.85))
        .padding(.vertical, 6)
        .padding(.horizontal, 10)
        .background(.white.opacity(0.14))
        .clipShape(Capsule())
    }
}

/// Segmented capsule progress bar across the top of the quiz — one
/// segment per question, filled up through the current one.
private struct QuizProgressBar: View {
    let total: Int
    let currentIndex: Int

    var body: some View {
        HStack(spacing: 5) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index <= currentIndex ? Color.white : Color.white.opacity(0.22))
                    .frame(height: 5)
            }
        }
    }
}

// MARK: - Completion

/// Shown once the exercise (quiz, or picture deck) is finished. "Go Back"
/// pops the whole chapterIntro -> exerciseIntro -> exercise -> (quiz ->)
/// completion run in one go, landing back on the chapters list.
///
/// `score` is non-nil only when this completion follows a quiz (dialogue
/// chapters); picture chapters finish straight here with no score to show.
/// On first appearance, this screen writes the chapter's completion (and
/// score, if any) back to Firestore via `coursesViewModel`.
struct ExerciseCompletionScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    let chapter: Chapter
    let score: QuizScore?
    @Binding var path: [CourseRoute]
    let coursesViewModel: CoursesViewModel

    @State private var badgeAppeared = false

    private var exerciseItemCountLabel: String {
        switch chapter.exercise {
        case .dialogue(let e): return "\(e.questions.count) Questions"
        case .picture(let e): return "\(e.cards.count) Words"
        }
    }

    var body: some View {
        ZStack {
            AppBackground()
            DecorativeBlobs(sizeClass: sizeClass)

            VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
                Spacer()

                ZStack {
                    Circle()
                        .fill(.white.opacity(0.12))
                        .frame(width: 210, height: 210)
                    Circle()
                        .fill(.white.opacity(0.16))
                        .frame(width: 160, height: 160)
                    Circle()
                        .fill(Color.white)
                        .frame(width: 108, height: 108)
                        .shadow(color: .black.opacity(0.18), radius: 20, y: 10)
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 50))
                        .foregroundColor(AppColors.primaryButton)
                }
                .scaleEffect(badgeAppeared ? 1 : 0.6)
                .opacity(badgeAppeared ? 1 : 0)
                .onAppear {
                    withAnimation(.spring(response: 0.55, dampingFraction: 0.65)) {
                        badgeAppeared = true
                    }
                }

                VStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                    Text("Exercise Completed!")
                        .font(.system(size: AppSizing.font(.logo, for: sizeClass), weight: .heavy))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)

                    Text("Great work finishing \u{201C}\(chapter.title)\u{201D}.")
                        .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.88))
                        .multilineTextAlignment(.center)
                }

                // Score pill — only shown when this completion came from a quiz.
                if let score {
                    HStack(spacing: 6) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 12, weight: .semibold))
                        Text("Score: \(score.correct)/\(score.total) (\(score.percentage)%)")
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    }
                    .foregroundColor(AppColors.primaryButton)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 14)
                    .background(Color.white)
                    .clipShape(Capsule())
                }

                HStack(spacing: 10) {
                    CompletionStatChip(icon: "book.fill", label: exerciseItemCountLabel, sizeClass: sizeClass)
                    CompletionStatChip(icon: "text.book.closed.fill", label: course.title, sizeClass: sizeClass)
                }
                .padding(.top, AppSizing.spacing(2, for: sizeClass))

                Spacer()
                Spacer()
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            GlowCTAButton(title: "Go Back to Chapters", icon: "arrow.uturn.left") {
                // path is currently: [chapters, chapterIntro, exerciseIntro,
                // exercise, (quiz,) completion] — drop everything after the
                // first entry so we land back on the chapters list.
                path.removeLast(path.count - 1)
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        }
        // Keyed on course.id + chapter.number + score so it fires once per
        // time this screen is actually reached with these values — not on
        // every re-render. markChapterComplete's arrayUnion makes a repeat
        // write harmless anyway (e.g. re-entering the same completion
        // screen); this just avoids the redundant network call. Mirrors
        // Android's LaunchedEffect(course.id, chapter.number, score).
        .task(id: "\(course.id)-\(chapter.number)-\(score?.percentage ?? -1)") {
            coursesViewModel.markChapterComplete(
                courseId: course.id,
                chapterNumber: chapter.number,
                scorePercentage: score?.percentage
            )
        }
    }
}

/// Small rounded chip used in a row on the completion screen to surface
/// a couple of quick facts about what was just finished.
private struct CompletionStatChip: View {
    let icon: String
    let label: String
    let sizeClass: AppSizeClass

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
            Text(label)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                .lineLimit(1)
        }
        .foregroundColor(.white)
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
        .background(.white.opacity(0.16))
        .clipShape(Capsule())
    }
}
