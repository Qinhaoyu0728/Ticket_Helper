package com.example.tickethelper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.model.ShowConfigRepository
import com.example.tickethelper.util.VipTicketData
import com.example.tickethelper.util.VipTicketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTicketScreen(navController: NavController) {
    val context = LocalContext.current
    val vipTicketService = remember { VipTicketService.create() }
    val coroutineScope = rememberCoroutineScope()

    // 多配置选择相关
    val configs = ShowConfigRepository.defaultConfigs
    var selectedConfig by remember { mutableStateOf(configs.first()) }
    var expanded by remember { mutableStateOf(false) }

    // 数据状态管理
    var vipTicketData by remember { mutableStateOf<VipTicketData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 加载数据函数
    fun loadVipTicketData() {
        isLoading = true
        errorMessage = ""
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = vipTicketService.getVipTicketStatus(
                    showId = selectedConfig.showId,
                    sign = selectedConfig.sign
                )

                withContext(Dispatchers.Main) {
                    if (response.rtnCode == "10000" && response.data != null) {
                        vipTicketData = response.data
                    } else {
                        errorMessage = "获取数据失败: ${response.rtnMessage}"
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "网络错误: ${e.message ?: "未知错误"}"
                    isLoading = false
                }
            }
        }
    }

    // 选中项变化时加载数据
    LaunchedEffect(selectedConfig) {
        loadVipTicketData()
    }

    val (showHelpDialog, setShowHelpDialog) = remember { mutableStateOf(false) }

    // 说明对话框
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { setShowHelpDialog(false) },
            title = { Text("国内接口查询说明") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("国内接口是指使用久事体育官方接口查询，因此可以查询到铂金体验之旅、加油中国周、A下PLUS、迪士尼联名套票、B看台MXGP套票、E看台车迷应援区的余票\n" +
                            "但由于被ban IP的可能性比第三方接口的可能性大，为了确保您的IP安全且可用，因此无法启用自动查询功能。")
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
                title = { Text("国内接口查询") },
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
                    IconButton(onClick = { loadVipTicketData() }) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
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
            // 配置选择器（下拉菜单）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前选择: ${selectedConfig.name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "展开选择"
                        )
                    }
                }

                // 下拉菜单
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    configs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config.name) },
                            onClick = {
                                selectedConfig = config
                                expanded = false
                            },
                            modifier = if (selectedConfig == config) {
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }

            // 内容展示
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    errorMessage.isNotEmpty() -> {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    vipTicketData != null -> {
                        // 外层LazyColumn
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            // 遍历票种列表
                            items(vipTicketData!!.showSessionModelList) { session ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        // 票种名称
                                        Text(
                                            text = session.sessionName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        // 时间信息
                                        Text(
                                            text = session.beginTime,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // 票种详情列表
                                        Column {
                                            session.priceInfoModelList.forEachIndexed { index, priceInfo ->
                                                // 每个票种详情项
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 12.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                            shape = MaterialTheme.shapes.small
                                                        )
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = priceInfo.priceName,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "价格: ¥${priceInfo.price / 100}",  // 分转元
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )

                                                        Text(
                                                            text = "余票: ${if (priceInfo.stock > 0) priceInfo.stock else "无"}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (priceInfo.stock > 0) Color.Green else Color.Red
                                                        )
                                                    }

                                                    Text(
                                                        text = "状态: ${if (priceInfo.sessionStatus == "ONSALE") "已开票" else "不可购买"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "暂无数据",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}