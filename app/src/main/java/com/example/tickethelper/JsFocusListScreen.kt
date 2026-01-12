package com.example.tickethelper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tickethelper.data.JsTicketMonitorDataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun JsFocusListScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStore = JsTicketMonitorDataStore(context)
    var selectedItems by remember { mutableStateOf(emptyList<JsTicketMonitorDataStore.FocusItem>()) }
    var allFocusItems by remember { mutableStateOf(dataStore.defaultFocusItems) }

    // 加载关注列表
    LaunchedEffect(Unit) {
        dataStore.focusList.collectLatest { list ->
            selectedItems = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理关注列表") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 关注列表
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allFocusItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        onClick = {
                            selectedItems = if (selectedItems.contains(item)) {
                                selectedItems - item
                            } else {
                                selectedItems + item
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "详情: ${item.info}", style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = selectedItems.contains(item),
                                    onCheckedChange = {
                                        selectedItems = if (it) {
                                            selectedItems + item
                                        } else {
                                            selectedItems - item
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 添加自定义项（预留）
            Button(
                onClick = {
                    val customItem = JsTicketMonitorDataStore.FocusItem(
                        name = "自定义赛事${allFocusItems.size + 1}",
                        showId = "show_${allFocusItems.size + 1}",
                        sign = "sign_${allFocusItems.size + 1}",
                        info = "info_${allFocusItems.size + 1}"
                    )
                    allFocusItems = allFocusItems + customItem
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text("添加自定义关注项（暂不可用）")
            }

            // 保存按钮
            Button(
                onClick = {
                    // 保存选中的关注列表
                    kotlinx.coroutines.GlobalScope.launch {
                        dataStore.saveFocusList(selectedItems)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("保存")
            }
        }
    }
}