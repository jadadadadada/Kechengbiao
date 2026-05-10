package com.example.schedule.ui.screen.week

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedule.data.db.Course
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.GridLine
import com.example.schedule.ui.theme.ScheduleBackground
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.theme.darken
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TIME_COLUMN_WIDTH = 46.dp
private val SECTION_HEIGHT = 58.dp
private val HEADER_HEIGHT = 54.dp
private val COURSE_PADDING = 3.dp

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

private val dayHeaders = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScheduleScreen(
    viewModel: ScheduleViewModel = koinViewModel(),
    onCourseClick: (Course) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWeekPicker by remember { mutableStateOf(false) }
    var showMainMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val dayDates = remember(uiState.semesterStartDate, uiState.displayedWeek) {
        val weekStartDate = uiState.semesterStartDate.plusWeeks((uiState.displayedWeek - 1).toLong())
        (0..6).map { weekStartDate.plusDays(it.toLong()) }
    }
    val todayDate = LocalDate.now()
    val weekLabel = if (uiState.displayedWeek == uiState.currentWeek) {
        "本周 · 第${uiState.displayedWeek}周"
    } else {
        "第${uiState.displayedWeek}周"
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
                                text = { Text("回到本周") },
                                onClick = {
                                    viewModel.refreshWeek()
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("选择周次") },
                                onClick = {
                                    showWeekPicker = true
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
                        Row(
                            modifier = Modifier.clickable { showWeekPicker = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "课程表",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "选择周次",
                                modifier = Modifier.size(18.dp),
                                tint = TextPrimary
                            )
                        }
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showPreviousWeek() },
                        enabled = uiState.displayedWeek > 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一周")
                    }
                    IconButton(onClick = { showWeekPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择周次")
                    }
                    IconButton(
                        onClick = { viewModel.showNextWeek() },
                        enabled = uiState.displayedWeek < uiState.totalWeeks
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一周")
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
                                text = { Text("回到本周") },
                                onClick = {
                                    viewModel.refreshWeek()
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("选择周次") },
                                onClick = {
                                    showWeekPicker = true
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
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ScheduleBackground)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 一屏保持 5 个日期列的舒适密度，周六周日通过横向滑动查看。
            val dayColumnWidth = remember(maxWidth) { (maxWidth - TIME_COLUMN_WIDTH) / 5 }
            val contentWidth = remember(dayColumnWidth) { TIME_COLUMN_WIDTH + dayColumnWidth * 7 }
            val gridHeight = remember { SECTION_HEIGHT * 12 }
            val visibleCourses = remember(uiState.courseList, uiState.displayedWeek) {
                uiState.courseList.filter { course ->
                    course.isEnabled &&
                        course.dayOfWeek in 1..7 &&
                        uiState.displayedWeek in course.startWeek..course.endWeek &&
                        when (course.oddEvenWeek) {
                            1 -> uiState.displayedWeek % 2 == 1
                            2 -> uiState.displayedWeek % 2 == 0
                            else -> true
                        }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.width(contentWidth)) {
                    WeekHeader(
                        dayDates = dayDates,
                        todayDate = todayDate,
                        dayColumnWidth = dayColumnWidth
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight)
                    ) {
                        ScheduleGrid(dayColumnWidth = dayColumnWidth)

                        visibleCourses.forEach { course ->
                            val offsetX = TIME_COLUMN_WIDTH +
                                dayColumnWidth * (course.dayOfWeek - 1) +
                                COURSE_PADDING
                            val offsetY = SECTION_HEIGHT * (course.startSection - 1) + COURSE_PADDING
                            val cardWidth = dayColumnWidth - COURSE_PADDING * 2
                            val cardHeight = SECTION_HEIGHT *
                                (course.endSection - course.startSection + 1) -
                                COURSE_PADDING * 2

                            CourseCard(
                                course = course,
                                modifier = Modifier
                                    .offset(x = offsetX, y = offsetY)
                                    .width(cardWidth)
                                    .height(cardHeight),
                                onClick = { onCourseClick(course) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWeekPicker) {
        WeekPickerDialog(
            currentWeek = uiState.currentWeek,
            selectedWeek = uiState.displayedWeek,
            totalWeeks = uiState.totalWeeks,
            onSelect = { week ->
                viewModel.selectDisplayedWeek(week)
                showWeekPicker = false
            },
            onDismiss = { showWeekPicker = false }
        )
    }
}

@Composable
private fun WeekPickerDialog(
    currentWeek: Int,
    selectedWeek: Int,
    totalWeeks: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择周次") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..totalWeeks).chunked(4).forEach { rowWeeks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowWeeks.forEach { week ->
                            val selected = week == selectedWeek
                            val isCurrent = week == currentWeek
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) DeepGreen else AppBackground)
                                    .clickable { onSelect(week) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCurrent) "本周" else "$week",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else TextPrimary,
                                    fontWeight = if (selected || isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                        repeat(4 - rowWeeks.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun WeekHeader(
    dayDates: List<LocalDate>,
    todayDate: LocalDate,
    dayColumnWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT),
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(modifier = Modifier.width(TIME_COLUMN_WIDTH))
        dayHeaders.forEachIndexed { index, day ->
            val date = dayDates[index]
            val isToday = date == todayDate
            Box(
                modifier = Modifier
                    .width(dayColumnWidth)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = if (isToday) {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepGreen)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    } else {
                        Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isToday) Color.White else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MM/dd")),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isToday) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleGrid(dayColumnWidth: Dp) {
    val gridLine = GridLine.copy(alpha = 0.65f)
    val timeLine = GridLine.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                val timeColumnWidth = TIME_COLUMN_WIDTH.toPx()
                val sectionHeight = SECTION_HEIGHT.toPx()
                val columnWidth = dayColumnWidth.toPx()

                for (section in 0..12) {
                    val y = sectionHeight * section
                    drawLine(
                        color = gridLine,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }

                drawLine(
                    color = timeLine,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = timeLine,
                    start = Offset(timeColumnWidth, 0f),
                    end = Offset(timeColumnWidth, size.height),
                    strokeWidth = strokeWidth
                )

                for (day in 1..7) {
                    val x = timeColumnWidth + columnWidth * day
                    drawLine(
                        color = gridLine,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .width(TIME_COLUMN_WIDTH)
                .fillMaxHeight()
        ) {
            for (section in 1..12) {
                val (startTime, _) = sectionTimes[section]!!
                Box(
                    modifier = Modifier
                        .width(TIME_COLUMN_WIDTH)
                        .height(SECTION_HEIGHT),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = startTime.removePrefix("0"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "第${section}节",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val courseColor = Color(course.color)
    val cardBg = courseColor.copy(alpha = 0.18f)
    val textColor = courseColor.darken(0.48f)
    val borderColor = courseColor.copy(alpha = 0.45f)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.8.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = course.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = course.classroom,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = textColor.copy(alpha = 0.82f)
            )
        }
    }
}
