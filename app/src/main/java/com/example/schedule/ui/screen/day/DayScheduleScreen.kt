package com.example.schedule.ui.screen.day

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedule.data.db.Course
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.CardSurface
import com.example.schedule.ui.theme.CompletedBadgeBg
import com.example.schedule.ui.theme.CompletedGray
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.InProgressBadgeBg
import com.example.schedule.ui.theme.InProgressBadgeText
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val sectionTimes = mapOf(
    1 to ("08:00" to "08:45"),
    2 to ("08:50" to "09:35"),
    3 to ("09:50" to "10:35"),
    4 to ("10:40" to "11:25"),
    5 to ("11:30" to "12:15"),
    6 to ("13:30" to "14:15"),
    7 to ("14:20" to "15:05"),
    8 to ("15:20" to "16:05"),
    9 to ("16:10" to "16:55"),
    10 to ("17:00" to "17:45"),
    11 to ("19:00" to "19:45"),
    12 to ("19:50" to "20:35"),
)

private enum class CourseStatus {
    IN_PROGRESS,
    COMPLETED,
    UPCOMING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleScreen(
    viewModel: ScheduleViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onCourseClick: (Course) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayDate = LocalDate.now()
    val dayChinese = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val todayCourses = remember(uiState.courseList, uiState.currentWeek, todayDate) {
        uiState.courseList.filter { course ->
            course.isEnabled &&
                course.dayOfWeek == todayDate.dayOfWeek.value &&
                uiState.currentWeek in course.startWeek..course.endWeek &&
                when (course.oddEvenWeek) {
                    1 -> uiState.currentWeek % 2 == 1
                    2 -> uiState.currentWeek % 2 == 0
                    else -> true
                }
        }
    }

    var tick by remember { mutableIntStateOf(0) }
    var showMainMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showMainMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                        DropdownMenu(
                            expanded = showMainMenu,
                            onDismissRequest = { showMainMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新状态") },
                                onClick = {
                                    tick++
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新增课程") },
                                onClick = {
                                    onAddClick()
                                    showMainMenu = false
                                }
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "今日课程",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${todayDate.monthValue}月${todayDate.dayOfMonth}日 · ${dayChinese[todayDate.dayOfWeek.value - 1]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新状态") },
                                onClick = {
                                    tick++
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新增课程") },
                                onClick = {
                                    onAddClick()
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = DeepGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增课程")
            }
        }
    ) { paddingValues ->
        if (todayCourses.isEmpty()) {
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            val (inProgressCourses, upcomingCourses, completedCourses) = remember(todayCourses, tick) {
                classifyCourses(todayCourses, LocalTime.now())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                if (completedCourses.isNotEmpty()) {
                    item(key = "header_completed") { SectionHeader("已完成") }
                    items(completedCourses, key = { it.id }) { course ->
                        DayCourseCard(course = course, status = CourseStatus.COMPLETED, onClick = { onCourseClick(course) })
                    }
                }

                if (inProgressCourses.isNotEmpty()) {
                    item(key = "header_in_progress") { SectionHeader("进行中") }
                    items(inProgressCourses, key = { it.id }) { course ->
                        DayCourseCard(course = course, status = CourseStatus.IN_PROGRESS, onClick = { onCourseClick(course) })
                    }
                }

                if (upcomingCourses.isNotEmpty()) {
                    item(key = "header_upcoming") { SectionHeader("待上课程") }
                    items(upcomingCourses, key = { it.id }) { course ->
                        DayCourseCard(course = course, status = CourseStatus.UPCOMING, onClick = { onCourseClick(course) })
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DayCourseCard(
    course: Course,
    status: CourseStatus,
    onClick: () -> Unit
) {
    val courseColor = Color(course.color)
    val (startTime, endTime) = sectionTimes[course.startSection]?.first to
        sectionTimes[course.endSection]?.second

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (status == CourseStatus.IN_PROGRESS) {
            androidx.compose.foundation.BorderStroke(0.8.dp, DeepGreen.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            if (status == CourseStatus.IN_PROGRESS) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(DeepGreen)
                )
            }

            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .padding(start = if (status == CourseStatus.IN_PROGRESS) 12.dp else 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = startTime ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = endTime ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .align(Alignment.CenterVertically)
                    .background(TextSecondary.copy(alpha = 0.18f))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(courseColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                InfoLine(icon = Icons.Default.Place, text = course.classroom)
                Spacer(modifier = Modifier.height(4.dp))
                InfoLine(icon = Icons.Default.Person, text = course.teacher)
            }

            Column(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .padding(end = 12.dp, top = 14.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                when (status) {
                    CourseStatus.IN_PROGRESS -> StatusBadge("下一节", InProgressBadgeBg, InProgressBadgeText)
                    CourseStatus.COMPLETED -> StatusBadge("已完成", CompletedBadgeBg, CompletedGray)
                    CourseStatus.UPCOMING -> Spacer(modifier = Modifier.height(20.dp))
                }
                Icon(
                    imageVector = if (status == CourseStatus.COMPLETED) Icons.AutoMirrored.Filled.Article else Icons.Default.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = TextSecondary
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(text: String, background: Color, content: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = content,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "今天没有课程",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "可以休息一下，或添加新的课程安排",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun classifyCourses(courses: List<Course>, now: LocalTime): Triple<List<Course>, List<Course>, List<Course>> {
    val inProgress = mutableListOf<Course>()
    val upcoming = mutableListOf<Course>()
    val completed = mutableListOf<Course>()

    courses.sortedBy { it.startSection }.forEach { course ->
        val (startStr, _) = sectionTimes[course.startSection] ?: return@forEach
        val (_, endStr) = sectionTimes[course.endSection] ?: return@forEach
        val start = LocalTime.parse(startStr, DateTimeFormatter.ofPattern("HH:mm"))
        val end = LocalTime.parse(endStr, DateTimeFormatter.ofPattern("HH:mm"))

        when {
            now.isAfter(end) -> completed.add(course)
            !now.isBefore(start) && !now.isAfter(end) -> inProgress.add(course)
            else -> upcoming.add(course)
        }
    }

    return Triple(inProgress, upcoming, completed)
}
