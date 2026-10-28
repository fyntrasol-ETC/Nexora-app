import Foundation
import FirebaseAuth
import FirebaseFirestore
import Combine
import GoogleSignIn
import AuthenticationServices
import CryptoKit
import UIKit

@MainActor
final class AuthViewModel: NSObject, ObservableObject {

    // MARK: - Published State

    @Published var userProfile: UserProfile?
    @Published var isAuthenticated: Bool = false
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    @Published var isAnonymous: Bool = false

    var isRealAccount: Bool { isAuthenticated && !isAnonymous }

    @Published var isInitializing: Bool = true

    // MARK: - Firebase

    private let db = Firestore.firestore()

    private var authStateHandle: AuthStateDidChangeListenerHandle?

    private var profileListenerHandle: ListenerRegistration?

    // MARK: - Apple Sign-In

    private var currentAppleNonce: String?

    // MARK: - User Subcollections

    private let userSubcollections = [
        "courseProgress",
        "words",
        "videoActivity",
        "badges",
        "weeklyActivity"
    ]

    // MARK: - Initialization

    override init() {

        super.init()

        Self.signOutIfFreshInstall()

        authStateHandle = Auth.auth().addStateDidChangeListener {
            [weak self] _, user in

            guard let self else {
                return
            }

            Task { @MainActor in
                await self.handleAuthChange(user: user)
            }
        }
    }

    deinit {
        if let authStateHandle {
            Auth.auth().removeStateDidChangeListener(authStateHandle)
        }
        profileListenerHandle?.remove()
    }

    // MARK: - Fresh Install Detection

    private static func signOutIfFreshInstall() {

        let hasLaunchedBeforeKey = "com.elli.hasLaunchedBefore"
        let defaults = UserDefaults.standard

        guard !defaults.bool(forKey: hasLaunchedBeforeKey) else {
            return
        }

        try? Auth.auth().signOut()

        defaults.set(true, forKey: hasLaunchedBeforeKey)
    }

    // MARK: - Authentication State

    private func handleAuthChange(user: FirebaseAuth.User?) async {

        defer {
            isInitializing = false
        }

        guard let user else {

            stopListeningToProfile()
            userProfile = nil
            isAuthenticated = false
            isAnonymous = false

            return
        }

        isAuthenticated = true
        isAnonymous = user.isAnonymous

        await listenToProfile(uid: user.uid)
    }

    // MARK: - Anonymous Session

    func signInAnonymously() async {

        guard Auth.auth().currentUser == nil else { return }

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {
            try await Auth.auth().signInAnonymously()
        } catch {
            errorMessage = "Couldn't start your session: \(error.localizedDescription)"
        }
    }

    // MARK: - Anonymous → Real Account Upgrade

    private func linkOrSignIn(with credential: AuthCredential) async throws {

        if let currentUser = Auth.auth().currentUser, currentUser.isAnonymous {

            do {
                try await currentUser.link(with: credential)
                try await syncAfterCredentialChange()
                return
            } catch let error as NSError {

                let code = AuthErrorCode(rawValue: error.code)
                guard code == .credentialAlreadyInUse || code == .emailAlreadyInUse else {
                    throw error
                }

                if let updatedCredential = error.userInfo[AuthErrorUserInfoUpdatedCredentialKey] as? AuthCredential {
                    try await Auth.auth().signIn(with: updatedCredential)
                    try await syncAfterCredentialChange()
                    return
                }

                throw error
            }
        }

        try await Auth.auth().signIn(with: credential)
        try await syncAfterCredentialChange()
    }

    private func syncAfterCredentialChange() async throws {
        try await Auth.auth().currentUser?.getIDTokenResult(forcingRefresh: true)
        await handleAuthChange(user: Auth.auth().currentUser)
    }

    // MARK: - Profile Listener

    private func listenToProfile(uid: String) async {

        stopListeningToProfile()

        do {

            let snapshot = try await db
                .collection("users")
                .document(uid)
                .getDocument()

            if !snapshot.exists,
               let firebaseUser = Auth.auth().currentUser {

                let profile = UserProfile.newProfile(
                    uid: firebaseUser.uid,
                    displayName: firebaseUser.displayName ?? "",
                    email: firebaseUser.email ?? ""
                )

                try db
                    .collection("users")
                    .document(uid)
                    .setData(from: profile)
            }

        } catch {

            errorMessage =
                "Couldn't load profile: \(error.localizedDescription)"
        }

        profileListenerHandle = db
            .collection("users")
            .document(uid)
            .addSnapshotListener { [weak self] snapshot, error in

                guard let self else {
                    return
                }

                Task { @MainActor in

                    if let error {

                        self.errorMessage =
                            "Couldn't load profile: \(error.localizedDescription)"
                        return
                    }

                    guard let snapshot, snapshot.exists else {
                        return
                    }

                    do {

                        self.userProfile = try snapshot.data(
                            as: UserProfile.self
                        )

                    } catch {

                        self.errorMessage =
                            "Couldn't load profile: \(error.localizedDescription)"
                    }
                }
            }
    }

    private func stopListeningToProfile() {
        profileListenerHandle?.remove()
        profileListenerHandle = nil
    }

    // MARK: - Password Validation

    static func validatePassword(_ password: String) -> String? {

        guard password.count >= 8 else {
            return "Password must be at least 8 characters long."
        }

        let hasLetter = password.range(
            of: "[A-Za-z]",
            options: .regularExpression
        ) != nil

        guard hasLetter else {
            return "Password must contain at least one alphabet."
        }

        let hasNumber = password.range(
            of: "[0-9]",
            options: .regularExpression
        ) != nil

        guard hasNumber else {
            return "Password must contain at least one number."
        }

        let hasSpecialCharacter = password.range(
            of: "[^A-Za-z0-9]",
            options: .regularExpression
        ) != nil

        guard hasSpecialCharacter else {
            return "Password must contain at least one special character."
        }

        return nil
    }

    // MARK: - Sign In

    func signIn(
        email: String,
        password: String
    ) async {

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {

            try await Auth.auth().signIn(
                withEmail: email,
                password: password
            )

        } catch {

            errorMessage = friendlyAuthError(error)
        }
    }

    // MARK: - Sign In With Google

    func signInWithGoogle() async {

        guard let rootViewController = Self.topViewController() else {
            errorMessage = "Unable to present Google Sign-In."
            return
        }

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {

            let result = try await GIDSignIn.sharedInstance.signIn(
                withPresenting: rootViewController
            )

            guard let idToken = result.user.idToken?.tokenString else {
                errorMessage = "Google Sign-In failed: missing ID token."
                return
            }

            // Grab the name from Google's own profile up front — this is
            // the only reliable source, since the credential itself
            // carries no name.
            let googleName = result.user.profile?.name

            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: result.user.accessToken.tokenString
            )

            try await linkOrSignIn(with: credential)

            guard let uid = Auth.auth().currentUser?.uid else { return }

            // Keep Auth.auth().currentUser?.displayName in sync too, for
            // anything that reads it directly rather than from Firestore.
            if let googleName,
               let user = Auth.auth().currentUser,
               user.displayName != googleName {

                let changeRequest = user.createProfileChangeRequest()
                changeRequest.displayName = googleName
                try? await changeRequest.commitChanges()
            }

            // FIX: The guest session already seeded a blank users/{uid}
            // doc back when signInAnonymously() ran (see
            // listenToProfile's "create if missing" branch) — so by the
            // time Google gets linked here, that doc already exists and
            // listenToProfile will NOT touch its displayName again. Push
            // the Google name in explicitly, same pattern signUp() uses
            // for the email/password path.
            if let googleName, !googleName.isEmpty {

                try? await db.collection("users").document(uid).setData(
                    ["displayName": googleName],
                    merge: true
                )
            }

            // listenToProfile's live snapshot listener (already attached
            // via handleAuthChange -> listenToProfile) will pick up this
            // write automatically and update `userProfile`.

        } catch {

            if (error as NSError).code == GIDSignInError.canceled.rawValue {
                return
            }

            errorMessage = friendlyAuthError(error)
        }
    }
    // MARK: - Sign In With Apple

    func signInWithApple() {

        let nonce = Self.randomNonceString()
        currentAppleNonce = nonce

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    // MARK: - Apple Sign-In Helpers

    private static func topViewController(
        _ base: UIViewController? = UIApplication.shared
            .connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?.rootViewController
    ) -> UIViewController? {

        if let nav = base as? UINavigationController {
            return topViewController(nav.visibleViewController)
        }
        if let tab = base as? UITabBarController,
           let selected = tab.selectedViewController {
            return topViewController(selected)
        }
        if let presented = base?.presentedViewController {
            return topViewController(presented)
        }
        return base
    }

    private static func randomNonceString(length: Int = 32) -> String {

        let charset: [Character] =
            Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")

        var result = ""
        var remainingLength = length

        while remainingLength > 0 {

            let randoms: [UInt8] = (0..<16).map { _ in
                var random: UInt8 = 0
                _ = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                return random
            }

            randoms.forEach { random in

                guard remainingLength > 0 else { return }

                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }

        return result
    }

    private static func sha256(_ input: String) -> String {

        let inputData = Data(input.utf8)
        let hashed = SHA256.hash(data: inputData)

        return hashed.compactMap { String(format: "%02x", $0) }.joined()
    }

    // MARK: - Sign Up

    func signUp(
        fullName: String,
        email: String,
        password: String
    ) async {

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        if let passwordError = Self.validatePassword(password) {

            errorMessage = passwordError
            return
        }

        do {

            let credential = EmailAuthProvider.credential(
                withEmail: email,
                password: password
            )

            try await linkOrSignIn(with: credential)

            guard let uid = Auth.auth().currentUser?.uid else { return }

            let profileRef = db.collection("users").document(uid)
            let existingSnapshot = try await profileRef.getDocument()

            if existingSnapshot.exists {

                try await profileRef.setData(
                    ["displayName": fullName, "email": email],
                    merge: true
                )

            } else {

                let profile = UserProfile.newProfile(
                    uid: uid,
                    displayName: fullName,
                    email: email
                )

                try profileRef.setData(from: profile)
            }

        } catch {

            errorMessage = friendlyAuthError(error)
        }
    }

    // MARK: - Sign Out

    func signOut() {

        do {

            try Auth.auth().signOut()
            GIDSignIn.sharedInstance.signOut()

        } catch {

            errorMessage = error.localizedDescription
        }
    }

    // MARK: - Password Reset

    func sendPasswordReset(email: String) async {

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {

            try await Auth.auth().sendPasswordReset(
                withEmail: email
            )

        } catch {

            errorMessage = friendlyAuthError(error)
        }
    }

    // MARK: - Change Password

    func changePassword(
        currentPassword: String,
        newPassword: String
    ) async -> Bool {

        guard
            let user = Auth.auth().currentUser,
            let email = user.email
        else {

            errorMessage = "No signed-in user."
            return false
        }

        if let passwordError = Self.validatePassword(newPassword) {

            errorMessage = passwordError
            return false
        }

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {

            let credential = EmailAuthProvider.credential(
                withEmail: email,
                password: currentPassword
            )

            try await user.reauthenticate(
                with: credential
            )

            try await user.updatePassword(
                to: newPassword
            )

            return true

        } catch {

            errorMessage = friendlyAuthError(error)
            return false
        }
    }

    // MARK: - Delete Account

    func deleteAccount(
        password: String
    ) async -> Bool {

        guard
            let user = Auth.auth().currentUser,
            let email = user.email
        else {

            errorMessage = "No signed-in user."
            return false
        }

        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        do {

            let credential = EmailAuthProvider.credential(
                withEmail: email,
                password: password
            )

            try await user.reauthenticate(
                with: credential
            )

            let uid = user.uid

            stopListeningToProfile()

            for subcollection in userSubcollections {

                try await deleteAllDocs(
                    in: db
                        .collection("users")
                        .document(uid)
                        .collection(subcollection)
                )
            }

            try await db
                .collection("users")
                .document(uid)
                .delete()

            try await user.delete()

            userProfile = nil
            isAuthenticated = false

            return true

        } catch {

            errorMessage = friendlyAuthError(error)
            return false
        }
    }

    // MARK: - Delete Collection Documents

    private func deleteAllDocs(
        in collection: CollectionReference
    ) async throws {

        let snapshot = try await collection.getDocuments()

        guard !snapshot.documents.isEmpty else {
            return
        }

        for chunk in snapshot.documents.chunked(into: 500) {

            let batch = db.batch()

            for document in chunk {

                batch.deleteDocument(
                    document.reference
                )
            }

            try await batch.commit()
        }
    }

    // MARK: - Friendly Firebase Errors

    private func friendlyAuthError(
        _ error: Error
    ) -> String {

        let nsError = error as NSError

        guard
            let errorCode = AuthErrorCode(
                rawValue: nsError.code
            )
        else {
            return error.localizedDescription
        }

        switch errorCode {

        case .invalidEmail:
            return "Please enter a valid email address."

        case .wrongPassword:
            return "Incorrect password."

        case .userNotFound:
            return "No account was found with this email."

        case .emailAlreadyInUse:
            return "An account already exists with this email."

        case .weakPassword:
            return "Password is too weak."

        case .networkError:
            return "Please check your internet connection."

        case .tooManyRequests:
            return "Too many attempts. Please try again later."

        case .userDisabled:
            return "This account has been disabled."

        case .invalidCredential:
            return "The email or password is incorrect."

        default:
            return error.localizedDescription
        }
    }
}

// MARK: - Apple Sign-In Delegates

extension AuthViewModel: ASAuthorizationControllerDelegate {

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {

        guard
            let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let identityTokenData = appleIDCredential.identityToken,
            let identityToken = String(data: identityTokenData, encoding: .utf8)
        else {
            Task { @MainActor in
                self.errorMessage = "Apple Sign-In failed: missing identity token."
            }
            return
        }

        Task { @MainActor in

            guard let nonce = self.currentAppleNonce else {
                self.errorMessage = "Apple Sign-In failed: missing nonce."
                return
            }

            self.isLoading = true
            self.errorMessage = nil

            defer { self.isLoading = false }

            let credential = OAuthProvider.appleCredential(
                withIDToken: identityToken,
                rawNonce: nonce,
                fullName: appleIDCredential.fullName
            )

            do {
                try await self.linkOrSignIn(with: credential)
            } catch {
                print("🔴 Apple Sign-In error:", error)
                self.errorMessage = self.friendlyAuthError(error)
            }
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        let nsError = error as NSError
        guard nsError.code != ASAuthorizationError.canceled.rawValue else {
            return
        }
        Task { @MainActor in
            self.errorMessage = error.localizedDescription
        }
    }
}

extension AuthViewModel: ASAuthorizationControllerPresentationContextProviding {

    nonisolated func presentationAnchor(
        for controller: ASAuthorizationController
    ) -> ASPresentationAnchor {

        MainActor.assumeIsolated {
            UIApplication.shared
                .connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow }
                .first ?? ASPresentationAnchor()
        }
    }
}

// MARK: - Array Chunking

private extension Array {

    func chunked(into size: Int) -> [[Element]] {

        stride(
            from: 0,
            to: count,
            by: size
        ).map {

            Array(
                self[
                    $0..<Swift.min(
                        $0 + size,
                        count
                    )
                ]
            )
        }
    }
}
