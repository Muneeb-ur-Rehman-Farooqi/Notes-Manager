package com.muneeb.coursemanager.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(context: Context) {
    private val dataStore = context.dataStore

    private companion object {
        val SELECTED_EDUCATION_LEVEL = stringPreferencesKey("selected_education_level")
        val SELECTED_GROUP = stringPreferencesKey("selected_group")
        val SELECTED_SEMESTER_ID = longPreferencesKey("selected_semester_id")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val selectedEducationLevel: Flow<String?> = dataStore.data
        .map { preferences -> preferences[SELECTED_EDUCATION_LEVEL] }

    val selectedGroup: Flow<String?> = dataStore.data
        .map { preferences -> preferences[SELECTED_GROUP] }

    val selectedSemesterId: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[SELECTED_SEMESTER_ID] }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_DARK_MODE] ?: false }

    suspend fun setSelectedEducationLevel(level: String?) {
        dataStore.edit { preferences ->
            if (level == null) preferences.remove(SELECTED_EDUCATION_LEVEL)
            else preferences[SELECTED_EDUCATION_LEVEL] = level
        }
    }

    suspend fun setSelectedGroup(group: String?) {
        dataStore.edit { preferences ->
            if (group == null) preferences.remove(SELECTED_GROUP)
            else preferences[SELECTED_GROUP] = group
        }
    }

    suspend fun setSelectedSemesterId(semesterId: Long?) {
        dataStore.edit { preferences ->
            if (semesterId == null) preferences.remove(SELECTED_SEMESTER_ID)
            else preferences[SELECTED_SEMESTER_ID] = semesterId
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }
}