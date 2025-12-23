package com.example.tickethelper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tickethelper.data.HiddenFeaturesDataStore
import com.example.tickethelper.navigation.NavigationRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreFeaturesScreen(navController: NavController) {
    val context = LocalContext.current
    val hiddenFeaturesDataStore = HiddenFeaturesDataStore.getInstance(context)

    var hiddenFeatures by remember { mutableStateOf(emptySet<String>()) }
    var hiddenFeatureList by remember { mutableStateOf(emptyList<Feature>()) }

    // 加载隐藏的功能
    LaunchedEffect(Unit) {
        hiddenFeaturesDataStore.getHiddenFeatures.collect { hidden ->
            hiddenFeatures = hidden
            // 筛选出隐藏的功能
            hiddenFeatureList = featuresList.filter { hidden.contains(it.name) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更多功能") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (hiddenFeatureList.isEmpty()) {
            // 空状态：无隐藏卡片时显示提示
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "无卡片\n可在设置中将卡片加入更多",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp // 确保换行效果
                    )

                    // 可添加跳转按钮（可选）
                    TextButton(
                        onClick = {
                            navController.navigate(NavigationRoutes.SETTINGS)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("去设置")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hiddenFeatureList) { feature ->
                    FeatureCard(
                        feature = feature,
                        navController = navController
                    )
                }
            }
        }
    }
}