//
//  AppSizing.swift
//  Elli
//
//  Every size in the app (font, spacing, control height, radius) should
//  come from here, keyed off `appSizeClass`, instead of a literal number
//  in the view. That's what keeps things from looking oversized on
//  iPhone or undersized/stretched on iPad.
//
//  Usage in a view:
//
//      @Environment(\.appSizeClass) private var sizeClass
//      ...
//      Text("Hello")
//          .font(.system(size: AppSizing.font(.title, for: sizeClass)))
//

import SwiftUI

// MARK: - Size class

enum AppSizeClass: Equatable {
    case compact   // iPhone (any size)
    case expanded  // iPad

    /// Big enough that iPad reads like a native iPad layout rather than a
    /// blown-up iPhone screen, without going cartoonish at the large sizes
    /// (buttons, headlines) where over-scaling shows the most.
    ///
    /// This used to be a flat 1.4x for every iPad. That's what was making
    /// 13" iPads (like the Air M4) look sparse/"floating" — an 11" iPad
    /// mini and a 13" iPad Air got the exact same multiplier, so the
    /// bigger screen just showed more empty canvas instead of bigger,
    /// more confident content. Scaling now steps up with the device's
    /// actual longest screen dimension, which is a stable proxy for
    /// iPad size regardless of current orientation.
    var scale: CGFloat {
        switch self {
        case .compact:
            return 1.0
        case .expanded:
            let longestSide = max(UIScreen.main.bounds.width, UIScreen.main.bounds.height)
            switch longestSide {
            case ..<1120:       // 11" / 10.9" iPads
                return 1.4
            case 1120..<1300:   // 12.9" iPad Pro
                return 1.55
            default:             // 13" iPad Pro / iPad Air (M4)
                return 1.7
            }
        }
    }
}

private struct AppSizeClassKey: EnvironmentKey {
    static let defaultValue: AppSizeClass = .compact
}

extension EnvironmentValues {
    var appSizeClass: AppSizeClass {
        get { self[AppSizeClassKey.self] }
        set { self[AppSizeClassKey.self] = newValue }
    }
}

/// Wrap the root of the app in this once. It reads the real horizontal
/// size class from SwiftUI and republishes it as our own `appSizeClass`
/// so every child view can key off one consistent value.
struct AppSizeClassProvider<Content: View>: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @ViewBuilder var content: Content

    var body: some View {
        let resolved: AppSizeClass = horizontalSizeClass == .regular ? .expanded : .compact
        content
            .environment(\.appSizeClass, resolved)
    }
}

// MARK: - Font tokens

enum AppFontStyle {
    case logo          // "ellii" wordmark
    case largeTitle     // "Welcome back", "Change password"
    case title          // section-level headline
    case headline       // card titles
    case subheadline     // supporting copy under a headline
    case body
    case callout
    case caption
    case button
    case avatarInitials
}

// MARK: - Sizing tokens

enum AppSizing {

    static func font(_ style: AppFontStyle, for sizeClass: AppSizeClass) -> CGFloat {
        let base: CGFloat
        switch style {
        case .logo: base = 34
        case .largeTitle: base = 26
        case .title: base = 22
        case .headline: base = 18
        case .subheadline: base = 15
        case .body: base = 16
        case .callout: base = 14
        case .caption: base = 12
        case .button: base = 17
        case .avatarInitials: base = 28
        }
        return (base * sizeClass.scale).rounded()
    }

    /// 4pt-grid spacing. Pass a multiplier: `AppSizing.spacing(2, for: sizeClass)` == 8pt on iPhone.
    static func spacing(_ multiplier: CGFloat, for sizeClass: AppSizeClass) -> CGFloat {
        (4 * multiplier * sizeClass.scale).rounded()
    }

    static func iconSize(_ sizeClass: AppSizeClass) -> CGFloat { (24 * sizeClass.scale).rounded() }
    static func avatarSize(_ sizeClass: AppSizeClass) -> CGFloat { (88 * sizeClass.scale).rounded() }
    static func buttonHeight(_ sizeClass: AppSizeClass) -> CGFloat { (54 * sizeClass.scale).rounded() }
    static func fieldHeight(_ sizeClass: AppSizeClass) -> CGFloat { (52 * sizeClass.scale).rounded() }
    static func cornerRadius(_ sizeClass: AppSizeClass) -> CGFloat { (14 * sizeClass.scale).rounded() }
    static func cardCornerRadius(_ sizeClass: AppSizeClass) -> CGFloat { (18 * sizeClass.scale).rounded() }
    static func smallCornerRadius(_ sizeClass: AppSizeClass) -> CGFloat { (10 * sizeClass.scale).rounded() }

    static func horizontalPadding(_ sizeClass: AppSizeClass) -> CGFloat { (24 * sizeClass.scale).rounded() }
    static func sectionSpacing(_ sizeClass: AppSizeClass) -> CGFloat { (28 * sizeClass.scale).rounded() }

    /// On iPad we don't stretch forms edge to edge — we cap the column
    /// width and let it float centered, the way Settings-style screens do.
    /// Raised from 640 -> 760: 640 was tuned for an 11" iPad and left a
    /// noticeably wider dead margin on 13" screens.
    static func maxContentWidth(_ sizeClass: AppSizeClass) -> CGFloat {
        sizeClass == .expanded ? 760 : .infinity
    }

    /// Fixed square size for the flashcard — deliberately NOT stretched to
    /// fill the screen, but the previous 340pt hard cap was fighting the
    /// scale increase above: on a 13" iPad it kept the card iPhone-sized
    /// while floating in a much bigger canvas. Raised the cap to 460 so
    /// larger iPads actually get a visibly bigger card.
    static func flashcardSize(_ sizeClass: AppSizeClass) -> CGFloat {
        min(300 * sizeClass.scale, 460).rounded()
    }

    /// Fixed square size for the course cards in the horizontally
    /// scrolling course rows (Courses tab). Deliberately fixed rather
    /// than proportional to screen width — that's what lets the next
    /// card peek in from the edge as a scroll affordance.
    static func courseCardSize(_ sizeClass: AppSizeClass) -> CGFloat {
        (132 * sizeClass.scale).rounded()
    }

    /// Reserved space at the bottom of scrollable root screens so content
    /// (list end, CTA buttons) never sits behind the floating tab bar,
    /// same idea as Profile's `Spacer(minLength: 110)`.
    static func tabBarClearance(_ sizeClass: AppSizeClass) -> CGFloat {
        (100 * sizeClass.scale).rounded()
    }
}

#if DEBUG
extension View {
    /// Use this in `#Preview` blocks instead of hardcoding
    /// every preview to render at iPhone scale even when the Xcode canvas
    /// device was set to an iPad — which is why iPad previews looked
    /// undersized. This lets the real, selected preview device decide,
    /// exactly like the live app does via `AppSizeClassProvider`.
    func previewSizeClass() -> some View {
        AppSizeClassProvider { self }
    }
}
#endif
