
import SwiftUI

// MARK: - Topics

/// 10 topics to start. Easy to extend later — just add a case and two
/// `VideoItem`s for it in `VideoItem.sampleData`.
enum VideoTopic: String, CaseIterable, Hashable {
    case pronunciation = "Pronunciation"
    case grammar = "Grammar"
    case vocabulary = "Vocabulary"
    case listeningSpeaking = "Listening & Speaking"
    case businessEnglish = "Business English"
    case idiomsPhrases = "Idioms & Phrases"
    case tenses = "Tenses"
    case everydayConversation = "Everyday Conversation"
    case travelEnglish = "Travel English"
    case kidsBeginners = "Kids & Beginners"

    /// Cycles through the brand gradient so every topic still reads as
    /// "our app" instead of borrowing per-topic colors from a reference UI.
    var tint: Color {
        switch self {
        case .pronunciation, .businessEnglish, .tenses:
            return AppColors.gradientTop
        case .grammar, .idiomsPhrases, .travelEnglish:
            return AppColors.gradientMid
        case .vocabulary, .listeningSpeaking, .everydayConversation, .kidsBeginners:
            return AppColors.gradientBottom
        }
    }

    /// Lowercase, SNAKE-CASE slug used to build bundled video filenames.
    /// Must match Android's `VideoCatalog` ids EXACTLY (e.g.
    /// "listening_speaking", not "listening-speaking") — this string is
    /// used as the Firestore document id under videoActivity/{id}, so any
    /// mismatch between platforms means favorites silently stop syncing
    /// for that topic.
    var slug: String {
        switch self {
        case .pronunciation: return "pronunciation"
        case .grammar: return "grammar"
        case .vocabulary: return "vocabulary"
        case .listeningSpeaking: return "listening_speaking"
        case .businessEnglish: return "business_english"
        case .idiomsPhrases: return "idioms_phrases"
        case .tenses: return "tenses"
        case .everydayConversation: return "everyday_conversation"
        case .travelEnglish: return "travel_english"
        case .kidsBeginners: return "kids_beginners"
        }
    }
}

/// What the feed is currently filtered to.
enum VideoFilter: Hashable {
    case all
    case favorites
    case topic(VideoTopic)
}

// MARK: - Video item

struct VideoItem: Identifiable, Hashable {
    var id: String { videoFileName }
    let title: String
    let duration: String
    let topic: VideoTopic

    /// Filename (without extension) of the bundled video, e.g.
    /// "pronunciation_1" -> add "pronunciation_1.mp4" to the Xcode
    /// project (drag it in with "Copy items if needed" checked and this
    /// target's membership checked). VideoPlayerViewModel loads it via
    /// `Bundle.main.url(forResource:withExtension:)`.
    //
    // When you move to Firestore/Storage later, swap this for a
    // `videoURL: URL` (Storage download URL) instead — only
    // VideoPlayerViewModel needs to change where it builds the AVPlayer
    // from, nothing else has to move.
    let videoFileName: String

    var subtitle: String { topic.rawValue }

    // TODO: replace with the real catalog fetched from your backend.
    // 2 videos per topic for now — add more VideoItems here per topic
    // later without touching the ViewModel or the screen.
    // videoFileName below assumes files named "<topic-slug>_1.mp4" and
    // "<topic-slug>_2.mp4" — rename your mp4s to match, or edit the
    // filenames here to match whatever you actually add to Xcode.
    static let sampleData: [VideoItem] = VideoTopic.allCases.flatMap { topic -> [VideoItem] in
        [
            VideoItem(title: "\(topic.rawValue): Getting Started", duration: "04:32", topic: topic, videoFileName: "\(topic.slug)_1"),
            VideoItem(title: "\(topic.rawValue): Common Mistakes", duration: "07:15", topic: topic, videoFileName: "\(topic.slug)_2")
        ]
    }
}

// MARK: - Playback speed

enum PlaybackSpeed: Double, CaseIterable, Identifiable {
    case normal = 1.0
    case oneAndHalf = 1.5
    case fast = 2.0

    var id: Double { rawValue }

    var label: String {
        switch self {
        case .normal: return "1x"
        case .oneAndHalf: return "1.5x"
        case .fast: return "2x"
        }
    }
}
