import SwiftUI
import Combine

@MainActor
final class PictureExerciseViewModel: ObservableObject {
    let cards: [PictureCard]

    @Published private(set) var currentIndex = 0
    @Published var userInput: String = ""
    @Published private(set) var isRevealed = false
    @Published private(set) var correctCount = 0

    init(cards: [PictureCard]) {
        self.cards = cards
    }

    var currentCard: PictureCard? { cards.indices.contains(currentIndex) ? cards[currentIndex] : nil }
    var isLastCard: Bool { currentIndex == cards.count - 1 }
    var progressLabel: String { "Word \(currentIndex + 1) of \(cards.count)" }

    var isGuessCorrect: Bool {
        guard let card = currentCard else { return false }
        let guess = userInput.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return guess == card.answer.lowercased()
    }

    func reveal() {
        guard !isRevealed, currentCard != nil else { return }
        if isGuessCorrect { correctCount += 1 }
        isRevealed = true
    }

    /// Advances to the next card. Returns true once the deck is finished.
    func advanceOrFinish() -> Bool {
        guard !isLastCard else { return true }
        currentIndex += 1
        userInput = ""
        isRevealed = false
        return false
    }
}
