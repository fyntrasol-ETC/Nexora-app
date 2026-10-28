import SwiftUI

struct LoginScreen: View {

    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.appSizeClass) private var sizeClass

    @State private var email = ""
    @State private var password = ""
    @State private var isPasswordVisible = false
    @State private var localError: String?

    var body: some View {

        ZStack {

            AppBackground()

            GeometryReader { geometry in

                ScrollView(showsIndicators: false) {

                    VStack(spacing: AppSizing.spacing(6, for: sizeClass)) {

                        Spacer(minLength: AppSizing.spacing(4, for: sizeClass))

                        // MARK: - Header

                        VStack(spacing: AppSizing.spacing(3.5, for: sizeClass)) {

                            ZStack {

                                RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass))
                                    .fill(Color.white.opacity(0.9))
                                    .frame(width: 70 * sizeClass.scale, height: 70 * sizeClass.scale)

                                Image(systemName: "graduationcap.fill")
                                    .font(.system(size: AppSizing.font(.title, for: sizeClass)))
                                    .foregroundColor(AppColors.primaryButton)
                            }

                            Text("Welcome Back")
                                .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass), weight: .bold))
                                .foregroundColor(.white)

                            Text("Master the world's most spoken\nlanguage with ease.")
                                .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                                .multilineTextAlignment(.center)
                                .foregroundColor(.white.opacity(0.9))
                        }

                        // MARK: - Login Card

                        VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {

                            LabeledField(
                                title: "Email Address"
                            ) {

                                AppTextField(
                                    placeholder: "you@example.com",
                                    text: $email
                                )
                            }

                            LabeledField(
                                title: "Password"
                            ) {

                                AppSecureField(
                                    placeholder: "••••••••",
                                    text: $password,
                                    isVisible: $isPasswordVisible
                                )
                            }

                            // MARK: Forgot Password

                            HStack {

                                Spacer()

                                NavigationLink(
                                    destination: ForgotPasswordScreen()
                                ) {
                                    Text("Forgot Password?")
                                        .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .medium))
                                        .foregroundColor(AppColors.link)
                                }
                            }

                            // MARK: Error

                            if let message = localError ?? authViewModel.errorMessage {

                                Text(message)
                                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                                    .foregroundColor(.red)
                                    .multilineTextAlignment(.center)
                            }

                            // MARK: Sign In Button

                            Button {
                                signIn()
                            } label: {

                                HStack {

                                    if authViewModel.isLoading {

                                        ProgressView()
                                            .tint(.white)

                                    } else {

                                        Text("Sign In")
                                            .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
                                    }
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: AppSizing.buttonHeight(sizeClass))
                                .background(AppColors.primaryButton)
                                .clipShape(Capsule())
                            }
                            .disabled(authViewModel.isLoading)
                            .padding(.top, AppSizing.spacing(1, for: sizeClass))

                            // MARK: Divider

                            HStack {

                                Rectangle()
                                    .fill(AppColors.fieldBorder)
                                    .frame(height: 1)

                                Text("OR CONTINUE WITH")
                                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
                                    .foregroundColor(AppColors.placeholderText)
                                    .fixedSize()

                                Rectangle()
                                    .fill(AppColors.fieldBorder)
                                    .frame(height: 1)
                            }
                            .padding(.vertical, AppSizing.spacing(1, for: sizeClass))

                            // MARK: Social Login

                            HStack(spacing: AppSizing.spacing(3, for: sizeClass)) {

                                SocialButton(
                                    icon: "google",
                                    label: "Google",
                                    isSystemImage: false
                                ) {
                                    Task {
                                        await authViewModel.signInWithGoogle()
                                    }
                                }

                                SocialButton(
                                    icon: "apple",
                                    label: "Apple",
                                    isSystemImage: false
                                ) {
                                    authViewModel.signInWithApple()
                                }
                            }
                        }
                        .padding(AppSizing.spacing(6, for: sizeClass))
                        .background(AppColors.cardBackground)
                        .clipShape(
                            RoundedRectangle(cornerRadius: 28 * sizeClass.scale)
                        )
                        .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))
                        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass) * 0.83)

                        // MARK: Sign Up

                        HStack(spacing: AppSizing.spacing(1, for: sizeClass)) {

                            Text("Don't have an account?")
                                .foregroundColor(.white.opacity(0.9))

                            NavigationLink(
                                destination: RegisterScreen()
                            ) {
                                Text("Sign Up")
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .underline()
                            }
                        }
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass)))
                        .padding(.top, AppSizing.spacing(2, for: sizeClass))

                        Spacer(minLength: AppSizing.spacing(4, for: sizeClass))
                    }
                    .frame(minHeight: geometry.size.height)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: - Sign In

    private func signIn() {

        localError = nil

        guard !email.isEmpty else {
            localError = "Please enter your email address."
            return
        }

        guard !password.isEmpty else {
            localError = "Please enter your password."
            return
        }

        Task {

            await authViewModel.signIn(
                email: email,
                password: password
            )

            // DO NOT manually change appState.flow here.
            //
            // AuthViewModel updates isAuthenticated after
            // Firebase authentication succeeds.
            //
            // ElliApp observes isAuthenticated and automatically
            // changes AppState to .main.
        }
    }
}
