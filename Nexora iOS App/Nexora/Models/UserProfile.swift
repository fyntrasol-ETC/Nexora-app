import Foundation
import FirebaseFirestore


struct UserProfile: Codable, Identifiable {

    @DocumentID var id: String?          

    var displayName: String
    var email: String
    var photoURL: String?
    var nativeLanguage: String?
    var avatarSeed: String?
    @ServerTimestamp var createdAt: Timestamp?

    var totalTimeSpentSeconds: Int
    var currentStreak: Int
    var longestStreak: Int

    enum CodingKeys: String, CodingKey {
        case id
        case displayName
        case email
        case photoURL
        case nativeLanguage
        case avatarSeed
        case createdAt
        case totalTimeSpentSeconds
        case currentStreak
        case longestStreak
    }

    /// Use when creating the doc on sign-up. createdAt is left nil so
    /// @ServerTimestamp fills it in on write — never set this client-side,
    /// since the security rules also block it being rewritten afterwards.
    static func newProfile(uid: String, displayName: String, email: String) -> UserProfile {
        UserProfile(
            id: uid,
            displayName: displayName,
            email: email,
            photoURL: nil,
            nativeLanguage: nil,
            avatarSeed: AvatarOptions.defaultSeed,
            createdAt: nil,
            totalTimeSpentSeconds: 0,
            currentStreak: 0,
            longestStreak: 0
        )
    }
}
