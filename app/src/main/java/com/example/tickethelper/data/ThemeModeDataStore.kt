package com.example.tickethelper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 定义主题模式枚举
enum class AppThemeMode {
    LIGHT, DARK, FOLLOW_SYSTEM
}

class ThemeModeDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val Context.dataStore by preferencesDataStore(name = "theme_settings")

        fun getInstance(context: Context): ThemeModeDataStore {
            return ThemeModeDataStore(context.dataStore)
        }
    }

    // 获取主题
    val getThemeMode: Flow<AppThemeMode> = dataStore.data
        .map { preferences ->
            val modeStr = preferences[THEME_MODE_KEY]
            when (modeStr) {
                "LIGHT" -> AppThemeMode.LIGHT
                "DARK" -> AppThemeMode.DARK
                else -> AppThemeMode.FOLLOW_SYSTEM
            }
        }

    // 保存主题
    suspend fun saveThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}