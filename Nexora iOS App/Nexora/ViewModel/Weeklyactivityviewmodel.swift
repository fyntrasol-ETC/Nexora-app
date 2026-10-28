//
//  WeeklyActivityViewModel.swift
//  Elli
//
//  Backs WeeklyCheckInCard on ProfileScreen. Kept in lockstep with
//  Android's WeeklyCheckInViewModel:
//    - the week is computed in UTC (Sunday-start), same as Android's
//      `today.minusDays(today.dayOfWeek.value % 7)` in ZoneOffset.UTC.
//    - `days` is driven entirely by a live Firestore listener attached in
//      configure(uid:) — there's no local-only "fetch once" state to fall
//      out of sync with Android.
//    - checkInToday() no longer flips local state optimistically before
//      the write is confirmed; it awaits the actual Firestore write (like
//      Android's suspend call) and only clears `isCheckingIn` once that
//      completes. The listener then reflects the change, same
//      single-source-of-truth pattern as Android's comment: "No manual
//      state flip needed — Firestore is the source of truth."
//

import Foundation
import Combine
import FirebaseFirestore

enum WeeklyDayState: Equatable {
    case completed   // checked in — green tick
    case missed      // past, never checked in — red x
    case today       // current day, tappable
    case upcoming    // hasn't happened yet — neutral
}

struct WeeklyDayModel: Identifiable {
    let id = UUID()
    let dateId: String   // yyyy-MM-dd, UTC — the same id used as the Firestore doc id
    let label: String
    var state: WeeklyDayState
}

@MainActor
final class WeeklyActivityViewModel: ObservableObject {
    @Published private(set) var days: [WeeklyDayModel] = WeeklyActivityViewModel.emptyWeek()
    @Published private(set) var isCheckingIn: Bool = false

    private let service = WeeklyActivityService()
    private var uid: String?
    private var listener: ListenerRegistration?

    /// True once the current session is a real account. Per spec,
    /// checking in requires a real account — ProfileScreen should check
    /// `authViewModel.isRealAccount` before rendering the tappable
    /// check-in state at all (showing a `SignInPromptCard` instead for a
    /// guest); this flag is the same gate applied a second time inside
    /// `checkInToday()` itself as a safety net.
    private(set) var isRealAccount = false

    /// Sunday...Saturday labels, matching Android's day-pill order.
    private static let symbols = ["S", "M", "T", "W", "T", "F", "S"]

    private static var utcCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        calendar.firstWeekday = 1 // Sunday
        return calendar
    }()

    /// Midnight UTC on the Sunday of the week containing `date`. Mirrors
    /// Android's Sunday-start week, but computed in UTC on both sides now
    /// instead of iOS using the device's local calendar.
    private static func startOfWeek(containing date: Date) -> Date {
        let components = utcCalendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        return utcCalendar.date(from: components) ?? date
    }

    private static func weekDates(containing date: Date = Date()) -> [Date] {
        let sunday = startOfWeek(containing: date)
        return (0..<7).compactMap { utcCalendar.date(byAdding: .day, value: $0, to: sunday) }
    }

    private static func emptyWeek() -> [WeeklyDayModel] {
        let todayId = WeeklyActivityService.dateId(for: Date())
        return weekDates().enumerated().map { index, date in
            let dateId = WeeklyActivityService.dateId(for: date)
            let state: WeeklyDayState = dateId == todayId ? .today
                : (dateId < todayId ? .missed : .upcoming) // yyyy-MM-dd sorts chronologically
            return WeeklyDayModel(dateId: dateId, label: symbols[index], state: state)
        }
    }

    /// Rebuilds the week from a set of checked-in dateIds — the same shape
    /// as Android's `buildState(checkedInDates:)`.
    private static func buildDays(checkedInDateIds: Set<String>) -> [WeeklyDayModel] {
        let todayId = WeeklyActivityService.dateId(for: Date())
        return weekDates().enumerated().map { index, date in
            let dateId = WeeklyActivityService.dateId(for: date)
            let state: WeeklyDayState
            if checkedInDateIds.contains(dateId) {
                state = .completed
            } else if dateId == todayId {
                state = .today
            } else if dateId < todayId {
                state = .missed
            } else {
                state = .upcoming
            }
            return WeeklyDayModel(dateId: dateId, label: symbols[index], state: state)
        }
    }

    var checkedInCount: Int { days.filter { $0.state == .completed }.count }

    var isTodayCheckedIn: Bool {
        let todayId = WeeklyActivityService.dateId(for: Date())
        return days.first(where: { $0.dateId == todayId })?.state == .completed
    }

    /// Call once the signed-in user's uid is known (ProfileScreen's
    /// `.onAppear` / `.task`, same pattern as before). Attaches a live
    /// listener for this week's dateIds — replaces the old one-shot
    /// `fetchActiveDays` call, so check-ins from Android (or another
    /// session) now show up here without needing to relaunch the app.
    ///
    /// NOTE: this is no longer `async` — it just attaches a listener and
    /// returns immediately. If your ProfileScreen currently does
    /// `.task { await viewModel.configure(uid: uid) }`, drop the `await`:
    /// `.task { viewModel.configure(uid: uid) }` (or call it from
    /// `.onAppear` instead).
    func configure(uid: String, isRealAccount: Bool) {
        guard self.uid != uid || self.isRealAccount != isRealAccount else { return }
        self.uid = uid
        self.isRealAccount = isRealAccount
        listener?.remove()

        // A guest has never been allowed to check in, so there's
        // nothing checked-in to listen for yet — `days` just stays the
        // placeholder empty week from init.
        guard isRealAccount else { return }

        let dateIds = Self.weekDates().map { WeeklyActivityService.dateId(for: $0) }

        listener = service.observeActiveDays(uid: uid, dateIds: dateIds) { [weak self] activeDateIds in
            guard let self else { return }
            Task { @MainActor in
                self.days = Self.buildDays(checkedInDateIds: activeDateIds)
            }
        }
    }

    /// Marks today checked in. Awaits the actual Firestore write instead
    /// of flipping local state first — if the write fails, `days` simply
    /// never shows today as completed (and the error is logged), rather
    /// than showing a false "checked in" that quietly reverts on next
    /// launch.
    func checkInToday() {
        guard isRealAccount, let uid, !isTodayCheckedIn, !isCheckingIn else { return }

        isCheckingIn = true
        let dateId = WeeklyActivityService.dateId(for: Date())

        Task {
            defer { isCheckingIn = false }
            do {
                try await service.checkIn(uid: uid, dateId: dateId)
                // No manual state flip here — the listener attached in
                // configure(uid:) will receive the update and rebuild
                // `days`, same single-source-of-truth flow as Android.
            } catch {
                print("WeeklyActivityViewModel: check-in failed for \(dateId) — \(error.localizedDescription)")
            }
        }
    }

    deinit {
        listener?.remove()
    }
}
