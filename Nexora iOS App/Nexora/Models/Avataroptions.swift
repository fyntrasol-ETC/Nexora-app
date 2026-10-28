
import SwiftUI

enum AvatarOptions {
    private static let style = "notionists"
    private static let baseURL = "https://api.dicebear.com/9.x/\(style)/png"
    static let defaultSeed = "nexora-01"

    /// Keep this list identical to AvatarOptions.kt's curatedSeeds —
    /// update both files together if you add/remove options.
    static let curatedSeeds: [String] = [
        "nexora-01", "nexora-02", "nexora-03", "nexora-04",
        "nexora-05", "nexora-06", "nexora-07", "nexora-08",
        "nexora-09", "nexora-10", "nexora-11", "nexora-12",
        "nexora-13", "nexora-14", "nexora-15", "nexora-16",
        "nexora-17", "nexora-18", "nexora-19", "nexora-20",
        "nexora-21", "nexora-22", "nexora-23", "nexora-24"
    ]

    static func url(for seed: String, size: Int = 256) -> URL? {
        URL(string: "\(baseURL)?seed=\(seed)&size=\(size)&backgroundType=gradientLinear")
    }

    static func resolveSeed(_ stored: String?) -> String {
        guard let stored, !stored.isEmpty, curatedSeeds.contains(stored) else {
            return defaultSeed
        }
        return stored
    }
}

/// Renders a single DiceBear avatar for the given seed. Equivalent to
/// DiceBearAvatar.kt on Android — same seed in, same circular image out.
struct DiceBearAvatar: View {
    let seed: String
    let size: CGFloat

    var body: some View {
        AsyncImage(url: AvatarOptions.url(for: seed)) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            case .failure:
                Circle().fill(Color(.systemGray5))
            default:
                Circle()
                    .fill(Color(.systemGray5))
                    .overlay(ProgressView())
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }
}

/// Selectable grid tile version, for the "Change avatar" picker screen.
struct DiceBearAvatarOption: View {
    let seed: String
    let selected: Bool
    let onTap: () -> Void

    private let tileSize: CGFloat = 76

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Circle()
                .fill(selected ? Color.accentColor.opacity(0.14) : Color.clear)
                .frame(width: tileSize, height: tileSize)
                .overlay(DiceBearAvatar(seed: seed, size: tileSize - 12))
            if selected {
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white)
                    .padding(5)
                    .background(Circle().fill(Color.accentColor))
                    .offset(x: -2, y: -2)
            }
        }
        .contentShape(Circle())
        .onTapGesture(perform: onTap)
    }
}
