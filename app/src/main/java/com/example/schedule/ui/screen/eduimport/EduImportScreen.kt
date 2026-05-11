package com.example.schedule.ui.screen.eduimport

import android.annotation.SuppressLint
import android.os.Message
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedule.ui.theme.AppBackground
import com.example.schedule.ui.theme.DeepGreen
import com.example.schedule.ui.theme.TextPrimary
import com.example.schedule.ui.theme.TextSecondary
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import com.example.schedule.util.CourseImportPreview
import com.example.schedule.util.EduScheduleHtmlImporter
import com.example.schedule.util.EduScheduleImportStatus
import org.json.JSONTokener
import org.koin.androidx.compose.koinViewModel

private const val EDU_SYSTEM_URL = "https://jw.gxstnu.edu.cn"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EduImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var urlText by remember { mutableStateOf(EDU_SYSTEM_URL) }
    var isLoading by remember { mutableStateOf(false) }
    var loadMessage by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<CourseImportPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingImportSummary by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun openCurrentUrl() {
        val normalizedUrl = normalizeUrl(urlText)
        if (normalizedUrl.isBlank()) {
            importError = "请输入教务系统网址"
            return
        }
        focusManager.clearFocus()
        loadMessage = null
        importError = null
        urlText = normalizedUrl
        webView?.loadUrl(normalizedUrl) ?: run {
            importError = "网页还没有准备好"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { currentWebView ->
                currentWebView.stopLoading()
                currentWebView.loadUrl("about:blank")
                currentWebView.removeAllViews()
                currentWebView.destroy()
            }
            webView = null
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "教务系统导入",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "自动打开，登录后进入全学期课表",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        TextButton(
                            onClick = {
                                val currentWebView = webView
                                if (currentWebView == null) {
                                    importError = "网页还没有准备好"
                                } else if (currentWebView.url.isNullOrBlank() || currentWebView.url == "about:blank") {
                                    loadMessage = "正在打开教务系统，请登录后再识别"
                                    openCurrentUrl()
                                } else {
                                    currentWebView.evaluateJavascript(
                                        EDU_EXTRACT_SCRIPT
                                    ) { result ->
                                        val payload = decodeJavascriptString(result)
                                        val importResult = EduScheduleHtmlImporter.parse(payload, uiState.courseList)
                                        when (importResult.status) {
                                            EduScheduleImportStatus.READY -> {
                                                loadMessage = importResult.message
                                                importPreview = importResult.preview
                                            }
                                            EduScheduleImportStatus.OPENING_TIMETABLE -> {
                                                loadMessage = importResult.message
                                            }
                                            EduScheduleImportStatus.NOT_FOUND,
                                            EduScheduleImportStatus.ERROR -> {
                                                importError = importResult.message.ifBlank { "请先登录并打开学期理论课表" }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("识别", color = DeepGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("教务系统 URL") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { openCurrentUrl() }
                        )
                    )
                    Button(
                        onClick = { openCurrentUrl() },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                    ) {
                        Text("打开")
                    }
                }

                loadMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.height(2.dp),
                    color = DeepGreen,
                    trackColor = AppBackground
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        WebView.setWebContentsDebuggingEnabled(true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.loadWithOverviewMode = false
                        settings.useWideViewPort = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val parent = view ?: return false
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val childWebView = WebView(parent.context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(false)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            childView: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString().orEmpty()
                                            if (targetUrl.isNotBlank()) {
                                                parent.loadUrl(targetUrl)
                                            }
                                            return true
                                        }

                                        override fun onPageFinished(childView: WebView?, childUrl: String?) {
                                            if (!childUrl.isNullOrBlank() && childUrl != "about:blank") {
                                                parent.loadUrl(childUrl)
                                            }
                                            (childView?.parent as? ViewGroup)?.removeView(childView)
                                            childView?.destroy()
                                        }
                                    }
                                }
                                transport.webView = childWebView
                                resultMsg.sendToTarget()
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                loadMessage = null
                                view?.settings?.applyEduViewport(url.orEmpty())
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (!url.isNullOrBlank() && url != "about:blank") {
                                    urlText = url
                                }
                                view?.settings?.applyEduViewport(url.orEmpty())
                                view?.evaluateJavascript(
                                    if (url.orEmpty().contains("/jsxsd/framework/xsMainV")) {
                                        EDU_MAIN_PAGE_FIX_SCRIPT
                                    } else {
                                        LOGIN_PAGE_FIX_SCRIPT
                                    },
                                    null
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadMessage = "网页加载失败：${error?.description ?: "未知错误"}"
                                }
                            }
                        }
                        webView = this
                        loadUrl(normalizeUrl(urlText).ifBlank { EDU_SYSTEM_URL })
                    }
                }
            )
        }
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
                    text = "可导入 ${preview.validCourses.size} 门，重复 ${preview.duplicateRows.size} 门，错误 ${preview.errorRows.size} 行。",
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

private fun decodeJavascriptString(value: String?): String {
    return runCatching {
        JSONTokener(value ?: "\"\"").nextValue() as? String
    }.getOrNull() ?: value.orEmpty()
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun WebSettings.applyEduViewport(url: String) {
    val isMainPage = url.contains("/jsxsd/framework")
    loadWithOverviewMode = isMainPage
    useWideViewPort = isMainPage
    textZoom = 100
}

private val EDU_EXTRACT_SCRIPT = """
(function() {
  var COURSE_MENU_ID = 'NEW_XSD_PYGL_WDKB_XQLLKB';
  var COURSE_TIMETABLE_PATH = '/jsxsd/xskb/xskb_list.do';

  function clean(text) {
    return (text || '').replace(/\s+/g, ' ').trim();
  }

  function absoluteUrl(path) {
    try {
      return new URL(path, location.origin).href;
    } catch (error) {
      return 'https://jw.gxstnu.edu.cn' + path;
    }
  }

  function safeDocument(win) {
    try {
      return win.document;
    } catch (error) {
      return null;
    }
  }

  function findTimetable(win, depth) {
    if (!win || depth > 6) return null;
    var doc = safeDocument(win);
    if (!doc) return null;

    var table = doc.querySelector('table.qz-weeklyTable');
    if (table) return table;

    var frames = doc.querySelectorAll('iframe');
    for (var i = 0; i < frames.length; i++) {
      try {
        var found = findTimetable(frames[i].contentWindow, depth + 1);
        if (found) return found;
      } catch (error) {}
    }
    return null;
  }

  function findTimetableMenu(doc) {
    if (!doc) return null;
    var menu = doc.querySelector('[data-id="' + COURSE_MENU_ID + '"]');
    if (menu) return menu;

    var menus = doc.querySelectorAll('[data-src], .menu-three');
    for (var i = 0; i < menus.length; i++) {
      var item = menus[i];
      var src = item.getAttribute('data-src') || '';
      var title = clean(item.textContent);
      if (src.indexOf('/xskb/xskb_list.do') !== -1 || title.indexOf('学期理论课表') !== -1) {
        return item;
      }
    }
    return null;
  }

  function openTimetableIfPossible() {
    var menu = findTimetableMenu(document);
    var targetSrc = (menu && menu.getAttribute('data-src')) || COURSE_TIMETABLE_PATH;

    try {
      if (menu && typeof window.createPage === 'function' && window.jQuery) {
        window.createPage(window.jQuery(menu));
      } else if (menu) {
        menu.click();
      }
      var iframe = document.querySelector('.main-right-content iframe[id="' + COURSE_MENU_ID + '"]') ||
        document.querySelector('iframe[src*="/xskb/xskb_list.do"]');
      if (iframe) {
        iframe.src = targetSrc;
        iframe.style.display = 'block';
      } else {
        location.href = absoluteUrl(targetSrc);
      }
      return true;
    } catch (error) {
      try {
        location.href = absoluteUrl(targetSrc);
        return true;
      } catch (ignored) {}
    }
    return false;
  }

  function openTimetableDirectly() {
    try {
      location.href = absoluteUrl(COURSE_TIMETABLE_PATH);
      return true;
    } catch (error) {
      return openTimetableIfPossible();
    }
  }

  function courseCodeFrom(text) {
    return clean(text).replace(/^;?\s*课程号[:：]?/, '');
  }

  function buildDetailFromLines(item) {
    var lines = (item.innerText || '')
      .split(/\n+/)
      .map(function(line) { return clean(line); })
      .filter(Boolean);
    var teacherLine = lines.find(function(line) { return /^教师[:：]/.test(line); }) || '';
    var sectionLine = lines.find(function(line) { return /\d{1,2}\s*[~\-～]\s*\d{1,2}\s*小?节|\d{1,2}\s*小?节/.test(line); }) || '';
    var weekLine = lines.find(function(line) { return /\[[^\]]+周\]\s*星期[一二三四五六日天]/.test(line); }) || '';
    var teacher = teacherLine.replace(/^教师[:：]\s*/, '');
    var sectionMatch = sectionLine.match(/(\d{1,2})\s*[~\-～]\s*(\d{1,2})\s*小?节/) || sectionLine.match(/(\d{1,2})\s*小?节/);
    var weekMatch = weekLine.match(/\[([^\]]+周)\]\s*星期[一二三四五六日天]/);
    var classroom = lines
      .filter(function(line) {
        return line !== teacherLine &&
          line !== sectionLine &&
          line !== weekLine &&
          !/^课程号[:：]?/.test(line);
      })
      .pop() || '';

    if (!teacher || !sectionMatch || !weekMatch) return '';
    var startSection = parseInt(sectionMatch[1], 10);
    var endSection = parseInt(sectionMatch[2] || sectionMatch[1], 10);
    return '老师:' + teacher + ';时间:' + weekMatch[1] + '[' + startSection + '-' + endSection + '节];地点:' + classroom;
  }

  function courseItemsIn(root) {
    var seen = [];
    var items = root.querySelectorAll('li.courselists-item, .courselists-item');
    for (var i = 0; i < items.length; i++) {
      if (seen.indexOf(items[i]) === -1) seen.push(items[i]);
    }
    return seen;
  }

  function collectColumnCenters(table) {
    var centers = [];
    var cells = table.querySelectorAll('td[name="kbDataTd"], td.qz-weeklyTable-td:not(.qz-weeklyTable-label):not(.qz-weeklyTable-detailtext)');
    for (var i = 0; i < cells.length; i++) {
      var rect = cells[i].getBoundingClientRect();
      if (rect.width <= 0) continue;
      centers.push(rect.left + rect.width / 2);
    }
    centers.sort(function(a, b) { return a - b; });

    var clusters = [];
    for (var centerIndex = 0; centerIndex < centers.length; centerIndex++) {
      var center = centers[centerIndex];
      var last = clusters[clusters.length - 1];
      if (!last || Math.abs(last.value - center) > 12) {
        clusters.push({ value: center, count: 1 });
      } else {
        last.value = (last.value * last.count + center) / (last.count + 1);
        last.count++;
      }
    }

    return clusters
      .sort(function(a, b) { return b.count - a.count; })
      .slice(0, 7)
      .map(function(cluster) { return cluster.value; })
      .sort(function(a, b) { return a - b; });
  }

  function dayFromCell(td, columnCenters) {
    if (columnCenters.length < 7) return 0;
    var rect = td.getBoundingClientRect();
    var center = rect.left + rect.width / 2;
    var nearest = 0;
    var nearestDistance = Infinity;
    for (var i = 0; i < columnCenters.length; i++) {
      var distance = Math.abs(columnCenters[i] - center);
      if (distance < nearestDistance) {
        nearest = i;
        nearestDistance = distance;
      }
    }
    return nearest + 1;
  }

  function dayNumber(text) {
    var days = { '一': 1, '二': 2, '三': 3, '四': 4, '五': 5, '六': 6, '日': 7, '天': 7 };
    var match = (text || '').match(/星期([一二三四五六日天])/);
    return match ? days[match[1]] : 0;
  }

  function isCurrentWeekTable(table) {
    var firstHeader = clean(table.querySelector('thead th') ? table.querySelector('thead th').textContent : '');
    var headerText = clean(table.querySelector('thead') ? table.querySelector('thead').textContent : '');
    return firstHeader.indexOf('节次') !== -1 && /\d{2}-\d{2}/.test(headerText);
  }

  function parseCompactCourseItems(doc) {
    var result = [];
    var items = doc.querySelectorAll('table.qz-weeklyTable li.courselists-item, table.qz-weeklyTable .courselists-item');
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
      var titleElement = item.querySelector('.qz-hasCourse-title');
      var name = clean(titleElement ? titleElement.textContent : '');
      var lines = (item.innerText || '')
        .split(/\n+/)
        .map(function(line) { return clean(line); })
        .filter(Boolean);
      var teacherLine = lines.find(function(line) { return /^教师[:：]/.test(line); }) || '';
      var sectionLine = lines.find(function(line) { return /\d{1,2}\s*[~\-～]\s*\d{1,2}\s*小?节|\d{1,2}\s*小?节/.test(line); }) || '';
      var weekLine = lines.find(function(line) { return /\[[^\]]+周\]\s*星期[一二三四五六日天]/.test(line); }) || '';
      var teacher = teacherLine.replace(/^教师[:：]\s*/, '');
      var sectionMatch = sectionLine.match(/(\d{1,2})\s*[~\-～]\s*(\d{1,2})\s*小?节/) || sectionLine.match(/(\d{1,2})\s*小?节/);
      var weekMatch = weekLine.match(/\[([^\]]+周)\]\s*星期[一二三四五六日天]/);
      var dayOfWeek = dayNumber(weekLine);
      var classroom = lines
        .filter(function(line) {
          return line !== name &&
            line !== teacherLine &&
            line !== sectionLine &&
            line !== weekLine;
        })
        .pop() || '';

      if (name && teacher && sectionMatch && weekMatch && dayOfWeek) {
        var startSection = parseInt(sectionMatch[1], 10);
        var endSection = parseInt(sectionMatch[2] || sectionMatch[1], 10);
        result.push({
          rowNumber: i + 1,
          dayOfWeek: dayOfWeek,
          name: name,
          detail: '老师:' + teacher + ';时间:' + weekMatch[1] + '[' + startSection + '-' + endSection + '节];地点:' + classroom,
          courseCode: ''
        });
      }
    }
    return result;
  }

  var table = findTimetable(window, 0);
  if (!table) {
    if (openTimetableIfPossible()) {
      return JSON.stringify({
        status: 'opening',
        message: '已打开学期理论课表，请等待加载完成后再次点击识别',
        courses: []
      });
    }
    return JSON.stringify({
      status: 'not_found',
      message: '未找到课表，请先登录并进入“学期理论课表”',
      courses: []
    });
  }

  if (isCurrentWeekTable(table)) {
    if (openTimetableDirectly()) {
      return JSON.stringify({
        status: 'opening',
        message: '当前是本周课表，正在直接打开学期理论课表；请等待加载完成后再次点击识别',
        courses: []
      });
    }
  }

  var courses = [];
  var columnCenters = collectColumnCenters(table);
  var dataCells = table.querySelectorAll('td[name="kbDataTd"], td.qz-weeklyTable-td:not(.qz-weeklyTable-label):not(.qz-weeklyTable-detailtext)');
  var currentWeekOnly = isCurrentWeekTable(table);

  for (var cellIndex = 0; cellIndex < dataCells.length; cellIndex++) {
    var td = dataCells[cellIndex];
    var dayOfWeek = dayFromCell(td, columnCenters);
    if (!dayOfWeek) continue;

    var items = courseItemsIn(td);
    for (var itemIndex = 0; itemIndex < items.length; itemIndex++) {
      var item = items[itemIndex];
      var titleElement = item.querySelector('.qz-hasCourse-title');
      var detailElement = item.querySelector('.qz-hasCourse-abbrinfo');
      var codeElement = item.querySelector('span[name="kchSpan"]');
      var name = clean(titleElement ? titleElement.textContent : '');
      var detail = clean(detailElement ? detailElement.textContent : '') || buildDetailFromLines(item);

      if (name && detail) {
        courses.push({
          rowNumber: cellIndex + 1,
          dayOfWeek: dayOfWeek,
          name: name,
          detail: detail,
          courseCode: courseCodeFrom(codeElement ? codeElement.textContent : '')
        });
      }
    }
  }

  if (!courses.length) {
    courses = parseCompactCourseItems(table.ownerDocument);
  }

  if (!courses.length) {
    return JSON.stringify({
      status: 'not_found',
      message: '已找到课表表格，但没有识别到课程块',
      courses: []
    });
  }

  if (currentWeekOnly) {
    return JSON.stringify({
      status: 'not_found',
      message: '当前页面是“本周课表”，页面源码里只有 ' + courses.length + ' 条本周课程。请在教务系统进入“学期理论课表/全学期课表”后再识别，否则会漏掉其它周次课程。',
      courses: []
    });
  }

  return JSON.stringify({
    status: 'ready',
    message: '识别到 ' + courses.length + ' 条课程明细',
    courses: courses
  });
})();
""".trimIndent()

private val EDU_MAIN_PAGE_FIX_SCRIPT = """
(function() {
  if (!location.pathname.includes('/jsxsd/framework')) return;

  var viewport = document.querySelector('meta[name="viewport"]');
  if (!viewport) {
    viewport = document.createElement('meta');
    viewport.name = 'viewport';
    document.head.appendChild(viewport);
  }
  viewport.content = 'width=1280, initial-scale=0.84, minimum-scale=0.25, maximum-scale=3, user-scalable=yes';

  var style = document.getElementById('schedule-main-page-fix');
  if (!style) {
    style = document.createElement('style');
    style.id = 'schedule-main-page-fix';
    document.head.appendChild(style);
  }

  style.textContent = [
    'html, body { width: 1280px !important; min-width: 1280px !important; height: 100% !important; min-height: 100% !important; overflow: auto !important; }',
    'body { transform-origin: 0 0 !important; }',
    '.layui-layout, .layui-body, .qz-body, .qz-content { min-width: 1280px !important; }',
    '#person { width: 100% !important; min-width: 1036px !important; height: calc(100vh - 92px) !important; min-height: 820px !important; }',
    'iframe { max-width: none !important; }'
  ].join('\n');

  window.dispatchEvent(new Event('resize'));
})();
""".trimIndent()

private val LOGIN_PAGE_FIX_SCRIPT = """
(function() {
  if (!document.querySelector('.login')) return;

  var style = document.getElementById('schedule-login-page-fix');
  if (!style) {
    style = document.createElement('style');
    style.id = 'schedule-login-page-fix';
    document.head.appendChild(style);
  }

  style.textContent = [
    'html, body { width: 100% !important; height: 100% !important; min-height: 100% !important; overflow: auto !important; }',
    '.login { min-height: 100vh !important; height: 100vh !important; overflow: visible !important; background-size: cover !important; }',
    '.logo { left: calc(50% - 96px) !important; top: 24px !important; }',
    '.form-container { position: absolute !important; left: 0 !important; right: 0 !important; top: 92px !important; width: 100% !important; box-sizing: border-box !important; }',
    '.layui-form { width: 100% !important; height: auto !important; min-height: 560px !important; padding: 0 20px 24px !important; box-sizing: border-box !important; }',
    '.layui-form .layui-input-wrap > input, .layui-form .rail, .layui-form .btn { width: 100% !important; box-sizing: border-box !important; }',
    '.layui-form .valid-code-bar > input { width: 58% !important; }',
    '.layui-form .valid-code-bar .auth-code-img { width: 40% !important; margin-left: 2% !important; object-fit: contain !important; }'
  ].join('\n');
})();
""".trimIndent()
