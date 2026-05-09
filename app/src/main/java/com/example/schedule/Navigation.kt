package com.example.schedule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object WeekSchedule : Screen("week")
    data object DaySchedule : Screen("day")
    data object CourseDetail : Screen("detail/{courseId}") {
        fun createRoute(courseId: Int = -1) = "detail/$courseId"
    }
    data object CourseList : Screen("course_list")
    data object Profile : Screen("profile")
    data object EduImport : Screen("edu_import")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val bottomNavItems = listOf(
    BottomNavItem("课程表", Icons.Default.CalendarMonth, Screen.WeekSchedule),
    BottomNavItem("今日", Icons.Default.Today, Screen.DaySchedule),
    BottomNavItem("课程", Icons.Default.FolderOpen, Screen.CourseList),
    BottomNavItem("我的", Icons.Default.Person, Screen.Profile),
)
