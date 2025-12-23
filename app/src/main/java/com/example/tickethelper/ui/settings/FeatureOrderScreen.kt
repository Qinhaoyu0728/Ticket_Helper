package com.example.tickethelper.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.MyApplication
import com.example.tickethelper.Feature
import com.example.tickethelper.featuresList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureOrderScreen(navController: NavController) {
    val context = LocalContext.current
    val featureOrderDataStore = (context.applicationContext as MyApplication).featureOrderDataStore
    val scope = rememberCoroutineScope()

    // 状态管理
    var features by remember { mutableStateOf(featuresList.toList()) }
    var selectedOrder by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var currentStep by remember { mutableStateOf(1) }
    var isSaving by remember { mutableStateOf(false) }

    // 加载排序
    LaunchedEffect(Unit) {
        featureOrderDataStore.getFeatureOrder.collect { order ->
            if (order.isNotEmpty()) {
                features = featuresList.sortedBy { order.indexOf(it.name) }
                val newOrder = mutableMapOf<String, Int>()
                order.forEachIndexed { index, featureName ->
                    newOrder[featureName] = index + 1
                }
                selectedOrder = newOrder
                currentStep = order.size + 1
            }
        }
    }

    // 保存排序
    fun saveFeatureOrder() {
        isSaving = true
        scope.launch {
            val allFeatureNames = featuresList.map { it.name }
            val sortedNames = features.sortedBy { selectedOrder[it.name] ?: Int.MAX_VALUE }
                .map { it.name }
            val remainingNames = allFeatureNames - sortedNames.toSet()
            val finalOrder = sortedNames + remainingNames

            featureOrderDataStore.saveFeatureOrder(finalOrder)

            selectedOrder = emptyMap()
            currentStep = 1

            val savedOrder = featureOrderDataStore.getCurrentOrder()
            if (savedOrder.isNotEmpty()) {
                features = featuresList.sortedBy { savedOrder.indexOf(it.name) }
            }

            isSaving = false
        }
    }

    // 重置
    fun resetSelection() {
        selectedOrder = emptyMap()
        currentStep = 1
    }

    // 处理项点击
    fun handleItemClick(feature: Feature) {
        val newOrder = selectedOrder.toMutableMap()

        if (newOrder.containsKey(feature.name)) {
            val removedStep = newOrder.remove(feature.name) ?: return
            newOrder.forEach { (name, step) ->
                if (step > removedStep) {
                    newOrder[name] = step - 1
                }
            }
            currentStep--
        } else {
            newOrder[feature.name] = currentStep
            currentStep++
        }

        selectedOrder = newOrder.toMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能卡片排序") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveFeatureOrder() },
                        enabled = !isSaving && selectedOrder.isNotEmpty()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存排序")
                        }
                    }
                    TextButton(onClick = {
                        features = featuresList.toList()
                        resetSelection()
                    }) {
                        Text("恢复默认")
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
                "按想要的顺序点击功能项（已选 ${selectedOrder.size}/${features.size}）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(features, key = { it.name }) { feature ->
                    val isSelected = selectedOrder.containsKey(feature.name)
                    val orderNumber = selectedOrder[feature.name]

                    // 可点击项
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics { role = Role.Button },
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 6.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        onClick = { handleItemClick(feature) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 复选框
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .background(
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = orderNumber.toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

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

                            // 名称
                            Text(
                                text = feature.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            // 选中状态
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 提示
            Text(
                "操作提示：点击项选择排序，再次点击取消，数字表示顺序",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}