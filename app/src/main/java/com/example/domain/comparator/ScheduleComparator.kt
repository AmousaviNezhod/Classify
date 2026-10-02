package com.example.domain.comparator

import com.example.domain.model.DiffType
import com.example.domain.model.FieldDiff
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleClassDiff
import com.example.domain.model.ScheduleComparisonSummary
import com.example.domain.normalizer.PersianTextNormalizer

object ScheduleComparator {

    /**
     * Compares the user's base registered schedule with the latest university schedule.
     */
    fun compare(
        currentSchedule: NormalizedSchedule,
        universitySchedule: NormalizedSchedule
    ): ScheduleComparisonSummary {
        val currentClasses = currentSchedule.classes
        val universityClasses = universitySchedule.classes

        val diffs = mutableListOf<ScheduleClassDiff>()
        val matchedUniversityClasses = mutableSetOf<String>()

        var unchangedCount = 0
        var modifiedCount = 0
        var addedCount = 0
        var removedCount = 0

        // 1. Process current classes against university classes
        for (current in currentClasses) {
            val candidateMatch = findBestMatch(current, universityClasses, matchedUniversityClasses)

            if (candidateMatch == null) {
                // Class was in current schedule but not found in university schedule
                diffs.add(
                    ScheduleClassDiff(
                        courseName = current.courseName,
                        groupCode = current.groupCode,
                        diffType = DiffType.REMOVED,
                        currentClass = current,
                        universityClass = null
                    )
                )
                removedCount++
            } else {
                matchedUniversityClasses.add(candidateMatch.id)
                val fieldChanges = computeFieldDifferences(current, candidateMatch)

                if (fieldChanges.isEmpty()) {
                    diffs.add(
                        ScheduleClassDiff(
                            courseName = current.courseName,
                            groupCode = current.groupCode,
                            diffType = DiffType.UNCHANGED,
                            currentClass = current,
                            universityClass = candidateMatch
                        )
                    )
                    unchangedCount++
                } else {
                    diffs.add(
                        ScheduleClassDiff(
                            courseName = current.courseName,
                            groupCode = current.groupCode,
                            diffType = DiffType.MODIFIED,
                            currentClass = current,
                            universityClass = candidateMatch,
                            changes = fieldChanges
                        )
                    )
                    modifiedCount++
                }
            }
        }

        // 2. Identify new classes in university schedule that user hasn't registered
        for (uniClass in universityClasses) {
            if (!matchedUniversityClasses.contains(uniClass.id)) {
                diffs.add(
                    ScheduleClassDiff(
                        courseName = uniClass.courseName,
                        groupCode = uniClass.groupCode,
                        diffType = DiffType.ADDED,
                        currentClass = null,
                        universityClass = uniClass
                    )
                )
                addedCount++
            }
        }

        return ScheduleComparisonSummary(
            totalChecked = diffs.size,
            unchangedCount = unchangedCount,
            modifiedCount = modifiedCount,
            addedCount = addedCount,
            removedCount = removedCount,
            diffs = diffs.sortedWith(
                compareBy(
                    {
                        when (it.diffType) {
                            DiffType.MODIFIED -> 0
                            DiffType.REMOVED -> 1
                            DiffType.ADDED -> 2
                            DiffType.UNCHANGED -> 3
                        }
                    },
                    { it.courseName }
                )
            )
        )
    }

    private fun findBestMatch(
        target: ScheduleClass,
        candidates: List<ScheduleClass>,
        alreadyMatched: Set<String>
    ): ScheduleClass? {
        val targetNorm = PersianTextNormalizer.cleanCourseName(target.courseName)

        // 1. Exact match by course name and group code
        val exactMatch = candidates.firstOrNull { candidate ->
            !alreadyMatched.contains(candidate.id) &&
                    PersianTextNormalizer.cleanCourseName(candidate.courseName) == targetNorm &&
                    (candidate.groupCode == target.groupCode || target.groupCode.isEmpty())
        }
        if (exactMatch != null) return exactMatch

        // 2. Match by course code if present
        if (target.courseCode.isNotBlank()) {
            val codeMatch = candidates.firstOrNull { candidate ->
                !alreadyMatched.contains(candidate.id) &&
                        candidate.courseCode == target.courseCode
            }
            if (codeMatch != null) return codeMatch
        }

        // 3. Fuzzy name match (contains or startsWith)
        return candidates.firstOrNull { candidate ->
            if (alreadyMatched.contains(candidate.id)) return@firstOrNull false
            val candNorm = PersianTextNormalizer.cleanCourseName(candidate.courseName)
            candNorm == targetNorm ||
                    (targetNorm.length >= 4 && (candNorm.contains(targetNorm) || targetNorm.contains(candNorm)))
        }
    }

    private fun computeFieldDifferences(
        oldClass: ScheduleClass,
        newClass: ScheduleClass
    ): List<FieldDiff> {
        val changes = mutableListOf<FieldDiff>()

        // Day of week
        if (oldClass.dayOfWeek != newClass.dayOfWeek) {
            changes.add(
                FieldDiff(
                    fieldName = "روز برگزاری",
                    oldValue = oldClass.dayOfWeek,
                    newValue = newClass.dayOfWeek
                )
            )
        }

        // Time range
        val oldTime = "${oldClass.startTime} الی ${oldClass.endTime}"
        val newTime = "${newClass.startTime} الی ${newClass.endTime}"
        if (oldClass.startTime != newClass.startTime || oldClass.endTime != newClass.endTime) {
            changes.add(
                FieldDiff(
                    fieldName = "ساعت برگزاری",
                    oldValue = oldTime,
                    newValue = newTime
                )
            )
        }

        // Instructor / Teacher
        if (oldClass.teacher.isNotBlank() && newClass.teacher.isNotBlank() &&
            PersianTextNormalizer.normalizeText(oldClass.teacher) != PersianTextNormalizer.normalizeText(newClass.teacher) &&
            !newClass.teacher.contains("مشخص نشده")
        ) {
            changes.add(
                FieldDiff(
                    fieldName = "استاد",
                    oldValue = oldClass.teacher,
                    newValue = newClass.teacher
                )
            )
        }

        // Classroom
        if (oldClass.classroom.isNotBlank() && newClass.classroom.isNotBlank() &&
            PersianTextNormalizer.normalizeText(oldClass.classroom) != PersianTextNormalizer.normalizeText(newClass.classroom) &&
            !newClass.classroom.contains("نامشخص")
        ) {
            changes.add(
                FieldDiff(
                    fieldName = "کلاس/مکان",
                    oldValue = oldClass.classroom,
                    newValue = newClass.classroom
                )
            )
        }

        // Group code
        if (oldClass.groupCode.isNotBlank() && newClass.groupCode.isNotBlank() &&
            oldClass.groupCode != newClass.groupCode
        ) {
            changes.add(
                FieldDiff(
                    fieldName = "گروه",
                    oldValue = oldClass.groupCode,
                    newValue = newClass.groupCode
                )
            )
        }

        return changes
    }
}
