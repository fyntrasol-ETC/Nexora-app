

import Foundation
import FirebaseFirestore

final class VideoActivityService {

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    private func collection(uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("videoActivity")
    }

    /// Starts a live listener on this user's videoActivity collection and
    /// calls `onChange` with the current favorited set every time
    /// Firestore changes — including changes made from another device or
    /// platform. Replaces any previously-running listener (safe to call
    /// again on re-sign-in).
    func observeFavoriteIDs(uid: String, onChange: @escaping (Set<String>) -> Void) {
        listener?.remove()
        listener = collection(uid: uid).addSnapshotListener { snapshot, error in
            guard let snapshot else {
                if let error {
                    print("VideoActivityService: listener error — \(error.localizedDescription)")
                }
                return
            }

            let favorites = Set(snapshot.documents.compactMap { doc -> String? in
                doc.data()["isFavorited"] as? Bool == true ? doc.documentID : nil
            })

            Task { @MainActor in
                onChange(favorites)
            }
        }
    }

    /// Stops the live listener. Call when the owning view model is torn
    /// down so it doesn't keep firing after nobody's listening.
    func stopObserving() {
        listener?.remove()
        listener = nil
    }

    /// Favoriting merges `isFavorited: true` onto the doc (so it never
    /// clobbers watched/lastPositionSeconds/watchCount once that lands).
    /// Unfavoriting DELETES the doc outright, so it's removed from the
    /// list entirely rather than just flipping a boolean to false.
    func setFavorited(uid: String, videoId: String, isFavorited: Bool) {
        let docRef = collection(uid: uid).document(videoId)

        if isFavorited {
            docRef.setData(["isFavorited": true], merge: true) { error in
                if let error {
                    print("VideoActivityService setFavorited failed for \(videoId): \(error.localizedDescription)")
                }
            }
        } else {
            docRef.delete { error in
                if let error {
                    print("VideoActivityService delete failed for \(videoId): \(error.localizedDescription)")
                }
            }
        }
    }
}
