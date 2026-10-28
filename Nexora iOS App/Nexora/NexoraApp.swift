import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct NexoraApp: App {

    @StateObject private var authViewModel: AuthViewModel

    init() {
        // Initialize Firebase before creating AuthViewModel
        FirebaseApp.configure()

        // Configure Google Sign-In using the client ID Firebase
        // already knows about from GoogleService-Info.plist.
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        }

        _authViewModel = StateObject(
            wrappedValue: AuthViewModel()
        )
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(authViewModel)
                .onOpenURL { url in
                    // Routes Google's OAuth redirect back into the SDK.
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
