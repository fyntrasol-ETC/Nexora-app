//
//  BadgesViewModel.swift
//  Nexora
//
//  Mirrors Android's BadgesViewModel: listens to courseProgress + badges,
//  auto-writes a badge doc the first time a course becomes fully complete.
//  Reuses CourseProgressRepository for the courseProgress listener so this
//  reads the exact same CourseProgress model the Courses tab uses, instead
//  of re-parsing Firestore docs a second time.
//
//  Rules make badges write-once (create only), so badgeWriteAttempted just
//  stops us firing a doomed repeat create() more than once per session.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore
import Combine

struct BadgesUiState {
    var progressByCourseId: [String: CourseProgress] = [:]
    var unlockedBadgeIds: Set<String> = []
    var isLoading: Bool = true
}

@MainActor
final class BadgesViewModel: ObservableObject {
    @Published private(set) var uiState = BadgesUiState()

    private let db = Firestore.firestore()
    private let progressRepository = CourseProgressRepository()
    private var uid: String? { Auth.auth().currentUser?.uid }

    private var progressListener: ListenerRegistration?
    private var badgesListener: ListenerRegistration?
    private var badgeWriteAttempted: Set<String> = []

    init() {
        listenToProgress()
        listenToBadges()
    }

    private func listenToProgress() {
        guard let uid else { return }
        progressListener = progressRepository.observeAllProgress(uid: uid) { [weak self] progress in
            guard let self else { return }
            self.uiState.progressByCourseId = progress
            self.uiState.isLoading = false
            self.awardAnyNewlyCompletedBadges(progress)
        }
    }

    private func listenToBadges() {
        guard let uid else { return }
        badgesListener = db.collection("users").document(uid)
            .collection("badges")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self, let snapshot else { return }
                self.uiState.unlockedBadgeIds = Set(snapshot.documents.map { $0.documentID })
            }
    }

    private func awardAnyNewlyCompletedBadges(_ progress: [String: CourseProgress]) {
        guard let uid else { return }
        let alreadyUnlocked = uiState.unlockedBadgeIds

        for course in CourseCatalog.courses {
            guard let courseProgress = progress[course.id] else { continue }
            guard isCourseFullyComplete(course, completedChapterIds: courseProgress.completedChapterIds) else { continue }
            guard let badge = BadgeCatalog.badgeFor(course.id) else { continue }
            guard !alreadyUnlocked.contains(badge.id), !badgeWriteAttempted.contains(badge.id) else { continue }

            badgeWriteAttempted.insert(badge.id)
            db.collection("users").document(uid)
                .collection("badges").document(badge.id)
                .setData(["earnedAt": Timestamp(date: Date())])
        }
    }

    deinit {
        progressListener?.remove()
        badgesListener?.remove()
    }
}
