package com.example.schedule.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedule.util.CourseCsvImporter
import com.example.schedule.util.CourseImportPreview
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.SurfaceCard
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ConfirmAction {
    CLEAR_COURSES,
    RESET_SAMPLES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEduImportClick: () -> Unit = {},
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var showMainMenu by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<CourseImportPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingImportSummary by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    val enabledCount = uiState.courseList.count { it.isEnabled }
    val disabledCount = uiState.courseList.size - enabledCount
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取文件")
        }.onSuccess { text ->
            importPreview = CourseCsvImporter.parse(text, uiState.courseList)
            importError = null
        }.onFailure { error ->
            importError = error.message ?: "导入失败"
        }
    }
    val templateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(CSV_TEMPLATE)
            } ?: error("无法创建文件")
        }.onSuccess {
            Toast.makeText(context, "CSV 模板已导出", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
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
                                text = { Text("教务系统导入") },
                                onClick = {
                                    onEduImportClick()
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导入 CSV") },
                                onClick = {
                                    csvLauncher.launch("text/*")
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出 CSV 模板") },
                                onClick = {
                                    templateLauncher.launch("课程导入模板.csv")
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("重置示例课程") },
                                onClick = {
                                    confirmAction = ConfirmAction.RESET_SAMPLES
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清空全部课程") },
                                onClick = {
                                    confirmAction = ConfirmAction.CLEAR_COURSES
                                    showMainMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("回到本周") },
                                onClick = {
                                    viewModel.refreshWeek()
                                    showMainMenu = false
                                }
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "我的",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "学期、数据与应用设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
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
            CurrentWeekCard(
                currentWeek = uiState.currentWeek,
                totalWeeks = uiState.totalWeeks,
                startDate = uiState.semesterStartDate,
                dateText = uiState.semesterStartDate.format(dateFormatter)
            )

            SettingsCard(title = "学期设置") {
                SettingRow(
                    icon = Icons.Default.Today,
                    title = "学期开始日期",
                    subtitle = uiState.semesterStartDate.format(dateFormatter),
                    controls = {
                        StepButton(text = "-7天") {
                            viewModel.updateSemesterStartDate(uiState.semesterStartDate.minusWeeks(1))
                        }
                        StepButton(text = "-1天") {
                            viewModel.updateSemesterStartDate(uiState.semesterStartDate.minusDays(1))
                        }
                        StepButton(text = "+1天") {
                            viewModel.updateSemesterStartDate(uiState.semesterStartDate.plusDays(1))
                        }
                        StepButton(text = "+7天") {
                            viewModel.updateSemesterStartDate(uiState.semesterStartDate.plusWeeks(1))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "学期总周数",
                    subtitle = "${uiState.totalWeeks} 周",
                    controls = {
                        RoundIconButton(
                            icon = Icons.Default.Remove,
                            contentDescription = "减少总周数",
                            onClick = { viewModel.updateTotalWeeks(uiState.totalWeeks - 1) }
                        )
                        Text(
                            text = uiState.totalWeeks.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        RoundIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "增加总周数",
                            onClick = { viewModel.updateTotalWeeks(uiState.totalWeeks + 1) }
                        )
                    }
                )
            }

            SettingsCard(title = "数据管理") {
                CourseStatsRow(
                    totalCount = uiState.courseList.size,
                    enabledCount = enabledCount,
                    disabledCount = disabledCount
                )
                Spacer(modifier = Modifier.height(12.dp))
                ActionRow(
                    icon = Icons.Default.Public,
                    title = "教务系统导入",
                    subtitle = "登录学校教务系统后，识别当前课表页面并导入",
                    color = DeepGreen,
                    onClick = onEduImportClick
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionRow(
                    icon = Icons.Default.UploadFile,
                    title = "导入 CSV",
                    subtitle = "从本地 CSV 文件导入课程，默认跳过重复课程",
                    color = DeepGreen,
                    onClick = { csvLauncher.launch("text/*") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionRow(
                    icon = Icons.Default.Storage,
                    title = "导出 CSV 模板",
                    subtitle = "生成可直接填写的课程导入模板",
                    color = DeepGreen,
                    onClick = { templateLauncher.launch("课程导入模板.csv") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionRow(
                    icon = Icons.Default.Refresh,
                    title = "重置示例课程",
                    subtitle = "清空现有课程，并重新生成默认课程数据",
                    color = DeepGreen,
                    onClick = { confirmAction = ConfirmAction.RESET_SAMPLES }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "清空全部课程",
                    subtitle = "删除所有课程数据，学期设置会保留",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { confirmAction = ConfirmAction.CLEAR_COURSES }
                )
            }

            SettingsCard(title = "应用信息") {
                InfoRow(
                    icon = Icons.Default.Info,
                    title = "课程表",
                    subtitle = "版本 1.0.0"
                )
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(
                    icon = Icons.Default.Storage,
                    title = "本地存储",
                    subtitle = "课程使用 Room 保存，学期设置使用 SharedPreferences 保存"
                )
            }
        }
    }

    confirmAction?.let { action ->
        ConfirmDialog(
            action = action,
            onDismiss = { confirmAction = null },
            onConfirm = {
                when (action) {
                    ConfirmAction.CLEAR_COURSES -> viewModel.clearAllCourses()
                    ConfirmAction.RESET_SAMPLES -> viewModel.resetSampleCourses()
                }
                confirmAction = null
            }
        )
    }

    importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onDismiss = { importPreview = null },
            onConfirm = {
                pendingImportSummary = preview.validCourses.size to preview.duplicateRows.size
                viewModel.importCourses(preview.validCourses)
                importPreview = null
            }
        )
    }

    pendingImportSummary?.let { (imported, duplicated) ->
        Toast.makeText(context, "已导入 $imported 门，跳过 $duplicated 门重复课程", Toast.LENGTH_SHORT).show()
        pendingImportSummary = null
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("导入失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { importError = null }) {
                    Text("知道了")
                }
            }
        )
    }
}

private const val CSV_TEMPLATE =
    "课程名称,教师,教室,星期,开始节次,结束节次,开始周,结束周,单双周,颜色,提醒分钟,备注,启用\n" +
        "高等数学,张老师,A201,周一,1,2,1,16,每周,#C5E0B4,15,,是\n" +
        "数据结构,李老师,C402,周三,5,6,1,16,每周,#E0C5B4,0,实验楼304,是\n" +
        "大学英语,陈老师,B305,周五,1,2,1,16,每周,#B4D4E0,10,,是\n"

@Composable
private fun CurrentWeekCard(
    currentWeek: Int,
    totalWeeks: Int,
    startDate: LocalDate,
    dateText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "当前第 $currentWeek 周",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "共 $totalWeeks 周 · 开始于 $dateText",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "本周范围：${startDate.plusWeeks((currentWeek - 1).toLong()).format(DateTimeFormatter.ofPattern("M/d"))} - ${startDate.plusWeeks((currentWeek - 1).toLong()).plusDays(6).format(DateTimeFormatter.ofPattern("M/d"))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    controls: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = TextPrimary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = controls)
    }
}

@Composable
private fun CourseStatsRow(
    totalCount: Int,
    enabledCount: Int,
    disabledCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatItem(label = "全部", value = totalCount, modifier = Modifier.weight(1f))
        StatItem(label = "启用", value = enabledCount, modifier = Modifier.weight(1f))
        StatItem(label = "停用", value = disabledCount, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = color)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = TextPrimary)
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = text, color = DeepGreen, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(AppBackground)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = TextPrimary)
    }
}

@Composable
private fun ConfirmDialog(
    action: ConfirmAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isClear = action == ConfirmAction.CLEAR_COURSES
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isClear) "清空全部课程？" else "重置示例课程？") },
        text = {
            Text(
                if (isClear) {
                    "这会删除所有课程数据，但保留学期设置。此操作无法撤销。"
                } else {
                    "这会先清空现有课程，再重新生成默认示例课程。此操作无法撤销。"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = if (isClear) "清空" else "重置",
                    color = if (isClear) MaterialTheme.colorScheme.error else DeepGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ImportPreviewDialog(
    preview: CourseImportPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入预览") },
        text = {
            Column {
                Text(
                    text = "可导入 ${preview.validCourses.size} 门，重复 ${preview.duplicateRows.size} 行，错误 ${preview.errorRows.size} 行。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                if (preview.duplicateRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "重复课程会自动跳过。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (preview.errorRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .height(150.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preview.errorRows.take(8).forEach { error ->
                            Text(
                                text = "第${error.rowNumber}行：${error.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (preview.errorRows.size > 8) {
                            Text(
                                text = "还有 ${preview.errorRows.size - 8} 行错误未显示",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = preview.validCourses.isNotEmpty()
            ) {
                Text("确认导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
