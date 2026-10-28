//
//  VideoFeedViewModel.swift
//  Elli
//
//  Owns all state for VideoFeedScreen: the catalog, which ones are
//  favorited, and the active filter. The screen just renders whatever
//  this exposes and forwards taps back into it.
//
//  Favorited state is mirrored to users/{uid}/videoActivity/{videoId}
//  via VideoActivityService, and is now kept LIVE via a Firestore
//  snapshot listener (see VideoActivityService.observeFavoriteIDs) —
//  so a favorite/unfavorite made on Android (or another device) shows
//  up here immediately, not just after relaunch.
//

import Foundation
import Combine

@MainActor
final class VideoFeedViewModel: ObservableObject {
    @Published private(set) var videos: [VideoItem]
    @Published private(set) var favoriteIDs: Set<VideoItem.ID> = []
    @Published var activeFilter: VideoFilter = .all

    private let activityService = VideoActivityService()
    private var uid: String?

    /// True once the current session is a real account. Per spec,
    /// videos themselves are gated behind a real account — the feed can
    /// still list thumbnails/titles for a guest, but VideoFeedScreen
    /// should check this before pushing into VideoPlayerScreen and show
    /// a `SignInRequiredScreen` ("Sign in to watch videos") instead when
    /// it's false. Kept here (rather than read straight off
    /// AuthViewModel in the screen) so the favoriting logic below can
    /// gate on the exact same flag.
    private(set) var isRealAccount = false

    init(videos: [VideoItem] = VideoItem.sampleData) {
        self.videos = videos
    }

    deinit {
        activityService.stopObserving()
    }

    /// Call once the signed-in user's uid is known (e.g. from
    /// VideoFeedScreen's `.task`, reading `authViewModel.userProfile?.id`
    /// and `authViewModel.isRealAccount`). Starts a live Firestore
    /// listener for this user's favorited videos — only for a real
    /// account, since a guest has never been allowed to write a favorite
    /// in the first place. Safe to call again on re-sign-in — it just
    /// restarts the listener against the new uid.
    func configure(uid: String, isRealAccount: Bool) {
        self.uid = uid
        self.isRealAccount = isRealAccount

        guard isRealAccount else { return }

        activityService.observeFavoriteIDs(uid: uid) { [weak self] ids in
            self?.favoriteIDs = ids
        }
    }

    var filteredVideos: [VideoItem] {
        switch activeFilter {
        case .all:
            return videos
        case .favorites:
            return videos.filter { favoriteIDs.contains($0.id) }
        case .topic(let topic):
            return videos.filter { $0.topic == topic }
        }
    }

    var emptyStateMessage: String {
        activeFilter == .favorites ? "No favorites yet" : "No videos in this topic yet"
    }

    func isFavorited(_ video: VideoItem) -> Bool {
        favoriteIDs.contains(video.id)
    }

    func toggleFavorite(_ video: VideoItem) {
        let isNowFavorited: Bool

        if favoriteIDs.contains(video.id) {
            favoriteIDs.remove(video.id)
            isNowFavorited = false
        } else {
            favoriteIDs.insert(video.id)
            isNowFavorited = true
        }

        if let uid, isRealAccount {
            activityService.setFavorited(uid: uid, videoId: video.id, isFavorited: isNowFavorited)
        }
    }
}
