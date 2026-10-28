
import Foundation

struct CourseBadge: Identifiable, Hashable {
    let id: String
    let courseId: String
    let title: String
    let accent: CourseAccent
    let chapterCount: Int
}

enum BadgeCatalog {
    // Named to match Android's BadgeCatalog.badgeIdForCourse() exactly.
    static func badgeIdForCourse(_ courseId: String) -> String {
        "course-complete-\(courseId)"
    }

    static let badges: [CourseBadge] = CourseCatalog.courses.map { course in
        CourseBadge(
            id: badgeIdForCourse(course.id),
            courseId: course.id,
            title: course.title,
            accent: course.accent,
            chapterCount: course.chapters.count
        )
    }

    // Named to match Android's BadgeCatalog.badgeFor() exactly.
    static func badgeFor(_ courseId: String) -> CourseBadge? {
        badges.first { $0.courseId == courseId }
    }
}

/// True once every chapter in `course` appears in `completedChapterIds`.
func isCourseFullyComplete(_ course: Course, completedChapterIds: [Int]) -> Bool {
    guard !course.chapters.isEmpty else { return false }
    let completed = Set(completedChapterIds)
    return course.chapters.allSatisfy { completed.contains($0.number) }
}

/// Small lookup helper — Android's CourseCatalog has the equivalent
/// courseById(), used by BadgesScreen's "Go to course" action.
extension CourseCatalog {
    static func courseById(_ id: String) -> Course? {
        courses.first { $0.id == id }
    }
}
