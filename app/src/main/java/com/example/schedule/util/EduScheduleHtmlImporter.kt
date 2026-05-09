package com.example.schedule.util

import com.example.schedule.data.db.Course
import org.json.JSONArray
import org.json.JSONObject

enum class EduScheduleImportStatus {
    READY,
    OPENING_TIMETABLE,
    NOT_FOUND,
    ERROR
}

data class EduScheduleHtmlImportResult(
    val status: EduScheduleImportStatus,
    val message: String,
    val preview: CourseImportPreview
)

object EduScheduleHtmlImporter {

    private val colors = listOf(
        0xFFB4D4E0.toInt(),
        0xFFC5E0B4.toInt(),
        0xFFE0C5B4.toInt(),
        0xFFD4B4E0.toInt(),
        0xFFE0D4B4.toInt(),
        0xFFB4E0D4.toInt(),
        0xFF7FD77D.toInt(),
        0xFF5B8DEF.toInt(),
        0xFFB08AE8.toInt(),
        0xFFF2A344.toInt()
    )

    fun parse(payload: String, existingCourses: List<Course>): EduScheduleHtmlImportResult {
        val emptyPreview = CourseImportPreview(emptyList(), emptyList(), emptyList())
        val root = runCatching { JSONObject(payload) }.getOrElse { error ->
            return EduScheduleHtmlImportResult(
                status = EduScheduleImportStatus.ERROR,
                message = error.message ?: "无法读取教务系统返回内容",
                preview = emptyPreview
            )
        }

        val status = when (root.optString("status")) {
            "ready" -> EduScheduleImportStatus.READY
            "opening" -> EduScheduleImportStatus.OPENING_TIMETABLE
            "not_found" -> EduScheduleImportStatus.NOT_FOUND
            else -> EduScheduleImportStatus.ERROR
        }
        val message = root.optString("message")

        if (status != EduScheduleImportStatus.READY) {
            return EduScheduleHtmlImportResult(status, message, emptyPreview)
        }

        val rows = root.optJSONArray("courses") ?: JSONArray()
        val existingKeys = existingCourses.map { it.duplicateKey() }.toMutableSet()
        val importedKeys = mutableSetOf<String>()
        val validCourses = mutableListOf<Course>()
        val duplicateRows = mutableListOf<CourseImportDuplicate>()
        val errorRows = mutableListOf<CourseImportError>()

        for (index in 0 until rows.length()) {
            val item = rows.optJSONObject(index) ?: continue
            val rowNumber = item.optInt("rowNumber", index + 1)
            runCatching {
                val name = item.optString("name").trim()
                val dayOfWeek = item.optInt("dayOfWeek")
                val detail = item.optString("detail").normalizeSpaces()
                val courseCode = item.optString("courseCode").trim()
                require(name.isNotBlank()) { "课程名为空" }
                require(dayOfWeek in 1..7) { "星期无法识别" }

                val teacher = extractBetween(detail, "老师:", ";时间:")
                    .ifBlank { extractBetween(detail, "老师：", ";时间：") }
                val timeText = extractBetween(detail, ";时间:", ";地点:")
                    .ifBlank { extractBetween(detail, ";时间：", ";地点：") }
                val classroom = detail.substringAfter(";地点:", detail)
                    .substringAfter(";地点：", detail.substringAfter(";地点:", ""))
                    .trim()

                require(timeText.isNotBlank()) { "时间无法识别" }
                val parsedTime = parseTime(timeText)
                val noteParts = buildList {
                    if (parsedTime.weekRanges.size > 1) add("教务系统原始周次：${parsedTime.originalWeeks}周")
                    if (courseCode.isNotBlank()) add("课程号：$courseCode")
                }

                parsedTime.weekRanges.forEach { weekRange ->
                    val course = Course(
                        name = name,
                        teacher = teacher,
                        classroom = classroom,
                        dayOfWeek = dayOfWeek,
                        startSection = parsedTime.startSection,
                        endSection = parsedTime.endSection,
                        startWeek = weekRange.first,
                        endWeek = weekRange.last,
                        color = colors[validCourses.size % colors.size],
                        note = noteParts.joinToString("；"),
                        isEnabled = true,
                        reminderMinutes = 0,
                        oddEvenWeek = 0
                    )

                    val key = course.duplicateKey()
                    if (key in existingKeys || key in importedKeys) {
                        duplicateRows += CourseImportDuplicate(rowNumber, course)
                    } else {
                        validCourses += course
                        importedKeys += key
                    }
                }
            }.onFailure { error ->
                errorRows += CourseImportError(
                    rowNumber = rowNumber,
                    message = error.message ?: "解析失败",
                    rawText = item.toString()
                )
            }
        }

        val preview = CourseImportPreview(validCourses, duplicateRows, errorRows)
        val finalMessage = message.ifBlank {
            "识别到 ${validCourses.size} 门可导入课程"
        }
        return EduScheduleHtmlImportResult(status, finalMessage, preview)
    }

    private fun extractBetween(text: String, start: String, end: String): String {
        val startIndex = text.indexOf(start)
        if (startIndex < 0) return ""
        val from = startIndex + start.length
        val endIndex = text.indexOf(end, from)
        return if (endIndex >= 0) text.substring(from, endIndex).trim() else text.substring(from).trim()
    }

    private fun parseTime(text: String): ParsedTime {
        val match = Regex("""(.+?)周\s*\[\s*(\d{1,2})(?:\s*[-~～]\s*(\d{1,2}))?\s*节\s*]""")
            .find(text)
            ?: error("时间格式无法识别")
        val weekText = match.groupValues[1].trim()
        val startSection = match.groupValues[2].toInt()
        val endSection = match.groupValues.getOrNull(3).orEmpty().ifBlank { match.groupValues[2] }.toInt()
        require(startSection in 1..12 && endSection in 1..12 && endSection >= startSection) { "节次范围不合法" }
        return ParsedTime(
            originalWeeks = weekText,
            weekRanges = parseWeekRanges(weekText),
            startSection = startSection,
            endSection = endSection
        )
    }

    private fun parseWeekRanges(text: String): List<IntRange> {
        val ranges = text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { part ->
                val numbers = Regex("""\d+""").findAll(part).map { it.value.toInt() }.toList()
                require(numbers.isNotEmpty()) { "周次为空" }
                val start = numbers.first()
                val end = numbers.getOrElse(1) { start }
                require(start in 1..30 && end in 1..30 && end >= start) { "周次范围不合法" }
                start..end
            }
        require(ranges.isNotEmpty()) { "周次为空" }
        return ranges
    }

    private fun String.normalizeSpaces(): String {
        return replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun Course.duplicateKey(): String {
        return listOf(
            name.trim(),
            teacher.trim(),
            dayOfWeek.toString(),
            startSection.toString(),
            endSection.toString(),
            startWeek.toString(),
            endWeek.toString(),
            classroom.trim()
        ).joinToString("|").lowercase()
    }

    private data class ParsedTime(
        val originalWeeks: String,
        val weekRanges: List<IntRange>,
        val startSection: Int,
        val endSection: Int
    )
}
