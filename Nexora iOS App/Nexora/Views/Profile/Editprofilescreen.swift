//
//  EditProfileScreen.swift
//  Nexora
//
//  Matches Android's EditProfileScreen: back + "Edit profile" header,
//  centered avatar with a "Change avatar" pill beneath it, a Name field,
//  and a Save button gated on the name actually having changed.
//
//  Reuses this app's existing AppBackground / ScreenHeader / AppColors —
//  same components SettingsScreen.swift already relies on — so this slots
//  into the same visual language without new shared infrastructure.
//

import SwiftUI

struct EditProfileScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @ObservedObject var editProfileViewModel: EditProfileViewModel
    @Binding var path: NavigationPath

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                ScreenHeader(title: "Edit profile", onBack: { path.removeLast() })

                ScrollView {
                    VStack(spacing: 28) {
                        avatarSection
                        nameSection
                        saveButton
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 28)
                }
            }
        }
        .navigationBarHidden(true)
        // Seeds the view model from the shared AuthViewModel the first
        // time this screen appears — see EditProfileViewModel.bind(to:).
        .task {
            editProfileViewModel.bind(to: authViewModel)
        }
    }

    private var avatarSection: some View {
        VStack(spacing: 14) {
            DiceBearAvatar(seed: editProfileViewModel.avatarSeed, size: 96)
                .shadow(color: .black.opacity(0.2), radius: 10, y: 4)

            Button {
                path.append(ProfileRoute.changeAvatar)
            } label: {
                Text("Change avatar")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.primaryButton)
                    .padding(.horizontal, 22)
                    .padding(.vertical, 12)
                    .background(Color.white)
                    .clipShape(Capsule())
                    .shadow(color: .black.opacity(0.12), radius: 6, y: 3)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var nameSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Name")
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.85))

            TextField("", text: $editProfileViewModel.nameInput)
                .font(.system(size: 16))
                .foregroundColor(AppColors.primaryText)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: .black.opacity(0.1), radius: 6, y: 3)

            if let error = editProfileViewModel.errorMessage {
                Text(error)
                    .font(.system(size: 13))
                    .foregroundColor(.red)
            }
        }
    }

    private var saveButton: some View {
        Button {
            editProfileViewModel.save()
        } label: {
            ZStack {
                if editProfileViewModel.isSaving {
                    ProgressView().tint(.white)
                } else {
                    Text("Save")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(AppColors.primaryButton)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .disabled(!editProfileViewModel.isNameChanged || editProfileViewModel.isSaving)
        .opacity(editProfileViewModel.isNameChanged && !editProfileViewModel.isSaving ? 1 : 0.5)
    }
}
