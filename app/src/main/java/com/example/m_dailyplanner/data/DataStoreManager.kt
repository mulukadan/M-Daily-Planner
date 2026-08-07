package com.example.m_dailyplanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val CARRY_FORWARD_COUNT = intPreferencesKey("carry_forward_count")
        val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }

    // Count of ALL pending tasks dated before today (not just yesterday's), so tasks
    // left unfinished across multiple unopened days are never silently dropped from the prompt.
    val carryForwardEvent: Flow<CarryForwardData?> = context.dataStore.data.map { preferences ->
        val count = preferences[CARRY_FORWARD_COUNT] ?: 0
        if (count > 0) CarryForwardData(count) else null
    }

    val showOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_ONBOARDING] ?: true
    }

    suspend fun setCarryForward(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[CARRY_FORWARD_COUNT] = count
        }
    }

    suspend fun clearCarryForward() {
        context.dataStore.edit { preferences ->
            preferences.remove(CARRY_FORWARD_COUNT)
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ONBOARDING] = false
        }
    }
}

data class CarryForwardData(val count: Int)
