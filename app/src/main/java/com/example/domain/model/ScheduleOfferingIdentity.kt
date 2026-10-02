package com.example.domain.model

import com.example.domain.normalizer.PersianTextNormalizer
import java.util.Locale

/** Stable identity for one selectable class offering, not merely a course code. */
object ScheduleOfferingIdentity {
    fun key(course: ScheduleClass): String = listOf(
        course.courseCode.ifBlank { course.courseName },
        PersianTextNormalizer.normalizeCourseName(course.courseName),
        course.groupCode,
        course.dayIndex.toString(),
        course.dayOfWeek,
        course.startTime,
        course.endTime,
        course.classroom,
        course.teacher,
        course.parity,
        course.units.toString()
    ).joinToString("|") { normalize(it) }

    /** Match an imported meeting to a PDF offering using normalized course and meeting fields. */
    fun findCatalogMatch(offering: ScheduleClass, catalog: List<ScheduleClass>): ScheduleClass? {
        val offeringCode = normalize(offering.courseCode)
        val offeringName = normalizeCourseName(offering.courseName)
        val candidates = catalog.mapNotNull { candidate ->
            if (candidate.dayIndex != offering.dayIndex ||
                normalizeTime(candidate.startTime) != normalizeTime(offering.startTime) ||
                normalizeTime(candidate.endTime) != normalizeTime(offering.endTime)
            ) return@mapNotNull null

            val candidateCode = normalize(candidate.courseCode)
            val candidateName = normalizeCourseName(candidate.courseName)
            val sameCode = offeringCode.isNotBlank() && candidateCode == offeringCode
            val nameScore = courseNameScore(offeringName, candidateName)
            if (!sameCode && nameScore == 0) return@mapNotNull null
            if (offeringCode.isNotBlank() && candidateCode.isNotBlank() && candidateCode != offeringCode && nameScore < 80) {
                return@mapNotNull null
            }

            var score = if (sameCode) 100 else nameScore
            if (nameScore >= 80) score += 5
            val offeringGroup = normalizeGroup(offering.groupCode)
            val candidateGroup = normalizeGroup(candidate.groupCode)
            if (offeringGroup.isNotBlank() && candidateGroup == offeringGroup) score += 20
            val offeringParity = normalize(offering.parity)
            val candidateParity = normalize(candidate.parity)
            if (offeringParity.isNotBlank() && candidateParity == offeringParity) score += 10
            candidate to score
        }
        if (candidates.isEmpty()) return null

        val maxScore = candidates.maxOf { it.second }
        val bestMatches = candidates.filter { it.second == maxScore }
        return bestMatches.singleOrNull()?.first
    }

    private fun courseNameScore(first: String, second: String): Int {
        if (first.isBlank() || second.isBlank()) return 0
        if (first == second) return 80
        return 0
    }

    private fun normalizeTime(value: String): String = PersianTextNormalizer.normalizeTime(value)

    private fun normalizeGroup(value: String): String {
        val normalized = normalize(value)
        return normalized.toIntOrNull()?.toString() ?: normalized
    }

    /** Fill details omitted by an imported schedule without replacing user-provided values. */
    fun enrichFromCatalog(offering: ScheduleClass, catalog: List<ScheduleClass>): ScheduleClass {
        val match = findCatalogMatch(offering, catalog) ?: return offering
        return offering.copy(
            courseCode = offering.courseCode.ifBlank { match.courseCode },
            teacher = offering.teacher.ifBlank { match.teacher },
            classroom = offering.classroom.ifBlank { match.classroom },
            parity = offering.parity.ifBlank { match.parity },
            units = offering.units.takeIf { it > 0 } ?: match.units
        )
    }

    fun searchText(course: ScheduleClass): String = normalize(
        listOf(
            PersianTextNormalizer.normalizeCourseName(course.courseName),
            course.courseCode,
            course.groupCode,
            course.dayOfWeek,
            course.startTime,
            course.endTime,
            course.classroom,
            course.teacher,
            course.parity,
            course.units.toString(),
            course.notes
        ).joinToString(" ")
    )

    private fun normalizeCourseName(value: String): String =
        PersianTextNormalizer.normalizeCourseNameForMatch(value)

    private fun normalize(value: String): String =
        PersianTextNormalizer.toAsciiDigits(PersianTextNormalizer.normalizeText(value))
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]"), "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")
}
