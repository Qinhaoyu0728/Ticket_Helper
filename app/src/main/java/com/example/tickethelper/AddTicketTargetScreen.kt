package com.example.tickethelper

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.PredefinedTicketTarget
import com.example.tickethelper.data.TicketTarget
import com.example.tickethelper.data.TicketTargetDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTicketTargetScreen(navController: NavController) {
    val context = LocalContext.current
    val ticketTargetDataStore = TicketTargetDataStore.getInstance(context)
    val coroutineScope = rememberCoroutineScope()

    // 内置抢票目标列表（可根据需求扩展）
    val predefinedTargets = listOf(
        PredefinedTicketTarget(
            category = "F1 A铂金看台",
            targetId = "6931340104da960001241d03",
            description = "F1 2026赛季 A铂金看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 A看台",
            targetId = "693133c84996310001244e59",
            description = "F1 2026赛季 A看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 B看台",
            targetId = "693132f64996310001244995",
            description = "F1 2026赛季 B看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 H看台",
            targetId = "6931529204da960001255be6",
            description = "F1 2026赛季 H看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 K看台",
            targetId = "693152ad04da960001255d56",
            description = "F1 2026赛季 K看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 E看台",
            targetId = "693152c604da960001255ee5",
            description = "F1 2026赛季 E看台门票"
        ),
        PredefinedTicketTarget(
            category = "F1 草地看台",
            targetId = "693153534996310001259a9f",
            description = "F1 2026赛季 草地看台门票"
        ),
        PredefinedTicketTarget(
            category = "通知测试",
            targetId = "6932f40a04da9600013549e6",
            description = "通知测试 测试完成后请删除"
        )
    )

    // 状态管理
    var expanded by remember { mutableStateOf(false) } // 下拉列表展开状态
    var selectedPredefinedIndex by remember { mutableStateOf(-1) } // 选中的内置目标索引（-1表示未选中）
    var customCategory by remember { mutableStateOf("") } // 自定义类目
    var customTargetId by remember { mutableStateOf("") } // 自定义ID
    var isSaving by remember { mutableStateOf(false) }

    // 选中项文本（用于下拉框显示）
    val selectedText = when {
        selectedPredefinedIndex >= 0 -> predefinedTargets[selectedPredefinedIndex].category
        else -> "选择内置目标或手动输入"
    }

    // 当选中内置目标变化时，自动填充输入框
    LaunchedEffect(selectedPredefinedIndex) {
        if (selectedPredefinedIndex >= 0) {
            val selected = predefinedTargets[selectedPredefinedIndex]
            customCategory = selected.category
            customTargetId = selected.targetId
        }
    }

    // 保存逻辑
    fun saveTarget() {
        val category = customCategory.trim()
        val targetId = customTargetId.trim()

        if (category.isBlank() || targetId.isBlank() || isSaving) {
            Toast.makeText(context, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                ticketTargetDataStore.saveTicketTarget(
                    TicketTarget(
                        targetId = targetId,
                        category = category
                    )
                )
                withContext(Dispatchers.Main) {
                    isSaving = false
                    navController.popBackStack() // 返回列表页
                    Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加抢票目标") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = ::saveTarget,
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
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
                .padding(16.dp)
        ) {
            // 内置目标下拉选择
            Text(
                text = "选择内置目标（可选）",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ExposedDropdownMenu
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded } // 点击切换展开/收起状态
            ) {
                // 下拉输入框
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = {}, // 禁止手动输入，仅通过下拉选择
                    readOnly = true, // 只读模式
                    label = { Text("内置目标列表") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), // 标记为菜单锚点，用于定位下拉列表
                )

                // 下拉列表内容（带展开/收起动画）
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }, // 点击外部收起
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 内置目标选项
                    predefinedTargets.forEachIndexed { index, target ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = target.category,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = target.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedPredefinedIndex = index // 选中当前项
                                expanded = false // 收起列表
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 分隔线
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // 自定义选项
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "自定义目标",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            selectedPredefinedIndex = -1 // 重置为自定义
                            customCategory = ""
                            customTargetId = ""
                            expanded = false // 收起列表
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 手动输入区域
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = "目标信息（可编辑）",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("类目名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("例如：F1 A看台") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customTargetId,
                    onValueChange = { customTargetId = it },
                    label = { Text("目标ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("例如：693133c84996310001244e59") },
                    supportingText = { Text("从目标页面URL中获取的ID") }
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = "自动余票查询暂不支持以下目标ID",
                    style = MaterialTheme.typography.labelMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "铂金体验之旅、加油中国周、A下PLUS、迪士尼联名票、B看台MXGP套票、E看台车迷应援区",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "（可前往国内接口查询查看以上票目信息！）",
                    style = MaterialTheme.typography.labelMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}