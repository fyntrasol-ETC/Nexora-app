
import Foundation
import FirebaseFirestore

final class WordProgressService {

    private let db = Firestore.firestore()

    private func collection(uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("words")
    }

    /// Fetches every per-user word-progress doc, keyed by wordId, so the
    /// caller can merge it onto the bundled Word catalog.
    func fetchAll(uid: String) async throws -> [String: (status: Word.Status, isSaved: Bool)] {

        let snapshot = try await collection(uid: uid).getDocuments()

        var result: [String: (status: Word.Status, isSaved: Bool)] = [:]

        for document in snapshot.documents {
            let data = document.data()

            guard
                let statusRaw = data["status"] as? String,
                let status = Word.Status(rawValue: statusRaw)
            else {
                continue
            }

            let isSaved = data["isSaved"] as? Bool ?? false

            result[document.documentID] = (status, isSaved)
        }

        return result
    }

    /// Create-or-merge a word's current status/isSaved. Per the schema
    /// note in firestore.rules: only call this once a word has actually
    /// left `.unknown` or been saved — don't pre-create empty docs for
    /// every bundled word, that's wasted Spark quota for no benefit.
    func upsert(uid: String, word: Word) {

        let data: [String: Any] = [
            "status": word.status.rawValue,
            "isSaved": word.isSaved,
            "updatedAt": FieldValue.serverTimestamp()
        ]

        collection(uid: uid).document(word.id).setData(data, merge: true) { error in
            if let error {
                print("WordProgressService upsert failed for \(word.id): \(error.localizedDescription)")
            }
        }
    }

    /// Deletes a word's per-user progress doc — called on swipe-to-delete,
    /// so a removed word doesn't silently reappear with stale status the
    /// next time this user signs in on this or another device.
    func delete(uid: String, wordId: String) {

        collection(uid: uid).document(wordId).delete { error in
            if let error {
                print("WordProgressService delete failed for \(wordId): \(error.localizedDescription)")
            }
        }
    }
}
