package com.example.tickethelper.ui.settings

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.tickethelper.data.AppSettingsDataStore
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTicketSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val appIdDataStore = remember { AppSettingsDataStore.getInstance(context) }
    var appId by remember { mutableStateOf("") }
    var showSavedDialog by remember { mutableStateOf(false) }
    var autoExpandTickets by remember {
        mutableStateOf(appIdDataStore.getAutoExpandConfigSync())
    }

    val scope = rememberCoroutineScope()

    // 加载设置
    LaunchedEffect(Unit) {
        // app_id
        appIdDataStore.getAppId.collect { savedAppId ->
            appId = savedAppId ?: ""
        }
        // 自动展开
        appIdDataStore.getAutoExpandConfig.collect { enabled ->
            autoExpandTickets = enabled
        }
    }

    // 保存app_id
    fun saveAppId(input: String) {
        CoroutineScope(Dispatchers.IO).launch {
            appIdDataStore.saveAppId(input)
            withContext(Dispatchers.Main) {
                showSavedDialog = true
            }
        }
    }

    fun saveAutoExpandConfig(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            // 强制写入并立即验证
            appIdDataStore.saveAutoExpandConfig(enabled)
            // 读取验证
            val savedValue = appIdDataStore.getAutoExpandConfig.first()
            Log.d("AutoExpandConfig", "保存后验证：$savedValue")

            withContext(Dispatchers.Main) {
                showSavedDialog = true
                delay(1500)
                showSavedDialog = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("国内接口设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 定义app_id
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "定义app_id",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("请输入app_id") },
                    trailingIcon = {
                        IconButton(onClick = { saveAppId(appId) }) {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { saveAppId(appId) }
                    )
                )
            }

            // 自动展开
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "票目展示设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "展开票目信息",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "有票时展开详情，无票时显示简略信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoExpandTickets,
                        onCheckedChange = {
                            autoExpandTickets = it
                            saveAutoExpandConfig(it)
                        }
                    )
                }
            }
        }

        // 保存成功
        if (showSavedDialog) {
            Toast.makeText(
                context,
                "配置已保存",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}