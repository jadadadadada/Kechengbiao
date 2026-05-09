package com.example.schedule.util

import com.example.schedule.data.db.Course

data class CourseImportPreview(
    val validCourses: List<Course>,
    val duplicateRows: List<CourseImportDuplicate>,
    val errorRows: List<CourseImportError>
)

data class CourseImportDuplicate(
    val rowNumber: Int,
    val course: Course
)

data class CourseImportError(
    val rowNumber: Int,
    val message: String,
    val rawText: String
)

object CourseCsvImporter {

    private val colorOptions = listOf(
        0xFF7FD77D.toInt(),
        0xFF5B8DEF.toInt(),
        0xFFB08AE8.toInt(),
        0xFFF2A344.toInt(),
        0xFFE97880.toInt(),
        0xFF79CBC6.toInt(),
        0xFFC5E0B4.toInt(),
        0xFFB4D4E0.toInt(),
        0xFFE0C5B4.toInt(),
        0xFFE0D4B4.toInt(),
    )

    fun parse(csvText: String, existingCourses: List<Course>): CourseImportPreview {
        val lines = csvText
            .removePrefix("\uFEFF")
            .lines()
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return CourseImportPreview(
                validCourses = emptyList(),
                duplicateRows = emptyList(),
                errorRows = listOf(CourseImportError(1, "文件为空", ""))
            )
        }

        val header = parseCsvLine(lines.first()).map { normalizeHeader(it) }
        val rows = lines.drop(1)
        val existingKeys = existingCourses.map { it.duplicateKey() }.toMutableSet()
        val importedKeys = mutableSetOf<String>()
        val validCourses = mutableListOf<Course>()
        val duplicateRows = mutableListOf<CourseImportDuplicate>()
        val errorRows = mutableListOf<CourseImportError>()

        rows.forEachIndexed { index, rawLine ->
            val rowNumber = index + 2
            val columns = parseCsvLine(rawLine)
            val row = header.mapIndexedNotNull { columnIndex, name ->
                name.takeIf { it.isNotBlank() }?.let { it to columns.getOrElse(columnIndex) { "" }.trim() }
            }.toMap()

            val result = parseCourse(row, rowNumber)
            result.fold(
                onSuccess = { course ->
                    val keyedCourse = course.copy(color = course.color.takeIf { it != 0 }
                        ?: colorOptions[(rowNumber - 2) % colorOptions.size])
                    val key = keyedCourse.duplicateKey()
                    if (key in existingKeys || key in importedKeys) {
                        duplicateRows += CourseImportDuplicate(rowNumber, keyedCourse)
                    } else {
                        validCourses += keyedCourse
                        importedKeys += key
                    }
                },
                onFailure = { error ->
                    errorRows += CourseImportError(rowNumber, error.message ?: "无法解析", rawLine)
                }
            )
        }

        return CourseImportPreview(validCourses, duplicateRows, errorRows)
    }

    private fun parseCourse(row: Map<String, String>, rowNumber: Int): Result<Course> = runCatching {
        val name = row.value("课程名称", "课程名", "name").trim()
        require(name.isNotBlank()) { "课程名为空" }

        val dayOfWeek = parseDay(row.value("星期", "周几", "day"))
            ?: error("星期无法识别")
        val startSection = row.value("开始节次", "开始节", "start").toIntOrNull()
            ?: error("开始节次不是数字")
        val endSection = row.value("结束节次", "结束节", "end").toIntOrNull()
            ?: error("结束节次不是数字")
        val startWeek = row.value("开始周", "startWeek").toIntOrNull()
            ?: error("开始周不是数字")
        val endWeek = row.value("结束周", "endWeek").toIntOrNull()
            ?: error("结束周不是数字")

        require(dayOfWeek in 1..7) { "星期必须是 1-7 或 周一到周日" }
        require(startSection in 1..12) { "开始节次必须是 1-12" }
        require(endSection in 1..12) { "结束节次必须是 1-12" }
        require(endSection >= startSection) { "结束节次不能小于开始节次" }
        require(startWeek in 1..30) { "开始周必须是 1-30" }
        require(endWeek in 1..30) { "结束周必须是 1-30" }
        require(endWeek >= startWeek) { "结束周不能小于开始周" }

        val reminderMinutes = row.value("提醒分钟", "提醒", "reminder").ifBlank { "0" }.toIntOrNull()
            ?: error("提醒分钟不是数字")

        Course(
            name = name,
            teacher = row.value("教师", "老师", "teacher"),
            classroom = row.value("教室", "地点", "classroom"),
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection,
            startWeek = startWeek,
            endWeek = endWeek,
            oddEvenWeek = parseOddEven(row.value("单双周", "重复", "oddEven")),
            color = parseColor(row.value("颜色", "color")),
            reminderMinutes = reminderMinutes.coerceIn(0, 180),
            note = row.value("备注", "note"),
            isEnabled = parseEnabled(row.value("启用", "enabled"))
        )
    }.recoverCatching { error ->
        throw IllegalArgumentException("第 $rowNumber 行：${error.message}")
    }

    private fun Map<String, String>.value(vararg keys: String): String {
        keys.forEach { key ->
            val normalized = normalizeHeader(key)
            this[normalized]?.let { return it }
        }
        return ""
    }

    private fun normalizeHeader(text: String): String = text.trim().lowercase()

    private fun parseDay(text: String): Int? = when (text.trim()) {
        "1", "周一", "星期一", "一" -> 1
        "2", "周二", "星期二", "二" -> 2
        "3", "周三", "星期三", "三" -> 3
        "4", "周四", "星期四", "四" -> 4
        "5", "周五", "星期五", "五" -> 5
        "6", "周六", "星期六", "六" -> 6
        "7", "周日", "周天", "星期日", "星期天", "日", "天" -> 7
        else -> null
    }

    private fun parseOddEven(text: String): Int = when (text.trim().lowercase()) {
        "单周", "单", "odd", "1" -> 1
        "双周", "双", "even", "2" -> 2
        else -> 0
    }

    private fun parseEnabled(text: String): Boolean = when (text.trim().lowercase()) {
        "否", "false", "0", "no", "停用", "禁用" -> false
        else -> true
    }

    private fun parseColor(text: String): Int {
        val normalized = text.trim().removePrefix("#")
        if (normalized.isBlank()) return 0
        return runCatching {
            val rgb = normalized.toLong(16)
            (0xFF000000 or rgb).toInt()
        }.getOrDefault(0)
    }

    private fun Course.duplicateKey(): String {
        return listOf(
            name.trim(),
            dayOfWeek.toString(),
            startSection.toString(),
            endSection.toString(),
            classroom.trim()
        ).joinToString("|").lowercase()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        result += current.toString()
        return result
    }
}
