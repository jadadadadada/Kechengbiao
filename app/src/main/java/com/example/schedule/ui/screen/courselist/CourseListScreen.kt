package com.example.schedule.ui.screen.courselist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.SurfaceCard
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.theme.darken
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel

private val dayLabels = listOf("全部", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val enabledFilters = listOf("全部", "启用", "停用")
private val weekFilters = listOf("全部周次", "本周有效")
private val oddEvenLabels = listOf("每周", "单周", "双周")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: ScheduleViewModel = koinViewModel(),
    onCourseClick: (Course) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableIntStateOf(0) }
    var selectedEnabledFilter by remember { mutableIntStateOf(0) }
    var selectedWeekFilter by remember { mutableIntStateOf(0) }
    var keyword by remember { mutableStateOf("") }
    var showFilterPanel by remember { mutableStateOf(true) }
    var showMainMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    fun clearFilters() {
        selectedDay = 0
        selectedEnabledFilter = 0
        selectedWeekFilter = 0
        keyword = ""
    }

    val filteredCourses = remember(
        uiState.courseList,
        uiState.currentWeek,
        selectedDay,
        selectedEnabledFilter,
        selectedWeekFilter,
        keyword
    ) {
        uiState.courseList
            .filter { course -> selectedDay == 0 || course.dayOfWeek == selectedDay }
            .filter { course ->
                when (selectedEnabledFilter) {
                    1 -> course.isEnabled
                    2 -> !course.isEnabled
                    else -> true
                }
            }
            .filter { course ->
                selectedWeekFilter == 0 || course.isActiveInWeek(uiState.currentWeek)
            }
            .filter { course ->
                val query = keyword.trim()
                query.isEmpty() ||
                    course.name.contains(query, ignoreCase = true) ||
                    course.teacher.contains(query, ignoreCase = true) ||
                    course.classroom.contains(query, ignoreCase = true)
            }
            .sortedWith(compareBy<Course> { it.dayOfWeek }.thenBy { it.startSection }.thenBy { it.name })
    }

    val groupedCourses = remember(filteredCourses) {
        filteredCourses.groupBy { it.dayOfWeek }.toSortedMap()
    }
    val courseStats = remember(uiState.courseList, uiState.currentWeek) {
        CourseStatsData(
            totalCount = uiState.courseList.size,
            enabledCount = uiState.courseList.count { it.isEnabled },
            activeThisWeekCount = uiState.courseList.count { it.isEnabled && it.isActiveInWeek(uiState.currentWeek) }
        )
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
                                text = { Text("显示全部课程") },
                                onClick = {
                                    clearFilters()
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("只看启用课程") },
                                onClick = {
                                    selectedEnabledFilter = 1
                                    showFilterPanel = true
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("只看本周课程") },
                                onClick = {
                                    selectedWeekFilter = 1
                                    showFilterPanel = true
                                    showMainMenu = false
                                }
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "课程",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredCourses.size} 门课程",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(Icons.Default.FilterList, contentDescription = if (showFilterPanel) "隐藏筛选" else "显示筛选")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("清空筛选") },
                                onClick = {
                                    clearFilters()
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showFilterPanel) "隐藏筛选区" else "显示筛选区") },
                                onClick = {
                                    showFilterPanel = !showFilterPanel
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
        ) {
            CourseStats(
                stats = courseStats
            )

            Spacer(modifier = Modifier.height(12.dp))

            SearchBox(keyword = keyword, onKeywordChange = { keyword = it })

            if (showFilterPanel) {
                Spacer(modifier = Modifier.height(12.dp))

                FilterChips(labels = dayLabels, selectedIndex = selectedDay, onSelect = { selectedDay = it })

                Spacer(modifier = Modifier.height(8.dp))

                FilterChips(
                    labels = enabledFilters,
                    selectedIndex = selectedEnabledFilter,
                    onSelect = { selectedEnabledFilter = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FilterChips(
                    labels = weekFilters,
                    selectedIndex = selectedWeekFilter,
                    onSelect = { selectedWeekFilter = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCourses.isEmpty()) {
                EmptyCourseState(
                    hasAnyCourse = uiState.courseList.isNotEmpty(),
                    onAddClick = onAddClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 92.dp)
                ) {
                    groupedCourses.forEach { (dayOfWeek, courses) ->
                        item(key = "day_$dayOfWeek") {
                            DayGroupHeader(dayOfWeek = dayOfWeek, count = courses.size)
                        }
                        items(courses, key = { it.id }) { course ->
                            CourseListCard(
                                course = course,
                                currentWeek = uiState.currentWeek,
                                onClick = { onCourseClick(course) },
                                onToggleEnabled = {
                                    viewModel.updateCourse(course.copy(isEnabled = !course.isEnabled))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseStats(
    stats: CourseStatsData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(title = "全部", value = stats.totalCount.toString(), modifier = Modifier.weight(1f))
        StatPill(title = "启用", value = stats.enabledCount.toString(), modifier = Modifier.weight(1f))
        StatPill(title = "本周", value = stats.activeThisWeekCount.toString(), modifier = Modifier.weight(1f))
    }
}

private data class CourseStatsData(
    val totalCount: Int,
    val enabledCount: Int,
    val activeThisWeekCount: Int
)

@Composable
private fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SearchBox(
    keyword: String,
    onKeywordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
        },
        placeholder = {
            Text("搜索课程、老师或教室", color = TextSecondary)
        },
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun FilterChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) DeepGreen else SurfaceCard)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (selected) Color.White else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DayGroupHeader(dayOfWeek: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabels[dayOfWeek],
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "$count 门",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun CourseListCard(
    course: Course,
    currentWeek: Int,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    val courseColor = Color(course.color)
    val titleColor = courseColor.darken(0.5f)
    val activeThisWeek = course.isActiveInWeek(currentWeek)
    val cardAlpha = if (course.isEnabled) 1f else 0.58f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = cardAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(courseColor.copy(alpha = if (course.isEnabled) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.name.firstOrNull()?.toString() ?: "课",
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(courseColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

                InfoLine(icon = Icons.Default.Place, text = course.classroom.ifBlank { "未填写教室" })
                Spacer(modifier = Modifier.height(4.dp))
                InfoLine(icon = Icons.Default.Person, text = course.teacher.ifBlank { "未填写教师" })
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Switch(
                    checked = course.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
                WeekStatusPill(enabled = course.isEnabled, activeThisWeek = activeThisWeek)
                Text(
                    text = "第${course.startSection}-${course.endSection}节",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = "${course.startWeek}-${course.endWeek}周 · ${oddEvenLabels[course.oddEvenWeek.coerceIn(0, 2)]}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
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
private fun WeekStatusPill(enabled: Boolean, activeThisWeek: Boolean) {
    val text = when {
        !enabled -> "停用"
        activeThisWeek -> "本周"
        else -> "非本周"
    }
    val color = when {
        !enabled -> TextSecondary
        activeThisWeek -> DeepGreen
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyCourseState(
    hasAnyCourse: Boolean,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = TextSecondary.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasAnyCourse) "没有符合条件的课程" else "还没有课程",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasAnyCourse) "换个筛选条件试试" else "添加第一门课程后会显示在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (!hasAnyCourse) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(DeepGreen)
                        .clickable(onClick = onAddClick)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("新增课程", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun Course.isActiveInWeek(week: Int): Boolean {
    if (week !in startWeek..endWeek) return false
    return when (oddEvenWeek) {
        1 -> week % 2 == 1
        2 -> week % 2 == 0
        else -> true
    }
}
