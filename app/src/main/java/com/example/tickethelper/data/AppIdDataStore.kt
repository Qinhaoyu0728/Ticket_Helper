package com.example.tickethelper.data

// 国内接口查询所有设置项

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings_preferences"
)

class AppSettingsDataStore private constructor(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val APP_ID_KEY = stringPreferencesKey("app_id")

        private val AUTO_EXPAND_TICKETS = booleanPreferencesKey("auto_expand_tickets")

        @Volatile
        private var INSTANCE: AppSettingsDataStore? = null

        fun getInstance(context: Context): AppSettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                val instance = AppSettingsDataStore(
                    context.applicationContext.appSettingsDataStore
                )
                INSTANCE = instance
                instance
            }
        }
    }

    fun getAutoExpandConfigSync(): Boolean {
        return runBlocking {
            dataStore.data.first()[AUTO_EXPAND_TICKETS] ?: true
        }
    }

    val getAppId: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[APP_ID_KEY]
        }

    suspend fun saveAppId(appId: String) {
        dataStore.edit { preferences ->
            preferences[APP_ID_KEY] = appId.trim()
        }
    }

    val getAutoExpandConfig: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { it[AUTO_EXPAND_TICKETS] ?: true }

    suspend fun saveAutoExpandConfig(enabled: Boolean) {
        dataStore.edit { it[AUTO_EXPAND_TICKETS] = enabled }
    }

    suspend fun clearAllSettings() {
        dataStore.edit { it.clear() }
    }
}