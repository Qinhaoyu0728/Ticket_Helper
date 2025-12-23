package com.example.tickethelper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.tickethelper.util.LogCollector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    navController: NavController
) {
    // 收集实时日志流
    val logList = LogCollector.logList.collectAsStateWithLifecycle(initialValue = emptyList())
    val (showHelpDialog, setShowHelpDialog) = remember { mutableStateOf(false) }

    // 日志说明对话框
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { setShowHelpDialog(false) },
            title = { Text("日志说明") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("日志用于自动余票监控的调试\n如果程序发生了报错，请立即将日志截图并反馈！")
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
                title = { Text("Log Viewer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { setShowHelpDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "日志说明"
                        )
                    }
                    // 清空日志按钮
                    IconButton(onClick = { LogCollector.clearLogs() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空日志"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black) // 黑色背景更易读日志
        ) {
            if (logList.value.isEmpty()) {
                // 空日志提示
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = "暂无日志数据\n开启自动刷新后将显示监控日志",
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // 日志列表（倒序显示，最新的在底部）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    reverseLayout = true // 倒序
                ) {
                    items(logList.value.reversed()) { logItem ->
                        LogItemRow(logItem = logItem)
                    }
                }
            }

//            // 清空日志按钮（底部快捷操作）
//            Button(
//                onClick = { LogCollector.clearLogs() },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .align(androidx.compose.ui.Alignment.BottomCenter)
//            ) {
//                Text("清空所有日志")
//            }
        }
    }
}

/**
 * 单条日志行
 */
@Composable
fun LogItemRow(logItem: LogCollector.LogItem) {
    // 根据日志级别设置文字颜色
    val textColor = when (logItem.level) {
        "D" -> Color.Cyan // Debug -> 青色
        "I" -> Color.Green // Info -> 绿色
        "W" -> Color.Yellow // Warn -> 黄色
        "E" -> Color.Red // Error -> 红色
        "V" -> Color.Gray // Verbose -> 灰色
        else -> Color.White
    }

    Text(
        text = "[${logItem.time}] [${logItem.level}] ${logItem.message}",
        color = textColor,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}