package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fueltrack_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val KEY_SELECTED_VEHICLE_ID = longPreferencesKey("selected_vehicle_id")

    val selectedVehicleIdFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_VEHICLE_ID]
    }

    suspend fun setSelectedVehicleId(vehicleId: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_VEHICLE_ID] = vehicleId
        }
    }

    suspend fun clearSelectedVehicleId() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_SELECTED_VEHICLE_ID)
        }
    }
}
