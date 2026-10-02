package com.example.domain.parser

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.PdfScheduleParseResult
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.UUID

object PdfScheduleParser {
    private const val TAG = "PdfScheduleParser"
    private const val POSITIONED_PREFIX = "@@ROW@@"
    private val devDiagnosticsEnabled = BuildConfig.DEBUG
    private val rowRegex = Regex("^@@ROW@@([0-9]+),([0-9.]+)@@(.*)$")
    private val cellRegex = Regex("⟦(-?[0-9]+)⟧([^⟦]*)")
    private data class Cell(val x: Float, val text: String)
    private data class Row(val page: Int, val y: Float, val cells: List<Cell>)
    private data class Glyph(val page: Int, val x: Float, val y: Float, val width: Float, val fontSize: Float, val text: String)
    private data class TimeSlot(val left: Float, val right: Float, val start: String, val end: String)
    private data class ScheduleTable(val titleIndex: Int, val headerIndex: Int, val endIndex: Int, val title: String, val day: Pair<String, Int>, val slots: List<TimeSlot>)
    private data class SectionAnchor(val rowIndex: Int, val groupCode: String, val room: String)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", e)
        }
    }

    fun extractText(file: File, context: Context? = null): String {
        context?.let(::init)
        return try {
            PDDocument.load(file).use { PDFTextStripper().apply { sortByPosition = true }.getText(it) ?: "" }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from ${file.name} using PDFBox: ${e.message}")
            fallbackExtractText(file)
        }
    }

    fun extractText(inputStream: InputStream, context: Context? = null): String {
        context?.let(::init)
        return try {
            PDDocument.load(inputStream).use { PDFTextStripper().apply { sortByPosition = true }.getText(it) ?: "" }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from stream: ${e.message}")
            ""
        }
    }

    fun parsePdfFile(file: File, scheduleId: String = "university_schedule", context: Context? = null): List<ScheduleClass> =
        parsePdfFileDetailed(file, scheduleId, context).classes

    fun parsePdfFileDetailed(file: File, scheduleId: String = "university_schedule", context: Context? = null): PdfScheduleParseResult {
        context?.let(::init)
        if (!file.exists() || file.length() == 0L) return PdfScheduleParseResult(diagnostics = listOf("PDF file is missing or empty"))
        val texts = extractTextAlternatives(file)
        if (texts.isEmpty()) {
            Log.w(TAG, "Extracted text was empty for file: ${file.name}")
            return PdfScheduleParseResult(diagnostics = listOf("PDF text extraction returned no text"))
        }
        return parseTextCandidates(texts, scheduleId, file.name)
    }

    fun parseScheduleText(rawText: String, scheduleId: String, sourceTag: String = ""): List<ScheduleClass> =
        parseScheduleTextDetailed(rawText, scheduleId, sourceTag).classes

    fun parseScheduleTextDetailed(rawText: String, scheduleId: String, sourceTag: String = ""): PdfScheduleParseResult =
        parseTextCandidates(listOf(rawText), scheduleId, sourceTag)

    /** Test/debug entry for the same coordinate-row representation emitted by PDFBox. */
    fun parsePositionedScheduleText(rawText: String, scheduleId: String, sourceTag: String = ""): PdfScheduleParseResult =
        parseTextCandidates(listOf(rawText), scheduleId, sourceTag)

    /** Run independent logical-order and position-aware PDF reads; retain both as parser inputs. */
    private fun extractTextAlternatives(file: File): List<String> {
        val result = mutableListOf<String>()
        try {
            PDDocument.load(file).use { document ->
                positionRows(document).takeIf(String::isNotBlank)?.let(result::add)
                val normalText = PDFTextStripper().apply { sortByPosition = true }.getText(document)
                normalText.takeIf(String::isNotBlank)?.let(result::add)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from ${file.name} using PDFBox: ${e.message}")
            fallbackExtractText(file).takeIf(String::isNotBlank)?.let(result::add)
        }
        return result.distinct()
    }

    /** Reconstruct visual text rows using glyph geometry; PDF text offsets are unreliable for RTL. */
    private fun positionRows(document: PDDocument): String {
        val glyphs = mutableListOf<Glyph>()
        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                textPositions.forEach { position ->
                    val value = position.unicode.orEmpty()
                    if (value.isNotBlank()) glyphs += Glyph(
                        page = currentPageNo,
                        x = position.xDirAdj,
                        y = position.yDirAdj,
                        width = position.widthDirAdj,
                        fontSize = position.fontSizeInPt,
                        text = value
                    )
                }
            }
        }
        stripper.sortByPosition = true
        stripper.getText(document)

        val rows = glyphs.groupBy(Glyph::page).toSortedMap().flatMap { (page, pageGlyphs) ->
            val baselines = mutableListOf<MutableList<Glyph>>()
            pageGlyphs.sortedBy(Glyph::y).forEach { glyph ->
                val baseline = baselines.lastOrNull()
                    ?.takeIf { kotlin.math.abs(it.map(Glyph::y).average() - glyph.y) <= 2.8 }
                    ?: mutableListOf<Glyph>().also(baselines::add)
                baseline += glyph
            }
            baselines.mapNotNull { baseline ->
                val chunks = mutableListOf<MutableList<Glyph>>()
                baseline.sortedBy(Glyph::x).forEach { glyph ->
                    val previous = chunks.lastOrNull()?.lastOrNull()
                    val gap = previous?.let { glyph.x - (it.x + it.width) } ?: 0f
                    val threshold = maxOf(4f, glyph.fontSize * 1.35f)
                    if (previous == null || gap <= threshold) {
                        if (previous == null) chunks += mutableListOf(glyph) else chunks.last() += glyph
                    } else chunks += mutableListOf(glyph)
                }
                val cells = chunks.mapNotNull { chunk ->
                    val rtl = chunk.any { glyph -> glyph.text.any { it in '\u0600'..'\u06FF' } }
                    val ordered = if (rtl) chunk.sortedByDescending(Glyph::x) else chunk.sortedBy(Glyph::x)
                    val value = buildString {
                        var previous: Glyph? = null
                        ordered.forEach { glyph ->
                            val prior = previous
                            val gap = prior?.let {
                                if (rtl) it.x - (glyph.x + glyph.width) else glyph.x - (it.x + it.width)
                            } ?: 0f
                            if (prior != null && gap > maxOf(1f, glyph.fontSize * 0.22f)) append(' ')
                            append(glyph.text)
                            previous = glyph
                        }
                    }.trim()
                    value.takeIf(String::isNotBlank)?.let { Cell(chunk.minOf(Glyph::x), PersianTextNormalizer.normalizeText(it)) }
                }
                if (cells.isEmpty()) null else Row(page, baseline.map(Glyph::y).average().toFloat(), cells)
            }
        }
        return rows.joinToString("\n") { row ->
            val cells = row.cells.sortedBy(Cell::x).joinToString("") { "⟦${it.x.toInt()}⟧${it.text}" }
            "$POSITIONED_PREFIX${row.page},${"%.1f".format(Locale.US, row.y)}@@$cells"
        }
    }

    private fun parseTextCandidates(texts: List<String>, scheduleId: String, sourceTag: String): PdfScheduleParseResult {
        val positionedText = texts.firstOrNull { it.startsWith(POSITIONED_PREFIX) }
        if (positionedText != null) {
            val positionedNormal = parsePositionedNormalTables(positionedText, scheduleId, sourceTag)
            val positionedWorkshops = parsePositionedRows(positionedText, scheduleId, sourceTag)
            val hasNormalTable = positionedText.lineSequence()
                .mapNotNull(::parsePositionedRow)
                .groupBy(Row::page)
                .values
                .any { rows ->
                    rows.any { row ->
                        val title = row.cells.joinToString(" ") { it.text }
                        isTableTitle(title) && isScheduleTableTitle(title) && extractDay(title) != null
                    }
                }
            if (positionedNormal.isNotEmpty() || positionedWorkshops.isNotEmpty()) {
                val announcements = texts.filterNot { it.startsWith(POSITIONED_PREFIX) }
                    .flatMap { text ->
                        val lines = text.lines().map(PersianTextNormalizer::normalizeTextPreservingSpacing).filter(String::isNotBlank)
                        parseAnnouncementLines(lines, scheduleId, sourceTag)
                    }
                val combinedNormal = (positionedNormal + announcements).distinctBy(ScheduleOfferingIdentity::key)
                // Once the positioned read identifies the schedule table, don't fall
                // through to a differently ordered text serialization and shift fields.
                return buildParseResult(combinedNormal, positionedWorkshops, positionedText)
            }
        }

        // PDFBox reads are alternative serializations of one document; never merge their rows.
        val candidates = texts.filterNot { it.startsWith(POSITIONED_PREFIX) }.map { text ->
            val lines = text.lines().map(PersianTextNormalizer::normalizeTextPreservingSpacing).filter(String::isNotBlank)
            val parsed = buildList {
                addAll(parseSadjadMatrix(lines, scheduleId, sourceTag))
                addAll(parseLabAndSiteSchedule(lines, scheduleId, sourceTag))
                addAll(parseAnnouncementLines(lines, scheduleId, sourceTag))
                addAll(parseLinearLines(lines, scheduleId, sourceTag))
            }
            normalizeAndValidate(parsed)
        }.filter(List<ScheduleClass>::isNotEmpty)

        val best = candidates.maxByOrNull { rows ->
            rows.size * 10 + rows.count { isValidCourseName(it.courseName) }
        }.orEmpty()
        val bestWorkshopRows = texts.filterNot { it.startsWith(POSITIONED_PREFIX) }
            .map { text ->
                val lines = text.lines().map(PersianTextNormalizer::normalizeTextPreservingSpacing).filter(String::isNotBlank)
                normalizeAndValidate(parseLabAndSiteSchedule(lines, scheduleId, sourceTag))
            }
            .maxByOrNull(List<ScheduleClass>::size)
            .orEmpty()
        val bestNormalRows = best.filterNot { course ->
            course.notes.contains("آزمایشگاه") || course.notes.contains("کارگاه") || course.notes.contains("سایت")
        }
        return buildParseResult(bestNormalRows, bestWorkshopRows, "")
    }

    private fun buildParseResult(normal: List<ScheduleClass>, workshops: List<ScheduleClass>, positionedText: String): PdfScheduleParseResult {
        val normalizedNormal = normalizeAndValidate(normal).distinctBy(ScheduleOfferingIdentity::key)
        val normalizedWorkshops = normalizeAndValidate(workshops).distinctBy(ScheduleOfferingIdentity::key)
        val titles = if (positionedText.isBlank()) 0 else positionedText.lineSequence()
            .mapNotNull(::parsePositionedRow)
            .count { row -> row.cells.joinToString(" ") { it.text }.let(::isTableTitle) }
        val groups = (normalizedNormal + normalizedWorkshops).map { it.groupCode }.filter(String::isNotBlank).toSet()
        val diagnostics = if (devDiagnosticsEnabled) (normalizedNormal + normalizedWorkshops).map { course ->
            "day=${course.dayOfWeek}, course_name=${course.courseName}, course_code=${course.courseCode}, group=${course.groupCode}, teacher=${course.teacher}, location=${course.classroom}, start=${course.startTime}, end=${course.endTime}"
        } else emptyList()
        val tableCount = if (positionedText.isNotBlank()) titles else {
            maxOf(titles, normalizedNormal.map { it.dayIndex to it.parity }.distinct().size)
        }
        return PdfScheduleParseResult(normalizedNormal, normalizedWorkshops, tableCount, groups, diagnostics)
    }

    private fun normalizeAndValidate(classes: List<ScheduleClass>): List<ScheduleClass> = classes.map { course ->
        course.copy(
            courseName = PersianTextNormalizer.normalizeCourseName(course.courseName),
            courseCode = PersianTextNormalizer.toAsciiDigits(course.courseCode),
            teacher = PersianTextNormalizer.normalizeText(course.teacher),
            dayOfWeek = PersianTextNormalizer.normalizeText(course.dayOfWeek),
            groupCode = PersianTextNormalizer.toAsciiDigits(course.groupCode),
            classroom = PersianTextNormalizer.normalizeText(course.classroom),
            startTime = PersianTextNormalizer.normalizeTime(course.startTime),
            endTime = PersianTextNormalizer.normalizeTime(course.endTime)
        )
    }.filter { isValidCourseName(it.courseName) }

    private fun isValidCourseName(value: String): Boolean {
        val name = PersianTextNormalizer.normalizeCourseName(value)
        return name.length >= 2 && !name.matches(Regex("[0-9\\s./-]+")) &&
            !name.matches(Regex("(?:اتاق|ساعت|گروه|گ)\\s*[0-9۰-۹]*"))
    }

    private fun parsePositionedRow(line: String): Row? {
        val match = rowRegex.matchEntire(line) ?: return null
        val cells = cellRegex.findAll(match.groupValues[3]).mapNotNull { cellMatch ->
            val value = PersianTextNormalizer.normalizeText(cellMatch.groupValues[2])
            value.takeIf(String::isNotBlank)?.let { Cell(cellMatch.groupValues[1].toFloat(), it) }
        }.toList()
        return Row(match.groupValues[1].toInt(), match.groupValues[2].toFloat(), cells)
    }

    private fun isTableTitle(text: String): Boolean {
        val title = PersianTextNormalizer.normalizeText(text).replace(" ", "")
        return title.contains("برنامه") && (title.contains("کلاس") || title.contains("کالسی")) &&
            (title.contains("روز") || listOf("آزمایشگاه", "کارگاه", "سایت").any(title::contains))
    }

    private fun parseTimeSlots(row: Row): List<TimeSlot> {
        val entries = row.cells.mapNotNull { cell ->
            val match = Regex("([0-9۰-۹]{1,2})\\s*[-–—]\\s*([0-9۰-۹]{1,2})").find(cell.text) ?: return@mapNotNull null
            val start = PersianTextNormalizer.toAsciiDigits(match.groupValues[1]).toIntOrNull() ?: return@mapNotNull null
            val end = PersianTextNormalizer.toAsciiDigits(match.groupValues[2]).toIntOrNull() ?: return@mapNotNull null
            if (start !in 0..23 || end !in 1..24 || start >= end) return@mapNotNull null
            cell.x to (start to end)
        }.distinctBy { it.second }.sortedBy { it.first }
        if (entries.size < 4) return emptyList()
        val edges = entries.map { it.first }.zipWithNext().map { (left, right) -> (left + right) / 2f }
        return entries.mapIndexed { index, (x, pair) ->
            TimeSlot(
                left = if (index == 0) Float.NEGATIVE_INFINITY else edges[index - 1],
                right = if (index == entries.lastIndex) Float.POSITIVE_INFINITY else edges[index],
                start = "%02d:00".format(pair.first),
                end = "%02d:00".format(pair.second)
            )
        }
    }

    private fun detectScheduleTables(rows: List<Row>): List<ScheduleTable> {
        val allTitles = rows.mapIndexedNotNull { index, row ->
            val title = row.cells.joinToString(" ") { it.text }
            index.takeIf { isTableTitle(title) }?.let { it to title }
        }
        val scheduleTitles = allTitles.filter { (_, title) -> isScheduleTableTitle(title) }
        return scheduleTitles.mapNotNull { (titleIndex, title) ->
            val tableEnd = allTitles.firstOrNull { it.first > titleIndex }?.first ?: rows.size
            val header = (titleIndex + 1 until tableEnd).firstNotNullOfOrNull { index ->
                val slots = parseTimeSlots(rows[index])
                if (slots.isEmpty()) null else index to slots
            } ?: return@mapNotNull null
            val day = extractDay(title) ?: return@mapNotNull null
            ScheduleTable(titleIndex, header.first, tableEnd, title, day, header.second)
        }
    }

    private fun slotForX(slots: List<TimeSlot>, x: Float): TimeSlot? = slots.firstOrNull { x >= it.left && x < it.right }

    private fun isScheduleTableTitle(text: String): Boolean {
        val title = PersianTextNormalizer.normalizeText(text).replace(" ", "")
        return isTableTitle(text) && listOf("آزمایشگاه", "کارگاه", "سایت").none(title::contains)
    }

    private data class ParsedCourseCell(
        val courseName: String,
        val courseCode: String,
        val groupCode: String,
        val teacher: String
    )

    private fun parseAdditionalGroups(lines: List<String>): List<String> = lines.flatMap { line ->
        Regex("(?:گروه|گ)(?:(?:\\s*[-–:]?\\s*)|(?:[:：،/-]+\\s*))([0-9۰-۹]+)")
            .findAll(line)
            .map { PersianTextNormalizer.toAsciiDigits(it.groupValues[1]) }
            .toList()
    }.distinct()

    private fun parseNormalCourseCell(lines: List<String>): ParsedCourseCell? {
        if (lines.isEmpty()) return null
        val groupPattern = Regex("(?:^|\\s)(?:گروه|گ)[\\s.:،/-]*([0-9۰-۹]+)(?=$|\\s|[-–:])")
        val codePattern = Regex("(?:کد(?: درس)?|code)\\s*[:：-]?\\s*([0-9۰-۹]+)", RegexOption.IGNORE_CASE)
        var group = ""
        var code = ""
        val nameParts = mutableListOf<String>()
        val teacherParts = mutableListOf<String>()
        lines.forEachIndexed { index, rawLine ->
            var line = PersianTextNormalizer.normalizeCourseName(rawLine)
            val codeMatch = codePattern.find(line)
            if (codeMatch != null) {
                code = PersianTextNormalizer.toAsciiDigits(codeMatch.groupValues[1])
                line = line.replace(codeMatch.value, " ")
            }
            // A bare numeric token has no reliable meaning without an explicit
            // column label; it must never be promoted to a course name or code.
            if (line.matches(Regex("[0-9۰-۹]+"))) return@forEachIndexed
            val groupMatch = groupPattern.find(line)
            if (groupMatch != null) {
                val parsedGroup = PersianTextNormalizer.toAsciiDigits(groupMatch.groupValues[1])
                val after = line.substring(groupMatch.range.last + 1).trim().trimStart('-', '–', ':', '،')
                val before = line.substring(0, groupMatch.range.first).trim().trimEnd('-', '–', ':', '،')
                if (before.isNotBlank()) nameParts += before
                if (group.isBlank()) group = parsedGroup
                if (after.isNotBlank()) teacherParts += after
                return@forEachIndexed
            }
            if (line.isBlank()) return@forEachIndexed
            if (Regex("^(?:دکتر|مهندس|استاد|خانم|آقای|سید)\\s+").containsMatchIn(line)) {
                teacherParts += line
            } else if (index > 0 && lines[index - 1].contains(Regex("(?:^|\\s)(?:گروه|گ)\\s*[0-9۰-۹]+"))) {
                teacherParts += line
            } else {
                nameParts += line
            }
        }
        val name = PersianTextNormalizer.normalizeCourseName(nameParts.joinToString(" ") { it.replace(Regex("[-–:،]+$"), "").trim() })
            .replace(Regex("^(?:درس|نام درس|عنوان درس)[:：\\s-]*"), "")
        if (!isValidCourseName(name)) return null
        val teacher = PersianTextNormalizer.normalizeText(teacherParts.filter(String::isNotBlank).joinToString(" "))
        return ParsedCourseCell(name, code, group, teacher)
    }

    private fun parsePositionedNormalTables(text: String, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        val pages = text.lineSequence().mapNotNull(::parsePositionedRow).groupBy(Row::page)
        val results = mutableListOf<ScheduleClass>()

        pages.toSortedMap().values.forEach { pageRows ->
            val rows = pageRows.sortedBy(Row::y)
            detectScheduleTables(rows).forEach tableLoop@{ table ->
                val header = rows[table.headerIndex]
                val timeXs = header.cells.filter { cell ->
                    Regex("[0-9۰-۹]{1,2}\\s*[-–—]\\s*[0-9۰-۹]{1,2}").containsMatchIn(cell.text)
                }.map(Cell::x)
                val lastTimeColumnX = timeXs.maxOrNull() ?: return@tableLoop

                // Only interpret a numeric column when the PDF itself labels it. In
                // particular, never guess that a trailing number is a room or group.
                val labelRows = rows.subList(table.titleIndex + 1, table.headerIndex + 1)
                fun headerX(labels: List<String>): Float? = labelRows.asSequence()
                    .flatMap { it.cells.asSequence() }
                    .firstOrNull { cell ->
                        val label = PersianTextNormalizer.normalizeText(cell.text).replace(" ", "")
                        labels.any(label::contains)
                    }?.x

                val groupHeaderX = headerX(listOf("گروه"))
                // 'کلاس' alone may mean section/class number, so only unambiguous
                // physical-location labels are eligible for classroom mapping.
                val roomHeaderX = headerX(listOf("اتاق", "مکان", "محل"))
                val inferredRoomX = if (roomHeaderX == null) {
                    (table.headerIndex + 1 until table.endIndex).asSequence()
                        .flatMap { rows[it].cells.asSequence() }
                        .filter { cell ->
                            cell.x > lastTimeColumnX && (
                                cell.text.matches(Regex("[1-3][0-9]{2}")) ||
                                cell.text.matches(Regex("(?:[0-9۰-۹]{1,3}/)?[0-9۰-۹]{1,4}"))
                            )
                        }
                        .map { it.x }
                        .firstOrNull()
                } else null
                val effectiveRoomX = roomHeaderX ?: inferredRoomX
                if (groupHeaderX == null && effectiveRoomX == null) return@tableLoop

                val dataStart = table.headerIndex + 1
                val anchors = (dataStart until table.endIndex).mapNotNull { rowIndex ->
                    val row = rows[rowIndex]
                    fun valueAt(x: Float?, valid: (String) -> Boolean): String {
                        if (x == null) return ""
                        return row.cells
                            .filter { kotlin.math.abs(it.x - x) <= 25f }
                            .map { PersianTextNormalizer.normalizeText(it.text).trim() }
                            .firstOrNull(valid)
                            ?.let(PersianTextNormalizer::toAsciiDigits)
                            .orEmpty()
                    }
                    val group = valueAt(groupHeaderX) { it.matches(Regex("[0-9۰-۹]{1,3}")) }
                    val room = valueAt(effectiveRoomX) {
                        it.isNotBlank() && !it.any(Char::isLetter) && it.matches(Regex("(?:[0-9۰-۹]{1,3}/)?[0-9۰-۹]{1,4}"))
                    }
                    if (group.isBlank() && room.isBlank()) null else SectionAnchor(rowIndex, group, room)
                }.distinctBy(SectionAnchor::rowIndex)
                if (anchors.isEmpty()) return@tableLoop

                val titleText = PersianTextNormalizer.normalizeText(table.title)
                val parity = when {
                    titleText.contains("زوج") -> "زوج"
                    titleText.contains("فرد") -> "فرد"
                    else -> ""
                }
                val building = Regex("ساختمان\\s*([0-9۰-۹]+)").find(titleText)
                    ?.groupValues?.get(1)?.let(PersianTextNormalizer::toAsciiDigits).orEmpty()

                anchors.forEachIndexed { anchorIndex, anchor ->
                    val anchorY = rows[anchor.rowIndex].y
                    val top = if (anchorIndex == 0) rows[table.headerIndex].y else
                        (rows[anchors[anchorIndex - 1].rowIndex].y + anchorY) / 2f
                    val bottom = if (anchorIndex == anchors.lastIndex) {
                        rows.getOrNull(table.endIndex)?.y?.let { (anchorY + it) / 2f } ?: anchorY + 24f
                    } else (anchorY + rows[anchors[anchorIndex + 1].rowIndex].y) / 2f

                    table.slots.forEach slotLoop@{ slot ->
                        val lines = rows.filter { it.y > top && it.y <= bottom }
                            .mapNotNull { row ->
                                val courseCells = row.cells.filter { cell ->
                                    cell.x <= lastTimeColumnX + 4f && slotForX(table.slots, cell.x) == slot
                                }
                                courseCells.takeIf(List<Cell>::isNotEmpty)?.let { row.y to it }
                            }
                            .groupBy({ it.first }, { it.second })
                            .toSortedMap()
                            .values
                            .map { sameLine ->
                                sameLine.flatten().sortedByDescending(Cell::x).joinToString(" ") { it.text }
                                    .let(PersianTextNormalizer::normalizeCourseName)
                            }
                            .filter(String::isNotBlank)
                                            .filterNot { line ->
                                val compact = line.replace(" ", "")
                                compact.matches(Regex("[0-9۰-۹]{2,4}")) ||
                                    Regex("[0-9۰-۹]{1,2}\\s*[-–—]\\s*[0-9۰-۹]{1,2}").containsMatchIn(compact) ||
                                    Regex("^(?:روز|کلاس|اتاق|گروه|ساعت|مکان|استاد)(?:نام|کد)?$").containsMatchIn(compact)
                            }
                        val parsed = parseNormalCourseCell(lines) ?: return@slotLoop
                        val room = anchor.room
                        val classroom = when {
                            room.isBlank() -> ""
                            room.contains('/') -> formatRoom(room)
                            building.isNotBlank() -> "ساختمان ${PersianTextNormalizer.toPersianDigits(building)} - کلاس ${PersianTextNormalizer.toPersianDigits(room)}"
                            else -> "کلاس ${PersianTextNormalizer.toPersianDigits(room)}"
                        }
                        val explicitGroups = parseAdditionalGroups(lines)
                        val groups = when {
                            explicitGroups.isNotEmpty() -> explicitGroups
                            anchor.groupCode.isNotBlank() -> listOf(anchor.groupCode)
                            parsed.groupCode.isNotBlank() -> listOf(parsed.groupCode)
                            else -> listOf("")
                        }
                        groups.forEach { group ->
                            results += ScheduleClass(
                                id = "${scheduleId}_grid_${UUID.randomUUID().toString().take(8)}",
                                scheduleId = scheduleId,
                                courseName = parsed.courseName,
                                courseCode = parsed.courseCode,
                                teacher = parsed.teacher,
                                dayOfWeek = table.day.first,
                                dayIndex = table.day.second,
                                startTime = slot.start,
                                endTime = slot.end,
                                classroom = classroom,
                                groupCode = group,
                                parity = parity,
                                notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "جدول برنامه کلاسی"
                            )
                        }
                    }
                }
            }
        }
        return results.distinctBy(ScheduleOfferingIdentity::key)
    }

    private fun extractDay(title: String): Pair<String, Int>? {
        val normalized = PersianTextNormalizer.normalizeText(title)
        return when {
            normalized.contains("پنجشنبه") || normalized.contains("پنج شنبه") -> "پنج‌شنبه" to 5
            normalized.contains("چهارشنبه") || normalized.contains("چهار شنبه") -> "چهارشنبه" to 4
            normalized.contains("سهشنبه") || normalized.contains("سه شنبه") -> "سه‌شنبه" to 3
            normalized.contains("دوشنبه") -> "دوشنبه" to 2
            normalized.contains("یکشنبه") || normalized.contains("يكشنبه") -> "یکشنبه" to 1
            normalized.contains("شنبه") -> "شنبه" to 0
            else -> null
        }
    }

    /** Existing room/time workshop-table parser; invoked only for the workshop layout. */
    private fun parsePositionedRows(text: String, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        val pages = text.lines().mapNotNull { line ->
            val m = rowRegex.matchEntire(line) ?: return@mapNotNull null
            val cells = cellRegex.findAll(m.groupValues[3]).mapNotNull { cm ->
                val value = PersianTextNormalizer.normalizeText(cm.groupValues[2])
                value.takeIf(String::isNotBlank)?.let { Cell(cm.groupValues[1].toFloat(), it) }
            }.toList()
            Row(m.groupValues[1].toInt(), m.groupValues[2].toFloat(), cells)
        }.groupBy(Row::page).toSortedMap()
        val roomRegex = Regex("(?:[1-3]/[0-9۰-۹]{1,3}|سایت\\s*[0-9۰-۹]+)")
        val teacherRegex = Regex("^(?:(?:(?:خانم|آقای)\\s+)?(?:مهندس|دکتر|استاد)\\s+|سید\\s+).{2,}$")
        val defaultTimes = listOf("18:00" to "20:00", "16:00" to "18:00", "14:00" to "16:00", "12:00" to "14:00", "10:00" to "12:00", "08:00" to "10:00", "07:00" to "08:00")
        val results = mutableListOf<ScheduleClass>()
        var activeDay: Pair<String, Int> = "شنبه" to 0
        pages.forEach { (pageNumber, unsortedRows) ->
            val rows = unsortedRows.sortedBy(Row::y)
            val pageDay = activeDay
            var timeColumns = listOf(76f, 174f, 271f, 367f, 464f, 563f, 638f).zip(defaultTimes)
            val explicitDay = rows.firstOrNull { row -> row.cells.any { cell ->
                val label = cell.text.trim()
                label in listOf("شنبه", "یکشنبه", "دوشنبه", "سه شنبه", "سهشنبه", "چهارشنبه", "چهار شنبه", "پنجشنبه", "پنج شنبه")
            } }?.let { normalizeWorkshopDay(it.cells.joinToString(" ") { cell -> cell.text }) } ?: when (pageNumber) {
                3 -> "شنبه" to 0
                4 -> "یکشنبه" to 1
                5 -> "دوشنبه" to 2
                6 -> "سه‌شنبه" to 3
                7 -> "چهارشنبه" to 4
                8 -> "پنج‌شنبه" to 5
                else -> null
            }
            val hasWorkshopHeader = rows.any { row -> row.cells.any { cell -> cell.text.contains("کارگاه") || cell.text.contains("آزمایشگاه") || cell.text.contains("سایت") } }
        val schedulePage = rows.any { row -> row.cells.any { cell -> Regex("[0-9۰-۹]{1,2}\\s*-\\s*[0-9۰-۹]{1,2}").containsMatchIn(cell.text) } } &&
            rows.any { row -> row.cells.any { cell -> cell.x > 620f && roomRegex.containsMatchIn(cell.text) } } &&
            rows.any { row -> row.cells.any { it.text.trim() in listOf("اتاق", "ساعت", "اتاق ساعت", "ساعت اتاق") } } && hasWorkshopHeader
            if (!schedulePage) return@forEach
            if (schedulePage && explicitDay != null) activeDay = explicitDay
            val dayForPage = if (schedulePage) explicitDay ?: pageDay else pageDay
            rows.forEach { row ->
                val headerSlots = row.cells.mapNotNull { cell ->
                    val m = Regex("([0-9۰-۹]{1,2})\\s*-\\s*([0-9۰-۹]{1,2})").find(cell.text) ?: return@mapNotNull null
                    val start = PersianTextNormalizer.toAsciiDigits(m.groupValues[1]).toIntOrNull() ?: return@mapNotNull null
                    val end = PersianTextNormalizer.toAsciiDigits(m.groupValues[2]).toIntOrNull() ?: return@mapNotNull null
                    cell.x to ("%02d:00".format(start) to "%02d:00".format(end))
                }
                if (headerSlots.size >= 5) timeColumns = headerSlots.sortedBy { it.first }
            }
            rows.forEachIndexed { index, roomRow ->
                val roomCell = roomRow.cells.firstOrNull { it.x > 620f && roomRegex.containsMatchIn(it.text) } ?: return@forEachIndexed
                val courseRow = rows.getOrNull(index - 1) ?: return@forEachIndexed
                val teacherRow = rows.getOrNull(index + 1) ?: return@forEachIndexed
                if (roomRow.y - courseRow.y !in 3f..22f || teacherRow.y - roomRow.y !in 3f..22f) return@forEachIndexed
                val roomPrefix = roomRow.cells.firstOrNull { it.text == "نظام مهندسی" }?.text.orEmpty()
                val classroom = formatRoom(listOf(roomPrefix, roomCell.text).filter(String::isNotBlank).joinToString(" "))
                val courses = courseRow.cells.filter { cell ->
                    cell.x < 620f && cell.text.isNotBlank() && !cell.text.contains("ساعت") &&
                        !teacherRegex.containsMatchIn(cell.text) && !roomRegex.containsMatchIn(cell.text) &&
                        !cell.text.matches(Regex("[0-9۰-۹\\s./-]+")) && cell.text != "نظام مهندسی"
                }
                val teachers = teacherRow.cells.filter { teacherRegex.containsMatchIn(it.text) }
                val hasTableHeaderNearby = rows.take(index).takeLast(8).any { previous ->
                    previous.cells.any { it.text.trim() in listOf("ساعت", "اتاق", "اتاق ساعت", "ساعت اتاق") || it.text.contains("18 - 20") }
                }
                val isTrailingRoomLabel = hasTableHeaderNearby && courseRow.cells.any { cell ->
                    cell.x > 620f && (cell.text.contains("اتاق") || cell.text.contains("ساعت"))
                }
                courses.forEach courseLoop@{ course ->
                    val name = course.text.replace(Regex("\\s{2,}"), " ").trim()
                    if (name.isBlank() || isTrailingRoomLabel) return@courseLoop
                    val time = timeColumns.minByOrNull { kotlin.math.abs(it.first - course.x) }?.second ?: ("08:00" to "10:00")
                    val teacher = teachers.minByOrNull { kotlin.math.abs(it.x - course.x) }?.text.orEmpty()
                    val parity = when { name.contains("زوج") -> "زوج"; name.contains("فرد") -> "فرد"; else -> "" }
                    results += ScheduleClass(
                        id = "${scheduleId}_position_${UUID.randomUUID().toString().take(8)}", scheduleId = scheduleId,
                        courseName = name.replace(Regex("\\s*\\((?:زوج|فرد)\\)"), "").trim(), teacher = teacher,
                        dayOfWeek = dayForPage.first, dayIndex = dayForPage.second,
                        startTime = time.first, endTime = time.second, classroom = classroom,
                        groupCode = "1", parity = parity,
                        notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "آزمایشگاه / کارگاه / سایت")
                }
            }
            if (schedulePage && explicitDay == null && pageNumber < 3) activeDay = nextWorkshopDay(activeDay)
        }
        return results
    }

    private fun nextWorkshopDay(day: Pair<String, Int>): Pair<String, Int> = when (day.second) {
        0 -> "یکشنبه" to 1
        1 -> "دوشنبه" to 2
        2 -> "سه‌شنبه" to 3
        3 -> "چهارشنبه" to 4
        4 -> "پنج‌شنبه" to 5
        else -> day
    }

    private fun normalizeWorkshopDay(text: String): Pair<String, Int> {
        val compact = PersianTextNormalizer.normalizeText(text).replace(" ", "")
        return when {
            compact.contains("چهارشنبه") -> "چهارشنبه" to 4
            compact.contains("پنجشنبه") -> "پنج‌شنبه" to 5
            compact.contains("سهشنبه") -> "سه‌شنبه" to 3
            compact.contains("یکشنبه") -> "یکشنبه" to 1
            compact.contains("دوشنبه") -> "دوشنبه" to 2
            else -> "شنبه" to 0
        }
    }

    private fun formatRoom(raw: String): String {
        val room = PersianTextNormalizer.normalizeText(raw)
        val asciiRoom = PersianTextNormalizer.toAsciiDigits(room)
        val number = Regex("[1-3]/[0-9]{1,3}").find(asciiRoom)?.value
        val site = Regex("سایت\\s*[0-9۰-۹]+").find(room)?.value
        return when {
            number != null && site != null -> "نظام مهندسی $site ($number)"
            number != null -> "ساختمان ${number.substringBefore("/")} - کلاس/اتاق ${number.substringAfter("/")}"
            else -> room
        }
    }

    private fun parseSadjadMatrix(lines: List<String>, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        val hasHeader = lines.any { it.contains("8-10") || it.contains("8 - 10") || it.contains("18-20") && it.contains("16-18") }
        if (!hasHeader || lines.none { line -> line.contains("برنامه") && line.contains("کلاس") && line.contains("روز") }) return emptyList()
        val results = mutableListOf<ScheduleClass>()
        var day: Pair<String, Int> = "شنبه" to 0
        var parity = ""
        var building = ""
        val slots = listOf("08:00" to "10:00", "10:00" to "12:00", "12:00" to "14:00", "14:00" to "16:00", "16:00" to "18:00", "18:00" to "20:00")
        val roomRegex = Regex("(?<![0-9۰-۹])([1-3][0-9]{2})(?![0-9۰-۹])")
        lines.forEachIndexed { index, line ->
            if (line.contains("برنامه") && (line.contains("کلاس") || line.contains("کالسی")) && line.contains("روز")) {
                day = PersianTextNormalizer.normalizeDay(line)
                parity = when { line.contains("فرد") -> "فرد"; line.contains("زوج") -> "زوج"; else -> "" }
                building = if (line.contains("ساختمان")) line.substringAfter("ساختمان").filter { it.isDigit() || it in '۰'..'۹' }.take(2) else building
            }
            if (!line.contains(Regex("(?:^|\\s)(?:گ|گروه)\\s*[0-9۰-۹]"))) return@forEachIndexed
            val roomNum = roomRegex.findAll(line).lastOrNull()?.groupValues?.get(1).orEmpty()
            val classroom = if (roomNum.isNotBlank()) if (building.isNotBlank()) "ساختمان $building - کلاس $roomNum" else "کلاس $roomNum" else ""
            val chunks = line.split(Regex("(?=(?:^|\\s)(?:گ|گروه)\\s*[0-9۰-۹])")).map(String::trim).filter { it.contains(Regex("(?:گ|گروه)\\s*[0-9۰-۹]")) }
            val previous = lines.getOrNull(index - 1).orEmpty()
            val courseNames = splitCourseNames(previous)
            chunks.forEachIndexed { k, raw ->
                val group = PersianTextNormalizer.toAsciiDigits(Regex("(?:گ|گروه)\\s*([0-9۰-۹/]+)").find(raw)?.groupValues?.get(1) ?: "1")
                val teacher = raw.replace(Regex("^(?:گ|گروه)\\s*[0-9۰-۹/]+[-–:]*"), "").replace(roomRegex, "").replace("فرد", "").replace("زوج", "").trim()
                val course = courseNames.getOrNull(k) ?: courseNames.lastOrNull() ?: return@forEachIndexed
                val slot = slots[(chunks.size - 1 - k).coerceIn(0, slots.lastIndex)]
                val range = Regex("(?:گ|گروه)\\s*[0-9۰-۹/-]+\\s*[-–]\\s*(?:گ|گروه)\\s*([0-9۰-۹]+)").find(raw)
                val groups = (listOf(group) + range?.groupValues?.get(1)?.let(PersianTextNormalizer::toAsciiDigits).orEmpty())
                    .filter(String::isNotBlank).distinct()
                groups.forEach { selectedGroup ->
                    results += ScheduleClass(
                        id = "${scheduleId}_matrix_${UUID.randomUUID().toString().take(8)}", scheduleId = scheduleId,
                        courseName = PersianTextNormalizer.cleanCourseName(course), teacher = PersianTextNormalizer.normalizeText(teacher),
                        dayOfWeek = day.first, dayIndex = day.second, startTime = slot.first, endTime = slot.second,
                        classroom = classroom, groupCode = selectedGroup.ifEmpty { "1" }, parity = parity,
                        notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "")
                }
            }
        }
        return results
    }

    private fun parseLabAndSiteSchedule(lines: List<String>, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        if (lines.none { it.contains("آزمایشگاه") || it.contains("سایت") || it.contains("کارگاه") } ||
            lines.none { it.contains("اتاق") && it.contains("ساعت") || it.contains("8 - 10") && it.contains("18 - 20") }) return emptyList()
        val results = mutableListOf<ScheduleClass>()
        var day: Pair<String, Int> = "شنبه" to 0
        val slots = listOf("08:00" to "10:00", "10:00" to "12:00", "12:00" to "14:00", "14:00" to "16:00", "16:00" to "18:00", "18:00" to "20:00")
        val roomRegex = Regex("(?:[1-3]/[0-9]{1,3}|نظام مهندسی سایت[0-9۰-۹]+(?:\\([0-9/]+\\))?|سایت\\s*[0-9۰-۹]+|کارگاه\\s*[\\u0600-\\u06FF0-9]+)")
        lines.forEachIndexed { i, line ->
            if (isExplicitDayLine(line)) { day = PersianTextNormalizer.normalizeDay(line); return@forEachIndexed }
            if (line.contains("ساعت") || line.contains("درس") || line.contains("استاد")) return@forEachIndexed
            val roomMatch = roomRegex.find(line) ?: return@forEachIndexed
            val roomRaw = roomMatch.value
            val classroom = if (roomRaw.contains('/')) "ساختمان ${roomRaw.substringBefore('/')} - کلاس/اتاق ${roomRaw.substringAfter('/')}" else roomRaw
            val courseLine = lines.getOrNull(i - 1).orEmpty()
            val teacherLine = lines.getOrNull(i + 1).orEmpty()
            val courses = courseLine.split(Regex("\\s{2,}")).map(String::trim).filter { it.length >= 3 }
            val teachers = teacherLine.split(Regex("\\s{2,}")).map(String::trim).filter { it.length >= 3 }
            courses.forEachIndexed { k, course ->
                val teacher = teachers.getOrNull(k) ?: teachers.firstOrNull().orEmpty()
                val slot = slots[(courses.size - 1 - k).coerceIn(0, slots.lastIndex)]
                results += ScheduleClass(
                    id = "${scheduleId}_lab_${UUID.randomUUID().toString().take(8)}", scheduleId = scheduleId,
                    courseName = PersianTextNormalizer.cleanCourseName(course), teacher = PersianTextNormalizer.normalizeText(teacher),
                    dayOfWeek = day.first, dayIndex = day.second, startTime = slot.first, endTime = slot.second,
                    classroom = classroom, groupCode = "1", notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "آزمایشگاه / کارگاه / سایت")
            }
        }
        return results
    }

    private fun splitCourseNames(text: String): List<String> = if (text.isBlank()) emptyList() else {
        val split = text.split(Regex("\\s{2,}")).map(PersianTextNormalizer::cleanCourseName).filter { it.length >= 3 }
        split.ifEmpty { listOf(PersianTextNormalizer.cleanCourseName(text)) }
    }

    private fun parseAnnouncementLines(lines: List<String>, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        val result = mutableListOf<ScheduleClass>()
        val pattern = Regex("کلاس\\s+(.+?)\\s+گروه\\s*([0-9۰-۹]+)\\s*[-–]\\s*([\\u0600-\\u06FF]+)\\s+ساعت\\s+([0-9۰-۹]+-[0-9۰-۹]+)\\s+در\\s+(.+?)(?:\\s+برگزار|$)")
        lines.forEach { line -> pattern.find(line)?.let { m ->
            val group = PersianTextNormalizer.toAsciiDigits(m.groupValues[2])
            val (start, end) = PersianTextNormalizer.extractTimeRange(m.groupValues[4]) ?: ("08:00" to "10:00")
            val (day, dayIndex) = PersianTextNormalizer.normalizeDay(line)
            result += ScheduleClass(id = "${scheduleId}_announcement_${UUID.randomUUID().toString().take(8)}", scheduleId = scheduleId,
                courseName = PersianTextNormalizer.cleanCourseName(m.groupValues[1]), teacher = PersianTextNormalizer.normalizeText(m.groupValues[3]),
                dayOfWeek = day, dayIndex = dayIndex, startTime = start, endTime = end, classroom = PersianTextNormalizer.normalizeText(m.groupValues[5]),
                groupCode = group.ifEmpty { "1" }, notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "")
        } }
        return result
    }

    private fun parseLinearLines(lines: List<String>, scheduleId: String, sourceTag: String): List<ScheduleClass> {
        val hasLinearSchedule = lines.any { line ->
            Regex("\\d{1,2}\\s*(?:-|الی|تا)\\s*\\d{1,2}").containsMatchIn(line) &&
                (line.contains("درس") || line.contains("گروه") || line.trimStart().startsWith("ساعت "))
        }
        if (!hasLinearSchedule) return emptyList()
        val result = mutableListOf<ScheduleClass>()
        var day: Pair<String, Int> = "شنبه" to 0
        val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?)\\s*(?:-|–|—|الی|تا)\\s*(\\d{1,2}(?::\\d{2})?)")
        val teacherRegex = Regex("(?:استاد[:\\s]+)?(دکتر|مهندس|استاد|خانم|آقای)\\s+([\\u0600-\\u06FF]+)(?:\\s+([\\u0600-\\u06FF]+))?")
        val classroomRegex = Regex("(کلاس|اتاق|سایت|کارگاه|آمفی[\\s‌]?تئاتر|آزمایشگاه)\\s*([۰-۹0-9\\u0600-\\u06FF]+)?")
        val groupRegex = Regex("(?:گروه|کد|گ)\\s*([۰-۹0-9]+)")
        lines.forEachIndexed { i, line ->
            if (isExplicitDayLine(line)) day = PersianTextNormalizer.normalizeDay(line)
            val tm = timeRegex.find(line) ?: return@forEachIndexed
            val a = tm.groupValues[1]; val b = tm.groupValues[2]
            val h1 = a.substringBefore(':').toIntOrNull() ?: 8; val h2 = b.substringBefore(':').toIntOrNull() ?: 10
            val (start, end) = if (h1 > h2 && h1 - h2 in 1..5) PersianTextNormalizer.normalizeTime(b) to PersianTextNormalizer.normalizeTime(a) else PersianTextNormalizer.normalizeTime(a) to PersianTextNormalizer.normalizeTime(b)
            val inlineDay = if (isExplicitDayLine(line)) PersianTextNormalizer.normalizeDay(line) else day
            val clean = line.replace(tm.value, "").replace(inlineDay.first, "").replace(Regex("^(?:ساعت|زمان|روز)\\s*"), "").trim()
            val room = classroomRegex.find(clean)?.value?.let(PersianTextNormalizer::normalizeText).orEmpty()
            val group = groupRegex.find(clean)?.groupValues?.get(1)?.let(PersianTextNormalizer::toAsciiDigits) ?: "1"
            val teacher = teacherRegex.find(clean)?.value.orEmpty()
            val remaining = clean.replace(room, "").replace(teacher, "").replace(Regex("(?:گروه|کد)\\s*[۰-۹0-9]+"), "").trim()
            val course = when {
                remaining.length >= 3 && !remaining.matches(Regex("^[0-9۰-۹\\s-]+$")) -> PersianTextNormalizer.cleanCourseName(remaining)
                i > 0 && isLikelyCourseName(lines[i - 1]) -> PersianTextNormalizer.cleanCourseName(lines[i - 1])
                i + 1 < lines.size && isLikelyCourseName(lines[i + 1]) -> PersianTextNormalizer.cleanCourseName(lines[i + 1])
                else -> ""
            }
            if (course.isNotBlank() && course != "نامشخص") result += ScheduleClass(id = "${scheduleId}_${UUID.randomUUID().toString().take(8)}", scheduleId = scheduleId,
                courseName = course, teacher = PersianTextNormalizer.normalizeText(teacher), dayOfWeek = inlineDay.first, dayIndex = inlineDay.second,
                startTime = start, endTime = end, classroom = room, groupCode = group,
                notes = if (sourceTag.isNotBlank()) "منبع: $sourceTag" else "")
        }
        return result
    }

    private fun isExplicitDayLine(line: String): Boolean {
        val value = line.trim()
        return listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "سه شنبه", "چهارشنبه", "پنج‌شنبه", "پنجشنبه", "جمعه")
            .any { value.startsWith(it) || value.startsWith("روز $it") }
    }

    private fun isLikelyCourseName(line: String): Boolean {
        val value = line.trim()
        if (value.length !in 3..60) return false
        if (listOf("برنامه", "هفتگی", "دانشگاه", "نیمسال", "ترم", "صفحه", "دانشکده", "گروه", "ساعت").any(value::startsWith)) return false
        return !value.matches(Regex("^[0-9۰-۹\\s-]+$"))
    }

    private fun fallbackExtractText(file: File): String = try {
        val bytes = file.readBytes(); val out = StringBuilder(); var stream = false; var i = 0
        while (i < bytes.size - 6) {
            if (!stream && bytes[i] == 's'.code.toByte() && bytes[i + 1] == 't'.code.toByte() && bytes[i + 2] == 'r'.code.toByte() && bytes[i + 3] == 'e'.code.toByte() && bytes[i + 4] == 'a'.code.toByte() && bytes[i + 5] == 'm'.code.toByte()) { stream = true; i += 6; continue }
            if (stream && bytes[i] == 'e'.code.toByte() && bytes[i + 1] == 'n'.code.toByte() && bytes[i + 2] == 'd'.code.toByte() && bytes[i + 3] == 's'.code.toByte() && bytes[i + 4] == 't'.code.toByte() && bytes[i + 5] == 'r'.code.toByte()) { stream = false; i += 9; continue }
            if (stream) { val b = bytes[i].toInt() and 0xFF; if (b in 32..126) out.append(b.toChar()) else if (b == 10 || b == 13) out.append('\n') }
            i++
        }
        out.toString()
    } catch (_: Exception) { "" }
}
