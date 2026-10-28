
import Foundation

extension TimeInterval {
    /// Formats as "m:ss", or "h:mm:ss" once past an hour. Guards against
    /// NaN/negative values that can briefly show up before the player
    /// has loaded (returns "0:00").
    var formattedAsPlaybackTime: String {
        guard isFinite, self >= 0 else { return "0:00" }
        let totalSeconds = Int(self.rounded())
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }
}
