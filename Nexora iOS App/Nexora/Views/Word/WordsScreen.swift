//
//  WordsScreen.swift
//  Elli
//
//  Words tab root. Background matches Profile (AppBackground). Word rows
//  live in a List so swipe-to-delete works natively; rows are styled to
//  still read as cards against the gradient. "Study words" is a floating
//  button pinned above the tab bar — not part of the scrolling content —
//  so it stays reachable no matter how long the word list gets.
//
//  `path` is owned by MainTabView (not this view) so MainTabView can see
//  how deep the stack is and hide the tab bar accordingly when a screen
//  is pushed — same pattern already used for the Profile tab.
//

import SwiftUI

// MARK: - Navigation

enum WordsRoute: Hashable {
    case startLearning
    case flashcards(title: String?, words: [Word])
}

// MARK: - Screen

struct WordsScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = WordsViewModel()
    @Binding var path: [WordsRoute]

    private enum Bucket { case dontKnow, know }
    @State private var selectedBucket: Bucket = .know

    private var displayedWords: [Word] {
        selectedBucket == .know ? viewModel.knownWords : viewModel.unknownWords
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack(alignment: .bottom) {
                AppBackground()

                VStack(spacing: 0) {
                    header
                        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                        .padding(.top, AppSizing.spacing(3, for: sizeClass))

                    bucketPills
                        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                        .padding(.top, AppSizing.spacing(4, for: sizeClass))

                    wordList
                        .padding(.top, AppSizing.spacing(3, for: sizeClass))
                }

                // Floating CTA — fixed to the screen, not the scroll content,
                // so it stays reachable at any list length. Sits just above
                // the floating tab bar and intentionally overlaps whatever
                // list content scrolls underneath it.
                studyButton
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                    .padding(.bottom, AppSizing.tabBarClearance(sizeClass))
            }
            .navigationBarHidden(true)
            .navigationDestination(for: WordsRoute.self) { route in
                switch route {
                case .startLearning:
                    StartLearningScreen(onSelectDifficulty: { level in
                        path.append(.flashcards(
                            title: "\(level.displayName) words",
                            words: viewModel.randomWords(for: level)
                        ))
                    })
                case .flashcards(let title, let words):
                    FlashcardDeckScreen(
                        title: title,
                        words: words,
                        onKnow: viewModel.markKnown,
                        onDontKnow: viewModel.markDontKnow,
                        onToggleSaved: viewModel.toggleSaved,
                        requiresSignInToSave: viewModel.sessionRequiresSignInToSave
                    )
                }
            }
        }
        .task {
            guard let uid = authViewModel.userProfile?.id else { return }
            await viewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
        }
    }

    // MARK: Header

    private var header: some View {
        HStack {
            Text("My Words")
                .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass), weight: .heavy))
                .foregroundColor(.white)

            Spacer()

            Button {
                withAnimation(.easeInOut(duration: 0.15)) {
                    viewModel.showSavedOnly.toggle()
                }
            } label: {
                Image(systemName: viewModel.showSavedOnly ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                    .font(.system(size: AppSizing.iconSize(sizeClass)))
                    .foregroundColor(.white)
            }
            .accessibilityLabel("Filter: show saved words only")

            Button {
                withAnimation(.easeInOut(duration: 0.15)) {
                    viewModel.sortAlphabetically.toggle()
                }
            } label: {
                Image(systemName: "arrow.up.arrow.down")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.85))
                    .foregroundColor(.white)
            }
            .accessibilityLabel("Sort alphabetically")
            .padding(.leading, AppSizing.spacing(3, for: sizeClass))
        }
    }

    // MARK: Bucket pills

    private var bucketPills: some View {
        HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            pillButton(
                title: "I don't know",
                count: viewModel.unknownWords.count,
                systemImage: "xmark",
                tint: AppColors.link,
                isSelected: selectedBucket == .dontKnow
            ) {
                withAnimation(.easeInOut(duration: 0.15)) { selectedBucket = .dontKnow }
            }

            pillButton(
                title: "I know",
                count: viewModel.knownWords.count,
                systemImage: "checkmark",
                tint: AppColors.accentRed,
                isSelected: selectedBucket == .know
            ) {
                withAnimation(.easeInOut(duration: 0.15)) { selectedBucket = .know }
            }
        }
    }

    private func pillButton(
        title: String,
        count: Int,
        systemImage: String,
        tint: Color,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                Image(systemName: systemImage)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .bold))
                    .foregroundColor(isSelected ? .white : tint)

                Text(title)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                    .foregroundColor(isSelected ? .white : AppColors.primaryText)

                Text("\(count)")
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .foregroundColor(isSelected ? AppColors.primaryButton : AppColors.primaryText)
                    .padding(.horizontal, AppSizing.spacing(2, for: sizeClass))
                    .padding(.vertical, 2)
                    .background(isSelected ? Color.white : AppColors.fieldBorder)
                    .clipShape(Capsule())
            }
            .padding(.horizontal, AppSizing.spacing(3, for: sizeClass))
            .frame(maxWidth: .infinity)
            .frame(height: AppSizing.buttonHeight(sizeClass) * 0.8)
            .background(isSelected ? AppColors.primaryButton : AppColors.cardBackground)
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.10), radius: 6, y: 3)
        }
    }

    // MARK: Word list

    private var wordList: some View {
        Group {
            if displayedWords.isEmpty {
                Spacer()
                Text(viewModel.showSavedOnly ? "No saved words here yet." : "Nothing in this list yet.")
                    .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                    .foregroundColor(.white.opacity(0.85))
                Spacer()
            } else {
                List {
                    ForEach(displayedWords) { word in
                        wordRow(word)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets(
                                top: AppSizing.spacing(1, for: sizeClass),
                                leading: AppSizing.horizontalPadding(sizeClass),
                                bottom: AppSizing.spacing(1, for: sizeClass),
                                trailing: AppSizing.horizontalPadding(sizeClass)
                            ))
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    withAnimation {
                                        viewModel.deleteWord(word)
                                    }
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }

                    Text("You reached the end 🎉")
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.85))
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                        .padding(.top, AppSizing.spacing(4, for: sizeClass))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)

                    // Reserves space so the last row and the "reached the
                    // end" text can scroll clear of the floating Study
                    // words button + tab bar instead of sitting under them.
                    Color.clear
                        .frame(height: AppSizing.buttonHeight(sizeClass) + AppSizing.tabBarClearance(sizeClass))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
    }

    private func wordRow(_ word: Word) -> some View {
        HStack {
            Text(word.text)
                .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                .foregroundColor(AppColors.primaryText)

            Spacer()

            Button {
                viewModel.toggleSaved(bookmarkToggled(word))
            } label: {
                Image(systemName: word.isSaved ? "bookmark.fill" : "bookmark")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7))
                    .foregroundColor(word.isSaved ? AppColors.primaryButton : AppColors.placeholderText)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
        .frame(height: AppSizing.buttonHeight(sizeClass))
        .background(AppColors.cardBackground)
        .cornerRadius(AppSizing.smallCornerRadius(sizeClass))
        .contentShape(Rectangle())
        .onTapGesture {
            // Tapping a word jumps straight into a deck scoped to this
            // bucket (skipping the Start Learning intro).
            let bucketTitle = selectedBucket == .know ? "Words I know" : "Words I don't know"
            path.append(.flashcards(title: bucketTitle, words: displayedWords))
        }
    }

    // MARK: Study button

    private var studyButton: some View {
        Button {
            path.append(.startLearning)
        } label: {
            HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                Text("Study words")
                Image(systemName: "arrow.right")
            }
            .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: AppSizing.buttonHeight(sizeClass))
            .background(AppColors.primaryButton)
            .cornerRadius(AppSizing.buttonHeight(sizeClass) / 2)
            .shadow(color: .black.opacity(0.25), radius: 12, y: 6)
        }
    }

    // MARK: Helpers

    private func bookmarkToggled(_ word: Word) -> Word {
        var updated = word
        updated.isSaved.toggle()
        return updated
    }
}

#if DEBUG
#Preview {
    WordsScreen(path: .constant([]))
        .environmentObject(AuthViewModel())
        .previewSizeClass()
}
#endif
