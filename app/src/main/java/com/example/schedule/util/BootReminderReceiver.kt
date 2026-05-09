package com.example.schedule.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.schedule.data.db.ScheduleDatabase
import com.example.schedule.data.repository.SemesterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = ScheduleDatabase.getInstance(context)
                val courses = database.courseDao().getAllCourses().first()
                val settings = SemesterRepository(context).settings.value
                ReminderScheduler(context).scheduleAll(
                    courses = courses,
                    semesterStartDate = settings.startDate,
                    totalWeeks = settings.totalWeeks
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
