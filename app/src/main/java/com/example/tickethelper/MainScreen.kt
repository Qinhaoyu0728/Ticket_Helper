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

    // 监听导航路由
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // 控制底部导航栏是否显示的状态
    var showBottomBar by remember { mutableStateOf(true) }

    // 需要导航条?
    LaunchedEffect(currentRoute) {
        // 主界面 yes
        showBottomBar = when (currentRoute) {
            NavigationRoutes.FEATURES,
            NavigationRoutes.ABOUT -> true
            else -> false // no
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
            composable(NavigationRoutes.SETTINGS) {
                SettingsScreen(navController = navController)
            }
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