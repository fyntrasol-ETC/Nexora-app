//
//  VideoFeedScreen.swift
//  Elli
//
//  Videos tab. Pure view — all state (catalog, favorites, active filter)
//  lives in VideoFeedViewModel. Background matches ProfileScreen's
//  `AppBackground()`.
//
//  Wire into MainTabView the same way it already is:
//  `case .videos: VideoFeedScreen()`
//

import SwiftUI

struct VideoFeedScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = VideoFeedViewModel()

    @State private var showFilterSheet = false
    @State private var playingVideo: VideoItem?

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                if viewModel.filteredVideos.isEmpty {
                    emptyState
                } else {
                    ScrollView {
                        VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
                            ForEach(viewModel.filteredVideos) { video in
                                VideoCard(
                                    video: video,
                                    sizeClass: sizeClass,
                                    isFavorited: viewModel.isFavorited(video),
                                    onToggleFavorite: { viewModel.toggleFavorite(video) },
                                    onPlay: { playingVideo = video }
                                )
                                .frame(maxWidth: .infinity)
                            }
                            Spacer(minLength: AppSizing.tabBarClearance(sizeClass))
                        }
                        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                        .padding(.top, AppSizing.spacing(3, for: sizeClass))
                        .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .sheet(isPresented: $showFilterSheet) {
            VideoFilterSheet(activeFilter: $viewModel.activeFilter)
                .presentationDetents([.fraction(0.85)])
                .presentationDragIndicator(.hidden)
        }
        .fullScreenCover(item: $playingVideo) { video in
            // Browsing the feed (thumbnails, titles, favorite counts) is
            // free for a guest; actually watching isn't. Reusing the
            // item-based cover for both cases keeps a single dismiss()
            // path — SignInRequiredScreen's own "Not now" and the
            // player's close button both just dismiss this same sheet.
            if authViewModel.isAnonymous {
                SignInRequiredScreen(message: "Create an account to watch videos.")
            } else {
                VideoPlayerScreen(video: video)
            }
        }
        .task {
            // configure() is no longer async — it starts a live Firestore
            // listener (see VideoActivityService.observeFavoriteIDs)
            // rather than doing a one-time fetch, so favorites made on
            // another device/platform show up here without relaunching.
            guard let uid = authViewModel.userProfile?.id else { return }
            viewModel.configure(uid: uid, isRealAccount: authViewModel.isRealAccount)
        }
    }

    // MARK: Header

    private var header: some View {
        HStack {
            Text("Video Feed")
                .font(.system(size: AppSizing.font(.title, for: sizeClass), weight: .bold))
                .foregroundColor(.white)

            Spacer()

            Button {
                showFilterSheet = true
            } label: {
                HStack(spacing: 6) {
                    Text("Filter")
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7))
                }
                .foregroundColor(AppColors.primaryButton)
                .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
                .padding(.vertical, AppSizing.spacing(2, for: sizeClass))
                .background(Color.white)
                .clipShape(Capsule())
            }
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(3, for: sizeClass))
        .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        .frame(maxWidth: AppSizing.maxContentWidth(sizeClass))
        .frame(maxWidth: .infinity)
    }

    // MARK: Empty state (e.g. Favorites filter with nothing saved yet)

    private var emptyState: some View {
        VStack(spacing: AppSizing.spacing(3, for: sizeClass)) {
            Image(systemName: "heart.slash")
                .font(.system(size: AppSizing.iconSize(sizeClass) * 1.5))
                .foregroundColor(.white.opacity(0.85))
            Text(viewModel.emptyStateMessage)
                .font(.system(size: AppSizing.font(.body, for: sizeClass), weight: .medium))
                .foregroundColor(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#if DEBUG
#Preview {
    VideoFeedScreen()
        .environmentObject(AuthViewModel())
        .previewSizeClass()
}
#endif
