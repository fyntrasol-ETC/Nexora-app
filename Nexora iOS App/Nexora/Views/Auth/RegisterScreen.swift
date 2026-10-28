import SwiftUI

struct RegisterScreen: View {

    @EnvironmentObject var authViewModel: AuthViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.appSizeClass) private var sizeClass

    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""

    @State private var isPasswordVisible = false
    @State private var agreedToTerms = false

    @State private var localError: String?

    var body: some View {

        ZStack {

            AppBackground()

            VStack(spacing: 0) {

                // MARK: - Top Bar

                AuthTopBar(showBackButton: false)

                ScrollView {

                    VStack(spacing: AppSizing.spacing(6, for: sizeClass)) {

                        // MARK: - Header

                        VStack(spacing: AppSizing.spacing(2, for: sizeClass)) {

                            Text("Create Account")
                                .font(
                                    .system(
                                        size: AppSizing.font(.largeTitle, for: sizeClass),
                                        weight: .bold
                                    )
                                )
                                .foregroundColor(.white)

                            Text(
                                "Join our global community and start your\nlanguage journey today."
                            )
                            .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                            .multilineTextAlignment(.center)
                            .foregroundColor(
                                .white.opacity(0.9)
                            )
                        }
                        .padding(.top, AppSizing.spacing(6, for: sizeClass))

                        // MARK: - Register Card

                        AuthCard {

                            // MARK: Full Name

                            LabeledField(
                                title: "Full Name"
                            ) {

                                AppTextField(
                                    placeholder: "Keiko Tanaka",
                                    text: $fullName
                                )
                            }

                            // MARK: Email

                            LabeledField(
                                title: "Email Address"
                            ) {

                                AppTextField(
                                    placeholder: "keiko@example.com",
                                    text: $email
                                )
                            }

                            // MARK: Password

                            LabeledField(
                                title: "Password"
                            ) {

                                AppSecureField(
                                    placeholder: "••••••••",
                                    text: $password,
                                    isVisible: $isPasswordVisible
                                )
                            }

                            // MARK: Password Requirements

                            VStack(
                                alignment: .leading,
                                spacing: AppSizing.spacing(1.5, for: sizeClass)
                            ) {

                                Text(
                                    "Password must contain:"
                                )
                                .font(
                                    .system(
                                        size: AppSizing.font(.caption, for: sizeClass),
                                        weight: .medium
                                    )
                                )
                                .foregroundColor(
                                    AppColors.placeholderText
                                )

                                passwordRequirement(
                                    text: "At least 8 characters",
                                    isValid: hasMinLength
                                )

                                passwordRequirement(
                                    text: "At least one alphabet",
                                    isValid: containsLetter
                                )

                                passwordRequirement(
                                    text: "At least one number",
                                    isValid: containsNumber
                                )

                                passwordRequirement(
                                    text: "At least one special character",
                                    isValid: containsSpecialCharacter
                                )
                            }

                            // MARK: Terms & Conditions

                            HStack(
                                alignment: .top,
                                spacing: AppSizing.spacing(2, for: sizeClass)
                            ) {

                                Button {

                                    agreedToTerms.toggle()

                                    // Clear the terms error when
                                    // the user accepts them.
                                    if agreedToTerms {
                                        localError = nil
                                    }

                                } label: {

                                    Image(
                                        systemName:
                                            agreedToTerms
                                            ? "checkmark.square.fill"
                                            : "square"
                                    )
                                    .foregroundColor(
                                        agreedToTerms
                                        ? AppColors.primaryButton
                                        : AppColors.placeholderText
                                    )
                                    .font(.system(size: AppSizing.font(.headline, for: sizeClass)))
                                }

                                Text(
                                    "I agree to the Terms & Conditions and Privacy Policy."
                                )
                                .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                .foregroundColor(
                                    AppColors.placeholderText
                                )
                            }

                            // MARK: Error

                            if let message =
                                localError ??
                                authViewModel.errorMessage {

                                Text(message)
                                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                    .foregroundColor(.red)
                                    .multilineTextAlignment(.center)
                            }

                            // MARK: Create Account Button

                            PrimaryButton(
                                title:
                                    authViewModel.isLoading
                                    ? "Creating..."
                                    : "Create Account"
                            ) {

                                signUp()
                            }
                            .disabled(
                                authViewModel.isLoading
                            )
                            .padding(.top, AppSizing.spacing(1, for: sizeClass))
                        }
                        .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))

                        // MARK: Login

                        HStack(spacing: AppSizing.spacing(1, for: sizeClass)) {

                            Text("Already have an account?")
                                .foregroundColor(
                                    .white.opacity(0.9)
                                )

                            Button {

                                dismiss()

                            } label: {

                                Text("Log In")
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .underline()
                            }
                        }
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))

                        Spacer(minLength: AppSizing.spacing(7.5, for: sizeClass))
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: - Password Validation

    private var hasMinLength: Bool {

        password.count >= 8
    }

    private var containsLetter: Bool {

        password.range(
            of: "[A-Za-z]",
            options: .regularExpression
        ) != nil
    }

    private var containsNumber: Bool {

        password.range(
            of: "[0-9]",
            options: .regularExpression
        ) != nil
    }

    private var containsSpecialCharacter: Bool {

        password.range(
            of: "[^A-Za-z0-9]",
            options: .regularExpression
        ) != nil
    }

    private var isPasswordValid: Bool {

        hasMinLength &&
        containsLetter &&
        containsNumber &&
        containsSpecialCharacter
    }

    // MARK: - Password Requirement Row

    @ViewBuilder
    private func passwordRequirement(
        text: String,
        isValid: Bool
    ) -> some View {

        HStack(spacing: AppSizing.spacing(1.5, for: sizeClass)) {

            Image(
                systemName:
                    isValid
                    ? "checkmark.circle.fill"
                    : "circle"
            )
            .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
            .foregroundColor(
                isValid
                ? AppColors.primaryButton
                : AppColors.placeholderText
            )

            Text(text)
                .font(.system(size: AppSizing.font(.caption, for: sizeClass) - 1))
                .foregroundColor(
                    AppColors.placeholderText
                )
        }
    }

    // MARK: - Sign Up

    private func signUp() {

        localError = nil
        authViewModel.errorMessage = nil

        // Full Name

        guard !fullName
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            .isEmpty
        else {

            localError = "Please enter your full name."
            return
        }

        // Email

        guard !email
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            .isEmpty
        else {

            localError = "Please enter your email address."
            return
        }

        // Password

        guard !password.isEmpty else {

            localError = "Please enter your password."
            return
        }

        // Password requirements

        guard isPasswordValid else {

            localError =
                "Password must contain at least one alphabet, one number, and one special character."

            return
        }

        // Terms

        guard agreedToTerms else {

            localError =
                "Please agree to the Terms & Conditions and Privacy Policy to continue."

            return
        }

        // Create account

        Task {

            await authViewModel.signUp(
                fullName: fullName
                    .trimmingCharacters(
                        in: .whitespacesAndNewlines
                    ),
                email: email
                    .trimmingCharacters(
                        in: .whitespacesAndNewlines
                    ),
                password: password
            )

            // DO NOT manually set:
            //
            // appState.flow = .main
            //
            // Firebase authentication state should
            // control the application flow.
        }
    }
}
