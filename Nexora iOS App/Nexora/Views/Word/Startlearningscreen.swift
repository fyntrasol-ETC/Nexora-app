//
//  StartLearningScreen.swift
//  Elli
//
//  The interstitial screen between "Study words" and the flashcard deck.
//  Lets the person pick a level; each level pulls 10 random words for
//  that difficulty and opens the flashcard deck scoped to them.
//

import SwiftUI

struct StartLearningScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @Environment(\.dismiss) private var dismiss

    let onSelectDifficulty: (Word.Difficulty) -> Void

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                HStack {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: AppSizing.iconSize(sizeClass)))
                            .foregroundColor(.white)
                    }
                    Spacer()
                }
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                .padding(.top, AppSizing.spacing(3, for: sizeClass))

                Spacer()

                VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                    Text("Start Learning")
                        .font(.system(size: AppSizing.font(.logo, for: sizeClass), weight: .heavy))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)

                    Text("Choose a level to begin")
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.85))
                }

                Spacer()

                VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
                    ForEach(Word.Difficulty.allCases, id: \.self) { level in
                        Button {
                            onSelectDifficulty(level)
                        } label: {
                            HStack {
                                Text(level.displayName)
                                    .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
                                    .foregroundColor(AppColors.primaryText)

                                Spacer()

                                Image(systemName: "arrow.right")
                                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .semibold))
                                    .foregroundColor(AppColors.primaryButton)
                            }
                            .padding(.horizontal, AppSizing.spacing(5, for: sizeClass))
                            .frame(height: AppSizing.buttonHeight(sizeClass))
                            .background(Color.white)
                            .cornerRadius(AppSizing.buttonHeight(sizeClass) / 2)
                            .shadow(color: .black.opacity(0.12), radius: 8, y: 4)
                        }
                    }
                }
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))

                Spacer()
                Spacer()
            }
        }
        .navigationBarHidden(true)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        StartLearningScreen(onSelectDifficulty: { _ in })
    }
    .previewSizeClass()
}
#endif
