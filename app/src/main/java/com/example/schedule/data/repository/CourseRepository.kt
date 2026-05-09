package com.example.schedule.data.repository

import com.example.schedule.data.db.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getAllCourses(): Flow<List<Course>>
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>>
    suspend fun getCourseById(id: Int): Course?
    suspend fun insertCourse(course: Course)
    suspend fun updateCourse(course: Course)
    suspend fun deleteCourse(course: Course)
    suspend fun deleteAll()
}
