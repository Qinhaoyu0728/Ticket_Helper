package com.example.tickethelper

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var appSize by remember { mutableStateOf("计算中...") }

    // 计算应用占用空间
    LaunchedEffect(Unit) {
        appSize = withContext(Dispatchers.IO) {
            calculateAppSize(context).ifEmpty { "获取失败" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于抢票助手特供版") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(bottom = 52.dp) // 底部预留空间
        ) {
            // 版本号卡片
            item {
                InfoCard(
                    title = "版本信息",
                    content = {
                        Text(
                            text = "当前版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})\n"+
                            "Coded by QinMike",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            // 简介卡片
            item {
                InfoCard(
                    title = "应用简介",
                    content = {
                        Text(
                            text = "主要功能包括：\n" +
                                    "- 久事体育F1余票查询\n" +
                                    "- 自定义门票查询\n" +
                                    "- 后台余票自动提醒（测试中）",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                )
            }

            item {
                InfoCard(
                    title = "联系我 & Feedback",
                    content = {
                        Text(
                            text = "邮箱：1436974384@qq.com\n" +
                                    "小红书：Qin Mike\n" +
                                    "抖音：QinMike",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            item {
                InfoCard(
                    title = "存储占用",
                    content = {
                        Text(
                            text = "已占用的储存空间: $appSize",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }
}

/**
 * 通用信息卡片组件
 */
@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

/**
 * 计算应用占用的存储空间
 */
private fun calculateAppSize(context: Context): String {
    return try {
        // 获取应用APK大小
        val apkSize = context.packageManager.getPackageInfo(context.packageName, 0).applicationInfo?.sourceDir
            .let { File(it).length() }

        // 获取应用数据目录大小
        val dataSize = context.filesDir.walkTopDown().sumOf { it.length() }

        // 计算总大小并格式化
        val totalSize = apkSize + dataSize
        formatFileSize(totalSize)
    } catch (e: Exception) {
        "获取失败"
    }
}

/**
 * 格式化文件大小显示
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> {
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            String.format("%.2f GB", gb)
        }
        bytes >= 1024 * 1024 -> {
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format("%.2f MB", mb)
        }
        bytes >= 1024 -> {
            val kb = bytes.toDouble() / 1024
            String.format("%.2f KB", kb)
        }
        else -> "$bytes B"
    }
}