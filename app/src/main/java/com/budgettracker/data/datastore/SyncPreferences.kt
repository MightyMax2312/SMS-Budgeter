package com.budgettracker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

class SyncPreferences(private val context: Context) {

    companion object {
        val LAST_SMS_SYNC = longPreferencesKey("last_sms_sync_time")
        val LAST_GMAIL_SYNC = longPreferencesKey("last_gmail_sync_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_ONBOARDING_IMPORT = longPreferencesKey("last_onboarding_import")
        val SELECTED_IMPORT_DAYS = longPreferencesKey("selected_import_days")
        val SELECTED_START_DATE = longPreferencesKey("selected_start_date")
    }

    val lastSmsSyncTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_SMS_SYNC] ?: 0L
    }

    val lastGmailSyncTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_GMAIL_SYNC] ?: 0L
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val selectedImportDays: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_IMPORT_DAYS] ?: 30L
    }

    val selectedStartDate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_START_DATE] ?: 0L
    }

    suspend fun updateLastSmsSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SMS_SYNC] = timestamp
        }
    }

    suspend fun updateLastGmailSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_GMAIL_SYNC] = timestamp
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSelectedImportDays(days: Long) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_IMPORT_DAYS] = days
        }
    }

    suspend fun setSelectedStartDate(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_START_DATE] = timestamp
        }
    }
}
