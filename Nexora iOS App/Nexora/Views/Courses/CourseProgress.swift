//
//  CourseProgress.swift
//  Nexora
//
//  Progress tracking for the Courses tab — mirrors Android's
//  CourseProgressRepository + CourseProgress/QuizScore models 1:1, so both
//  platforms read and write the exact same
//  users/{uid}/courseProgress/{courseId} shape (see firestore.rules).
//

import Foundation
import FirebaseFirestore

/// Maps to one users/{uid}/courseProgress/{courseId} document.
/// A course the person hasn't started yet simply has no entry in
/// CoursesViewModel.progressByCourseId — treat a missing entry as
/// "0% complete", not an error (same contract as the Android version).
struct CourseProgress: Hashable {
    var completedChapterIds: [Int] = []
    /// chapterNumber -> score percentage (0-100). Only ever set for
    /// dialogue+quiz chapters — picture-vocab chapters have no quiz score.
    var exerciseScores: [Int: Int] = [:]
}

/// Correct/total from a single quiz run — carried through the Quiz ->
/// Completion navigation and persisted from there.
struct QuizScore: Hashable {
    let correct: Int
    let total: Int

    var percentage: Int {
        total == 0 ? 0 : (correct * 100) / total
    }
}

/// Read/write for users/{uid}/courseProgress/{courseId}. Field names must
/// stay in sync with firestore.rules and with Android's
/// CourseProgressRepository.
final class CourseProgressRepository {

    private let db = Firestore.firestore()

    /// Live map of courseId -> CourseProgress for every course doc the
    /// person has touched. Firestore map keys are always strings on the
    /// wire, so the "3" -> 3 chapter-number parsing for exerciseScores
    /// happens here, same as the manual parsing on the Android side.
    @discardableResult
    func observeAllProgress(
        uid: String,
        onChange: @escaping ([String: CourseProgress]) -> Void
    ) -> ListenerRegistration {

        db.collection("users").document(uid)
            .collection("courseProgress")
            .addSnapshotListener { snapshot, error in

                guard error == nil, let documents = snapshot?.documents else {
                    onChange([:])
                    return
                }

                var map: [String: CourseProgress] = [:]

                for doc in documents {

                    let completedIds = (doc.get("completedChapterIds") as? [Any])?
                        .compactMap { Self.parseChapterNumber(from: $0) } ?? []

                    let rawScores = doc.get("exerciseScores") as? [String: Any] ?? [:]
                    var scores: [Int: Int] = [:]

                    for (key, value) in rawScores {
                        // Same dual-format tolerance as completedChapterIds
                        // below — the key itself may be "3" or
                        // "<courseId>-ch3" depending on which format wrote it.
                        if let chapterNumber = Self.parseChapterNumber(from: key),
                           let percentage = (value as? NSNumber)?.intValue {
                            scores[chapterNumber] = percentage
                        }
                    }

                    map[doc.documentID] = CourseProgress(
                        completedChapterIds: completedIds,
                        exerciseScores: scores
                    )
                }

                onChange(map)
            }
    }

    /// Accepts a chapter number written either as a raw Int/NSNumber
    /// (what this app writes via markChapterComplete below) or as a
    /// string like "beginner-greetings-ch3" (the format documented in
    /// firestore.rules' schema notes). Written defensively because it's
    /// unconfirmed which format Android's real course-progress writer
    /// actually uses on the wire — once that's confirmed with the
    /// Android side, this can be simplified back down to a single format.
    private static func parseChapterNumber(from raw: Any) -> Int? {
        if let number = raw as? NSNumber {
            return number.intValue
        }
        if let string = raw as? String {
            if let plain = Int(string) {
                return plain
            }
            if let chRange = string.range(of: "-ch") {
                return Int(string[chRange.upperBound...])
            }
        }
        return nil
    }

    /// Marks one chapter finished. Uses arrayUnion so completing an
    /// already-completed chapter again (a retake, or re-entering the same
    /// Completion screen) is a harmless no-op instead of a duplicate entry
    /// — the tick stays regardless of how many times this fires.
    ///
    /// scorePercentage, when present, is written with a dotted field path
    /// ("exerciseScores.<chapterNumber>") merged in, so it only touches
    /// that one chapter's score without disturbing any other chapter
    /// already in the map — a retake just overwrites its own entry.
    func markChapterComplete(
        uid: String,
        courseId: String,
        chapterNumber: Int,
        scorePercentage: Int? = nil
    ) async throws {

        var updates: [String: Any] = [
            "completedChapterIds": FieldValue.arrayUnion([chapterNumber]),
            "lastAccessedAt": FieldValue.serverTimestamp()
        ]

        if let scorePercentage {
            updates["exerciseScores.\(chapterNumber)"] = scorePercentage
        }

        try await db.collection("users").document(uid)
            .collection("courseProgress").document(courseId)
            .setData(updates, merge: true)
    }
}
