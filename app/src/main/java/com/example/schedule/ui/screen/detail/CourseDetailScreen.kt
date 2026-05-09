package com.example.schedule.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedule.data.db.Course
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.SurfaceCard
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.theme.courseColorOptions
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val reminderOptions = listOf(0, 5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int? = null,
    viewModel: ScheduleViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isNew = courseId == null

    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startSection by remember { mutableIntStateOf(1) }
    var endSection by remember { mutableIntStateOf(2) }
    var startWeek by remember { mutableIntStateOf(1) }
    var endWeek by remember { mutableIntStateOf(16) }
    var oddEvenWeek by remember { mutableIntStateOf(0) }
    var color by remember { mutableIntStateOf(courseColorOptions[1]) }
    var reminderMinutes by remember { mutableIntStateOf(15) }
    var isEnabled by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var loadedCourseId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(courseId) {
        if (courseId != null && courseId != loadedCourseId) {
            viewModel.uiState
                .first { !it.isLoading }
                .courseList
                .find { it.id == courseId }
                ?.let { course ->
                    name = course.name
                    teacher = course.teacher
                    classroom = course.classroom
                    dayOfWeek = course.dayOfWeek
                    startSection = course.startSection
                    endSection = course.endSection
                    startWeek = course.startWeek
                    endWeek = course.endWeek
                    oddEvenWeek = course.oddEvenWeek
                    color = course.color
                    reminderMinutes = course.reminderMinutes
                    isEnabled = course.isEnabled
                    note = course.note
                    loadedCourseId = courseId
                }
        }
    }

    fun saveCourse() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val course = Course(
            id = courseId ?: 0,
            name = name.trim(),
            teacher = teacher.trim(),
            classroom = classroom.trim(),
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection.coerceAtLeast(startSection),
            startWeek = startWeek,
            endWeek = endWeek.coerceAtLeast(startWeek),
            color = color,
            note = note.trim(),
            isEnabled = isEnabled,
            reminderMinutes = reminderMinutes,
            oddEvenWeek = oddEvenWeek
        )
        scope.launch {
            if (isNew) viewModel.addCourse(course) else viewModel.updateCourse(course)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(
                        text = if (isNew) "新增课程" else "课程详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除课程")
                        }
                    }
                    IconButton(onClick = { saveCourse() }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CourseHeaderCard(
                name = name,
                classroom = classroom,
                nameError = nameError,
                onNameChange = {
                    name = it
                    nameError = false
                },
                onClassroomChange = { classroom = it }
            )

            SettingsCard {
                DetailInputRow(
                    icon = Icons.Default.Person,
                    title = "任课教师",
                    value = teacher,
                    placeholder = "王老师",
                    onValueChange = { teacher = it }
                )
                DetailDivider()
                TimeSection(
                    dayOfWeek = dayOfWeek,
                    startSection = startSection,
                    endSection = endSection,
                    onDayChange = { dayOfWeek = it },
                    onStartChange = {
                        startSection = it
                        if (endSection < it) endSection = it
                    },
                    onEndChange = { endSection = it.coerceAtLeast(startSection) }
                )
                DetailDivider()
                WeekSection(
                    startWeek = startWeek,
                    endWeek = endWeek,
                    oddEvenWeek = oddEvenWeek,
                    onStartChange = {
                        startWeek = it
                        if (endWeek < it) endWeek = it
                    },
                    onEndChange = { endWeek = it.coerceAtLeast(startWeek) },
                    onOddEvenChange = { oddEvenWeek = it }
                )
                DetailDivider()
                ColorSection(selectedColor = color, onSelect = { color = it })
                DetailDivider()
                ReminderSection(selected = reminderMinutes, onSelect = { reminderMinutes = it })
                DetailDivider()
                NoteSection(note = note, onNoteChange = { if (it.length <= 200) note = it })
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用课程", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除课程") },
            text = { Text("确定删除“${name}”吗？这个操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.deleteCourse(
                            Course(
                                id = courseId ?: 0,
                                name = name,
                                teacher = teacher,
                                classroom = classroom,
                                dayOfWeek = dayOfWeek,
                                startSection = startSection,
                                endSection = endSection,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                color = color,
                                note = note,
                                isEnabled = isEnabled,
                                reminderMinutes = reminderMinutes,
                                oddEvenWeek = oddEvenWeek
                            )
                        )
                        onNavigateBack()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CourseHeaderCard(
    name: String,
    classroom: String,
    nameError: Boolean,
    onNameChange: (String) -> Unit,
    onClassroomChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF4E86F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                InlineEditText(
                    value = name,
                    placeholder = "课程名称",
                    onValueChange = onNameChange,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = if (nameError) MaterialTheme.colorScheme.error else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                InlineEditText(
                    value = classroom,
                    placeholder = "A301 教学楼",
                    onValueChange = onClassroomChange,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailInputRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    DetailRowShell(icon = icon, title = title) {
        InlineEditText(
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
    }
}

@Composable
private fun TimeSection(
    dayOfWeek: Int,
    startSection: Int,
    endSection: Int,
    onDayChange: (Int) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit
) {
    DetailRowShell(icon = Icons.Default.DateRange, title = "上课时间") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    ChoiceChip(
                        text = label,
                        selected = dayOfWeek == index + 1,
                        onClick = { onDayChange(index + 1) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberDropdown(
                    value = startSection,
                    range = 1..12,
                    suffix = "节",
                    onSelect = onStartChange,
                    modifier = Modifier.weight(1f)
                )
                NumberDropdown(
                    value = endSection,
                    range = startSection..12,
                    suffix = "节",
                    onSelect = onEndChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeekSection(
    startWeek: Int,
    endWeek: Int,
    oddEvenWeek: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onOddEvenChange: (Int) -> Unit
) {
    DetailRowShell(icon = Icons.Default.DateRange, title = "重复") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberDropdown(
                    value = startWeek,
                    range = 1..20,
                    suffix = "周",
                    onSelect = onStartChange,
                    modifier = Modifier.weight(1f)
                )
                NumberDropdown(
                    value = endWeek,
                    range = startWeek..20,
                    suffix = "周",
                    onSelect = onEndChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("每周", "单周", "双周").forEachIndexed { index, label ->
                    ChoiceChip(text = label, selected = oddEvenWeek == index, onClick = { onOddEvenChange(index) })
                }
            }
        }
    }
}

@Composable
private fun ColorSection(selectedColor: Int, onSelect: (Int) -> Unit) {
    DetailRowShell(icon = Icons.Default.Palette, title = "课程颜色") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            courseColorOptions.take(6).forEach { colorOption ->
                val selected = colorOption == selectedColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorOption))
                        .then(
                            if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(colorOption) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSection(selected: Int, onSelect: (Int) -> Unit) {
    DetailRowShell(icon = Icons.Default.NotificationsNone, title = "课前提醒") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            reminderOptions.forEach { minutes ->
                ChoiceChip(
                    text = if (minutes == 0) "无" else "${minutes}分",
                    selected = selected == minutes,
                    onClick = { onSelect(minutes) }
                )
            }
        }
    }
}

@Composable
private fun NoteSection(note: String, onNoteChange: (String) -> Unit) {
    DetailRowShell(icon = Icons.Default.Place, title = "备注") {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("可选填写课程备注信息") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DetailRowShell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = TextPrimary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            content()
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = TextSecondary.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 38.dp),
        thickness = 0.6.dp,
        color = TextSecondary.copy(alpha = 0.16f)
    )
}

@Composable
private fun InlineEditText(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(DeepGreen),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(color = TextSecondary.copy(alpha = 0.72f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) DeepGreen else AppBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberDropdown(
    value: Int,
    range: IntRange,
    suffix: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "第$value$suffix",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { option ->
                DropdownMenuItem(
                    text = { Text("第$option$suffix") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
