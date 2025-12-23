package com.example.tickethelper

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tickethelper.data.TicketTarget
import com.example.tickethelper.data.TicketTargetDataStore
import com.example.tickethelper.util.TicketService
import com.example.tickethelper.navigation.NavigationRoutes
import com.example.tickethelper.service.TicketRefreshService
import com.example.tickethelper.util.NotificationHelper
import com.example.tickethelper.util.TargetTicketStatus
import com.example.tickethelper.util.TicketSessionDetail
import com.example.tickethelper.util.sessionTypeMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketAssistantScreen(navController: NavController) {
    val context = LocalContext.current
    val ticketTargetDataStore = TicketTargetDataStore.getInstance(context)
    val ticketService = remember { TicketService.create() }
    val coroutineScope = rememberCoroutineScope()

    var ticketTargets by remember { mutableStateOf(emptyList<TicketTarget>()) }
    var targetStatusMap by remember { mutableStateOf<Map<String, TargetTicketStatus>>(emptyMap()) }
    var lastStatusMap by remember { mutableStateOf<Map<String, TargetTicketStatus>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var autoRefreshJob by remember { mutableStateOf<Job?>(null) }

    var selectedListStyle by remember { mutableStateOf("two_column") }
    var showOverallStatus by remember { mutableStateOf(true) }


    // 加载保存的目标
    LaunchedEffect(Unit) {
        ticketTargetDataStore.getTicketTargets.collect { targets ->
            ticketTargets = targets
        }
    }

    // 刷新余票状态
    fun refreshTicketStatus() {
        if (isRefreshing || ticketTargets.isEmpty()) return

        isRefreshing = true
        coroutineScope.launch(Dispatchers.IO) {
            val newStatusMap = mutableMapOf<String, TargetTicketStatus>()

            ticketTargets.forEach { target ->
                try {
                    val response = ticketService.getTicketStatus(target.targetId)
                    if (response.statusCode == 200) {
                        val sessions = response.data.sessionVOs
                        val sessionDetails = mutableListOf<TicketSessionDetail>()

                        // 解析每个票种状态
                        sessions.forEachIndexed { index, session ->
                            // 票种名称：按数量匹配（1个=三日票，多个按顺序）
                            val sessionType = if (sessions.size == 1) {
                                "三日票"
                            } else {
                                sessionTypeMapping.getOrElse(index) { "未知票种${index+1}" }
                            }

                            sessionDetails.add(
                                TicketSessionDetail(
                                    sessionId = session.bizShowSessionId,
                                    sessionType = sessionType,
                                    sessionStatus = session.sessionStatus
                                )
                            )
                        }

                        // 整体状态判断（只要有一个票种有票就标记为有票）
                        val overallStatus = if (sessionDetails.any { it.sessionStatus == "ON_SALE" }) {
                            "ON_SALE"
                        } else {
                            "LACK_OF_TICKET"
                        }

                        newStatusMap[target.id] = TargetTicketStatus(
                            overallStatus = overallStatus,
                            sessionDetails = sessionDetails
                        )
                    } else {
                        // 接口返回异常
                        newStatusMap[target.id] = TargetTicketStatus(
                            overallStatus = "ERROR",
                            sessionDetails = emptyList()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    newStatusMap[target.id] = TargetTicketStatus(
                        overallStatus = "ERROR",
                        sessionDetails = emptyList()
                    )
                }
            }

            // 检测状态变化（从缺票变为有票）
            newStatusMap.forEach { (targetId, newStatus) ->
                val oldStatus = lastStatusMap[targetId]
                val target = ticketTargets.find { it.id == targetId }
                // 只有整体状态从缺票变为有票时触发通知
                if (oldStatus?.overallStatus == "LACK_OF_TICKET" &&
                    newStatus.overallStatus == "ON_SALE" && target != null) {
                    NotificationHelper.showTicketAvailableNotification(
                        context = context,
                        targetName = target.category
                    )
                }
            }

            // 更新状态缓存
            lastStatusMap = newStatusMap.toMap()

            withContext(Dispatchers.Main) {
                targetStatusMap = newStatusMap
                isRefreshing = false
            }
        }
    }

//    // 启动自动刷新（每10秒一次）
//    fun startAutoRefresh() {
//        if (autoRefreshJob?.isActive == true) return
//
//        autoRefreshJob = coroutineScope.launch(Dispatchers.IO) {
//            while (true) {
//                delay(10000) // 10秒刷新一次
//                withContext(Dispatchers.Main) {
//                    refreshTicketStatus() // 调用已有刷新方法
//                }
//            }
//        }
//    }

    fun startAutoRefresh() {
        val intent = Intent(context, TicketRefreshService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopAutoRefresh() {
        val intent = Intent(context, TicketRefreshService::class.java)
        context.stopService(intent)
    }

//    // 停止自动刷新
//    fun stopAutoRefresh() {
//        autoRefreshJob?.cancel()
//    }

    // 监听自动刷新配置
    LaunchedEffect(Unit) {
        ticketTargetDataStore.getAutoRefreshConfig.collect { config ->
            // 启动或停止自动刷新
            if (config.enabled) {
                startAutoRefresh()
            } else {
                stopAutoRefresh()
            }

            selectedListStyle = config.listStyle // 同步样式配置
            showOverallStatus = config.showOverallStatus
        }
    }

    // 初始加载状态
    LaunchedEffect(Unit) {
        ticketTargetDataStore.getTicketTargets.collect { targets ->
            ticketTargets = targets
            if (targets.isNotEmpty()) {
                refreshTicketStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("抢票助手") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::refreshTicketStatus,
                        modifier = Modifier.size(24.dp)
                    ){
                        if (isRefreshing) {
                            // 刷新中显示进度指示器
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新数据"
                            )
                        }
                    }

                    IconButton(onClick = { navController.navigate(NavigationRoutes.TICKET_ADD) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加目标")
                    }

                    IconButton(onClick = {
                        navController.navigate(NavigationRoutes.AUTO_REFRESH_SETTINGS)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (ticketTargets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无抢票目标，点击右上角添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ticketTargets) { target ->
                        val status = targetStatusMap[target.id] ?: "LOADING"

                        val targetStatus = targetStatusMap[target.id] ?: TargetTicketStatus(
                            overallStatus = "LOADING",
                            sessionDetails = emptyList()
                        )

                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()

                        // 长按状态管理
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        var longPressStarted by remember { mutableStateOf(false) }

                        val longPressTimer = remember { mutableStateOf<Job?>(null) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            longPressStarted = true
                                            longPressTimer.value = coroutineScope.launch {
                                                delay(500) // 3秒后显示删除对话框
                                                withContext(Dispatchers.Main) {
                                                    showDeleteDialog = true
                                                }
                                            }
                                        },
                                        // 短按或长按取消时：取消计时器
                                        onPress = {
                                            tryAwaitRelease()
                                            longPressStarted = false
                                            longPressTimer.value?.cancel() // 取消计时器
                                        }
                                    )
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // 顶部：类目和整体状态
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = target.category,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    if (showOverallStatus) {
                                        // 整体状态标签
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                .background(
                                                    color = when (targetStatus.overallStatus) {
                                                        "ON_SALE" -> Color(0xFF4CAF50)
                                                        "LACK_OF_TICKET" -> Color(0xFFF44336)
                                                        "ERROR" -> Color(0xFFFF9800)
                                                        else -> Color(0xFF9E9E9E)
                                                    },
                                                    shape = MaterialTheme.shapes.small
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (targetStatus.overallStatus) {
                                                    "ON_SALE" -> "有票"
                                                    "LACK_OF_TICKET" -> "缺票"
                                                    "ERROR" -> "加载失败"
                                                    else -> "加载中"
                                                },
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }

                                // 目标ID
                                Text(
                                    text = "目标ID: ${target.targetId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )

                                // 分隔线
                                Divider(modifier = Modifier.padding(bottom = 8.dp))

                                // 票种详情列表
                                Text(
                                    text = "票种详情",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                // 根据选择的样式切换布局
                                if (selectedListStyle == "two_column") {
                                    // 两列布局（原有逻辑）
                                    val leftColumn = targetStatus.sessionDetails.take(2)
                                    val rightColumn = targetStatus.sessionDetails.drop(2)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            leftColumn.forEach { TicketSessionItem(detail = it) }
                                            repeat(2 - leftColumn.size) { Spacer(modifier = Modifier.height(28.dp)) }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            rightColumn.forEach { TicketSessionItem(detail = it) }
                                            repeat(2 - rightColumn.size) { Spacer(modifier = Modifier.height(28.dp)) }
                                        }
                                    }
                                } else {
                                    // 单列布局
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        targetStatus.sessionDetails.forEach { detail ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = detail.sessionType,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = when (detail.sessionStatus) {
                                                                "ON_SALE" -> Color(0xFF4CAF50)
                                                                "LACK_OF_TICKET" -> Color(0xFFF44336)
                                                                else -> Color(0xFF9E9E9E)
                                                            },
                                                            shape = MaterialTheme.shapes.small
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when {
                                                            detail.sessionStatus == "ON_SALE" -> "有票"
                                                            else -> "无票"
                                                        },
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 长按3秒后显示的删除确认对话框
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showDeleteDialog = false
                                    longPressTimer.value?.cancel() // 关闭对话框时取消计时器
                                },
                                title = { Text("确认删除") },
                                text = { Text("确定要删除「${target.category}」这个抢票目标吗？") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            // 执行删除操作
                                            coroutineScope.launch {
                                                // 删除数据存储中的条目
                                                ticketTargetDataStore.deleteTicketTarget(target.id)
                                                // 删除状态映射中的缓存
                                                targetStatusMap  = targetStatusMap.filterKeys { it != target.id }
                                                // 关闭对话框
                                                showDeleteDialog = false
                                                // 提示删除成功
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "已删除「${target.category}」",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Text("删除")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false
                                            longPressTimer.value?.cancel()
                                        }
                                    ) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 刷新按钮
            if (ticketTargets.isNotEmpty()) {
                IconButton(
                    onClick = ::refreshTicketStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isRefreshing
                ){
                    if (isRefreshing) {
                        // 刷新中显示进度指示器
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新数据"
                        )
                    }
                }
            }
        }
    }
}


// 独立的票种项组件（复用）
@Composable
private fun TicketSessionItem(detail: TicketSessionDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 票种名称
        Text(
            text = detail.sessionType,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 票种状态标签（缩小尺寸）
        Box(
            modifier = Modifier
                .background(
                    color = when (detail.sessionStatus) {
                        "ON_SALE" -> Color(0xFF4CAF50)
                        "LACK_OF_TICKET" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    },
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    detail.sessionStatus == "ON_SALE" -> "有票"
                    detail.sessionStatus == "LACK_OF_TICKET" -> "缺票"
                    else -> "未知"
                },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                fontSize = 10.sp // 缩小字体节省空间
            )
        }
    }
}
