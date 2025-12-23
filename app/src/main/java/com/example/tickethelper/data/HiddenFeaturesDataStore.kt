package com.example.tickethelper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HiddenFeaturesDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val HIDDEN_FEATURES_KEY = stringSetPreferencesKey("hidden_features")
        private val Context.dataStore by preferencesDataStore(name = "hidden_features")

        fun getInstance(context: Context): HiddenFeaturesDataStore {
            return HiddenFeaturesDataStore(context.dataStore)
        }
    }

    val getHiddenFeatures: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[HIDDEN_FEATURES_KEY] ?: emptySet()
        }

    suspend fun saveHiddenFeatures(hiddenFeatures: Set<String>) {
        dataStore.edit { preferences ->
            preferences[HIDDEN_FEATURES_KEY] = hiddenFeatures
        }
    }
}