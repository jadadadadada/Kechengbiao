package com.example.schedule

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.schedule.ui.screen.courselist.CourseListScreen
import com.example.schedule.ui.screen.day.DayScheduleScreen
import com.example.schedule.ui.screen.detail.CourseDetailScreen
import com.example.schedule.ui.screen.eduimport.EduImportScreen
import com.example.schedule.ui.screen.profile.ProfileScreen
import com.example.schedule.ui.screen.week.WeekScheduleScreen
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            ScheduleTheme {
                ScheduleApp()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}

@Composable
private fun ScheduleApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.WeekSchedule.route,
        Screen.DaySchedule.route,
        Screen.CourseList.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        containerColor = AppBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.height(78.dp),
                    containerColor = AppBackground,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(21.dp)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepGreen,
                                selectedTextColor = DeepGreen,
                                indicatorColor = DeepGreen.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Screen.WeekSchedule.route,
            modifier = Modifier.padding(bottom = if (showBottomBar) 78.dp else 0.dp)
        ) {
            composable(Screen.WeekSchedule.route) {
                WeekScheduleScreen(
                    onCourseClick = { course ->
                        navController.navigate(Screen.CourseDetail.createRoute(course.id))
                    },
                    onAddClick = {
                        navController.navigate(Screen.CourseDetail.createRoute(-1))
                    }
                )
            }

            composable(Screen.DaySchedule.route) {
                DayScheduleScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCourseClick = { course ->
                        navController.navigate(Screen.CourseDetail.createRoute(course.id))
                    },
                    onAddClick = {
                        navController.navigate(Screen.CourseDetail.createRoute(-1))
                    }
                )
            }

            composable(
                route = Screen.CourseDetail.route,
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getInt("courseId") ?: -1
                CourseDetailScreen(
                    courseId = if (courseId == -1) null else courseId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.CourseList.route) {
                CourseListScreen(
                    onCourseClick = { course ->
                        navController.navigate(Screen.CourseDetail.createRoute(course.id))
                    },
                    onAddClick = {
                        navController.navigate(Screen.CourseDetail.createRoute(-1))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEduImportClick = {
                        navController.navigate(Screen.EduImport.route)
                    }
                )
            }

            composable(Screen.EduImport.route) {
                EduImportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
