package com.example.schedule.data.repository

import com.example.schedule.data.db.Course
import com.example.schedule.data.db.CourseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CourseRepositoryImpl(private val courseDao: CourseDao) : CourseRepository {

    override fun getAllCourses(): Flow<List<Course>> = courseDao.getAllCourses()

    override fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>> = courseDao.getCoursesByDay(dayOfWeek)

    override suspend fun getCourseById(id: Int): Course? = courseDao.getCourseById(id)

    override suspend fun insertCourse(course: Course) {
        withContext(Dispatchers.IO) { courseDao.insertCourse(course) }
    }

    override suspend fun insertCourses(courses: List<Course>) {
        withContext(Dispatchers.IO) {
            if (courses.isNotEmpty()) courseDao.insertCourses(courses)
        }
    }

    override suspend fun updateCourse(course: Course) {
        withContext(Dispatchers.IO) { courseDao.updateCourse(course) }
    }

    override suspend fun deleteCourse(course: Course) {
        withContext(Dispatchers.IO) { courseDao.deleteCourse(course) }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) { courseDao.deleteAll() }
    }
}
