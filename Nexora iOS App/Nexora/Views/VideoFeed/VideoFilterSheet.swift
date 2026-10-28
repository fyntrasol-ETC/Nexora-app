//
//  VideoFilterSheet.swift
//  Elli
//
//  Sheet presented from VideoFeedScreen. Holds its own `pendingFilter`
//  draft so Clear/dismiss don't affect the feed until Apply is tapped;
//  the committed value lives back in VideoFeedViewModel via the binding.
//

import SwiftUI

struct VideoFilterSheet: View {
    @Environment(\.appSizeClass) private var sizeClass
    @Environment(\.dismiss) private var dismiss

    @Binding var activeFilter: VideoFilter
    @State private var pendingFilter: VideoFilter

    init(activeFilter: Binding<VideoFilter>) {
        self._activeFilter = activeFilter
        self._pendingFilter = State(initialValue: activeFilter.wrappedValue)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                        .foregroundColor(AppColors.primaryText)
                }
                Spacer()
                Text("Filter")
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(AppColors.primaryText)
                Spacer()
                Button("Clear") {
                    pendingFilter = .all
                }
                .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                .foregroundColor(AppColors.link)
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.top, AppSizing.spacing(4, for: sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))

            ScrollView {
                VStack(spacing: 10) {
                    filterRow(label: "All", isSelected: pendingFilter == .all) {
                        pendingFilter = .all
                    }
                    filterRow(label: "Favorites", icon: "heart.fill", isSelected: pendingFilter == .favorites) {
                        pendingFilter = .favorites
                    }

                    Divider()
                        .padding(.vertical, 4)

                    ForEach(VideoTopic.allCases, id: \.self) { topic in
                        filterRow(label: topic.rawValue, isSelected: pendingFilter == .topic(topic)) {
                            pendingFilter = .topic(topic)
                        }
                    }
                }
                .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            }

            Button {
                activeFilter = pendingFilter
                dismiss()
            } label: {
                Text("Apply Filters")
                    .font(.system(size: AppSizing.font(.button, for: sizeClass), weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: AppSizing.buttonHeight(sizeClass))
                    .background(AppColors.primaryButton)
                    .clipShape(Capsule())
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.vertical, AppSizing.spacing(3, for: sizeClass))
        }
        .background(Color(red: 0.97, green: 0.97, blue: 0.99).ignoresSafeArea())
    }

    private func filterRow(label: String, icon: String? = nil, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                }
                Text(label)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: isSelected ? .semibold : .regular))
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: AppSizing.font(.callout, for: sizeClass), weight: .semibold))
                }
            }
            .foregroundColor(isSelected ? .white : AppColors.primaryText)
            .padding(.horizontal, AppSizing.spacing(4, for: sizeClass))
            .padding(.vertical, AppSizing.spacing(3, for: sizeClass))
            .background(isSelected ? AppColors.primaryButton : AppColors.fieldBackground)
            .cornerRadius(AppSizing.smallCornerRadius(sizeClass))
        }
        .buttonStyle(.plain)
    }
}
