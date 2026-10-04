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

        // 1. Match the user's registered sessions against university offerings.
        // All plausible pairs are scored first and assigned greedily, strongest
        // claim first, so no session can steal the offering that belongs to a
        // better-fitting session of the same course regardless of input order.
        val matchByTarget = mutableMapOf<Int, ScheduleClass>()
        val scoredPairs = currentClasses.flatMapIndexed { index, target ->
            scoreMatches(target, universityClasses).map { (candidate, score) ->
                Triple(index, candidate, score)
            }
        }.sortedWith(
            compareByDescending<Triple<Int, ScheduleClass, Int>> { it.third }
                .thenBy { it.first }
        )
        for ((targetIndex, candidate, _) in scoredPairs) {
            if (targetIndex in matchByTarget) continue
            if (candidate.id in matchedUniversityClasses) continue
            matchByTarget[targetIndex] = candidate
            matchedUniversityClasses.add(candidate.id)
        }

        for ((targetIndex, current) in currentClasses.withIndex()) {
            val candidateMatch = matchByTarget[targetIndex]

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

    /**
     * Every plausible offering for [target] with its match score, identity gates
     * applied. Names are compared space- and punctuation-insensitively so
     * "برنامه نویسی مبتنی بروب" finds "برنامه نویسی مبتنی بر وب(کاردانی)".
     */
    private fun scoreMatches(
        target: ScheduleClass,
        candidates: List<ScheduleClass>
    ): List<Pair<ScheduleClass, Int>> {
        val targetNorm = PersianTextNormalizer.normalizeCourseNameForMatch(target.courseName)
        val targetCode = PersianTextNormalizer.toAsciiDigits(target.courseCode)
        val targetTeacher = PersianTextNormalizer.cleanTeacherName(target.teacher)
        val targetGroup = PersianTextNormalizer.toAsciiDigits(target.groupCode)

        return candidates.mapNotNull { cand ->
            val candNorm = PersianTextNormalizer.normalizeCourseNameForMatch(cand.courseName)
            val candCode = PersianTextNormalizer.toAsciiDigits(cand.courseCode)
            val sameCode = targetCode.isNotBlank() && candCode.isNotBlank() && targetCode == candCode
            val sameName = targetNorm.isNotBlank() && candNorm.isNotBlank() && (
                targetNorm == candNorm ||
                (targetNorm.length >= 4 && (candNorm.contains(targetNorm) || targetNorm.contains(candNorm)))
                )
            if (!sameCode && !sameName) return@mapNotNull null

            val candTeacher = PersianTextNormalizer.cleanTeacherName(cand.teacher)
            val sameTeacher = targetTeacher.isNotBlank() && candTeacher.isNotBlank() && (
                targetTeacher == candTeacher || candTeacher.contains(targetTeacher) || targetTeacher.contains(candTeacher)
                )
            val exactTime = cand.startTime == target.startTime && cand.endTime == target.endTime
            val sameDay = cand.dayIndex == target.dayIndex

            // Two known teachers that disagree means different sections — unless the
            // slot itself is identical, which reads as "same class, new teacher".
            if (targetTeacher.isNotBlank() && candTeacher.isNotBlank() && !sameTeacher && !(sameDay && exactTime)) {
                return@mapNotNull null
            }

            // A meeting on another day is only believable as "the class moved" when
            // the teacher or the course code corroborates the name; otherwise a
            // same-named section elsewhere would be claimed by mistake.
            if (!sameDay && !sameCode && !sameTeacher) {
                return@mapNotNull null
            }

            var score = 0
            if (sameCode) score += 100
            score += if (targetNorm == candNorm) 80 else 60

            // Day match is critical
            if (sameDay) score += 50
            if (sameTeacher) score += 40

            score += when {
                exactTime -> 40
                timesOverlap(cand.startTime, cand.endTime, target.startTime, target.endTime) -> 30
                else -> 0
            }

            // Group match
            val candGroup = PersianTextNormalizer.toAsciiDigits(cand.groupCode)
            if (targetGroup.isNotBlank() && candGroup.isNotBlank()) {
                score += if (targetGroup == candGroup) 30 else -15
            }

            // Parity match
            if (target.parity.isNotBlank() && cand.parity.isNotBlank()) {
                score += if (target.parity == cand.parity) 15 else -10
            }

            if (score >= 80) cand to score else null
        }
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

    private fun computeFieldDifferences(
        oldClass: ScheduleClass,
        newClass: ScheduleClass
    ): List<FieldDiff> {
        val changes = mutableListOf<FieldDiff>()

        // Day of week
        if (oldClass.dayOfWeek != newClass.dayOfWeek && oldClass.dayIndex != newClass.dayIndex) {
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
        val oldTeacherClean = PersianTextNormalizer.cleanTeacherName(oldClass.teacher)
        val newTeacherClean = PersianTextNormalizer.cleanTeacherName(newClass.teacher)
        if (oldTeacherClean.isNotBlank() && newTeacherClean.isNotBlank() &&
            oldTeacherClean != newTeacherClean &&
            !newTeacherClean.contains(oldTeacherClean) &&
            !oldTeacherClean.contains(newTeacherClean) &&
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
        val oldGroup = PersianTextNormalizer.toAsciiDigits(oldClass.groupCode)
        val newGroup = PersianTextNormalizer.toAsciiDigits(newClass.groupCode)
        if (oldGroup.isNotBlank() && newGroup.isNotBlank() && oldGroup != newGroup) {
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
