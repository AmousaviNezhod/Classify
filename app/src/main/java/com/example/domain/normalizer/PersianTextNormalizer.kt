package com.example.domain.normalizer

import java.text.Normalizer
import java.util.Locale

object PersianTextNormalizer {

    private val PERSIAN_DAYS = listOf(
        "شنبه",
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنج‌شنبه",
        "جمعه"
    )

    /**
     * Converts Arabic characters to standard Persian characters,
     * strips kashida, normalizes ZWNJ and trims duplicate whitespace.
     */
    fun normalizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var text = Normalizer.normalize(input, Normalizer.Form.NFKC)
            // Remove zero-width and directional marks; actual line breaks become word separators below.
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]"), "")
            .replace(Regex("[\\r\\n\\u0000-\\u001F\\u007F]"), " ")
            // Arabic letters to Persian
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('إ', 'ا')
            .replace('أ', 'ا')
            .replace('آ', 'ا')
            .replace("ء", "")
            // Remove Tatweel / Kashida
            .replace("ـ", "")
            // Normalize non-breaking spaces & ZWNJ
            .replace('\u00A0', ' ')
            .replace("\u200C", " ") // treat ZWNJ as soft space for matching
            .replace("\t", " ")

        // Replace multiple horizontal spaces with single space, preserving newlines
        text = text.replace(Regex("[^\\S\\r\\n]+"), " ")

        // Fix common PDF Lam-Alef ligature mis-extractions
        text = text
            .replace("کالس", "کلاس")
            .replace("سیاالت", "سیالات")
            .replace("اصالحی", "اصلاحی")
            .replace("میالد", "میلاد")
            .replace("اطالعات", "اطلاعات")
            .replace("اخالق", "اخلاق")
            .replace("انقالب", "انقلاب")
            .replace("سالمت", "سلامت")
            .replace("جالئیان", "جلائیان")

        return text.trim()
    }

    /**
     * Normalizes Persian text while preserving multiple spaces (crucial for column alignment in PDF tables).
     */
    fun normalizeTextPreservingSpacing(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var text = Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]"), "")
            .replace(Regex("[\\r\\n]+"), " ")
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('إ', 'ا')
            .replace('أ', 'ا')
            .replace('آ', 'ا')
            .replace("ء", "")
            .replace("ـ", "")
            .replace('\u00A0', ' ')
            .replace("\u200C", " ")
            .replace("کالس", "کلاس")
            .replace("سیاالت", "سیالات")
            .replace("اصالحی", "اصلاحی")
            .replace("میالد", "میلاد")
            .replace("اطالعات", "اطلاعات")
            .replace("اخالق", "اخلاق")
            .replace("انقالب", "انقلاب")
            .replace("سالمت", "سلامت")
            .replace("جالئیان", "جلائیان")
        return text.trimEnd()
    }

    private val REGEX_NEWLINES = Regex("[\\r\\n]+")
    private val REGEX_SPACES = Regex("\\s+")
    private val REGEX_PUNCT = Regex("[\\s\\p{Punct}،؛؟٪٫٬ـ]+")
    private val REGEX_PREFIX_CUT = Regex("^(?:ساعت|زمان|روز|کلاس|درس|نام درس|عنوان درس|مبحث|کد درس)[:\\s-]*")
    private val REGEX_TEACHER_CUT = Regex("(?:استاد|مدرس|دکتر|مهندس)(?:\\s*[:：]|\\s+|-)\\s*.*$")
    private val REGEX_LAB_PREFIX = Regex("^(?:ازمایشگاه|آزمایشگاه|کارگاه|از|آز)(?:\\s*[و/&+]\\s*(?:ازمایشگاه|آزمایشگاه|کارگاه|از|آز))?\\s+")
    private val REGEX_TRIM_PUNCT = Regex("^[\\s:：،,;؛.()\\-]+|[\\s:：،,;؛.()\\-]+$")

    /** Normalize extracted/imported course names without dropping meaningful digits. */
    fun normalizeCourseName(input: String?): String = normalizeText(input)
        .replace(REGEX_NEWLINES, " ")
        .replace(REGEX_SPACES, " ")
        .trim()

    /** Normalize punctuation and spacing consistently for strict course-name matching. */
    fun normalizeCourseNameForMatch(input: String?): String = toAsciiDigits(normalizeCourseName(input))
        .lowercase(Locale.ROOT)
        .replace(REGEX_PUNCT, "")

    /**
     * Converts Persian and Arabic digits to standard ASCII digits.
     */
    fun toAsciiDigits(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append((ch - '۰'))
                in '٠'..'٩' -> sb.append((ch - '٠'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts ASCII digits to Persian digits for UI display.
     */
    fun toPersianDigits(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(('۰'.code + (ch - '0')).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes a day name to standard Persian day representation.
     */
    fun normalizeDay(rawDay: String?): Pair<String, Int> {
        val normalized = normalizeText(rawDay).replace(" ", "")
        return when {
            normalized.contains("شنبه") && !normalized.contains("یک") && !normalized.contains("دو") &&
                    !normalized.contains("سه") && !normalized.contains("چهار") && !normalized.contains("پنج") ->
                "شنبه" to 0

            normalized.contains("یکشنبه") || normalized.contains("يكشنبه") ->
                "یکشنبه" to 1

            normalized.contains("دوشنبه") ->
                "دوشنبه" to 2

            normalized.contains("سه") && normalized.contains("شنبه") ->
                "سه‌شنبه" to 3

            normalized.contains("چهارشنبه") || normalized.contains("۴شنبه") ->
                "چهارشنبه" to 4

            normalized.contains("پنج") && normalized.contains("شنبه") ->
                "پنج‌شنبه" to 5

            normalized.contains("جمعه") ->
                "جمعه" to 6

            else -> (rawDay?.trim() ?: "نامشخص") to 0
        }
    }

    /**
     * Normalizes time into HH:mm format.
     */
    fun normalizeTime(rawTime: String?): String {
        if (rawTime.isNullOrBlank()) return "08:00"
        val ascii = toAsciiDigits(rawTime).trim()
        val parts = ascii.split(":")
        val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /**
     * Extracts start and end times from time expressions such as:
     * "08:00 - 10:00", "8 الی 10", "8:30 تا 10:00", "13-15", "8 to 10"
     */
    fun extractTimeRange(text: String): Pair<String, String>? {
        val ascii = toAsciiDigits(text)
        val pattern = Regex("(\\d{1,2}(?::\\d{2})?)\\s*(?:-|–|—|الی|تا|to)\\s*(\\d{1,2}(?::\\d{2})?)", RegexOption.IGNORE_CASE)
        val match = pattern.find(ascii) ?: return null
        val rawStart = match.groupValues[1]
        val rawEnd = match.groupValues[2]

        val h1 = rawStart.split(":").firstOrNull()?.toIntOrNull() ?: 8
        val h2 = rawEnd.split(":").firstOrNull()?.toIntOrNull() ?: 10

        return if (h1 > h2 && (h1 - h2) in 1..5) {
            normalizeTime(rawEnd) to normalizeTime(rawStart)
        } else {
            normalizeTime(rawStart) to normalizeTime(rawEnd)
        }
    }

    /**
     * Semantic normalization of course names:
     * Removes leading/trailing "درس", "آزمایشگاه", "کارگاه", digits, punctuation.
     */
    fun cleanCourseName(name: String): String {
        val cleaned = toAsciiDigits(normalizeCourseName(name))
            .replace(REGEX_PREFIX_CUT, "")
            // A teacher reference cuts the rest of the cell — but only when it is a
            // separate word: "مهندسی پی" is a course name, not "مهندس" + junk.
            .replace(REGEX_TEACHER_CUT, "")
            .replace(REGEX_LAB_PREFIX, "")
            .replace(REGEX_TRIM_PUNCT, "")
            .trim()
        // A class/room code by itself is not a course name. Keep digits inside real names.
        return cleaned.takeIf { value -> value.any(Char::isLetter) }.orEmpty()
    }

    /**
     * Normalizes a teacher name by removing academic/courtesy titles (دکتر، مهندس، استاد، ...)
     */
    fun cleanTeacherName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return normalizeText(name)
            .replace(Regex("^(?:دکتر|مهندس|استاد|سید|سیده|خانم|آقای)\\s+"), "")
            // PDF cells smuggle parity and group markers into the teacher field,
            // e.g. "یداللهی شاه راه *زوج", "کهرم*زوج گ1 -کلانی", "شریف زاده(تا71)".
            // They are not part of the name and must not split name matching.
            .replace(Regex("\\s*[*×•·]\\s*(?:زوج|فرد|هفتگی)?"), " ")
            .replace(Regex("\\([^()]*\\)"), " ")
            .replace(Regex("(?<![\\p{L}\\p{N}])(?:گروه|گ)\\s*\\d+(?![\\p{L}\\p{N}])"), " ")
            .replace(Regex("\\s*[-–—]\\s*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
