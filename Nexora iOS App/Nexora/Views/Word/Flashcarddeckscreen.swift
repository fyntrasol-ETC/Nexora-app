//
//  FlashcardDeckScreen.swift
//  Elli
//
//  The swipeable flashcard deck. Fixed-size square card (not stretched
//  to fill the screen), on the same brand gradient background used by
//  Splash/Login/StartLearningScreen so it reads as part of the app.
//
//  Swipe (or tap the check/cross buttons) right -> card tints with the
//  "know" accent and the word is marked known. Left -> card tints with
//  the "don't know" accent.
//
//  `title == nil` shows a plain X close button (entry via the general
//  "Study words" flow). `title != nil` shows a back chevron + that
//  title instead (entry via tapping straight into a bucket).
//

import SwiftUI

struct FlashcardDeckScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @Environment(\.dismiss) private var dismiss

    let title: String?
    let onKnow: (Word) -> Void
    let onDontKnow: (Word) -> Void
    let onToggleSaved: (Word) -> Void
    /// True when the current session is a guest — swiping still works
    /// and feels identical (see `words`/`currentIndex` below), but
    /// nothing gets written to Firestore (WordsViewModel.persist(_:)
    /// checks this same condition). Drives the sign-in prompt shown at
    /// the end of the deck instead of a plain "Done".
    var requiresSignInToSave: Bool = false

    @State private var words: [Word]
    @State private var currentIndex = 0
    @State private var dragOffset: CGSize = .zero
    @State private var showDefinition = false

    init(
        title: String?,
        words: [Word],
        onKnow: @escaping (Word) -> Void,
        onDontKnow: @escaping (Word) -> Void,
        onToggleSaved: @escaping (Word) -> Void,
        requiresSignInToSave: Bool = false
    ) {
        self.title = title
        self._words = State(initialValue: words)
        self.onKnow = onKnow
        self.onDontKnow = onDontKnow
        self.onToggleSaved = onToggleSaved
        self.requiresSignInToSave = requiresSignInToSave
    }

    private var currentWord: Word? {
        words.indices.contains(currentIndex) ? words[currentIndex] : nil
    }

    private var swipeTint: Color {
        dragOffset.width >= 0 ? AppColors.accentRed : AppColors.link
    }

    private var swipeProgress: CGFloat {
        min(abs(dragOffset.width) / 150, 1.0)
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: AppSizing.spacing(6, for: sizeClass)) {
                topBar

                Text("Do you know this word?")
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .medium))
                    .foregroundColor(AppColors.primaryText)
                    .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
                    .padding(.vertical, AppSizing.spacing(2, for: sizeClass))
                    .background(AppColors.fieldBackground)
                    .clipShape(Capsule())

                Spacer()

                if let word = currentWord {
                    card(for: word)
                } else {
                    completionState
                }

                Spacer()
                Spacer()
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                if let title {
                    HStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                        Image(systemName: "chevron.left")
                        Text(title)
                    }
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .semibold))
                } else {
                    Image(systemName: "xmark")
                        .font(.system(size: AppSizing.iconSize(sizeClass)))
                }
            }
            .foregroundColor(.white)

            Spacer()
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(3, for: sizeClass))
    }

    // MARK: Card

    private func card(for word: Word) -> some View {
        let side = AppSizing.flashcardSize(sizeClass)

        return VStack(spacing: 0) {
            HStack {
                Button {
                    AudioManager.shared.pronounce(word.text)
                } label: {
                    Image(systemName: "speaker.wave.2.fill")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.75))
                        .foregroundColor(AppColors.primaryText)
                }

                Spacer()

                Button {
                    toggleSaved(word)
                } label: {
                    Image(systemName: word.isSaved ? "bookmark.fill" : "bookmark")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.75))
                        .foregroundColor(word.isSaved ? AppColors.primaryButton : AppColors.primaryText)
                }
            }
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            .padding(.top, AppSizing.spacing(4, for: sizeClass))

            Spacer()

            VStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                Text(word.text)
                    .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .heavy))
                    .foregroundColor(AppColors.primaryText)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
                    .lineLimit(2)

                Text(word.partOfSpeech)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                    .foregroundColor(AppColors.placeholderText)

                if showDefinition && !word.definition.isEmpty {
                    VStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                        Text(word.definition)
                            .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                            .foregroundColor(AppColors.primaryText)
                            .multilineTextAlignment(.center)

                        if !word.example.isEmpty {
                            Text(word.example)
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                .foregroundColor(AppColors.placeholderText)
                                .italic()
                                .multilineTextAlignment(.center)
                        }
                    }
                    .padding(.top, AppSizing.spacing(2, for: sizeClass))
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))

            Spacer()

            HStack {
                roundButton(systemImage: "xmark", tint: AppColors.link) {
                    swipe(right: false)
                }

                Spacer()

                roundButton(systemImage: "info", tint: AppColors.primaryButton, filled: false) {
                    withAnimation(.easeInOut(duration: 0.2)) { showDefinition.toggle() }
                }

                Spacer()

                roundButton(systemImage: "checkmark", tint: AppColors.accentRed) {
                    swipe(right: true)
                }
            }
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
        }
        .frame(width: side, height: side)
        .background(AppColors.cardBackground)
        .cornerRadius(AppSizing.cardCornerRadius(sizeClass))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                .fill(swipeTint)
                .opacity(swipeProgress * 0.55)
        )
        .shadow(color: .black.opacity(0.18), radius: 14, y: 8)
        .offset(x: dragOffset.width)
        .rotationEffect(.degrees(Double(dragOffset.width / 20)))
        .gesture(
            DragGesture()
                .onChanged { value in
                    dragOffset = value.translation
                }
                .onEnded { value in
                    if value.translation.width > 110 {
                        swipe(right: true)
                    } else if value.translation.width < -110 {
                        swipe(right: false)
                    } else {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                            dragOffset = .zero
                        }
                    }
                }
        )
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: dragOffset)
    }

    private func roundButton(systemImage: String, tint: Color, filled: Bool = true, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                .foregroundColor(tint)
                .frame(width: AppSizing.buttonHeight(sizeClass) * 0.65, height: AppSizing.buttonHeight(sizeClass) * 0.65)
                .background(filled ? AppColors.fieldBackground : Color.clear)
                .overlay(
                    Circle().stroke(tint.opacity(filled ? 0 : 0.4), lineWidth: 1.5)
                )
                .clipShape(Circle())
        }
    }

    // MARK: Completion

    private var completionState: some View {
        VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
            Text("You reached the end 🎉")
                .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .semibold))
                .foregroundColor(.white)

            if requiresSignInToSave {
                // Everything they just swiped through was tracked in
                // `words` locally so the deck felt normal, but none of
                // it reached Firestore (see WordsViewModel.persist(_:)) —
                // this is the one moment we ask, rather than blocking
                // the swiping itself.
                SignInPromptCard(
                    title: "Sign in to save your words",
                    message: "Create an account so the words you just marked known, don't-know, or saved aren't lost when you leave.",
                    icon: "bookmark.fill"
                )
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            }

            Button {
                dismiss()
            } label: {
                Text("Done")
                    .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
                    .foregroundColor(AppColors.primaryButton)
                    .frame(width: 160, height: AppSizing.buttonHeight(sizeClass))
                    .background(Color.white)
                    .cornerRadius(AppSizing.buttonHeight(sizeClass) / 2)
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: Actions

    private func toggleSaved(_ word: Word) {
        guard let index = words.firstIndex(where: { $0.id == word.id }) else { return }
        words[index].isSaved.toggle()
        onToggleSaved(words[index])
    }

    private func swipe(right: Bool) {
        guard let word = currentWord else { return }

        withAnimation(.easeIn(duration: 0.22)) {
            dragOffset = CGSize(width: right ? 600 : -600, height: 0)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
            if right {
                onKnow(word)
            } else {
                onDontKnow(word)
            }
            currentIndex += 1
            dragOffset = .zero
            showDefinition = false
        }
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        FlashcardDeckScreen(
            title: nil,
            words: Word.sampleWords,
            onKnow: { _ in },
            onDontKnow: { _ in },
            onToggleSaved: { _ in }
        )
    }
    .previewSizeClass()
}
#endif
