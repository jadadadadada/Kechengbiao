package com.example.schedule.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.schedule.data.db.Course
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("course_reminders", Context.MODE_PRIVATE)

    fun scheduleAll(
        courses: List<Course>,
        semesterStartDate: LocalDate,
        totalWeeks: Int
    ) {
        cancelAll()

        val now = LocalDateTime.now()
        val requestCodes = mutableSetOf<String>()

        courses
            .filter { it.isEnabled && it.reminderMinutes > 0 }
            .forEach { course ->
                for (week in course.startWeek..course.endWeek.coerceAtMost(totalWeeks)) {
                    if (!course.isActiveInWeek(week)) continue

                    val startTime = sectionStartTimes[course.startSection] ?: continue
                    val courseDate = semesterStartDate
                        .plusWeeks((week - 1).toLong())
                        .plusDays((course.dayOfWeek - 1).toLong())
                    val reminderAt = LocalDateTime
                        .of(courseDate, startTime)
                        .minusMinutes(course.reminderMinutes.toLong())

                    if (!reminderAt.isAfter(now)) continue

                    val requestCode = course.id * 100 + week
                    val intent = Intent(context, CourseReminderReceiver::class.java).apply {
                        action = ACTION_COURSE_REMINDER
                        putExtra(EXTRA_COURSE_ID, course.id)
                        putExtra(EXTRA_COURSE_NAME, course.name)
                        putExtra(EXTRA_CLASSROOM, course.classroom)
                        putExtra(EXTRA_TEACHER, course.teacher)
                        putExtra(EXTRA_START_TIME, startTime.toString())
                        putExtra(EXTRA_REMINDER_MINUTES, course.reminderMinutes)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val triggerAtMillis = reminderAt
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    requestCodes += requestCode.toString()
                }
            }

        prefs.edit().putStringSet(KEY_REQUEST_CODES, requestCodes).apply()
    }

    fun cancelAll() {
        val requestCodes = prefs.getStringSet(KEY_REQUEST_CODES, emptySet()).orEmpty()
        requestCodes.forEach { code ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code.toIntOrNull() ?: return@forEach,
                Intent(context, CourseReminderReceiver::class.java).apply {
                    action = ACTION_COURSE_REMINDER
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        prefs.edit().remove(KEY_REQUEST_CODES).apply()
    }

    private fun Course.isActiveInWeek(week: Int): Boolean {
        if (week !in startWeek..endWeek) return false
        return when (oddEvenWeek) {
            1 -> week % 2 == 1
            2 -> week % 2 == 0
            else -> true
        }
    }

    companion object {
        const val ACTION_COURSE_REMINDER = "com.example.schedule.action.COURSE_REMINDER"
        const val CHANNEL_ID = "course_reminders"
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_CLASSROOM = "classroom"
        const val EXTRA_TEACHER = "teacher"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"

        private const val KEY_REQUEST_CODES = "request_codes"

        private val sectionStartTimes = mapOf(
            1 to LocalTime.of(8, 0),
            2 to LocalTime.of(8, 50),
            3 to LocalTime.of(9, 50),
            4 to LocalTime.of(10, 40),
            5 to LocalTime.of(11, 30),
            6 to LocalTime.of(13, 30),
            7 to LocalTime.of(14, 20),
            8 to LocalTime.of(15, 20),
            9 to LocalTime.of(16, 10),
            10 to LocalTime.of(17, 0),
            11 to LocalTime.of(19, 0),
            12 to LocalTime.of(19, 50),
        )
    }
}
