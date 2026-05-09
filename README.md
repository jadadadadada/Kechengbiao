# Kechengbiao
（本项目全由AI生成，使用了Codex与Claude Code，第一个AI练手项目，其实也没什么好说的。）
一个基于 Android 原生技术栈开发的课程表 App，用于管理个人课程、查看每日/每周课表，并支持从学校教务系统或 CSV 文件导入课程数据。

## 功能

- 周课表视图：按星期和节次展示课程安排。
- 今日课程视图：快速查看当天课程。
- 课程管理：新增、编辑、启用/停用、删除课程。
- 课程详情：支持课程名称、教师、教室、周次、节次、单双周、备注、颜色等信息。
- 课前提醒：可为课程设置提前提醒时间，支持开机后重新恢复提醒。
- 教务系统导入：内置 WebView 登录教务系统，识别课表页面并导入课程。
- CSV 导入：从本地 CSV 文件导入课程，默认跳过重复课程。
- CSV 模板导出：生成可填写的课程导入模板。
- 本地存储：课程数据保存在本机 Room 数据库中。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Compose Navigation
- Room
- Kotlin Flow / Coroutines
- Koin
- Android WebView

## 项目结构

```text
app/src/main/java/com/example/schedule/
├── data/
│   ├── db/              # Room Entity、DAO、Database
│   └── repository/      # 数据仓库
├── di/                  # Koin 依赖注入配置
├── ui/
│   ├── screen/          # 各页面：周视图、今日、课程列表、详情、我的、教务导入
│   ├── theme/           # 主题与颜色
│   └── viewmodel/       # ViewModel
└── util/                # 导入解析、提醒调度、通知接收器等工具
```

## 运行环境

- Android Studio
- JDK 17
- Android Gradle Plugin 8.7.3
- Kotlin 2.1.10
- minSdk 26
- targetSdk 34
- compileSdk 35

## 构建

在项目根目录执行：

```bash
./gradlew assembleDebug
```

Windows 下也可以执行：

```powershell
.\gradlew.bat assembleDebug
```

连接 Android 设备后安装调试包：

```powershell
.\gradlew.bat installDebug
```

## CSV 格式

CSV 导入支持以下字段：

```csv
课程名称,教师,教室,星期,开始节次,结束节次,开始周,结束周,单双周,颜色,提醒分钟,备注,启用
```

其中：

- `星期`：1-7，分别表示周一到周日。
- `单双周`：可填写每周、单周、双周，也可按 App 导出的模板填写。
- `提醒分钟`：0 表示不提醒。
- `启用`：用于控制课程是否显示和参与提醒。

## 教务系统导入

App 内置教务系统导入页面，默认面向当前已适配的教务系统课表页面。使用流程：

1. 在“我的”页面进入“教务系统导入”。
2. 登录教务系统。
3. 打开个人课表页面。
4. 点击识别并导入课程。

导入时会尽量识别课程名称、教师、教室、周次、节次、星期等信息；遇到非连续周次会拆分为多条课程记录。

## 数据说明

课程数据仅保存在本地设备中。教务系统导入过程通过 App 内 WebView 在本机完成页面识别，不依赖额外服务器。
