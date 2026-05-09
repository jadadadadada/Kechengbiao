package com.example.schedule.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class SemesterSettings(
    val startDate: LocalDate = LocalDate.of(2026, 2, 23),
    val totalWeeks: Int = 20
)

class SemesterRepository(context: Context) {

    private val prefs = context.getSharedPreferences("semester_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SemesterSettings> = _settings.asStateFlow()

    fun updateStartDate(date: LocalDate) {
        saveSettings(_settings.value.copy(startDate = date))
    }

    fun updateTotalWeeks(totalWeeks: Int) {
        saveSettings(_settings.value.copy(totalWeeks = totalWeeks.coerceIn(1, 30)))
    }

    fun hasInitializedSampleCourses(): Boolean {
        return prefs.getBoolean(KEY_SAMPLE_COURSES_INITIALIZED, false)
    }

    fun markSampleCoursesInitialized() {
        prefs.edit()
            .putBoolean(KEY_SAMPLE_COURSES_INITIALIZED, true)
            .apply()
    }

    private fun loadSettings(): SemesterSettings {
        val default = SemesterSettings()
        val startDate = runCatching {
            prefs.getString(KEY_START_DATE, null)?.let(LocalDate::parse)
        }.getOrNull() ?: default.startDate
        val totalWeeks = prefs.getInt(KEY_TOTAL_WEEKS, default.totalWeeks).coerceIn(1, 30)
        return SemesterSettings(startDate = startDate, totalWeeks = totalWeeks)
    }

    private fun saveSettings(settings: SemesterSettings) {
        prefs.edit()
            .putString(KEY_START_DATE, settings.startDate.toString())
            .putInt(KEY_TOTAL_WEEKS, settings.totalWeeks)
            .apply()
        _settings.value = settings
    }

    private companion object {
        const val KEY_START_DATE = "start_date"
        const val KEY_TOTAL_WEEKS = "total_weeks"
        const val KEY_SAMPLE_COURSES_INITIALIZED = "sample_courses_initialized"
    }
}
