package com.example.tickethelper

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.model.ShowConfigRepository
import com.example.tickethelper.util.LogCollector
import com.example.tickethelper.util.VipTicketService
import com.example.tickethelper.util.logE
import com.example.tickethelper.util.logI
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 数据类存储余票通知
data class TicketNotification(
    val time: String,
    val ticketType: String,
    val price: Int,
    val stock: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBoxScreen(
    navController: NavController,
    vipTicketService: VipTicketService
) {
    var isAutoRefreshEnabled by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(listOf<TicketNotification>()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // 自动查询逻辑
    LaunchedEffect(isAutoRefreshEnabled) {
        while (isAutoRefreshEnabled) {
            logI("appTic", "开始")

            try {
                // 获取ShowConfig中存储的票目ID
                val showIds = ShowConfigRepository.getShowIds()

                showIds.forEach { showId ->
                    // 使用现有VipTicketService查询
                    val response = vipTicketService.getVipTicketStatus(
                        showId = showId,
                        sign = ShowConfigRepository.getSignForShow(showId)
                    )

                    if (response.rtnCode == "0000") {
                        response.data?.showSessionModelList?.forEach { session ->
                            session.priceInfoModelList.forEach { priceInfo ->
                                // 检查是否有余票
                                if (priceInfo.stock > 0) {
                                    val newNotification = TicketNotification(
                                        time = dateFormat.format(Date()),
                                        ticketType = "${session.sessionName} - ${priceInfo.priceName}",
                                        price = priceInfo.price,
                                        stock = priceInfo.stock
                                    )

                                    // 避免重复添加相同通知
                                    if (!notifications.any {
                                            it.ticketType == newNotification.ticketType &&
                                                    it.time.substring(0, 16) == newNotification.time.substring(0, 16)
                                        }) {
                                        notifications = listOf(newNotification) + notifications
                                    }
                                }
                            }
                        }
                    } else {
                        logE("appTic","查询失败: ${response.rtnMessage}")
                    }
                }
            } catch (e: Exception) {
                logE("appTic","查询异常: ${e.message}")
            }

            // 每10秒刷新一次
            logI("appTic", "结束")
            delay(10000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息盒子") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            // 自动查询开关
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "开启自动查询",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Switch(
                    checked = isAutoRefreshEnabled,
                    onCheckedChange = { isAutoRefreshEnabled = it }
                )
            }

            // 余票通知列表
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAutoRefreshEnabled) "正在监控余票..." else "开启自动查询以监控余票",
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notification ->
                        TicketNotificationCard(notification)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketNotificationCard(notification: TicketNotification) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = notification.ticketType,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = "价格: ${notification.price}元  余票: ${notification.stock}张",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "监测时间: ${notification.time}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}