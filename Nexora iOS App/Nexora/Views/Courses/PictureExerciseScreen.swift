//
//  PictureExerciseScreen.swift
//  Elli
//
//  Second exercise type: an image, a text field to guess it, and a
//  3D flip reveal showing the answer + example sentence. "Finish" on
//  the last card lands on the same ExerciseCompletionScreen the
//  dialogue flow uses.
//

import SwiftUI



struct PictureExerciseScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    let course: Course
    let chapter: Chapter
    @Binding var path: [CourseRoute]
    @StateObject private var viewModel: PictureExerciseViewModel
    @FocusState private var inputFocused: Bool

    init(course: Course, chapter: Chapter, path: Binding<[CourseRoute]>) {
        self.course = course
        self.chapter = chapter
        self._path = path
        let cards: [PictureCard]
        if case .picture(let exercise) = chapter.exercise {
            cards = exercise.cards
        } else {
            cards = []
        }
        _viewModel = StateObject(wrappedValue: PictureExerciseViewModel(cards: cards))
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                if let card = viewModel.currentCard {
                    ScrollView {
                        VStack(spacing: AppSizing.spacing(5, for: sizeClass)) {
                            FlipCard(
                                card: card,
                                isRevealed: viewModel.isRevealed,
                                isCorrect: viewModel.isGuessCorrect,
                                sizeClass: sizeClass
                            )
                            .frame(height: 240)
                            .onTapGesture {
                                guard !viewModel.isRevealed else { return }
                                inputFocused = false
                                withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                                    viewModel.reveal()
                                }
                            }

                            VStack(spacing: 10) {
                                TextField("Type what you see...", text: $viewModel.userInput)
                                    .focused($inputFocused)
                                    .disabled(viewModel.isRevealed)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                                    .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                                    .padding(AppSizing.spacing(3, for: sizeClass))
                                    .background(Color.white.opacity(viewModel.isRevealed ? 0.6 : 0.92))
                                    .clipShape(RoundedRectangle(cornerRadius: AppSizing.smallCornerRadius(sizeClass), style: .continuous))
                                    .submitLabel(.done)
                                    .onSubmit {
                                        guard !viewModel.isRevealed else { return }
                                        withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                                            viewModel.reveal()
                                        }
                                    }

                                Text("Tap the card or type your guess, then reveal the answer.")
                                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                    .foregroundColor(.white.opacity(0.7))
                            }
                        }
                        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                        .padding(.top, AppSizing.spacing(4, for: sizeClass))
                        .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
                    }
                } else {
                    Spacer()
                    Text("No cards in this exercise yet.")
                        .foregroundColor(.white.opacity(0.8))
                    Spacer()
                }
            }
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            bottomButton
        }
        .animation(.easeInOut(duration: 0.2), value: viewModel.currentIndex)
    }

    private var header: some View {
        HStack {
            Button {
                path.removeLast()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }

            Spacer()

            VStack(spacing: 2) {
                Text("Picture Vocabulary")
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                if !viewModel.cards.isEmpty {
                    Text(viewModel.progressLabel)
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.7))
                }
            }

            Spacer()

            Button {
                path.removeAll()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(2, for: sizeClass))
        .padding(.bottom, AppSizing.spacing(2, for: sizeClass))
    }

    @ViewBuilder
    private var bottomButton: some View {
        if viewModel.isRevealed {
            GlowCTAButton(
                title: viewModel.isLastCard ? "Finish" : "Next Word",
                icon: viewModel.isLastCard ? "checkmark" : "arrow.right"
            ) {
                let finished = viewModel.advanceOrFinish()
                if finished {
                    path.append(.completion(course, chapter))
                }
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        } else if viewModel.currentCard != nil {
            GlowCTAButton(title: "Reveal Answer", icon: "eye.fill") {
                inputFocused = false
                withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                    viewModel.reveal()
                }
            }
            .opacity(viewModel.userInput.trimmingCharacters(in: .whitespaces).isEmpty ? 0.6 : 1)
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        }
    }
}

// MARK: - Flip card

/// Standard SwiftUI flip-card recipe: the whole card rotates 0->180 as
/// isRevealed flips, while the back face is pre-rotated -180 so the two
/// rotations cancel out and its text reads correctly once revealed,
/// instead of showing mirrored.
private struct FlipCard: View {
    let card: PictureCard
    let isRevealed: Bool
    let isCorrect: Bool
    let sizeClass: AppSizeClass

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.3, style: .continuous)
                .fill(Color.white)
                .shadow(color: .black.opacity(0.18), radius: 16, y: 8)

            imageFace
                .opacity(isRevealed ? 0 : 1)

            revealFace
                .opacity(isRevealed ? 1 : 0)
                .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
        }
        .rotation3DEffect(.degrees(isRevealed ? 180 : 0), axis: (x: 0, y: 1, z: 0))
    }

    private var imageFace: some View {
        VStack(spacing: 12) {
            if UIImage(named: card.imageName) != nil {
                Image(card.imageName)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 130)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            } else {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(AppColors.placeholderText.opacity(0.12))
                    Image(systemName: "photo")
                        .font(.system(size: 40))
                        .foregroundColor(AppColors.placeholderText)
                }
                .frame(height: 130)
            }
            Text("What is this?")
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.placeholderText)
        }
        .padding(20)
    }

    private var revealFace: some View {
        VStack(spacing: 10) {
            Image(systemName: isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                .font(.system(size: 32))
                // NOTE: swap `.red` below for `AppColors.accentRed` (or
                // whatever your palette's error/red color is actually
                // named) if you have one defined — it wasn't visible in
                // the files sent over, so this uses a plain system red
                // as a safe default that will compile either way.
                .foregroundColor(isCorrect ? .green : .red)
            Text(card.answer.capitalized)
                .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                .foregroundColor(AppColors.primaryText)
            Text(card.exampleSentence)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                .foregroundColor(AppColors.placeholderText)
                .multilineTextAlignment(.center)
        }
        .padding(20)
    }
}
