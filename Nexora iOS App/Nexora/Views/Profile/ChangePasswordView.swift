import SwiftUI

struct ChangePasswordScreen: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var isCurrentVisible = false
    @State private var isNewVisible = false
    @State private var isConfirmVisible = false
    @State private var localError: String?
    @State private var successMessage: String?

    private var hasMinLength: Bool { newPassword.count >= 8 }
    private var hasMixedChars: Bool {
        let hasLetter = newPassword.contains { $0.isLetter }
        let hasDigit = newPassword.contains { $0.isNumber }
        let hasSymbol = newPassword.contains { !$0.isLetter && !$0.isNumber }
        return hasLetter && hasDigit && hasSymbol
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                ScreenHeader(
                    title: "Change Password",
                    subtitle: "Protect your account by creating a strong, unique\npassword for Ellie.",
                    onBack: { dismiss() }
                )

                ScrollView {
                    VStack(spacing: 20) {
                        AuthCard {
                            LabeledField(title: "Current Password") {
                                AppSecureField(placeholder: "Enter your current password", text: $currentPassword, isVisible: $isCurrentVisible)
                            }
                            LabeledField(title: "New Password") {
                                AppSecureField(placeholder: "Create a new password", text: $newPassword, isVisible: $isNewVisible)
                            }
                            LabeledField(title: "Confirm New Password") {
                                AppSecureField(placeholder: "Re-type your new password", text: $confirmPassword, isVisible: $isConfirmVisible)
                            }

                            passwordRequirements

                            if let message = localError ?? authViewModel.errorMessage {
                                Text(message)
                                    .font(.system(size: 12))
                                    .foregroundColor(.red)
                            }
                            if let successMessage {
                                Text(successMessage)
                                    .font(.system(size: 12))
                                    .foregroundColor(.green)
                            }
                        }
                        .padding(.top, 24)

                        PrimaryButton(
                            title: authViewModel.isLoading ? "Updating..." : "Update Password",
                            icon: "arrow.triangle.2.circlepath"
                        ) {
                            updatePassword()
                        }
                        .disabled(authViewModel.isLoading)
                        .padding(.horizontal, 20)

                        Button("Cancel") { dismiss() }
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)

                        Text("Changing your password will log you out from all other\ndevices for security purposes.")
                            .font(.system(size: 12))
                            .multilineTextAlignment(.center)
                            .foregroundColor(.white.opacity(0.85))
                            .padding(.horizontal, 30)

                        Spacer(minLength: 30)
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }

    private var passwordRequirements: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Password must contain")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(AppColors.primaryText)
            requirementRow("At least 8 characters long", met: hasMinLength)
            requirementRow("A mix of letters, numbers & symbols", met: hasMixedChars)
        }
    }

    private func requirementRow(_ text: String, met: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: met ? "checkmark.circle.fill" : "circle")
                .foregroundColor(met ? .green : AppColors.placeholderText)
            Text(text)
                .font(.system(size: 13))
                .foregroundColor(AppColors.placeholderText)
        }
    }

    private func updatePassword() {
        localError = nil
        successMessage = nil

        guard !currentPassword.isEmpty, !newPassword.isEmpty, !confirmPassword.isEmpty else {
            localError = "Please fill in all fields."
            return
        }
        guard newPassword == confirmPassword else {
            localError = "New passwords do not match."
            return
        }
        guard hasMinLength else {
            localError = "New password must be at least 8 characters."
            return
        }

        Task {
            let didSucceed = await authViewModel.changePassword(
                currentPassword: currentPassword,
                newPassword: newPassword
            )
            if didSucceed {
                successMessage = "Password updated successfully."
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
            }
        }
    }
}
