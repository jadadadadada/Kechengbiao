package com.example.schedule.ui.theme

import androidx.compose.ui.graphics.Color

// 课程色板使用 ARGB Int 存储，显示时用 Color(course.color) 转换。
val courseColorOptions: List<Int> = listOf(
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

// ========== 设计 Token ==========

val AppBackground = Color(0xFFFAF8F2)
val ScheduleBackground = Color(0xFFFCFAF5)
val SurfaceWarm = Color(0xFFF4F1EA)
val SurfaceCard = Color(0xFFF8F6F0)
val GridLine = Color(0xFFE4E0D7)
val TextPrimary = Color(0xFF20241E)
val TextSecondary = Color(0xFF6D7169)

/** 今日高亮、进行中竖条、选中态主色。 */
val DeepGreen = Color(0xFF3E7B2B)

val InProgressBadgeBg = Color(0xFFE8F3DF)
val InProgressBadgeText = Color(0xFF3E7B2B)
val CompletedGray = Color(0xFF6F756D)
val CompletedBadgeBg = Color(0xFFECEFDF)
val CardSurface = Color(0xFFF8F6F0)

/** 将课程色加深，用于浅色卡片上的文字和边框。 */
fun Color.darken(fraction: Float): Color = Color(
    red = this.red * (1f - fraction),
    green = this.green * (1f - fraction),
    blue = this.blue * (1f - fraction),
    alpha = this.alpha
)
