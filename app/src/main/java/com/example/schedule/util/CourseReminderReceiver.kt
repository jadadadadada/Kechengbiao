package com.example.schedule.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.schedule.MainActivity
import com.example.schedule.R
import com.example.schedule.util.ReminderScheduler.Companion.CHANNEL_ID
import com.example.schedule.util.ReminderScheduler.Companion.EXTRA_CLASSROOM
import com.example.schedule.util.ReminderScheduler.Companion.EXTRA_COURSE_ID
import com.example.schedule.util.ReminderScheduler.Companion.EXTRA_COURSE_NAME
import com.example.schedule.util.ReminderScheduler.Companion.EXTRA_REMINDER_MINUTES
import com.example.schedule.util.ReminderScheduler.Companion.EXTRA_START_TIME

class CourseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val courseId = intent.getIntExtra(EXTRA_COURSE_ID, 0)
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME).orEmpty()
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM).orEmpty()
        val startTime = intent.getStringExtra(EXTRA_START_TIME).orEmpty()
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 0)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            courseId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$courseName 即将开始")
            .setContentText("${reminderMinutes}分钟后 $startTime 上课 · $classroom")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${reminderMinutes}分钟后 $startTime 上课，地点：$classroom")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(courseId, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "课前提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "课程开始前发送提醒"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
