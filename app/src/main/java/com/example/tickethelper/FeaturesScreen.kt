package com.example.tickethelper

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tickethelper.data.FeatureOrderDataStore
import com.example.tickethelper.data.HiddenFeaturesDataStore
import com.example.tickethelper.navigation.NavigationRoutes
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

// 数据类
data class Feature(
    val name: String,
    val icon: ImageVector,
    val color: Long
)

// 功能列表
val featuresList = listOf(
    Feature("抢票助手", Icons.Default.EventSeat, 0xFF9C27B0),
    Feature("国内接口查询", Icons.Default.Stars, 0xFF2196F3),
    Feature("JS余票监控", Icons.Default.Monitor, 0xFFF44336),
    Feature("日志", Icons.AutoMirrored.Filled.Assignment, 0xFFA3A8A7),
    Feature("更多", Icons.Default.MoreVert, 0xFF6B7280)
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    navController: NavController,
    featureOrderDataStore: FeatureOrderDataStore
) {

    var sortedFeatures by remember { mutableStateOf(featuresList) }

    val context = LocalContext.current
    val hiddenFeaturesDataStore = HiddenFeaturesDataStore.getInstance(context)
    var hiddenFeatures by remember { mutableStateOf(emptySet<String>()) }
    var visibleFeatures by remember { mutableStateOf(emptyList<Feature>()) }
    var hasHiddenFeatures by remember { mutableStateOf(false) }

    // 倒计时
    val targetDate = remember {
        // 2026年3月13日 00:00:00
        LocalDateTime.of(2026, 3, 13, 0, 0, 0)
    }
    val currentTime = LocalDateTime.now()
    val duration = Duration.between(currentTime, targetDate)

    var countdownText by remember { mutableStateOf("") }
    var isCountdownActive by remember { mutableStateOf(true) }

    // 加载排序
    LaunchedEffect(Unit) {
        featureOrderDataStore.getFeatureOrder.collect { order ->
            if (order.isNotEmpty()) {
                val allFeatures = featuresList.toMutableList()
                val sortedList = order.mapNotNull { orderName ->
                    allFeatures.find { it.name == orderName }?.also { allFeatures.remove(it) }
                }
                val finalList = sortedList + allFeatures

                // 更新UI
                sortedFeatures = finalList.toList()
            }
        }
    }

    // 加载隐藏功能
    LaunchedEffect(Unit) {
        hiddenFeaturesDataStore.getHiddenFeatures.collect { hidden ->
            hiddenFeatures = hidden
            // 过滤掉隐藏的功能 保留"更多"
            visibleFeatures = sortedFeatures.filter {
                !hidden.contains(it.name) || it.name == "更多"
            }
            // 检查
            hasHiddenFeatures = sortedFeatures.any { hidden.contains(it.name) }
        }
    }

    // 每秒更新倒计时
    LaunchedEffect(isCountdownActive) {
        if (!isCountdownActive) return@LaunchedEffect

        while (true) {
            val now = LocalDateTime.now()
            val diff = Duration.between(now, targetDate)

            if (diff.isNegative) {
                countdownText = "2026赛季F1上海站已开始！"
                isCountdownActive = false
                break
            }

            val days = diff.toDays()
            val hours = diff.toHours() % 24
            val minutes = diff.toMinutes() % 60
            val seconds = diff.seconds % 60

            countdownText = "距离2026赛季F1上海站还有：\n" +
                    "${days}天 ${hours}时 ${minutes}分 ${seconds}秒"

            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Helper") },
                actions = {
                    IconButton(onClick = {navController.navigate(NavigationRoutes.SETTINGS) }) {
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
            // 上海站倒计时
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // 功能卡片
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleFeatures) { feature ->
                    FeatureCard(feature = feature,
                        navController = navController)
                }
            }


        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(
    feature: Feature,
    navController: NavController,
) {

    Card(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = {when (feature.name) {

            "抢票助手" -> navController.navigate(NavigationRoutes.TICKET_ASSISTANT)

            "国内接口查询" -> navController.navigate(NavigationRoutes.APP_TICKET)

            "JS余票监控" -> navController.navigate(NavigationRoutes.JS_TICKET_MONITOR)

            "日志" -> navController.navigate(NavigationRoutes.LOG_SCREEN)

            "更多" -> navController.navigate(NavigationRoutes.MORE_FEATURES)

            else -> {}
        }}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = androidx.compose.ui.graphics.Color(feature.color).copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.name,
                    tint = androidx.compose.ui.graphics.Color(feature.color),
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = feature.name,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}