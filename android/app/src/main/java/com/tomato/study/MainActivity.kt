package com.tomato.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

private enum class Palette(val label: String, val primary: Color, val soft: Color, val tint: Color) {
    Forest("森林绿", Color(0xFF16834F), Color(0xFFE3F2E8), Color(0xFFF4F8F4)),
    Ocean("海盐蓝", Color(0xFF2867A8), Color(0xFFE2EEFB), Color(0xFFF3F7FC)),
    Berry("莓果粉", Color(0xFFA43E70), Color(0xFFF7E3EC), Color(0xFFFCF5F8)),
    Sunset("日落橙", Color(0xFFB65525), Color(0xFFF9E8DC), Color(0xFFFCF7F2))
}
private enum class Backdrop(val label: String, val color: Color) {
    Paper("纸感白", Color(0xFFFAFAF8)), Mist("雾灰", Color(0xFFF0F2F1)), Cream("奶油", Color(0xFFFFF8ED))
}
private data class ThemePrefs(val palette: Palette = Palette.Forest, val backdrop: Backdrop = Backdrop.Paper, val dark: Boolean = false)
private val Ink = Color(0xFF1C1B1F)
private val Muted = Color(0xFF727873)
private val SurfaceSoft = Color(0xFFF4F7F4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val store = remember { context.getSharedPreferences("visual_theme", MODE_PRIVATE) }
            var prefs by remember {
                mutableStateOf(ThemePrefs(
                    palette = Palette.entries.firstOrNull { it.name == store.getString("palette", Palette.Forest.name) } ?: Palette.Forest,
                    backdrop = Backdrop.entries.firstOrNull { it.name == store.getString("backdrop", Backdrop.Paper.name) } ?: Backdrop.Paper,
                    dark = store.getBoolean("dark", false)
                ))
            }
            TomatoTheme(prefs) { TomatoApp(prefs) { next ->
                prefs = next
                store.edit().putString("palette", next.palette.name).putString("backdrop", next.backdrop.name).putBoolean("dark", next.dark).apply()
            } }
        }
    }
}

@Composable
private fun TomatoTheme(prefs: ThemePrefs, content: @Composable () -> Unit) {
    val primary = prefs.palette.primary
    val soft = prefs.palette.soft
    MaterialTheme(
        colorScheme = if (prefs.dark) darkColorScheme(primary = primary.copy(red = (primary.red + .35f).coerceAtMost(1f), green = (primary.green + .35f).coerceAtMost(1f), blue = (primary.blue + .35f).coerceAtMost(1f)), onPrimary = Color(0xFF151515),
            secondary = primary.copy(alpha = .75f), primaryContainer = soft.copy(red = .20f, green = .24f, blue = .21f), onPrimaryContainer = Color(0xFFE2E3DE),
            secondaryContainer = soft.copy(red = .20f, green = .24f, blue = .21f), onSecondaryContainer = Color(0xFFE2E3DE),
            background = Color(0xFF111411), surface = Color(0xFF1B211D), onSurface = Color(0xFFE2E3DE), surfaceVariant = Color(0xFF303831)) else lightColorScheme(primary = primary, onPrimary = Color.White,
            primaryContainer = soft, onPrimaryContainer = primary, secondaryContainer = soft, onSecondaryContainer = primary,
            secondary = primary.copy(alpha = .8f), background = prefs.backdrop.color,
            surface = Color.White, onSurface = Ink, surfaceVariant = soft),
        content = content
    )
}

private enum class Page(val label: String) {
    Home("首页"), Focus("番茄钟"), Room("自习室"), Plan("计划"), CheckIn("打卡"), Stats("统计"), Profile("个人主页")
}

@Composable
private fun TomatoApp(themePrefs: ThemePrefs, onThemeChange: (ThemePrefs) -> Unit) {
    val dark = themePrefs.dark
    val accent = MaterialTheme.colorScheme.primary
    val accentSoft = MaterialTheme.colorScheme.secondaryContainer
    var page by remember { mutableStateOf(Page.Home) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(false) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var farmStats by remember { mutableStateOf(false) }
    var planType by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf("日") }
    var roomQuery by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("全部") }
    var selectedTag by remember { mutableStateOf("全部") }
    var task by remember { mutableStateOf("阅读《原子习惯》第三章") }
    var tasks by remember { mutableStateOf(listOf("阅读《原子习惯》第三章", "完成产品原型初稿", "整理本周学习笔记")) }
    var checked by remember { mutableStateOf(setOf(2)) }
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var mode by remember { mutableIntStateOf(25) }
    var running by remember { mutableStateOf(false) }
    var sessions by remember { mutableIntStateOf(6) }
    var newTask by remember { mutableStateOf("") }
    var joined by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (running && seconds == 0) { running = false; sessions++ }
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("外观与主题") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("主题色", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Palette.entries.forEach { palette ->
                            FilterChip(selected = themePrefs.palette == palette,
                                onClick = { onThemeChange(themePrefs.copy(palette = palette)) },
                                label = { Text(palette.label) }, leadingIcon = { Box(Modifier.size(12.dp).clip(CircleShape).background(palette.primary)) })
                        }
                    }
                    Text("页面背景", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Backdrop.entries.forEach { backdrop ->
                            FilterChip(selected = themePrefs.backdrop == backdrop,
                                onClick = { onThemeChange(themePrefs.copy(backdrop = backdrop)) },
                                label = { Text(backdrop.label) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("深色模式", fontWeight = FontWeight.SemiBold)
                            Text("降低暗光环境下的屏幕亮度感", fontSize = 11.sp, color = Muted)
                        }
                        Switch(checked = dark, onCheckedChange = { onThemeChange(themePrefs.copy(dark = it)) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("完成") } }
        )
    }
    if (showLogin) {
        AlertDialog(
            onDismissRequest = { showLogin = false },
            title = { Text("登录番茄自习室") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("登录后可在多台设备间同步计划与专注记录。", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(value = loginEmail, onValueChange = { loginEmail = it }, label = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = loginPassword, onValueChange = { loginPassword = it }, label = { Text("密码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("登录功能目前为界面原型，尚未连接认证服务。", color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = { Button(onClick = { if(loginEmail.isNotBlank()) { loggedIn = true; showLogin = false; page = Page.Profile } }) { Text("登录") } },
            dismissButton = { TextButton(onClick = { showLogin = false }) { Text("取消") } }
        )
    }
    val pageBackground by animateColorAsState(
        if (dark) Color(0xFF111411) else themePrefs.backdrop.color,
        label = "page-background"
    )

    Scaffold(
        containerColor = pageBackground,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Page.entries.filter { it != Page.Profile && it != Page.CheckIn }.forEach { item ->
                    val icon = when (item) {
                        Page.Home -> Icons.Default.Home
                        Page.Focus -> Icons.Default.Timer
                        Page.Room -> Icons.Default.Groups
                        Page.Plan -> Icons.Default.Checklist
                        Page.CheckIn -> Icons.Default.EventAvailable
                        Page.Stats -> Icons.Default.BarChart
                        Page.Profile -> Icons.Default.Person
                    }
                    NavigationBarItem(selected = page == item, onClick = { page = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) })
                }
            }
        }
    ) { padding ->
      AnimatedContent(targetState = page, transitionSpec = {
          (slideInHorizontally { it / 8 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 8 } + fadeOut())
      }, label = "screen-transition") { screen ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                Text(when(screen) {
                    Page.Home -> "下午好，专注练习生 ☀️"
                    Page.Focus -> "进入一段专注"
                    Page.Room -> "找个自习室，一起专注"
                    Page.Plan -> "把今天的计划排好"
                    Page.CheckIn -> "每天进步一点点"
                    Page.Stats -> "你的专注回顾"
                    Page.Profile -> "个人主页"
                }, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(when(screen) {
                    Page.Home -> "今天也为自己留一点专注时间。"
                    Page.Focus -> "选择专注或休息，开始当前这一轮。"
                    Page.Room -> "和同伴并肩学习，保持专注节奏。"
                    Page.Plan -> "拆分目标，逐项完成。"
                    Page.CheckIn -> "连续打卡 7 天，保持这个节奏。"
                    Page.Stats -> "每一段专注都值得记录。"
                    Page.Profile -> "管理账户、主题和个人偏好。"
                }, color = Muted, fontSize = 13.sp)
                }
                Row {
                    IconButton(onClick = { page = Page.Home }) { Icon(Icons.Default.Home, contentDescription = "主页") }
                    IconButton(onClick = { page = Page.Profile }) { Icon(Icons.Default.AccountCircle, contentDescription = "个人主页") }
                    IconButton(onClick = { showThemePicker = true }) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                }
                }
            }
            when (screen) {
                Page.Home -> {
                    item { StatCard(sessions) }
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                Text("准备好专注一会儿了吗？", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp)); Text("从当前计划开始，完成一小步就很好。", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(14.dp)); Button(onClick = { page = Page.Focus }, shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("开始专注")
                                }
                            }
                        }
                    }
                    item { SectionTitle("快速进入", "", {}) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeShortcut("自习室", "找同伴共学", Icons.Default.Groups, Modifier.weight(1f)) { page = Page.Room }
                            HomeShortcut("今日计划", "继续待办", Icons.Default.Checklist, Modifier.weight(1f)) { page = Page.Plan }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().clickable { page = Page.CheckIn }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                                    Text("连续打卡 7 天", fontWeight = FontWeight.SemiBold); Text("今天完成一轮专注，延续习惯", color = Muted, fontSize = 12.sp)
                                }; Icon(Icons.Default.ChevronRight, null, tint = Muted)
                            }
                        }
                    }
                }
                Page.Focus -> {
                    item {
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("专注计时", color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(25, 5).forEachIndexed { i, m ->
                                        FilterChip(selected = mode == m, onClick = { mode = m; seconds = m * 60; running = false }, label = { Text(listOf("专注", "休息")[i]) })
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                                    val progress by animateFloatAsState(1f - seconds.toFloat() / (mode * 60), label = "timer")
                                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 9.dp, trackColor = MaterialTheme.colorScheme.secondaryContainer, color = MaterialTheme.colorScheme.primary, strokeCap = StrokeCap.Round)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 48.sp, fontWeight = FontWeight.Medium)
                                        Text(if(running) "专注进行中" else "准备开始", color = Muted, fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("当前任务 · $task", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { running = !running }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(.75f)) { Text(if(running) "暂停专注" else "开始专注") }
                            }
                        }
                    }
                }
                Page.Room -> {
                    item { OutlinedTextField(value = roomQuery, onValueChange = { roomQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索自习室、地区或标签") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("全部", "北京", "上海", "杭州").forEach { FilterChip(selected = region == it, onClick = { region = it }, label = { Text(it) }) } } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("全部", "安静", "考研", "语言").forEach { FilterChip(selected = selectedTag == it, onClick = { selectedTag = it }, label = { Text("# $it") }) } } }
                    val availableRooms = listOf(
                        Triple("📚 安静图书馆", "北京 · 12 / 20 人 · 专注中", listOf("安静", "考研")),
                        Triple("🌱 早起学习团", "上海 · 6 / 10 人 · 可加入", listOf("语言", "早起")),
                        Triple("🪴 静音自习室", "杭州 · 3 / 8 人 · 安静陪伴", listOf("安静", "自习"))
                    ).filter { (name, detail, tags) ->
                        (roomQuery.isBlank() || name.contains(roomQuery, true) || detail.contains(roomQuery, true) || tags.any { it.contains(roomQuery, true) }) &&
                                (region == "全部" || detail.contains(region)) && (selectedTag == "全部" || tags.contains(selectedTag))
                    }
                    items(availableRooms) { room ->
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            RoomCard(room.first, room.second, { joined = true })
                            Text("标签：${room.third.joinToString(" · ") { "#${it}" }}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                    item {
                        OutlinedCard(shape = RoundedCornerShape(18.dp), onClick = { joined = true }) {
                            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text("创建自习室", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if(joined) item { Text("已加入房间 · 房间共同计时已同步（演示）", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                }
                Page.Plan -> {
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(false to "普通计划", true to "目标 / OKR").forEach { (okr, title) -> FilterChip(selected = planType == okr, onClick = { planType = okr }, label = { Text(title) }) } } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("日", "周", "月").forEach { FilterChip(selected = period == it, onClick = { period = it }, label = { Text("${it}视图") }) } } }
                    if (planType) item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("目标：完成 Android 初版", fontWeight = FontWeight.SemiBold)
                                Text("KR1 · 完成核心界面　 2 / 3", color = Muted, fontSize = 13.sp)
                                LinearProgressIndicator(progress = { .66f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                                Text("KR2 · 每周专注 15 小时　 8 / 15 h", color = Muted, fontSize = 13.sp)
                                LinearProgressIndicator(progress = { .53f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("${period}计划${if (planType) " · 关键行动" else ""}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(Modifier.height(12.dp))
                                tasks.forEachIndexed { index, title ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = checked.contains(index), onCheckedChange = { yes -> checked = if(yes) checked + index else checked - index })
                                        Text(title, modifier = Modifier.weight(1f), color = if(checked.contains(index)) Muted else Ink)
                                        Text("🍅 ${index + 1}", color = Muted, fontSize = 12.sp)
                                    }
                                }
                                OutlinedTextField(value = newTask, onValueChange = { newTask = it }, label = { Text("添加计划") }, trailingIcon = { IconButton(onClick = { if(newTask.isNotBlank()) { tasks = tasks + newTask; newTask = "" } }) { Icon(Icons.Default.Add, null) } }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Page.CheckIn -> {
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥", fontSize = 44.sp); Text("连续打卡 7 天", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("今天完成一个番茄钟即可打卡", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(16.dp)); Button(onClick = { checked = checked + 99 }, shape = RoundedCornerShape(14.dp)) { Text(if(checked.contains(99)) "今日已打卡 ✓" else "完成今日打卡") }
                                Spacer(Modifier.height(20.dp)); Text("本月打卡", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                                Spacer(Modifier.height(10.dp)); Text("一  二  三  四  五  六  日\n🟩  🟩  🟩  🟩  🟩  ·  🟩\n🟩  🟩  🟩  🟩  🟩  ·  ·", lineHeight = 34.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Page.Stats -> {
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = !farmStats, onClick = { farmStats = false }, label = { Text("简约统计") }); FilterChip(selected = farmStats, onClick = { farmStats = true }, label = { Text("专注农场") }) } }
                    item { StatCard(sessions) }
                    if(farmStats) item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5E9))) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌱", fontSize = 50.sp); Text("专注农场", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("完成专注可获得种子和饲料", color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.height(12.dp)); Text("🥕 🥬 🌻     🐰 🐈 🐥", fontSize = 27.sp)
                                Text("胡萝卜 2 · 小兔 1 · 小猫 1", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(20.dp)) {
                                Text("本周专注", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(18.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    listOf(30, 56, 42, 75, 52, 22, 14).forEachIndexed { i, value ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Box(Modifier.width(18.dp).height((value * 1.5f).dp).clip(RoundedCornerShape(8.dp)).background(if(i==3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer))
                                            Text(listOf("一","二","三","四","五","六","日")[i], color = Muted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Page.Profile -> {
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(58.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if(loggedIn) loginEmail else "访客用户", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(if(loggedIn) "已登录 · 数据仅保存在本机" else "登录后可同步你的专注数据", color = Muted, fontSize = 12.sp)
                                }
                                if(!loggedIn) Button(onClick = { showLogin = true }) { Text("登录") }
                            }
                        }
                    }
                    item { SectionTitle("我的专注", "查看统计", { page = Page.Stats }) }
                    item { StatCard(sessions) }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column {
                                ListItem(headlineContent = { Text("外观与主题") }, supportingContent = { Text("主题色、页面背景和深色模式") }, leadingContent = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { showThemePicker = true })
                                HorizontalDivider()
                                ListItem(headlineContent = { Text("设置") }, supportingContent = { Text("计时偏好、提醒与账户") }, leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { showThemePicker = true })
                            }
                        }
                    }
                    if(loggedIn) item { OutlinedButton(onClick = { loggedIn = false; page = Page.Profile }, modifier = Modifier.fillMaxWidth()) { Text("退出登录") } }
                }
            }
        }
      }
    }
}

@Composable private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (action.isNotBlank()) TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable private fun HomeShortcut(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp)); Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable private fun RoomCard(title: String, subtitle: String, onJoin: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp)); Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            FilledTonalButton(onClick = onJoin, shape = RoundedCornerShape(12.dp)) { Text("加入") }
        }
    }
}

@Composable private fun StatCard(sessions: Int) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("专注时长" to "2.5 小时", "完成番茄" to "$sessions 个", "连续打卡" to "7 天").forEach { (label,value) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(SurfaceSoft).padding(12.dp)) {
                    Text(label, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(6.dp)); Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
