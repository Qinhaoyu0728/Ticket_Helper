package com.example.tickethelper

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.example.tickethelper.data.AppThemeMode
import com.example.tickethelper.data.ThemeModeDataStore

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeDataStore = ThemeModeDataStore.getInstance(this)

        setContent {
            val systemInDarkMode = isSystemInDarkTheme()
            var themeMode by remember { mutableStateOf(AppThemeMode.FOLLOW_SYSTEM) }

            // 监听主题设置变化
            LaunchedEffect(Unit) {
                themeDataStore.getThemeMode.collect { mode ->
                    themeMode = mode
                }
            }

            // 确定当前是否使用深色模式
            val useDarkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.FOLLOW_SYSTEM -> systemInDarkMode
            }

            // 应用主题
            MaterialTheme(
                colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme(),
                content = {
                    MainScreen()
                }
            )
        }
    }
}