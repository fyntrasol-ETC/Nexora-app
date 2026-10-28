//
//  DialogueScreen.swift
//  Elli
//
//  Shows the chapter's dialogue as chat-style bubbles, alternating side
//  by speaker (with a small avatar initial) so it reads like a real
//  conversation. "Start Quiz" moves into the MCQ / fill-in-the-blank
//  questions about what was just read.
//
//  The CTA sits in a `.safeAreaInset(edge: .bottom)`, not inline in the
//  ScrollView — that keeps it pinned above the home indicator regardless
//  of whatever else is layered underneath.
//

import SwiftUI
import UIKit

struct DialogueScreen: View {
    @Environment(\.appSizeClass) private var sizeClass
    @EnvironmentObject var authViewModel: AuthViewModel
    let course: Course
    let chapter: Chapter
    @Binding var path: [CourseRoute]
    @State private var showingSignIn = false

    private var dialogueLines: [DialogueLine] {
        if case .dialogue(let exercise) = chapter.exercise {
            return exercise.dialogue
        }
        return []
    }

    var body: some View {
        ZStack {
            AppBackground()

            VStack(spacing: 0) {
                header

                ScrollView {
                    VStack(spacing: AppSizing.spacing(4, for: sizeClass)) {
                        ForEach(Array(dialogueLines.enumerated()), id: \.element.id) { index, line in
                            DialogueBubble(line: line, sizeClass: sizeClass)
                                .id(index)
                        }
                    }
                    .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
                    .padding(.top, AppSizing.spacing(3, for: sizeClass))
                    .padding(.bottom, AppSizing.spacing(4, for: sizeClass))
                }
            }
        }
        .navigationBarHidden(true)
        .safeAreaInset(edge: .bottom) {
            GlowCTAButton(title: "Start Quiz", icon: "arrow.right") {
                // Reading the dialogue is free for a guest — attempting
                // the quiz itself isn't. Same anonymous-session check as
                // the video/word/profile gates; the sheet upgrades the
                // SAME uid on sign-in (see AuthViewModel.linkOrSignIn),
                // so nothing about the dialogue they just read is lost.
                if authViewModel.isAnonymous {
                    showingSignIn = true
                } else {
                    path.append(.quiz(course, chapter))
                }
            }
            .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
            .padding(.bottom, AppSizing.spacing(3, for: sizeClass))
        }
        .sheet(isPresented: $showingSignIn) {
            AuthNavigationView()
        }
    }

    private var header: some View {
        HStack {
            Button {
                path.removeLast()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }

            Spacer()

            VStack(spacing: 2) {
                Text("Dialogue")
                    .font(.system(size: AppSizing.font(.headline, for: sizeClass), weight: .bold))
                    .foregroundColor(.white)
                Text(chapter.title)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass)))
                    .foregroundColor(.white.opacity(0.7))
            }

            Spacer()

            Button {
                path.removeAll()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: AppSizing.iconSize(sizeClass) * 0.7, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.15))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, AppSizing.horizontalPadding(sizeClass))
        .padding(.top, AppSizing.spacing(2, for: sizeClass))
        .padding(.bottom, AppSizing.spacing(2, for: sizeClass))
    }
}

private struct DialogueBubble: View {
    let line: DialogueLine
    let sizeClass: AppSizeClass

    private var isMia: Bool { line.speaker == .mia }

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if isMia {
                SpeakerAvatar(speaker: line.speaker, isMia: true, sizeClass: sizeClass)
            } else {
                Spacer(minLength: 44)
            }

            VStack(alignment: isMia ? .leading : .trailing, spacing: 4) {
                Text(line.speaker.rawValue)
                    .font(.system(size: AppSizing.font(.caption, for: sizeClass), weight: .bold))
                    .foregroundColor(isMia ? .white.opacity(0.65) : .white.opacity(0.85))

                Text(line.text)
                    .font(.system(size: AppSizing.font(.callout, for: sizeClass)))
                    .foregroundColor(isMia ? AppColors.primaryText : .white)
                    .padding(.vertical, AppSizing.spacing(2.5, for: sizeClass))
                    .padding(.horizontal, AppSizing.spacing(3, for: sizeClass))
                    .background(isMia ? Color.white : AppColors.primaryButton)
                    .clipShape(BubbleShape(isLeading: isMia, radius: AppSizing.cardCornerRadius(sizeClass) * 0.85))
                    .shadow(color: .black.opacity(isMia ? 0.08 : 0.18), radius: 8, y: 4)
            }
            .frame(maxWidth: .infinity, alignment: isMia ? .leading : .trailing)

            if isMia {
                Spacer(minLength: 44)
            } else {
                SpeakerAvatar(speaker: line.speaker, isMia: false, sizeClass: sizeClass)
            }
        }
    }
}

/// Small initial-letter avatar shown beside each dialogue bubble, tinted
/// to match its speaker's bubble color.
private struct SpeakerAvatar: View {
    let speaker: DialogueSpeaker
    let isMia: Bool
    let sizeClass: AppSizeClass

    var body: some View {
        ZStack {
            Circle()
                .fill(isMia ? Color.white.opacity(0.9) : AppColors.primaryButton)
            Text(String(speaker.rawValue.prefix(1)))
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(isMia ? AppColors.primaryButton : .white)
        }
        .frame(width: 30, height: 30)
        .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1))
    }
}

/// A rounded rectangle with one corner squared off toward its speaker's
/// avatar, giving the bubble a subtle "speech tail" without drawing an
/// actual pointer.
private struct BubbleShape: Shape {
    let isLeading: Bool
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        var corners: UIRectCorner = [.topLeft, .topRight, .bottomLeft, .bottomRight]
        corners.remove(isLeading ? .bottomLeft : .bottomRight)
        return Path(
            UIBezierPath(
                roundedRect: rect,
                byRoundingCorners: corners,
                cornerRadii: CGSize(width: radius, height: radius)
            ).cgPath
        )
    }
}
