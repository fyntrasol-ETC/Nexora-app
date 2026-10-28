//
//  EditProfileViewModel.swift
//  Nexora
//
//  Mirrors Android's EditProfileViewModel: nameInput is edited locally and
//  only written on Save; avatar changes are not gated by Save and persist
//  the moment they're picked. Writes go straight to users/{uid} (same
//  fields UserProfile.swift already declares), and — since this app
//  doesn't have a live per-screen listener on the profile doc like
//  Android's userRepository.observeUser Flow — this view model also
//  updates the shared AuthViewModel.userProfile in place after a
//  successful write, so the change shows up immediately everywhere else
//  in the app (e.g. the avatar in ProfileScreen's header) without
//  waiting for a fresh fetch.
//
//  MIGRATION: avatar is now a DiceBear seed string (see AvatarOptions.swift),
//  not a MascotAvatarType case — replaces the old avatarType/selectAvatar(_:
//  MascotAvatarType) shape.
//

import Foundation
import FirebaseFirestore
import Combine

@MainActor
final class EditProfileViewModel: ObservableObject {

    @Published var nameInput: String = ""
    @Published private(set) var storedName: String = ""
    @Published private(set) var avatarSeed: String = AvatarOptions.defaultSeed
    @Published private(set) var isSaving: Bool = false
    @Published var errorMessage: String?

    private weak var authViewModel: AuthViewModel?
    private let db = Firestore.firestore()
    private var hasBound = false

    /// Drives the Save button's enabled state — same rule as Android:
    /// non-blank and actually different from what's stored.
    var isNameChanged: Bool {
        !nameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && nameInput != storedName
    }

    /// Seeds this view model from the shared AuthViewModel the first time
    /// Edit Profile is opened. Guarded by hasBound so navigating back into
    /// this screen later doesn't stomp on text the person is actively
    /// typing with a stale snapshot — same "seed once" contract Android's
    /// nameInput.ifBlank { ... } gives it.
    func bind(to authViewModel: AuthViewModel) {
        self.authViewModel = authViewModel
        guard !hasBound else { return }
        hasBound = true

        if let profile = authViewModel.userProfile {
            storedName = profile.displayName
            nameInput = profile.displayName
            avatarSeed = AvatarOptions.resolveSeed(profile.avatarSeed)
        }
    }

    func save() {
        guard isNameChanged,
              authViewModel?.isRealAccount == true,
              let uid = authViewModel?.userProfile?.id else { return }

        let newName = nameInput
        isSaving = true
        errorMessage = nil

        Task {
            do {
                try await db.collection("users").document(uid)
                    .setData(["displayName": newName], merge: true)

                storedName = newName
                authViewModel?.userProfile?.displayName = newName
            } catch {
                errorMessage = "Could not save your name. Please try again."
            }
            isSaving = false
        }
    }

    /// Persists immediately — per the spec (and matching Android), avatar
    /// changes aren't gated by Save.
    func selectAvatar(_ seed: String) {
        guard authViewModel?.isRealAccount == true,
              let uid = authViewModel?.userProfile?.id else { return }

        avatarSeed = seed
        authViewModel?.userProfile?.avatarSeed = seed

        Task {
            try? await db.collection("users").document(uid)
                .setData(["avatarSeed": seed], merge: true)
        }
    }
}
