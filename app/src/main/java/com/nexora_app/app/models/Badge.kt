package com.nexora_app.app.models

/**
 * One badge per course in CourseCatalog. Unlocked when every chapter in
 * that course has been completed (see CourseProgress.completedChapterIds).
 *
 * `id` doubles as the Firestore doc id under users/{uid}/badges/{id} — see
 * firestore.rules' isKnownBadgeId(). Keep this catalog and that allow-list
 * in sync; courseById() below is what generates both.
 */
data class CourseBadge(
    val id: String,
    val courseId: String,
    val title: String,
    val accent: CourseAccent,
    val chapterCount: Int
)

object BadgeCatalog {

    fun badgeIdForCourse(courseId: String) = "course-complete-$courseId"

    val badges: List<CourseBadge> by lazy {
        CourseCatalog.courses.map { course ->
            CourseBadge(
                id = badgeIdForCourse(course.id),
                courseId = course.id,
                title = course.title,
                accent = course.accent,
                chapterCount = course.chapters.size
            )
        }
    }

    fun badgeFor(courseId: String): CourseBadge? = badges.firstOrNull { it.courseId == courseId }
}

/** True once every chapter in [course] appears in [completedChapterIds]. */
fun isCourseFullyComplete(course: Course, completedChapterIds: List<Int>): Boolean {
    if (course.chapters.isEmpty()) return false
    val completed = completedChapterIds.toHashSet()
    return course.chapters.all { it.number in completed }
}