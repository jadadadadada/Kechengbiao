package com.example.schedule.util

import com.example.schedule.data.db.Course

object EduScheduleTextImporter {

    private val colors = listOf(
        0xFFB4D4E0.toInt(),
        0xFFC5E0B4.toInt(),
        0xFFE0C5B4.toInt(),
        0xFFD4B4E0.toInt(),
        0xFFE0D4B4.toInt(),
        0xFFB4E0D4.toInt(),
    )

    fun parse(pageText: String, existingCourses: List<Course>): CourseImportPreview {
        val lines = pageText
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val existingKeys = existingCourses.map { it.duplicateKey() }.toMutableSet()
        val importedKeys = mutableSetOf<String>()
        val validCourses = mutableListOf<Course>()
        val duplicates = mutableListOf<CourseImportDuplicate>()
        val errors = mutableListOf<CourseImportError>()

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (!line.startsWith("教师") && !line.contains("教师：") && !line.contains("教师:")) {
                index++
                continue
            }

            val name = lines.getOrNull(index - 1).orEmpty()
            val teacher = line.substringAfter("教师", "").trimStart('：', ':').trim()
            val sectionLineIndex = (index + 1 until (index + 5).coerceAtMost(lines.size))
                .firstOrNull { lines[it].contains("小节") }
            val weekLineIndex = (index + 1 until (index + 6).coerceAtMost(lines.size))
                .firstOrNull { lines[it].contains("周") && lines[it].contains("星期") }

            if (name.isBlank() || sectionLineIndex == null || weekLineIndex == null) {
                errors += CourseImportError(index + 1, "无法识别课程块", line)
                index++
                continue
            }

            val sectionText = lines[sectionLineIndex]
            val weekText = lines[weekLineIndex]
            val classroom = lines.getOrNull(weekLineIndex + 1).orEmpty()

            runCatching {
                val (startSection, endSection) = parseSections(sectionText)
                val dayOfWeek = parseDay(weekText) ?: error("无法识别星期")
                val (weekRanges, originalWeekText) = parseWeeks(weekText)
                weekRanges.forEach { weekRange ->
                    val course = Course(
                        name = name,
                        teacher = teacher,
                        classroom = classroom,
                        dayOfWeek = dayOfWeek,
                        startSection = startSection,
                        endSection = endSection,
                        startWeek = weekRange.first,
                        endWeek = weekRange.last,
                        oddEvenWeek = parseOddEven(weekText),
                        color = colors[validCourses.size % colors.size],
                        reminderMinutes = 0,
                        note = if (weekRanges.size > 1) "教务系统原始周次：$originalWeekText" else "",
                        isEnabled = true
                    )

                    val key = course.duplicateKey()
                    if (key in existingKeys || key in importedKeys) {
                        duplicates += CourseImportDuplicate(index + 1, course)
                    } else {
                        validCourses += course
                        importedKeys += key
                    }
                }
            }.onFailure { error ->
                errors += CourseImportError(index + 1, error.message ?: "解析失败", lines.subList(index - 1, (weekLineIndex + 2).coerceAtMost(lines.size)).joinToString("\n"))
            }

            index = weekLineIndex + 1
        }

        if (validCourses.isEmpty() && duplicates.isEmpty() && errors.isEmpty()) {
            errors += CourseImportError(1, "当前页面没有识别到课程，请确认已经打开个人课表页面", "")
        }

        return CourseImportPreview(validCourses, duplicates, errors)
    }

    private fun parseSections(text: String): Pair<Int, Int> {
        val match = Regex("""(\d{1,2})\s*[~～\-]\s*(\d{1,2})\s*小节""").find(text)
            ?: error("无法识别节次")
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        require(start in 1..12 && end in 1..12 && end >= start) { "节次范围不合法" }
        return start to end
    }

    private fun parseWeeks(text: String): Pair<List<IntRange>, String> {
        val original = Regex("""\[([^\]]*?周)\]""").find(text)?.groupValues?.getOrNull(1)
            ?: Regex("""(\d[\d,\-~～]*周)""").find(text)?.groupValues?.getOrNull(1)
            ?: error("无法识别周次")
        val rangeText = original.removeSuffix("周")
        val ranges = rangeText
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
        return ranges to original
    }

    private fun parseDay(text: String): Int? = when {
        text.contains("星期一") || text.contains("周一") -> 1
        text.contains("星期二") || text.contains("周二") -> 2
        text.contains("星期三") || text.contains("周三") -> 3
        text.contains("星期四") || text.contains("周四") -> 4
        text.contains("星期五") || text.contains("周五") -> 5
        text.contains("星期六") || text.contains("周六") -> 6
        text.contains("星期日") || text.contains("星期天") || text.contains("周日") || text.contains("周天") -> 7
        else -> null
    }

    private fun parseOddEven(text: String): Int = when {
        text.contains("单周") -> 1
        text.contains("双周") -> 2
        else -> 0
    }

    private fun Course.duplicateKey(): String {
        return listOf(
            name.trim(),
            dayOfWeek.toString(),
            startSection.toString(),
            endSection.toString(),
            classroom.trim(),
            startWeek.toString(),
            endWeek.toString()
        ).joinToString("|").lowercase()
    }
}
