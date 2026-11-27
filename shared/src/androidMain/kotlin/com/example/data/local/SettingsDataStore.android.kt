package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

object AndroidSettingsDataStoreProvider {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getInstance(context: Context): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: createSettingsDataStore {
                context.applicationContext.filesDir.resolve(SETTINGS_DATASTORE_FILE).absolutePath
            }.also { instance = it }
        }
    }
}
