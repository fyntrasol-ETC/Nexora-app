

import Foundation
import FirebaseFirestore

final class WeeklyActivityService {

    private let db = Firestore.firestore()

    private func collection(uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("weeklyActivity")
    }

    private static let dateIdFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        // FIX: was `.current` (device local time zone). Android computes
        // dateId with `LocalDate.now(ZoneOffset.UTC)`, so this MUST be UTC
        // too — otherwise a check-in written near midnight in a non-UTC
        // time zone lands on a different Firestore document than the one
        // Android reads/writes for "today", and the two platforms fall
        // out of sync (or a check-in silently doesn't line up with what
        // the week query expects).
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter
    }()

    /// The `yyyy-MM-dd` document id for a given date, in UTC — matches
    /// Android's dateId and the `dateId.matches('^[0-9]{4}-...')` check in
    /// firestore.rules.
    static func dateId(for date: Date) -> String {
        dateIdFormatter.string(from: date)
    }

    /// Live-observes which of the given dateIds are marked `active: true`,
    /// mirroring Android's `WeeklyActivityRepository.observeCheckedInDates`
    /// (a Flow backed by addSnapshotListener). `onChange` fires with the
    /// current active subset every time Firestore's cache or server
    /// updates — including immediately after a local `checkIn` write is
    /// acknowledged — so the UI never needs a manual re-fetch to catch up.
    ///
    /// Firestore's document-ID `in` filter caps at 10 values, which is
    /// fine here since callers pass at most 7 (one week).
    ///
    /// Returns the `ListenerRegistration` so the caller can remove it when
    /// the screen/view model goes away (Android does the equivalent via
    /// `awaitClose { registration.remove() }` in its Flow).
    @discardableResult
    func observeActiveDays(
        uid: String,
        dateIds: [String],
        onChange: @escaping (Set<String>) -> Void
    ) -> ListenerRegistration? {
        guard !dateIds.isEmpty else {
            onChange([])
            return nil
        }

        return collection(uid: uid)
            .whereField(FieldPath.documentID(), in: dateIds)
            .addSnapshotListener { snapshot, error in
                if let error {
                    print("WeeklyActivityService observeActiveDays failed: \(error.localizedDescription)")
                    return
                }
                guard let snapshot else { return }

                var active: Set<String> = []
                for document in snapshot.documents {
                    if (document.data()["active"] as? Bool) == true {
                        active.insert(document.documentID)
                    }
                }
                onChange(active)
            }
    }

    /// Marks a single day checked in. Using the day's own dateId as the
    /// Firestore document id makes this naturally idempotent — checking
    /// in twice on the same day just overwrites the same doc.
    ///
    /// FIX: this is now `async throws` instead of fire-and-forget with a
    /// console-only error log. The old fire-and-forget version let the
    /// view model optimistically mark "checked in" in the UI *before*
    /// knowing whether the write actually succeeded — so a rejected write
    /// (rules, auth, connectivity) looked successful in the moment and
    /// then silently vanished on next launch when the real data was
    /// re-fetched. Awaiting the write lets the caller only treat it as
    /// checked-in once Firestore has actually confirmed it — matching
    /// Android's `checkInToday()`, which `await()`s the write and only
    /// clears its loading flag in a `finally` afterward.
    func checkIn(uid: String, dateId: String) async throws {
        let data: [String: Any] = [
            "active": true,
            "checkedInAt": FieldValue.serverTimestamp()
        ]
        try await collection(uid: uid).document(dateId).setData(data, merge: true)
    }
}
