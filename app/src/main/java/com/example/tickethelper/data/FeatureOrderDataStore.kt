package com.example.tickethelper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.featureOrderDataStore: DataStore<Preferences> by preferencesDataStore(name = "feature_order_prefs")

class FeatureOrderDataStore(private val context: Context) {
    private val VERSION_KEY = intPreferencesKey("order_version")
    private val FEATURE_ORDER_KEY = stringSetPreferencesKey("feature_order")

    val getFeatureOrder: Flow<List<String>> = context.featureOrderDataStore.data
        .map { preferences ->
            preferences[VERSION_KEY] ?: 0
            preferences[FEATURE_ORDER_KEY]?.toList() ?: emptyList()
        }

    // 保存并更新
    suspend fun saveFeatureOrder(order: List<String>) {
        context.featureOrderDataStore.edit { preferences ->
            val currentVersion = preferences[VERSION_KEY] ?: 0
            preferences[VERSION_KEY] = currentVersion + 1
            preferences[FEATURE_ORDER_KEY] = order.toSet()
        }
    }

    // 获取当前排序
    suspend fun getCurrentOrder(): List<String> {
        return getFeatureOrder.first()
    }
}