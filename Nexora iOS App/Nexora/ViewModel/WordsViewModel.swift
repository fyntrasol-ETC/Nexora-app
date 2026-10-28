//
//  WordsViewModel.swift
//  Elli
//
//  Single source of truth for word status + saved state. WordsScreen,
//  StartLearningScreen, and FlashcardDeckScreen all read/write through
//  this so "I Know" / "I Don't Know" / Saved stay in sync no matter
//  which screen made the change.
//
//  Every mutation that changes status or isSaved is mirrored to
//  users/{uid}/words/{wordId} via WordProgressService, so the same state
//  survives relaunch and shows up correctly on the Android side too.
//

import Foundation
import Combine

final class WordsViewModel: ObservableObject {
    @Published private(set) var words: [Word]
    @Published var showSavedOnly = false
    @Published var sortAlphabetically = false

    private let progressService = WordProgressService()
    private var uid: String?

    /// True once the current session is a real account (not an
    /// anonymous guest). Swiping "I Know" / "I Don't Know" and saving a
    /// word still update `words` locally either way — so the deck and
    /// FlashcardDeckScreen feel identical for guests — but `persist(_:)`
    /// only writes to Firestore once this is true, per spec: a guest's
    /// word choices don't survive without an account, and shouldn't be
    /// silently written to a uid that's about to be thrown away.
    private var isRealAccount = false

    /// For screens (e.g. FlashcardDeckScreen) that need to decide
    /// whether to show a "Sign in to save your words" prompt at the end
    /// of a session, or block saving a word mid-deck.
    var sessionRequiresSignInToSave: Bool { !isRealAccount }

    init(words: [Word] = Word.sampleWords) {
        self.words = words
    }

    /// Call once the signed-in user's uid is known (e.g. from WordsScreen's
    /// `.task`, reading `authViewModel.userProfile?.id`), passing whether
    /// that session is a real account (`authViewModel.isRealAccount`).
    /// Pulls this user's saved/known/don't-know state from Firestore and
    /// merges it onto the bundled word catalog. Safe to call again on
    /// re-sign-in — it just re-fetches and re-merges. For a guest
    /// session there's nothing to fetch yet (nothing was ever written),
    /// so this just records the uid/flag and returns.
    @MainActor
    func configure(uid: String, isRealAccount: Bool) async {
        self.uid = uid
        self.isRealAccount = isRealAccount

        guard isRealAccount else { return }

        do {
            let progress = try await progressService.fetchAll(uid: uid)

            for (wordId, state) in progress {
                if let index = words.firstIndex(where: { $0.id == wordId }) {
                    words[index].status = state.status
                    words[index].isSaved = state.isSaved
                }
            }
        } catch {
            // Non-fatal — the bundled catalog still works locally even if
            // this particular fetch failed (e.g. offline on first launch).
            print("WordsViewModel: couldn't load word progress — \(error.localizedDescription)")
        }
    }

    // MARK: - Buckets shown on WordsScreen
    // Both are empty until a word has actually been swiped/tapped on a
    // flashcard — words sitting at .unknown show up in neither.

    var knownWords: [Word] { display(words.filter { $0.status == .know }) }
    var unknownWords: [Word] { display(words.filter { $0.status == .dontKnow }) }

    private func display(_ list: [Word]) -> [Word] {
        let filtered = showSavedOnly ? list.filter { $0.isSaved } : list
        return sortAlphabetically ? filtered.sorted { $0.text < $1.text } : filtered
    }

    /// Backs the Beginner / Intermediate / Advanced buttons on
    /// StartLearningScreen. Prioritizes words at that level you haven't
    /// swiped on yet (still `.unknown`) so a study session isn't just
    /// re-serving words you already marked know/don't know. Falls back to
    /// the full level pool once everything's been studied at least once.
    func randomWords(for difficulty: Word.Difficulty, count: Int = 10) -> [Word] {
        let levelWords = words.filter { $0.difficulty == difficulty }
        let unstudied = levelWords.filter { $0.status == .unknown }
        let pool = unstudied.isEmpty ? levelWords : unstudied
        return Array(pool.shuffled().prefix(count))
    }

    // MARK: - Mutations (called from WordsScreen row taps and FlashcardDeckScreen)

    func markKnown(_ word: Word) { setStatus(.know, for: word) }
    func markDontKnow(_ word: Word) { setStatus(.dontKnow, for: word) }

    /// `word` arrives with `isSaved` already flipped by the caller, so this
    /// just persists that value rather than toggling again.
    func toggleSaved(_ word: Word) {
        guard let index = words.firstIndex(where: { $0.id == word.id }) else { return }
        words[index].isSaved = word.isSaved
        persist(words[index])
    }

    /// Removes a word entirely — used by swipe-to-delete on WordsScreen.
    /// A deleted word disappears from both buckets and is gone for good,
    /// as opposed to markKnown/markDontKnow which just change its status.
    /// Also deletes the matching Firestore doc, so it doesn't silently
    /// reappear on next sign-in with stale status. Guests never wrote a
    /// doc in the first place, so there's nothing remote to clean up.
    func deleteWord(_ word: Word) {
        words.removeAll { $0.id == word.id }

        if let uid, isRealAccount {
            progressService.delete(uid: uid, wordId: word.id)
        }
    }

    private func setStatus(_ status: Word.Status, for word: Word) {
        guard let index = words.firstIndex(where: { $0.id == word.id }) else { return }
        words[index].status = status
        persist(words[index])
    }

    /// Mirrors a word's current state to Firestore — but only for a real
    /// account. A guest's swipes still update `words` locally (so the
    /// deck/buckets behave identically in the moment), they just don't
    /// survive the session; the screen is expected to show a
    /// `SignInPromptCard` ("Sign in to save your words") once the person
    /// reaches the end of a guest deck. Per the schema note, only write
    /// once a word has left `.unknown` or been saved, so we don't
    /// pre-create empty docs for words nobody's touched yet.
    private func persist(_ word: Word) {
        guard let uid, isRealAccount else { return }
        guard word.status != .unknown || word.isSaved else { return }
        progressService.upsert(uid: uid, word: word)
    }
}
