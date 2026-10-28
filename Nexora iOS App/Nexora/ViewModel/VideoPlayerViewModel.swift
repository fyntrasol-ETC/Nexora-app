//
//  VideoPlayerViewModel.swift
//  Elli
//
//  Owns the AVPlayer instance, playback position, scrubbing, and the
//  show/hide state of the on-screen controls for VideoPlayerScreen.
//
//  Controls behavior (per Hadi's spec):
//  - On play, the overlay (play/pause button, progress bar, etc.) is
//    visible for 2 seconds, then fades out so the video isn't obscured.
//  - Tapping the video while the overlay is hidden just reveals it again.
//  - Tapping while the overlay is visible toggles play/pause.
//  - While paused, the overlay stays up indefinitely (no auto-hide) —
//    it only starts the 2s countdown again once playback resumes.
//
//  When you move sources to Firestore/Storage later, only
//  `setUpPlayer()` needs to change — swap the `Bundle.main.url` lookup
//  for `AVPlayer(url: video.videoURL)`.
//

import Foundation
import AVKit
import Combine

@MainActor
final class VideoPlayerViewModel: ObservableObject {
    let video: VideoItem

    @Published private(set) var player: AVPlayer?
    @Published private(set) var isPlaying = false
    @Published private(set) var fileMissing = false
    @Published private(set) var currentTime: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var showControls = true

    @Published var speed: PlaybackSpeed = .normal {
        didSet { applySpeedIfPlaying() }
    }

    private var timeObserverToken: Any?
    private var hideControlsTask: Task<Void, Never>?
    private var isScrubbing = false

    private static let autoHideDelayNanoseconds: UInt64 = 2_000_000_000 // 2 seconds

    init(video: VideoItem) {
        self.video = video
    }

    // MARK: Setup / teardown

    func setUpPlayer() {
        guard let url = Bundle.main.url(forResource: video.videoFileName, withExtension: "mp4") else {
            // File isn't in the bundle yet (or the name doesn't match) —
            // the screen falls back to a tint placeholder instead of crashing.
            fileMissing = true
            return
        }

        let asset = AVURLAsset(url: url)
        let item = AVPlayerItem(asset: asset)
        let newPlayer = AVPlayer(playerItem: item)
        player = newPlayer
        observeTime(on: newPlayer)

        Task { [weak self] in
            if let loadedDuration = try? await asset.load(.duration) {
                self?.duration = loadedDuration.seconds
            }
        }

        play()
    }

    func tearDown() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
        hideControlsTask?.cancel()
        player?.pause()
    }

    private func observeTime(on player: AVPlayer) {
        let interval = CMTime(seconds: 0.25, preferredTimescale: 600)
        timeObserverToken = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self, !self.isScrubbing else { return }
            self.currentTime = time.seconds
        }
    }

    // MARK: Play / pause

    func play() {
        player?.rate = Float(speed.rawValue)
        isPlaying = true
        revealControls(autoHide: true)
    }

    func togglePlayPause() {
        guard let player else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
            revealControls(autoHide: false) // stay visible while paused
        } else {
            player.rate = Float(speed.rawValue)
            isPlaying = true
            revealControls(autoHide: true)
        }
    }

    /// Tap anywhere on the video surface. If the overlay is hidden this
    /// just brings it back; if it's already visible, the tap acts as
    /// play/pause.
    func handleTap() {
        if showControls {
            togglePlayPause()
        } else {
            revealControls(autoHide: isPlaying)
        }
    }

    // MARK: Scrubbing (progress bar drag)

    func beginScrubbing() {
        isScrubbing = true
        hideControlsTask?.cancel()
    }

    /// Called continuously while dragging — updates the displayed time
    /// without seeking the player yet (cheap, keeps dragging smooth).
    func scrub(to seconds: Double) {
        currentTime = seconds
    }

    /// Called when the drag ends — this is what actually seeks the player.
    func endScrubbing(at seconds: Double) {
        isScrubbing = false
        let target = CMTime(seconds: seconds, preferredTimescale: 600)
        player?.seek(to: target, toleranceBefore: .zero, toleranceAfter: .zero)
        currentTime = seconds
        if isPlaying {
            revealControls(autoHide: true)
        }
    }

    // MARK: Speed

    private func applySpeedIfPlaying() {
        player?.rate = isPlaying ? Float(speed.rawValue) : 0
    }

    // MARK: Controls visibility

    private func revealControls(autoHide: Bool) {
        showControls = true
        hideControlsTask?.cancel()
        guard autoHide else { return }
        hideControlsTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.autoHideDelayNanoseconds)
            guard !Task.isCancelled else { return }
            self?.showControls = false
        }
    }
}
