//
//  CourseViewModels.swift
//  Nexora
//
//  Both ViewModels for the Courses tab live here since they're small and
//  tightly related to the same feature.
//

import Foundation
import Combine
import FirebaseAuth
import FirebaseFirestore

// MARK: - Courses list

/// Owns the course catalog for the Courses tab, plus the signed-in
/// person's live per-course progress (completed chapters + quiz scores),
/// backed by users/{uid}/courseProgress/{courseId}. `courses(for:)` is
/// what CoursesScreen uses to pull the row of courses to show under each
/// level heading; `progressByCourseId` feeds the progress bar on each
/// course card and the checkmarks on the chapter list — same contract as
/// Android's CoursesViewModel.
@MainActor
final class CoursesViewModel: ObservableObject {
    @Published private(set) var courses: [Course]
    @Published private(set) var progressByCourseId: [String: CourseProgress] = [:]

    private let progressRepository: CourseProgressRepository
    private var progressListener: ListenerRegistration?
    private var authHandle: AuthStateDidChangeListenerHandle?

    init(
        courses: [Course] = CourseCatalog.courses,
        progressRepository: CourseProgressRepository = CourseProgressRepository()
    ) {
        self.courses = courses
        self.progressRepository = progressRepository
        observeAuthState()
    }

    deinit {
        progressListener?.remove()
        if let authHandle {
            Auth.auth().removeStateDidChangeListener(authHandle)
        }
    }

    func courses(for level: CourseLevel) -> [Course] {
        courses.filter { $0.level == level }
    }

    /// Re-subscribes to progress whenever Firebase Auth's state actually
    /// changes, instead of checking Auth.auth().currentUser a single time
    /// in init. That one-shot check was the bug: Auth restores the signed-in
    /// session asynchronously, and if this view model happened to be created
    /// before that restore completed, currentUser was nil, the old guard
    /// bailed out, and no listener was ever attached — progress would
    /// silently never load for that view model instance, even though the
    /// same account had data on Android.
    ///
    /// addStateDidChangeListener fires immediately with the current state
    /// and again on every future change (sign-in, sign-out, account switch),
    /// so this covers cold launch, session restore, and account changes in
    /// one place. Any previously-attached progress listener is torn down
    /// before attaching a new one so we never end up listening for two
    /// different uids at once.
    private func observeAuthState() {
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self else { return }

            self.progressListener?.remove()
            self.progressListener = nil

            guard let uid = user?.uid else {
                Task { @MainActor in
                    self.progressByCourseId = [:]
                }
                return
            }

            self.progressListener = self.progressRepository.observeAllProgress(uid: uid) { [weak self] map in
                // Snapshot listener callbacks aren't guaranteed to land on
                // the main actor, and progressByCourseId is @Published —
                // hop back explicitly before writing to it.
                Task { @MainActor in
                    self?.progressByCourseId = map
                }
            }
        }
    }

    /// Called once when a chapter's Completion screen is reached.
    /// scorePercentage is nil for picture-vocab chapters (no quiz to score).
    func markChapterComplete(courseId: String, chapterNumber: Int, scorePercentage: Int? = nil) {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Task {
            try? await progressRepository.markChapterComplete(
                uid: uid,
                courseId: courseId,
                chapterNumber: chapterNumber,
                scorePercentage: scorePercentage
            )
        }
    }
}

// MARK: - Quiz progression

/// Owns progression through a chapter's quiz questions: which question is
/// current, what's selected, whether we're on the last one, and — new —
/// how many have been answered correctly so far, so a real final score
/// can be carried into the completion screen and persisted from there.
@MainActor
final class ExerciseQuizViewModel: ObservableObject {
    let questions: [QuizQuestion]

    @Published private(set) var currentIndex = 0
    @Published private(set) var selectedOptionIndex: Int?
    @Published private(set) var correctCount = 0

    init(questions: [QuizQuestion]) {
        self.questions = questions
    }

    var currentQuestion: QuizQuestion { questions[currentIndex] }
    var isLastQuestion: Bool { currentIndex == questions.count - 1 }
    var hasAnswered: Bool { selectedOptionIndex != nil }
    var progressLabel: String { "Question \(currentIndex + 1) of \(questions.count)" }

    func select(_ index: Int) {
        selectedOptionIndex = index
    }

    /// Advances to the next question, tallying whether the just-answered
    /// question was correct. Returns `true` once the quiz is finished
    /// (called on the last question) so the screen knows to navigate to
    /// the completion screen — with the final score — instead of showing
    /// another question.
    func advanceOrFinish() -> Bool {
        if selectedOptionIndex == currentQuestion.correctOptionIndex {
            correctCount += 1
        }
        guard !isLastQuestion else { return true }
        currentIndex += 1
        selectedOptionIndex = nil
        return false
    }
}
