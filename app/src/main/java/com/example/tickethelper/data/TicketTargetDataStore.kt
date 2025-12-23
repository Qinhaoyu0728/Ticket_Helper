package com.example.tickethelper.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ticket_targets")

class TicketTargetDataStore(private val context: Context) {
    companion object {
        private val TICKET_TARGETS_KEY = stringPreferencesKey("ticket_targets")
        private val AUTO_REFRESH_CONFIG_KEY = stringPreferencesKey("auto_refresh_config")
        val json = Json { ignoreUnknownKeys = true }

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: TicketTargetDataStore? = null
        fun getInstance(context: Context): TicketTargetDataStore {
            return INSTANCE ?: synchronized(this) {
                val instance = TicketTargetDataStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // 保存自动刷新配置
    suspend fun saveAutoRefreshConfig(
        enabled: Boolean,
        listStyle: String = "two_column",
        showOverallStatus: Boolean = true
    ) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_REFRESH_CONFIG_KEY] = json.encodeToString(
                AutoRefreshConfig(
                    enabled = enabled,
                    listStyle = listStyle,
                    showOverallStatus = showOverallStatus
                )
            )
        }
    }

    // 保存整体状态配置
    suspend fun saveShowOverallStatusConfig(show: Boolean) {
        context.dataStore.edit { preferences ->
            val currentConfig = getAutoRefreshConfig.first()
            preferences[AUTO_REFRESH_CONFIG_KEY] = json.encodeToString(
                currentConfig.copy(showOverallStatus = show)
            )
        }
    }

    // 保存样式配置
    suspend fun saveListStyleConfig(listStyle: String) {
        context.dataStore.edit { preferences ->
            val currentConfig = getAutoRefreshConfig.first()
            preferences[AUTO_REFRESH_CONFIG_KEY] = json.encodeToString(
                currentConfig.copy(listStyle = listStyle)
            )
        }
    }

    // 获取自动刷新配置
    val getAutoRefreshConfig: Flow<AutoRefreshConfig> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[AUTO_REFRESH_CONFIG_KEY] ?: "{}"
            try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                AutoRefreshConfig()
            }
        }

    val getTicketTargets: Flow<List<TicketTarget>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[TICKET_TARGETS_KEY] ?: "[]"
            try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun saveTicketTarget(target: TicketTarget) {
        context.dataStore.edit { preferences ->
            val currentTargets = getTicketTargets.first()
            val targetToSave = if (target.id.isEmpty()) {
                target.copy(id = UUID.randomUUID().toString())
            } else {
                target
            }
            val newTargets = currentTargets.filter { it.id != targetToSave.id } + targetToSave
            preferences[TICKET_TARGETS_KEY] = json.encodeToString(newTargets)
        }
    }

    suspend fun deleteTicketTarget(targetId: String) {
        context.dataStore.edit { preferences ->
            val currentTargets = getCurrentTargets()
            val newTargets = currentTargets.filter { it.id != targetId }
            preferences[TICKET_TARGETS_KEY] = json.encodeToString(newTargets)
        }
    }

    private suspend fun getCurrentTargets(): List<TicketTarget> {
        return getTicketTargets.first()
    }
}