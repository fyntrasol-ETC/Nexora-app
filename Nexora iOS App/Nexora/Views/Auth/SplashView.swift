import SwiftUI
import Combine

/// Purely visual. RootView decides when to leave this screen — driven by
/// AuthViewModel.isInitializing — so this view has no business setting any
/// navigation state itself. (Previously this screen forced a hardcoded
/// 3-second timer that raced against the real Firebase auth check, which
/// was the cause of the login/home-screen flicker bug.)
struct SplashScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @State private var progress: CGFloat = 0
    @State private var activeDot = 0

    private let dotTimer = Timer.publish(every: 0.4, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            AppBackground()

            VStack {
                Spacer()

                VStack(spacing: AppSizing.spacing(5, for: sizeClass)) {
                    ZStack {
                        RoundedRectangle(cornerRadius: AppSizing.cardCornerRadius(sizeClass) * 1.55)
                            .fill(Color.white.opacity(0.25))
                            .frame(width: 110 * sizeClass.scale, height: 110 * sizeClass.scale)
                        Image(systemName: "graduationcap.fill")
                            .font(.system(size: AppSizing.font(.largeTitle, for: sizeClass) * 1.54))
                            .foregroundColor(.white)
                    }

                    Text("Nexora")
                        .font(.system(size: AppSizing.font(.logo, for: sizeClass) * 1.3, weight: .heavy))
                        .foregroundColor(.white)

                    Text("Your English teacher workspace")
                        .font(.system(size: AppSizing.font(.body, for: sizeClass)))
                        .foregroundColor(.white.opacity(0.9))

                    HStack(spacing: AppSizing.spacing(2, for: sizeClass)) {
                        ForEach(0..<3, id: \.self) { index in
                            Circle()
                                .fill(Color.white.opacity(activeDot == index ? 1 : 0.4))
                                .frame(width: 8 * sizeClass.scale, height: 8 * sizeClass.scale)
                                .scaleEffect(activeDot == index ? 1.3 : 1.0)
                                .animation(.easeInOut(duration: 0.3), value: activeDot)
                        }
                    }
                    .padding(.top, AppSizing.spacing(1, for: sizeClass))
                }

                Spacer()

                VStack(spacing: AppSizing.spacing(2.5, for: sizeClass)) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(Color.white.opacity(0.3))
                                .frame(height: 3 * sizeClass.scale)
                            Capsule()
                                .fill(Color.white)
                                .frame(width: geo.size.width * progress, height: 3 * sizeClass.scale)
                        }
                    }
                    .frame(height: 3 * sizeClass.scale)
                    .frame(maxWidth: AppSizing.maxContentWidth(sizeClass) * 0.5)
                    .padding(.horizontal, AppSizing.spacing(15, for: sizeClass))

                    Text("SYNCING CURRICULUM...")
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .semibold))
                        .foregroundColor(.white.opacity(0.85))
                        .tracking(1.5)
                }
                .padding(.bottom, AppSizing.spacing(15, for: sizeClass))
            }
        }
        .onAppear {
            // Looping animation, purely decorative — repeats for as long as
            // this screen happens to stay on-screen. No flow-control logic
            // lives here anymore.
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true)) {
                progress = 1.0
            }
        }
        .onReceive(dotTimer) { _ in
            activeDot = (activeDot + 1) % 3
        }
    }
}
