

import Foundation

struct BadgeDefinition: Identifiable {
    let id: String        // must match badge IDs in the rules allow-list
    let title: String
    let description: String
    let iconName: String
}
