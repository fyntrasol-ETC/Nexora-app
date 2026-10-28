
import AVFoundation

final class AudioManager {
    static let shared = AudioManager()
    private let synthesizer = AVSpeechSynthesizer()

    private init() {}

    func pronounce(_ text: String, language: String = "en-US") {
        // Stop anything mid-utterance so rapid taps (or swiping right
        // after tapping speaker) don't queue speech behind the card
        // that already advanced.
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: language)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95
        synthesizer.speak(utterance)
    }
}
