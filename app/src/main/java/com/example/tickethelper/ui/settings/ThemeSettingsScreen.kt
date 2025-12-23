package com.example.tickethelper.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.AppThemeMode
import com.example.tickethelper.data.ThemeModeDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val themeDataStore = ThemeModeDataStore.getInstance(context)
    val scope = rememberCoroutineScope()

    // 当前选中的主题模式
    var selectedThemeMode by remember { mutableStateOf(AppThemeMode.FOLLOW_SYSTEM) }

    // 加载保存的主题设置
    LaunchedEffect(Unit) {
        themeDataStore.getThemeMode.collect { mode ->
            selectedThemeMode = mode
        }
    }

    // 保存主题设置
    fun saveThemeMode(mode: AppThemeMode) {
        scope.launch {
            themeDataStore.saveThemeMode(mode)
            selectedThemeMode = mode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题设置") },
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
            // 显示模式设置
            Text(
                "显示模式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 浅色模式选项
            RadioButtonItem(
                label = "浅色模式",
                selected = selectedThemeMode == AppThemeMode.LIGHT,
                onSelect = { saveThemeMode(AppThemeMode.LIGHT) }
            )

            // 深色模式选项
            RadioButtonItem(
                label = "深色模式",
                selected = selectedThemeMode == AppThemeMode.DARK,
                onSelect = { saveThemeMode(AppThemeMode.DARK) }
            )

            // 跟随系统选项
            RadioButtonItem(
                label = "跟随系统",
                selected = selectedThemeMode == AppThemeMode.FOLLOW_SYSTEM,
                onSelect = { saveThemeMode(AppThemeMode.FOLLOW_SYSTEM) }
            )
        }
    }
}

// 单选按钮项组件
@Composable
private fun RadioButtonItem(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}