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
    // 新增：版本戳，用于强制触发更新
    private val VERSION_KEY = intPreferencesKey("order_version")
    private val FEATURE_ORDER_KEY = stringSetPreferencesKey("feature_order")

    // 关键修复：返回包含版本戳的数据流，确保每次修改都能被感知
    val getFeatureOrder: Flow<List<String>> = context.featureOrderDataStore.data
        .map { preferences ->
            // 读取版本戳（无关紧要，只为触发flow更新）
            preferences[VERSION_KEY] ?: 0
            // 返回排序数据
            preferences[FEATURE_ORDER_KEY]?.toList() ?: emptyList()
        }

    // 保存排序并更新版本戳
    suspend fun saveFeatureOrder(order: List<String>) {
        context.featureOrderDataStore.edit { preferences ->
            // 读取当前版本
            val currentVersion = preferences[VERSION_KEY] ?: 0
            // 版本+1，确保数据流触发更新
            preferences[VERSION_KEY] = currentVersion + 1
            // 保存排序数据
            preferences[FEATURE_ORDER_KEY] = order.toSet()
        }
    }

    // 获取当前排序（用于验证）
    suspend fun getCurrentOrder(): List<String> {
        return getFeatureOrder.first()
    }
}