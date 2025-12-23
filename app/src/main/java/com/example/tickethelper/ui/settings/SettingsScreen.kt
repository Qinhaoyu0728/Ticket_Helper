package com.example.tickethelper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.HiddenFeaturesDataStore
import com.example.tickethelper.navigation.NavigationRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {

    val context = LocalContext.current

    // 初始化
    val hiddenFeaturesDataStore = HiddenFeaturesDataStore.getInstance(context)

    var hiddenFeatures by remember { mutableStateOf(emptySet<String>()) }

    // 加载隐藏功能
    LaunchedEffect(Unit) {
        hiddenFeaturesDataStore.getHiddenFeatures.collect { hidden ->
            hiddenFeatures = hidden
        }
    }

    fun toggleFeatureVisibility(featureName: String) {
        val newHiddenFeatures = if (hiddenFeatures.contains(featureName)) {
            // if 隐藏-->移除（显示）
            hiddenFeatures - featureName
        } else {
            // if not 隐藏-->添加（隐藏）
            hiddenFeatures + featureName
        }

        // 保存
        CoroutineScope(Dispatchers.IO).launch {
            hiddenFeaturesDataStore.saveHiddenFeatures(newHiddenFeatures)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .padding(16.dp)
        ) {
            // 设置
            SettingItem(
                icon = Icons.Default.Menu,
                title = "功能卡片排序",
                description = "调整主页功能卡片的显示顺序"
            ) {
                navController.navigate(NavigationRoutes.FEATURE_ORDER)
            }

            SettingItem(
                icon = Icons.Default.Menu,
                title = "隐藏功能到\"更多\"",
                description = "选择需要隐藏到\"更多\"中的功能卡片",
                onClick = { navController.navigate(NavigationRoutes.HIDE_FEATURES) }
            )

            SettingItem(
                icon = Icons.Default.Menu,
                title = "主题设置",
                description = "设置应用的显示模式",
                onClick = { navController.navigate(NavigationRoutes.THEME_SETTINGS) }
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "进入设置项",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}