
import AVFoundation
import UIKit

actor VideoThumbnailCache {
    static let shared = VideoThumbnailCache()

    private var cache: [String: UIImage] = [:]

    /// Returns a cached thumbnail for `fileName` (without extension), or
    /// generates one from the bundled mp4's first frame. Returns nil if
    /// the file isn't in the bundle yet — callers should keep showing
    /// their tint placeholder in that case.
    func thumbnail(for fileName: String) async -> UIImage? {
        if let cached = cache[fileName] {
            return cached
        }
        guard let url = Bundle.main.url(forResource: fileName, withExtension: "mp4") else {
            return nil
        }

        let asset = AVURLAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true

        // A hair after zero avoids the occasional all-black first frame
        // some encoders produce right at time zero.
        let requestedTime = CMTime(seconds: 0.1, preferredTimescale: 600)

        do {
            let result = try await generator.image(at: requestedTime)
            let image = UIImage(cgImage: result.image)
            cache[fileName] = image
            return image
        } catch {
            return nil
        }
    }
}
