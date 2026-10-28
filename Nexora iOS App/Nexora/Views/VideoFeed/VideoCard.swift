//
//  VideoCard.swift
//  Elli
//
//  Full-width card used in VideoFeedScreen's list. Fully stateless —
//  favorited status and taps are driven entirely by the parent's
//  ViewModel.
//
//  NOTE: this card used to render an async-generated thumbnail bitmap
//  from the video file. That thumbnail could come back with an
//  unexpected/very wide aspect ratio, which blew out this card's width
//  (and, since it's inside a ScrollView, the whole feed screen's width)
//  no matter how the frame was constrained. The cover below is drawn
//  entirely in SwiftUI (gradient + icon) instead of decoding any image,
//  so there's no bitmap that can ever mis-size the layout.
//

import SwiftUI

struct VideoCard: View {
    let video: VideoItem
    let sizeClass: AppSizeClass
    let isFavorited: Bool
    let onToggleFavorite: () -> Void
    let onPlay: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: onPlay) {
                ZStack(alignment: .bottomTrailing) {
                    coverBackground

                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 44))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                    Text(video.duration)
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.black.opacity(0.55))
                        .cornerRadius(6)
                        .padding(10)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 170)
                .clipped()
            }
            .buttonStyle(.plain)

            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(video.title)
                        .font(.system(size: AppSizing.font(.subheadline, for: sizeClass), weight: .semibold))
                        .foregroundColor(AppColors.primaryText)
                        .lineLimit(2)
                    Text(video.subtitle)
                        .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                        .foregroundColor(AppColors.placeholderText)
                }
                Spacer()
                Button(action: onToggleFavorite) {
                    Image(systemName: isFavorited ? "heart.fill" : "heart")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.75))
                        .foregroundColor(isFavorited ? AppColors.accentRed : AppColors.primaryButton)
                }
            }
            .padding(AppSizing.spacing(3, for: sizeClass))
        }
        .background(AppColors.cardBackground)
        .cornerRadius(AppSizing.cardCornerRadius(sizeClass))
        .clipped()
    }

    // MARK: Cover (no image decoding — gradient + watermark icon only)

    private var coverBackground: some View {
        ZStack {
            LinearGradient(
                colors: [video.topic.tint, video.topic.tint.opacity(0.65)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Image(systemName: "quote.bubble.fill")
                .font(.system(size: 64))
                .foregroundColor(.white.opacity(0.18))
                .rotationEffect(.degrees(-8))
        }
        .frame(maxWidth: .infinity)
        .frame(height: 170)
    }
}

#if DEBUG
#Preview {
    VideoCard(
        video: VideoItem.sampleData[0],
        sizeClass: .compact,
        isFavorited: false,
        onToggleFavorite: {},
        onPlay: {}
    )
    .padding()
}
#endif
