import SwiftUI

struct DeleteAccountScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var password = ""
    @State private var isPasswordVisible = false
    @State private var confirmationText = ""
    @State private var showConfirmAlert = false
    @State private var localError: String?

    private let requiredConfirmation = "DELETE"

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                ScreenHeader(title: "Delete Account", onBack: { dismiss() })

                ScrollView {
                    VStack(spacing: 24) {
                        VStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(Color.white.opacity(0.2))
                                    .frame(width: 90, height: 90)
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 34))
                                    .foregroundColor(.white)
                            }
                            .padding(.top, 20)

                            Text("Delete Your Account")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)

                            Text("This action is permanent. All your progress,\ncourses, and data will be erased and\ncannot be recovered.")
                                .font(.system(size: 14))
                                .multilineTextAlignment(.center)
                                .foregroundColor(.white.opacity(0.9))
                                .padding(.horizontal, 30)
                        }

                        AuthCard {
                            LabeledField(title: "Confirm Password") {
                                AppSecureField(placeholder: "••••••••", text: $password, isVisible: $isPasswordVisible)
                            }
                            LabeledField(title: "Type DELETE to confirm") {
                                AppTextField(placeholder: "DELETE", text: $confirmationText)
                            }

                            if let message = localError ?? authViewModel.errorMessage {
                                Text(message)
                                    .font(.system(size: 12))
                                    .foregroundColor(.red)
                            }

                            PrimaryButton(
                                title: authViewModel.isLoading ? "Deleting..." : "Delete My Account",
                                tint: .red
                            ) {
                                validateAndConfirm()
                            }
                            .disabled(authViewModel.isLoading)
                            .padding(.top, 4)
                        }

                        Text("or")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.white.opacity(0.85))

                        PrimaryButton(title: "Keep My Account", icon: "checkmark.circle") {
                            dismiss()
                        }
                        .padding(.horizontal, 20)

                        Spacer(minLength: 30)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .alert("Delete Account?", isPresented: $showConfirmAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                deleteAccount()
            }
        } message: {
            Text("This can't be undone. Are you absolutely sure you want to permanently delete your account?")
        }
    }

    private func validateAndConfirm() {
        localError = nil
        guard !password.isEmpty else {
            localError = "Please enter your password."
            return
        }
        guard confirmationText == requiredConfirmation else {
            localError = "Please type DELETE to confirm."
            return
        }
        showConfirmAlert = true
    }

    private func deleteAccount() {
        Task {
            // No need to manually flip any navigation flag here — AuthViewModel
            // sets isAuthenticated = false on success, and RootView reacts to
            // that automatically.
            await authViewModel.deleteAccount(password: password)
        }
    }
}
