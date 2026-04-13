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
        val CARRY_FORWARD_DATE = stringPreferencesKey("carry_forward_date")
        val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
    }

    val carryForwardEvent: Flow<CarryForwardData?> = context.dataStore.data.map { preferences ->
        val count = preferences[CARRY_FORWARD_COUNT] ?: 0
        val date = preferences[CARRY_FORWARD_DATE] ?: ""
        if (count > 0 && date.isNotEmpty()) {
            CarryForwardData(count, date)
        } else {
            null
        }
    }

    val showOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_ONBOARDING] ?: true
    }

    suspend fun setCarryForward(count: Int, date: String) {
        context.dataStore.edit { preferences ->
            preferences[CARRY_FORWARD_COUNT] = count
            preferences[CARRY_FORWARD_DATE] = date
        }
    }

    suspend fun clearCarryForward() {
        context.dataStore.edit { preferences ->
            preferences.remove(CARRY_FORWARD_COUNT)
            preferences.remove(CARRY_FORWARD_DATE)
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ONBOARDING] = false
        }
    }
}

data class CarryForwardData(val count: Int, val date: String)
