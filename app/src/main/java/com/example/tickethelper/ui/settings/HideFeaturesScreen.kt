package com.example.tickethelper.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.HiddenFeaturesDataStore
import com.example.tickethelper.featuresList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideFeaturesScreen(navController: NavController) {
    val context = LocalContext.current
    val hiddenFeaturesDataStore = HiddenFeaturesDataStore.getInstance(context)

    // 存储隐藏的功能
    var hiddenFeatures by remember { mutableStateOf(emptySet<String>()) }

    // 加载已保存的隐藏功能
    LaunchedEffect(Unit) {
        hiddenFeaturesDataStore.getHiddenFeatures.collect { hidden ->
            hiddenFeatures = hidden
        }
    }

    // 切换功能的隐藏状态
    fun toggleFeatureVisibility(featureName: String) {
        val newHiddenFeatures = if (hiddenFeatures.contains(featureName)) {
            hiddenFeatures - featureName // 取消隐藏
        } else {
            hiddenFeatures + featureName // 隐藏
        }

        CoroutineScope(Dispatchers.IO).launch {
            hiddenFeaturesDataStore.saveHiddenFeatures(newHiddenFeatures)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐藏功能到\"更多\"") },
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
            Text(
                "选择需要隐藏到\"更多\"中的功能",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            // 卡片式列表（与排序页面保持一致）
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(featuresList.filter { it.name != "更多" }) { feature ->
                    val isHidden = hiddenFeatures.contains(feature.name)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isHidden) 6.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHidden)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        onClick = { toggleFeatureVisibility(feature.name) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 功能图标
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color(feature.color).copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = feature.name,
                                    tint = Color(feature.color),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // 功能名称
                            Text(
                                text = feature.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            // 隐藏状态指示
                            Checkbox(
                                checked = isHidden,
                                onCheckedChange = { toggleFeatureVisibility(feature.name) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}