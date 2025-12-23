package com.example.tickethelper.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.TicketTargetDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoRefreshSettingsScreen(
    navController: NavController,
    ticketTargetDataStore: TicketTargetDataStore
) {
    var isAutoRefreshEnabled by remember { mutableStateOf(false) }
    var showOverallStatus by remember { mutableStateOf(true) }
    var selectedListStyle by remember { mutableStateOf("two_column") }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 加载配置
    LaunchedEffect(Unit) {
        ticketTargetDataStore.getAutoRefreshConfig.collect { config ->
            isAutoRefreshEnabled = config.enabled
            selectedListStyle = config.listStyle
            showOverallStatus = config.showOverallStatus
        }
    }

    // 保存配置
    @SuppressLint("CoroutineCreationDuringComposition")
    fun saveConfig() {
        if (isSaving) return
        isSaving = true
        coroutineScope.launch {
            ticketTargetDataStore.saveAutoRefreshConfig(
                enabled = isAutoRefreshEnabled,
                listStyle = selectedListStyle,
                showOverallStatus = showOverallStatus)
            isSaving = false
        }
    }

    // 保存样式配置
    fun saveStyleConfig(string: String) {
        coroutineScope.launch {
            ticketTargetDataStore.saveListStyleConfig(selectedListStyle)
        }
    }

    // 保存整体状态开关配置
    fun saveOverallStatusConfig(show: Boolean) {
        coroutineScope.launch {
            ticketTargetDataStore.saveShowOverallStatusConfig(show)
        }
    }

    val (showHelpDialog, setShowHelpDialog) = remember { mutableStateOf(false) }

    // 说明dial
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { setShowHelpDialog(false) },
            title = { Text("自动查询说明") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("·为了保证自动查询在后台运行正常，请务必在系统设置中打开【抢票通知】和【余票监控服务】两个通知权限\n" +
                            "·开启自动刷新监控后，请等待约5~10秒，待通知栏显示【F1余票监控中 正在监控x个目标】，表示自动监控服务启动成功\n" +
                            "·请保持应用在后台开启，请在系统设置中关闭省电策略，打开后台无限制运行。建议将本应用锁定到后台，防止误清。耗电量较高，建议合理使用！")
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
                title = { Text("抢票助手设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { setShowHelpDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "说明"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "自动刷新设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 自动刷新开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "开启自动刷新监测",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "开启后每10~13秒自动刷新余票状态",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isAutoRefreshEnabled,
                    onCheckedChange = {
                        isAutoRefreshEnabled = it
                        saveConfig()
                    },
                    enabled = !isSaving
                )
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            // 显示设置
            Text(
                text = "显示设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 整体状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "显示整体状态标签", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "开启后在条目顶部显示整体有票/缺票状态",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showOverallStatus,
                    onCheckedChange = {
                        showOverallStatus = it
                        saveOverallStatusConfig(it)
                    },
                    enabled = !isSaving
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 列表样式设置
            Text(
                text = "票种列表样式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 两列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedListStyle = "two_column"
                            saveStyleConfig("two_column")
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedListStyle == "two_column",
                        onClick = {
                            selectedListStyle = "two_column"
                            saveStyleConfig("two_column")
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "两列列表样式", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "票种详情分两列显示（左右各2个）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 单列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedListStyle = "single_column"
                            saveStyleConfig("single_column")
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedListStyle == "single_column",
                        onClick = {
                            selectedListStyle = "single_column"
                            saveStyleConfig("single_column")
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "单列列表样式", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "票种详情单列垂直显示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}