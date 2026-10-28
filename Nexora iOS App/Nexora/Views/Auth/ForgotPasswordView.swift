import SwiftUI

struct ForgotPasswordScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appSizeClass) private var sizeClass
    @State private var email = ""
    @State private var linkSent = false
    @State private var localError: String?

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                AuthTopBar(showBackButton: false)

                // Flexible space above and below the form centers it
                // vertically on a tall iPad canvas instead of leaving it
                // pinned under the top bar with empty gradient below.
                Spacer(minLength: AppSizing.spacing(4, for: sizeClass))

                VStack(spacing: AppSizing.spacing(5, for: sizeClass)) {
                    ZStack {
                        Circle()
                            .fill(Color.white.opacity(0.2))
                            .frame(width: 90 * sizeClass.scale, height: 90 * sizeClass.scale)
                        Image(systemName: "lock.rotation")
                            .font(.system(size: AppSizing.font(.title, for: sizeClass)))
                            .foregroundColor(.white)
                    }

                    Text("Reset Password")
                        .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass), weight: .bold))
                        .foregroundColor(.white)

                    Text("Enter your email address below and we'll\nsend you a secure link to reset your\naccount password.")
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white.opacity(0.9))
                        .frame(maxWidth: AppSizing.maxContentWidth(sizeClass) * 0.6)

                    AuthCard {
                        LabeledField(title: "Email Address") {
                            AppTextField(placeholder: "name@example.com", text: $email)
                        }

                        PrimaryButton(title: authViewModel.isLoading ? "Sending..." : "Send Link", icon: "arrow.right") {
                            sendResetLink()
                        }
                        .disabled(authViewModel.isLoading)

                        if let message = localError ?? authViewModel.errorMessage {
                            Text(message)
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                .foregroundColor(.red)
                        } else if linkSent {
                            Text("If an account exists for that email, a reset link is on its way.")
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                .foregroundColor(.green)
                        }
                    }
                    .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))

                    Button {
                        dismiss()
                    } label: {
                        HStack(spacing: AppSizing.spacing(1, for: sizeClass)) {
                            Image(systemName: "chevron.left")
                            Text("Back to Login")
                        }
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .medium))
                        .foregroundColor(.white)
                    }
                    .padding(.top, AppSizing.spacing(1.5, for: sizeClass))

                }

                Spacer(minLength: AppSizing.spacing(4, for: sizeClass))
            }
        }
        .navigationBarHidden(true)
    }

    private func sendResetLink() {
        localError = nil
        linkSent = false
        guard !email.isEmpty else {
            localError = "Please enter your email address."
            return
        }
        Task {
            await authViewModel.sendPasswordReset(email: email)
            linkSent = authViewModel.errorMessage == nil
        }
    }
}
