//
//  ChangeAvatarScreen.swift
//  Nexora
//
//  Matches Android's ChangeAvatarScreen: back + "Change Avatar" header, a
//  4-column grid of the curated DiceBear seeds, and a "Select avatar"
//  button at the bottom.
//
//  Selection is staged locally (pendingSelection) exactly like Android —
//  tapping a tile just highlights it; the write only happens when "Select
//  avatar" is tapped, via EditProfileViewModel.selectAvatar(_:), so
//  browsing the grid never fires a Firestore write on every tap.
//
//  MIGRATION: grid source is now AvatarOptions.curatedSeeds (DiceBear seed
//  strings) instead of MascotAvatarType.allCases, and tiles are
//  DiceBearAvatarOption instead of the old hand-drawn AvatarGridOption.
//

import SwiftUI

struct ChangeAvatarScreen: View {
    @ObservedObject var editProfileViewModel: EditProfileViewModel
    @Binding var path: NavigationPath

    @State private var pendingSelection: String

    init(editProfileViewModel: EditProfileViewModel, path: Binding<NavigationPath>) {
        self.editProfileViewModel = editProfileViewModel
        self._path = path
        self._pendingSelection = State(initialValue: editProfileViewModel.avatarSeed)
    }

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 16), count: 4)

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                ScreenHeader(title: "Change Avatar", onBack: { path.removeLast() })

                ScrollView {
                    LazyVGrid(columns: columns, spacing: 18) {
                        ForEach(AvatarOptions.curatedSeeds, id: \.self) { seed in
                            DiceBearAvatarOption(
                                seed: seed,
                                selected: seed == pendingSelection,
                                onTap: { pendingSelection = seed }
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 16)
                }

                Button {
                    editProfileViewModel.selectAvatar(pendingSelection)
                    path.removeLast()
                } label: {
                    Text("Select avatar")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(AppColors.primaryButton)
                        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
        }
        .navigationBarHidden(true)
    }
}
