package com.example.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schedule.data.db.Course
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.SemesterRepository
import com.example.schedule.data.repository.SemesterSettings
import com.example.schedule.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ScheduleUiState(
    val courseList: List<Course> = emptyList(),
    val todayCourses: List<Course> = emptyList(),
    val currentWeek: Int = 1,
    val displayedWeek: Int = 1,
    val semesterStartDate: LocalDate = SemesterSettings().startDate,
    val totalWeeks: Int = SemesterSettings().totalWeeks,
    val selectedDayOfWeek: Int = LocalDate.now().dayOfWeek.value,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ScheduleViewModel(
    private val repository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var reminderSyncJob: Job? = null
    private var lastReminderSignature: ReminderSignature? = null

    init {
        observeSemesterSettings()
        initData()
    }

    private fun observeSemesterSettings() {
        viewModelScope.launch {
            semesterRepository.settings.collect { settings ->
                _uiState.update { state ->
                    val actualWeek = calculateCurrentWeek(settings.startDate, settings.totalWeeks)
                    val shouldFollowActualWeek = state.displayedWeek == state.currentWeek
                    state.copy(
                        semesterStartDate = settings.startDate,
                        totalWeeks = settings.totalWeeks,
                        currentWeek = actualWeek,
                        displayedWeek = if (shouldFollowActualWeek) {
                            actualWeek
                        } else {
                            state.displayedWeek.coerceIn(1, settings.totalWeeks)
                        }
                    )
                }
                syncReminders()
            }
        }
    }

    private fun initData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                insertTestDataIfEmpty()
                loadCoursesObservable()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun insertTestDataIfEmpty() {
        val courses = repository.getAllCourses().first()
        if (courses.isNotEmpty()) {
            semesterRepository.markSampleCoursesInitialized()
            return
        }
        if (semesterRepository.hasInitializedSampleCourses()) return
        insertSampleCourses()
        semesterRepository.markSampleCoursesInitialized()
    }

    private suspend fun insertSampleCourses() {
        val samples = listOf(
            Course(
                name = "高等数学",
                teacher = "张老师",
                classroom = "A201",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFC5E0B4.toInt()
            ),
            Course(
                name = "数据结构",
                teacher = "李老师",
                classroom = "C402",
                dayOfWeek = 1,
                startSection = 3,
                endSection = 4,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFE0C5B4.toInt()
            ),
            Course(
                name = "大学英语",
                teacher = "陈老师",
                classroom = "B305",
                dayOfWeek = 3,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFB4D4E0.toInt()
            ),
            Course(
                name = "线性代数",
                teacher = "王老师",
                classroom = "A301",
                dayOfWeek = 3,
                startSection = 5,
                endSection = 6,
                startWeek = 1,
                endWeek = 16,
                color = 0xFF5B8DEF.toInt(),
                reminderMinutes = 15
            ),
            Course(
                name = "计算机网络",
                teacher = "陈老师",
                classroom = "C502",
                dayOfWeek = 2,
                startSection = 7,
                endSection = 8,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFE0D4B4.toInt(),
                reminderMinutes = 15
            ),
            Course(
                name = "数据结构实验",
                teacher = "李老师",
                classroom = "机房301",
                dayOfWeek = 3,
                startSection = 9,
                endSection = 10,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFD4B4E0.toInt()
            ),
            Course(
                name = "高等数学",
                teacher = "张老师",
                classroom = "A201",
                dayOfWeek = 5,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16,
                color = 0xFFC5E0B4.toInt()
            )
        )

        repository.insertCourses(samples)
    }

    private fun loadCoursesObservable() {
        viewModelScope.launch {
            repository.getAllCourses().collect { courses ->
                _uiState.update { state ->
                    state.copy(
                        courseList = courses,
                        todayCourses = courses.filter { it.dayOfWeek == state.selectedDayOfWeek },
                        isLoading = false
                    )
                }
                syncReminders(courses)
            }
        }
    }

    fun addCourse(course: Course) {
        viewModelScope.launch { repository.insertCourse(course) }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch { repository.updateCourse(course) }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun clearAllCourses() {
        viewModelScope.launch {
            reminderSyncJob?.cancel()
            lastReminderSignature = null
            withContext(Dispatchers.IO) {
                reminderScheduler.cancelAll()
            }
            repository.deleteAll()
        }
    }

    fun resetSampleCourses() {
        viewModelScope.launch {
            reminderSyncJob?.cancel()
            lastReminderSignature = null
            withContext(Dispatchers.IO) {
                reminderScheduler.cancelAll()
            }
            repository.deleteAll()
            insertSampleCourses()
            semesterRepository.markSampleCoursesInitialized()
        }
    }

    fun importCourses(courses: List<Course>) {
        viewModelScope.launch {
            repository.insertCourses(courses)
            semesterRepository.markSampleCoursesInitialized()
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _uiState.update { state ->
            state.copy(
                selectedDayOfWeek = dayOfWeek,
                todayCourses = state.courseList.filter { it.dayOfWeek == dayOfWeek }
            )
        }
    }

    fun updateSemesterStartDate(date: LocalDate) {
        semesterRepository.updateStartDate(date)
    }

    fun updateTotalWeeks(totalWeeks: Int) {
        semesterRepository.updateTotalWeeks(totalWeeks)
    }

    fun refreshWeek() {
        _uiState.update {
            val actualWeek = calculateCurrentWeek(it.semesterStartDate, it.totalWeeks)
            it.copy(currentWeek = actualWeek, displayedWeek = actualWeek)
        }
    }

    private fun syncReminders(courses: List<Course> = _uiState.value.courseList) {
        val state = _uiState.value
        val signature = ReminderSignature.from(
            courses = courses,
            semesterStartDate = state.semesterStartDate,
            totalWeeks = state.totalWeeks
        )
        if (signature == lastReminderSignature) return

        reminderSyncJob?.cancel()
        reminderSyncJob = viewModelScope.launch(Dispatchers.IO) {
            reminderScheduler.scheduleAll(
                courses = courses,
                semesterStartDate = state.semesterStartDate,
                totalWeeks = state.totalWeeks
            )
            lastReminderSignature = signature
        }
    }

    fun selectDisplayedWeek(week: Int) {
        _uiState.update { it.copy(displayedWeek = week.coerceIn(1, it.totalWeeks)) }
    }

    fun showPreviousWeek() {
        _uiState.update { it.copy(displayedWeek = (it.displayedWeek - 1).coerceAtLeast(1)) }
    }

    fun showNextWeek() {
        _uiState.update { it.copy(displayedWeek = (it.displayedWeek + 1).coerceAtMost(it.totalWeeks)) }
    }

    companion object {
        fun calculateCurrentWeek(semesterStart: LocalDate, totalWeeks: Int = 20): Int {
            val weeks = ChronoUnit.WEEKS.between(semesterStart, LocalDate.now()) + 1
            return weeks.toInt().coerceIn(1, totalWeeks.coerceAtLeast(1))
        }
    }
}

private data class ReminderSignature(
    val semesterStartDate: LocalDate,
    val totalWeeks: Int,
    val courses: List<CourseReminderSignature>
) {
    companion object {
        fun from(
            courses: List<Course>,
            semesterStartDate: LocalDate,
            totalWeeks: Int
        ): ReminderSignature {
            return ReminderSignature(
                semesterStartDate = semesterStartDate,
                totalWeeks = totalWeeks,
                courses = courses
                    .asSequence()
                    .filter { it.isEnabled && it.reminderMinutes > 0 }
                    .sortedBy { it.id }
                    .map { course ->
                        CourseReminderSignature(
                            id = course.id,
                            name = course.name,
                            teacher = course.teacher,
                            classroom = course.classroom,
                            dayOfWeek = course.dayOfWeek,
                            startSection = course.startSection,
                            startWeek = course.startWeek,
                            endWeek = course.endWeek,
                            oddEvenWeek = course.oddEvenWeek,
                            reminderMinutes = course.reminderMinutes
                        )
                    }
                    .toList()
            )
        }
    }
}

private data class CourseReminderSignature(
    val id: Int,
    val name: String,
    val teacher: String,
    val classroom: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val startWeek: Int,
    val endWeek: Int,
    val oddEvenWeek: Int,
    val reminderMinutes: Int
)
