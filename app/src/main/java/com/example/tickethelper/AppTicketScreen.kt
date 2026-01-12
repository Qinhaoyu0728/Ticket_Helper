package com.example.tickethelper

// 官方渠道久事体育接口查询 screen  原名VipTicketScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.AppSettingsDataStore
import com.example.tickethelper.model.ShowConfigRepository
import com.example.tickethelper.navigation.NavigationRoutes
import com.example.tickethelper.util.VipTicketData
import com.example.tickethelper.util.VipTicketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTicketScreen(navController: NavController) {
    val context = LocalContext.current
    // val vipTicketService = remember { VipTicketService.create() }
    val coroutineScope = rememberCoroutineScope()

    // 多配置
    val configs = ShowConfigRepository.defaultConfigs
    var selectedConfig by remember { mutableStateOf(configs.first()) }
    var expanded by remember { mutableStateOf(false) }

    // 数据状态
    var vipTicketData by remember { mutableStateOf<VipTicketData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val appIdDataStore = AppSettingsDataStore.getInstance(context)
    var appId by remember { mutableStateOf<String?>(null) }

    var autoExpandTickets by remember {
        mutableStateOf(appIdDataStore.getAutoExpandConfigSync())
    }

    LaunchedEffect(Unit) {
        // app_id
        appIdDataStore.getAppId.collect { id ->
            appId = id
        }
        // 自动展开
        appIdDataStore.getAutoExpandConfig.collect { enabled ->
            autoExpandTickets = enabled
        }
    }

    val vipTicketService = remember(appId) {
        VipTicketService.create(appId)
    }

    // 加载数据
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

    LaunchedEffect(selectedConfig) {
        loadVipTicketData()
    }

    val (showHelpDialog, setShowHelpDialog) = remember { mutableStateOf(false) }

    // 说明dial
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { setShowHelpDialog(false) },
            title = { Text("国内接口查询说明") },
            text = {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("国内接口是指使用JiuShi官方接口查询，因此可以查询到铂金体验之旅、加油中国周、A下PLUS、迪士尼联名套票、B看台MXGP套票、E看台车迷应援区的余票\n" +
                            "但由于被ban IP的可能性比第三方接口的可能性大，为了确保您的IP安全且可用，建议不要频繁刷新！")
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
                    IconButton(onClick = {
                        navController.navigate(NavigationRoutes.APP_TICKET_SETTINGS)
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
            // 下拉
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

            // 内容
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
                            text = "$errorMessage\n提示：如果使用了app_id，请重新刷新！",
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
                            // 遍历票种
                            items(vipTicketData!!.showSessionModelList) { session ->
                                // 判断是否有票
                                val hasAnyStock = session.priceInfoModelList.any { it.stock > 0 }

                                var isExpanded by remember {
                                    mutableStateOf(autoExpandTickets && hasAnyStock)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        // 切换展开/收起状态
                                        .clickable { isExpanded = !isExpanded }
                                        .animateContentSize(
                                            animationSpec = tween(
                                                durationMillis = 300, // 动画时长（毫秒）
                                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                                            )
                                        ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        // 收起模式：名称 + 余票
                                        // 名称
                                        Text(
                                            text = session.sessionName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        // 余票
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (autoExpandTickets) {
                                                Text(
                                                    text = if (hasAnyStock) "有票" else "无票",
                                                    color = if (hasAnyStock) Color.Green else Color.Red,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = if (isExpanded) "点击收起" else "点击展开",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded or !autoExpandTickets,
                                            // 展开动画：从下往上滑入 + 淡入
                                            enter = slideInVertically(
                                                initialOffsetY = { it / 4 }, // 初始偏移（自身高度的1/4）
                                                animationSpec = tween(300)
                                            ) + fadeIn(animationSpec = tween(300)),
                                            // 收起动画：从上往下滑出 + 淡出
                                            exit = slideOutVertically(
                                                targetOffsetY = { it / 4 },
                                                animationSpec = tween(300)
                                            ) + fadeOut(animationSpec = tween(300))
                                        ) {

                                        // 展开模式：详情
                                        //if (isExpanded or !autoExpandTickets) {
                                            Column {
                                                // 时间
                                                Text(
                                                    text = session.beginTime,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                // 价格/余票详情列表
                                                Column {
                                                    session.priceInfoModelList.forEachIndexed { index, priceInfo ->
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(bottom = 12.dp)
                                                                .background(
                                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                                        alpha = 0.3f
                                                                    ),
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