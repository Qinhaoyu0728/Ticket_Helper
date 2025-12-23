package com.example.tickethelper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Update
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

    // 更新地址
    val UPDATE_URLS = mapOf(
        "123云盘" to "https://www.123865.com/s/oTwEjv-Hra7d?pwd=PZZl#",
        "夸克云盘" to "https://pan.quark.cn/s/c7d6151fbd31",
        "Github" to "https://github.com/Qinhaoyu0728/Ticket_Helper"
    )
    val showUpdateDialog = remember { mutableStateOf(false) }

    // 跳转更新
    fun openUrlInBrowser(context: Context, urlKey: String) {
        val url = UPDATE_URLS[urlKey] ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开浏览器，请检查是否安装", Toast.LENGTH_SHORT).show()
        }
    }

    // 计算占用空间
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
            contentPadding = PaddingValues(bottom = 52.dp)
        ) {
            // 版本号
            item {
                InfoCard(
                    title = "版本信息",
                    content = {
                        Text(
                            text = "当前版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})\n"+
                            "Coded by QinMike\n",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 更新
                            Button(
                                onClick = { showUpdateDialog.value = true },
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Icon(
                                    Icons.Default.Update,
                                    contentDescription = "获取更新",
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("获取更新")
                            }

                            // Github
                            Button(
                                onClick = { openUrlInBrowser(context, "Github") },
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "开源网址",
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Github")
                            }
                        }
                    }
                )
            }

            // 简介
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

        // 更新
        if (showUpdateDialog.value) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog.value = false },
                title = { Text("获取更新") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("请选择下载渠道：")
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 123云盘
                        Button(
                            onClick = {
                                openUrlInBrowser(context, "123云盘")
                                showUpdateDialog.value = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("123云盘下载")
                        }

                        // 夸克云盘
                        Button(
                            onClick = {
                                openUrlInBrowser(context, "夸克云盘")
                                showUpdateDialog.value = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("夸克云盘下载")
                        }

                        Text(
                            text = "夸克云盘提取码：FEVC",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        TextButton(
                            onClick = { showUpdateDialog.value = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消")
                        }
                    }
                },

                dismissButton = {}
            )
        }
    }
}

/**
 * 通用信息卡片
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
 * 计算存储空间
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
 * 格式化文件大小
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