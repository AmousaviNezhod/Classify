package com.example.domain.model

import com.example.domain.normalizer.PersianTextNormalizer
import java.util.Locale

/** Stable identity for one selectable class offering, not merely a course code. */
object ScheduleOfferingIdentity {

    private val ZERO_WIDTH_REGEX = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

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
        val offeringTeacher = PersianTextNormalizer.cleanTeacherName(offering.teacher)

        val candidates = catalog.mapNotNull { candidate ->
            if (candidate.dayIndex != offering.dayIndex) return@mapNotNull null

            val exactTime = normalizeTime(candidate.startTime) == normalizeTime(offering.startTime) &&
                normalizeTime(candidate.endTime) == normalizeTime(offering.endTime)
            val timeOverlap = timesOverlap(candidate.startTime, candidate.endTime, offering.startTime, offering.endTime)
            if (!exactTime && !timeOverlap) return@mapNotNull null

            val candidateCode = normalize(candidate.courseCode)
            val candidateName = normalizeCourseName(candidate.courseName)
            val sameCode = offeringCode.isNotBlank() && candidateCode == offeringCode
            val nameScore = courseNameScore(offeringName, candidateName)
            if (!sameCode && nameScore == 0) return@mapNotNull null
            if (offeringCode.isNotBlank() && candidateCode.isNotBlank() && candidateCode != offeringCode && nameScore < 80) {
                return@mapNotNull null
            }

            var score = if (sameCode) 100 else nameScore
            if (exactTime) score += 40 else if (timeOverlap) score += 25

            val candidateTeacher = PersianTextNormalizer.cleanTeacherName(candidate.teacher)
            val sameTeacher = offeringTeacher.isNotBlank() && candidateTeacher.isNotBlank() &&
                (offeringTeacher == candidateTeacher || candidateTeacher.contains(offeringTeacher) || offeringTeacher.contains(candidateTeacher))
            // Two known teachers that disagree means a different section — never
            // borrow its room unless the meeting slot itself is identical.
            if (offeringTeacher.isNotBlank() && candidateTeacher.isNotBlank() && !sameTeacher && !exactTime) {
                return@mapNotNull null
            }
            if (sameTeacher) score += 40

            val offeringGroup = normalizeGroup(offering.groupCode)
            val candidateGroup = normalizeGroup(candidate.groupCode)
            if (offeringGroup.isNotBlank() && candidateGroup.isNotBlank() && candidateGroup == offeringGroup) score += 20
            val offeringParity = normalize(offering.parity)
            val candidateParity = normalize(candidate.parity)
            if (offeringParity.isNotBlank() && candidateParity == offeringParity) score += 10
            candidate to score
        }
        if (candidates.isEmpty()) return null

        val maxScore = candidates.maxOf { it.second }
        val bestMatches = candidates.filter { it.second == maxScore }
        if (bestMatches.size == 1) return bestMatches[0].first

        // If multiple best matches exist:
        // 1. If only one has a group code matching:
        val offeringGroup = normalizeGroup(offering.groupCode)
        if (offeringGroup.isNotBlank()) {
            val matchingGroup = bestMatches.filter { normalizeGroup(it.first.groupCode) == offeringGroup }
            if (matchingGroup.size == 1) return matchingGroup[0].first
        }

        // 2. If all best matches agree on the classroom (or only one has a non-blank classroom), they belong to the same room (e.g. زوج/فرد or duplicate table rows):
        val distinctRooms = bestMatches.map { it.first.classroom }.filter(String::isNotBlank).distinct()
        if (distinctRooms.size == 1) {
            return bestMatches.firstOrNull { it.first.classroom.isNotBlank() }?.first
        }

        // 3. Otherwise, if rooms disagree, it is truly ambiguous.
        return null
    }

    private fun timesOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
        val s1 = toMinutes(start1)
        val e1 = toMinutes(end1)
        val s2 = toMinutes(start2)
        val e2 = toMinutes(end2)
        return maxOf(s1, s2) < minOf(e1, e2)
    }

    private fun toMinutes(time: String): Int {
        val parts = PersianTextNormalizer.toAsciiDigits(time).split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun courseNameScore(first: String, second: String): Int {
        if (first.isBlank() || second.isBlank()) return 0
        if (first == second) return 80
        val clean1 = PersianTextNormalizer.cleanCourseName(first)
        val clean2 = PersianTextNormalizer.cleanCourseName(second)
        if (clean1.isNotBlank() && clean2.isNotBlank()) {
            if (clean1 == clean2) return 80
            if (clean1.contains(clean2) || clean2.contains(clean1)) return 70

            // Token overlap (e.g. "شبکه های کامپیوتری" vs "شبکه کامپیوتر")
            val t1 = clean1.split(" ").filter { it.length > 1 }.toSet()
            val t2 = clean2.split(" ").filter { it.length > 1 }.toSet()
            if (t1.isNotEmpty() && t2.isNotEmpty()) {
                val common = t1.intersect(t2).size
                val total = maxOf(t1.size, t2.size)
                if (common >= 2 || (total > 0 && common.toFloat() / total >= 0.6f)) return 65
            }
        }
        return 0
    }

    private fun normalizeTime(value: String): String = PersianTextNormalizer.normalizeTime(value)

    private fun normalizeGroup(value: String): String {
        val normalized = normalize(value)
        return normalized.toIntOrNull()?.toString() ?: normalized
    }

    private fun isPlaceholder(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isBlank() || trimmed == "نامشخص" || trimmed.contains("مشخص نشده") || trimmed == "تعیین نشده"
    }

    /** Fill details omitted by an imported schedule without replacing user-provided values. */
    fun enrichFromCatalog(offering: ScheduleClass, catalog: List<ScheduleClass>): ScheduleClass {
        val match = findCatalogMatch(offering, catalog) ?: return offering
        return offering.copy(
            courseCode = if (isPlaceholder(offering.courseCode)) match.courseCode else offering.courseCode,
            teacher = if (isPlaceholder(offering.teacher)) match.teacher else offering.teacher,
            classroom = if (isPlaceholder(offering.classroom)) match.classroom else offering.classroom,
            parity = if (isPlaceholder(offering.parity)) match.parity else offering.parity,
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
            .replace(ZERO_WIDTH_REGEX, "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace(MULTI_SPACE_REGEX, " ")
}
