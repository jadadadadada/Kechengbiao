package com.example.schedule.data.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,            // 主键，自增
    val name: String,           // 课程名称
    val teacher: String,        // 授课教师
    val classroom: String,      // 上课教室
    val dayOfWeek: Int,         // 星期几（1=周一，5=周五）
    val startSection: Int,      // 开始节次（1-based）
    val endSection: Int,        // 结束节次
    val startWeek: Int,         // 开始周
    val endWeek: Int,           // 结束周
    val color: Int,             // 课程颜色（ARGB Int）
    val note: String = "",      // 备注
    val isEnabled: Boolean = true, // 是否启用
    val reminderMinutes: Int = 0,  // 课前提醒分钟（0=不提醒）
    val oddEvenWeek: Int = 0    // 0=每周, 1=单周, 2=双周
)
