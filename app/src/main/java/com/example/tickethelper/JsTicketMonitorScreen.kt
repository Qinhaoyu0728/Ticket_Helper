package com.example.tickethelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.tickethelper.data.JsTicketMonitorDataStore
import com.example.tickethelper.service.JsTicketMonitorService
import com.example.tickethelper.util.AppIdTestUtil
import com.example.tickethelper.util.LogCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun JsTicketMonitorScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStore = JsTicketMonitorDataStore(context)
    var appId by remember { mutableStateOf("") }
    var maskedAppId by remember { mutableStateOf("") }
    var showAppIdDialog by remember { mutableStateOf(false) }
    var messageList by remember { mutableStateOf(emptyList<JsTicketMonitorDataStore.TicketMessage>()) }

    var serviceEnabled by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }

    val appIdValid by dataStore.appIdValid.collectAsState(initial = false)
    val appIdTested by dataStore.appIdTested.collectAsState(initial = false)

    // 加载AppId
    LaunchedEffect(Unit) {
        dataStore.appId.collectLatest { id ->
            appId = id ?: ""
            maskedAppId = dataStore.maskAppId(id)
        }

        serviceEnabled = isServiceRunning(context, JsTicketMonitorService::class.java)

        // 定时轮询检查服务状态（1秒一次）
        while (true) {
            delay(1000)
            val currentState = isServiceRunning(context, JsTicketMonitorService::class.java)
            if (currentState != serviceEnabled) {
                serviceEnabled = currentState // 同步状态
            }
        }
    }

    // 监听通知点击/服务关闭广播
    val serviceStatusReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.tickethelper.JST_SERVICE_STOPPED") {
                    Log.d("JST_SERVICE_STOPPED", "已收到退出广播")
                    //serviceEnabled = false // 强制同步状态为关闭
                    CoroutineScope(Dispatchers.Main).launch {
                        serviceEnabled = false
                    }
                }
            }
        }
    }

    // 注册广播接收器
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter("com.example.tickethelper.JST_SERVICE_STOPPED")
        ContextCompat.registerReceiver(
            context,
            serviceStatusReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d("ServiceBroadcast", "广播接收器已注册")

        onDispose {
            context.unregisterReceiver(serviceStatusReceiver)
            Log.d("ServiceBroadcast", "广播接收器已注销")
        }
    }

    // 加载消息盒子
    LaunchedEffect(Unit) {
        dataStore.messageBox.collectLatest { list ->
            messageList = list
        }
    }

    // 服务开关监听
    LaunchedEffect(serviceEnabled) {
        if (serviceEnabled) {
            // 启动服务
            if (appId.isEmpty()) {
                serviceEnabled = false
                Toast.makeText(context, "请先配置AppId再启动服务", Toast.LENGTH_SHORT).show()
            } else {
                startMonitorService(context)
                // 延迟检查服务状态
                delay(1000)
                val isRunning = isServiceRunning(context, JsTicketMonitorService::class.java)
                if (isRunning) {
                    serviceEnabled = true
                    Toast.makeText(context, "服务已启动，后台监控中", Toast.LENGTH_SHORT).show()
                } else {
                    serviceEnabled = false
                    Toast.makeText(context, "服务启动失败，请检查权限", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // 停止服务
            stopMonitorService(context)
            // 延迟检查停止状态
            delay(1000)
            val isRunning = isServiceRunning(context, JsTicketMonitorService::class.java)
            if (!isRunning) {
                serviceEnabled = false
                Toast.makeText(context, "服务已停止", Toast.LENGTH_SHORT).show()
            } else {
                // 强制停止
                //forceStopService(context)
                delay(500)
                val finalRunning = isServiceRunning(context, JsTicketMonitorService::class.java)
                serviceEnabled = finalRunning
                if (finalRunning) {
                    Toast.makeText(context, "服务停止失败，请手动关闭应用", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "服务已强制停止", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 测试AppId有效性
    fun testAppId() {
        if (appId.isBlank() || isTesting) return
        isTesting = true
        CoroutineScope(Dispatchers.Main).launch {
            // 获取第一个关注项作为测试用的showId/sign
            val focusItems = dataStore.focusList.first()
            val testShowId = focusItems.firstOrNull()?.showId ?: "test_show_id"
            val testSign = focusItems.firstOrNull()?.sign ?: "test_sign"

            // 执行测试
            val isValid = AppIdTestUtil.testAppIdValidity(appId, testShowId, testSign)
            // 保存测试结果
            dataStore.saveAppIdValidStatus(isValid)
            isTesting = false
        }
    }

    val (showHelpDialog, setShowHelpDialog) = remember { mutableStateOf(false) }
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { setShowHelpDialog(false) },
            title = { Text("JST余票监控服务说明") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("JiuShi余票监控服务（简称JST），使用JiuShi官方接口，因此不建议长时间使用此服务。\n" +
                            "使用前需填写AppId并测试，测试有效后才可启用服务。\n" +
                            "如果收到服务出错的通知，请稍事休息，过一会再开启服务，或尝试切换网络/移动数据重试。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { setShowHelpDialog(false) }
                ) {
                    Text("了解")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JS余票监控") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { setShowHelpDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "日志说明"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 管理关注列表按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { navController.navigate("js_focus_list") },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "管理关注列表→", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 管理AppId按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "管理AppId", style = MaterialTheme.typography.titleMedium)
                        // 服务开启时禁用修改按钮
                        Button(
                            onClick = { showAppIdDialog = true },
                            enabled = !serviceEnabled // 服务关闭时才可用
                        ) {
                            Text("修改")
                        }
                        // 测试
                        if (appId.isNotBlank()) {
                            Button(
                                onClick = ::testAppId,
                                enabled = !isTesting
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("测试中")
                                } else {
                                    Text("测试")
                                }
                            }
                        }
                    }

                    Text(
                        text = "当前AppId：" + maskedAppId.ifEmpty { "未配置AppId" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )


                    // 显示测试结果
//                    if (appIdTested) {
//                        Text(
//                            text = if (appIdValid) "AppId有效 ✅" else "AppId无效 ⚠️",
//                            color = if (appIdValid) Color.Green else Color.Red,
//                            modifier = Modifier.padding(top = 4.dp)
//                        )
//                    }
                }
            }

            // 服务启停
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "开启余票监控服务（JST）", style = MaterialTheme.typography.titleMedium)
                        if (appIdTested) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appIdValid) "✅" else "⚠️",
                                fontSize = 16.sp
                            )
                        }
                    }
                    Switch(
                        checked = serviceEnabled,
                        //onCheckedChange = { serviceEnabled = it },
                        onCheckedChange = { isChecked ->
                            serviceEnabled = isChecked
                            if (isChecked) {
                                ContextCompat.startForegroundService(
                                    context,
                                    Intent(context, JsTicketMonitorService::class.java)
                                )
                            } else {
                                context.stopService(Intent(context, JsTicketMonitorService::class.java))
                            }
                        },
                        enabled = appId.isNotBlank() && (appIdTested && appIdValid) // AppId为空或无效时禁用开关
                    )
                }
            }

            // 消息盒子
            Text(
                text = "消息盒子",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.Start)
            )

            // 内容
            if (messageList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无余票消息", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val sortedMessages = messageList.sortedByDescending { it.time }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sortedMessages) { message ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "赛事名称: ${message.name}", style = MaterialTheme.typography.titleMedium)
                                    Text(text = "剩余票数: ${message.stock}", style = MaterialTheme.typography.bodyLarge)
                                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                                        .format(message.time)
                                    Text(text = "发现时间: $timeStr", style = MaterialTheme.typography.bodySmall)
                                }

                                // 右 重复次数标签（count>1时显示）
                                if (message.count > 1) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${message.count}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AppId对话框
            if (showAppIdDialog) {
                var inputAppId by remember { mutableStateOf(appId) }
                AlertDialog(
                    onDismissRequest = { showAppIdDialog = false },
                    title = { Text("管理AppId") },
                    text = {
                        OutlinedTextField(
                            value = inputAppId,
                            onValueChange = { inputAppId = it },
                            label = { Text("请输入AppId") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // 保存AppId
                                kotlinx.coroutines.GlobalScope.launch {
                                    dataStore.saveAppId(inputAppId)
                                }
                                showAppIdDialog = false
                            }
                        ) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAppIdDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

// 检查服务 is 运行 ?
fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    // 遍历所有服务
    for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

// 启动监控服务
fun startMonitorService(context: Context) {
    val intent = Intent(context, JsTicketMonitorService::class.java)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 停止监控服务
fun stopMonitorService(context: Context) {
    val intent = Intent(context, JsTicketMonitorService::class.java)
    try {
        // 发送停止指令
        intent.action = JsTicketMonitorService.ACTION_STOP_SERVICE
        context.startService(intent)
        // stopService
        context.stopService(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}