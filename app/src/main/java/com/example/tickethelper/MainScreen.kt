package com.example.tickethelper

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tickethelper.data.TicketTargetDataStore
import com.example.tickethelper.datastore.dataStore
import com.example.tickethelper.navigation.NavigationRoutes
import com.example.tickethelper.ui.settings.AutoRefreshSettingsScreen
import com.example.tickethelper.ui.settings.FeatureOrderScreen
import com.example.tickethelper.ui.settings.HideFeaturesScreen
import com.example.tickethelper.ui.settings.SettingsScreen
import com.example.tickethelper.ui.settings.ThemeSettingsScreen

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val featureOrderDataStore = (context.applicationContext as MyApplication).featureOrderDataStore

    // 监听当前导航路由
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // 控制底部导航栏是否显示的状态
    var showBottomBar by remember { mutableStateOf(true) }

    // 监听路由变化，决定是否显示底部导航栏
    LaunchedEffect(currentRoute) {
        // 主界面（功能列表页、关于页）显示导航栏，其他功能页隐藏
        showBottomBar = when (currentRoute) {
            NavigationRoutes.FEATURES,
            NavigationRoutes.ABOUT -> true
            else -> false // 个人简介表单/详情页等功能页隐藏
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.FEATURES,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .fillMaxSize()

        ) {
            composable(NavigationRoutes.FEATURES) {
                FeaturesScreen(
                    navController = navController,
                    featureOrderDataStore = featureOrderDataStore
                )
            }
            composable(NavigationRoutes.ABOUT) {
                AboutScreen()
            }
            // 添加设置页面
            composable(NavigationRoutes.SETTINGS) {
                SettingsScreen(navController = navController)
            }
            // 添加功能排序页面
            composable(NavigationRoutes.FEATURE_ORDER) {
                FeatureOrderScreen(navController = navController)
            }
            composable(NavigationRoutes.MORE_FEATURES) {
                MoreFeaturesScreen(navController = navController)
            }
            composable(NavigationRoutes.HIDE_FEATURES) {
                HideFeaturesScreen(navController = navController)
            }
            composable(NavigationRoutes.THEME_SETTINGS) {
                ThemeSettingsScreen(navController = navController)
            }

            composable(NavigationRoutes.TICKET_ASSISTANT) {
                TicketAssistantScreen(navController = navController)
            }
            composable(NavigationRoutes.TICKET_ADD) {
                AddTicketTargetScreen(navController = navController)
            }


            composable(NavigationRoutes.AUTO_REFRESH_SETTINGS) {
                AutoRefreshSettingsScreen(
                    navController = navController,
                    ticketTargetDataStore = TicketTargetDataStore.getInstance(context)
                )
            }

            composable(NavigationRoutes.APP_TICKET) {
                AppTicketScreen(navController = navController)
            }

            composable(NavigationRoutes.LOG_SCREEN) {
                LogScreen(navController = navController)
            }
        }

        // 底部导航栏
        if (showBottomBar) {
            BottomNavigationBar(
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}