package com.example.tickethelper.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// 定义DataStore扩展属性
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "f1_meetings_preferences")