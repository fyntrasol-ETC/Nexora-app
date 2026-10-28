//
//  VideoPlayerScreen.swift
//  Elli
//
//  Full-screen player presented from VideoFeedScreen. Pure view — the
//  AVPlayer instance, playback position, scrubbing, and the overlay's
//  show/hide timing all live in VideoPlayerViewModel.
//

import SwiftUI
import AVKit

struct VideoPlayerScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel: VideoPlayerViewModel

    init(video: VideoItem) {
        _viewModel = StateObject(wrappedValue: VideoPlayerViewModel(video: video))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            playerSurface
                .ignoresSafeArea()
                // Whole video area is tappable: reveals the overlay if it's
                // hidden, or acts as play/pause if it's already showing.
                .contentShape(Rectangle())
                .onTapGesture { viewModel.handleTap() }

            if viewModel.showControls {
                centerPlayPauseButton
                    .transition(.opacity)
                    .allowsHitTesting(false) // taps fall through to playerSurface above
            }

            VStack(spacing: 0) {
                if viewModel.showControls {
                    topBar
                        .transition(.opacity)
                }
                Spacer()
                if viewModel.showControls {
                    bottomOverlay
                        .transition(.opacity)
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: viewModel.showControls)
        .onAppear { viewModel.setUpPlayer() }
        .onDisappear { viewModel.tearDown() }
    }

    // MARK: Player surface

    private var playerSurface: some View {
        ZStack {
            if let player = viewModel.player, !viewModel.fileMissing {
                VideoPlayer(player: player)
                    .disabled(true) // we drive play/pause + seeking ourselves
            } else {
                Rectangle().fill(viewModel.video.topic.tint)
                if viewModel.fileMissing {
                    VStack(spacing: 8) {
                        Image(systemName: "film")
                            .font(.system(size: 32))
                        Text("\(viewModel.video.videoFileName).mp4 not found in bundle")
                            .font(.caption)
                    }
                    .foregroundColor(.white.opacity(0.85))
                }
            }
        }
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.75, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(10)
                    .background(Color.white.opacity(0.15))
                    .clipShape(Circle())
            }
            Spacer()
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(3, for: sizeClass))
    }

    // MARK: Center play/pause icon (auto-hides while playing, per VM)

    private var centerPlayPauseButton: some View {
        Image(systemName: viewModel.isPlaying ? "pause.circle.fill" : "play.circle.fill")
            .font(.system(size: 64))
            .foregroundColor(.white)
    }

    // MARK: Bottom overlay — title, progress bar, speed control

    private var bottomOverlay: some View {
        VStack(alignment: .leading, spacing: AppSizing.spacing(2, for: sizeClass)) {
            Text(viewModel.video.title)
                .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                .foregroundColor(.white)
            Text(viewModel.video.subtitle)
                .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                .foregroundColor(.white.opacity(0.75))

            progressBar
                .padding(.top, AppSizing.spacing(2, for: sizeClass))

            HStack {
                speedMenu
                Spacer()
            }
        }
        .padding(AppSizing.horizontalPadding(sizeClass))
        .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Color.black.opacity(0), Color.black.opacity(0.65)],
                           startPoint: .top, endPoint: .bottom)
        )
    }

    private var progressBar: some View {
        let duration = max(viewModel.duration, 0.1)
        return VStack(spacing: 4) {
            Slider(
                value: Binding(
                    get: { viewModel.currentTime },
                    set: { viewModel.scrub(to: $0) }
                ),
                in: 0...duration,
                onEditingChanged: { isEditing in
                    if isEditing {
                        viewModel.beginScrubbing()
                    } else {
                        viewModel.endScrubbing(at: viewModel.currentTime)
                    }
                }
            )
            .tint(.white)

            HStack {
                Text(viewModel.currentTime.formattedAsPlaybackTime)
                Spacer()
                Text(viewModel.duration.formattedAsPlaybackTime)
            }
            .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
            .foregroundColor(.white.opacity(0.85))
        }
    }

    private var speedMenu: some View {
        Menu {
            ForEach(PlaybackSpeed.allCases) { option in
                Button {
                    viewModel.speed = option
                } label: {
                    if viewModel.speed == option {
                        Label(option.label, systemImage: "checkmark")
                    } else {
                        Text(option.label)
                    }
                }
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "speedometer")
                Text(viewModel.speed.label)
                    .fontWeight(.semibold)
            }
            .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
            .foregroundColor(.white)
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            .padding(.vertical, AppSizing.spacing(2, for: sizeClass))
            .background(Color.white.opacity(0.15))
            .clipShape(Capsule())
        }
    }
}

#if DEBUG
#Preview {
    VideoPlayerScreen(video: VideoItem.sampleData[0])
}
#endif
