package com.example.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ui.model.AppLanguage
import com.example.ui.model.AppSettingsState
import com.example.ui.model.AppThemeMode
import com.example.ui.model.CharsetEncoding
import com.example.domain.protocol.BUILTIN_PLOT_COLUMNS
import com.example.domain.protocol.BUILTIN_PLOT_KEYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

const val SETTINGS_DATASTORE_FILE = "serialscope_settings.preferences_pb"

fun createSettingsDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settingsFlow: Flow<AppSettingsState> = dataStore.data.map { prefs ->
        val default = AppSettingsState()
        AppSettingsState(
            appLanguage = prefs[KEY_APP_LANGUAGE]?.let { name ->
                AppLanguage.entries.firstOrNull { it.name == name }
            } ?: default.appLanguage,
            themeMode = prefs[KEY_THEME_MODE]?.let { name ->
                AppThemeMode.entries.firstOrNull { it.name == name }
            } ?: default.themeMode,
            charset = prefs[KEY_CHARSET]?.let { name ->
                CharsetEncoding.entries.firstOrNull { it.name == name }
            } ?: default.charset,
            terminalFontSizeSp = prefs[KEY_FONT_SIZE] ?: default.terminalFontSizeSp,
            frameTimeoutMs = prefs[KEY_FRAME_TIMEOUT] ?: default.frameTimeoutMs,
            maxBufferFrames = prefs[KEY_MAX_BUFFER] ?: default.maxBufferFrames,
            autoConnectOnUsbAttach = prefs[KEY_AUTO_CONNECT] ?: default.autoConnectOnUsbAttach,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: default.keepScreenOn,
            clearInputAfterSend = prefs[KEY_CLEAR_AFTER_SEND] ?: default.clearInputAfterSend,
            vibrateOnError = prefs[KEY_VIBRATE_ON_ERROR] ?: default.vibrateOnError,
            plotProfileId = selectedPlotProfile(prefs),
        )
    }

    suspend fun update(transform: (AppSettingsState) -> AppSettingsState) {
        dataStore.edit { prefs ->
            val current = AppSettingsState(
                appLanguage = prefs[KEY_APP_LANGUAGE]?.let { n -> AppLanguage.entries.firstOrNull { it.name == n } } ?: AppLanguage.SYSTEM,
                themeMode = prefs[KEY_THEME_MODE]?.let { n -> AppThemeMode.entries.firstOrNull { it.name == n } } ?: AppThemeMode.SYSTEM,
                charset = prefs[KEY_CHARSET]?.let { n -> CharsetEncoding.entries.firstOrNull { it.name == n } } ?: CharsetEncoding.UTF_8,
                terminalFontSizeSp = prefs[KEY_FONT_SIZE] ?: 12.5f,
                frameTimeoutMs = prefs[KEY_FRAME_TIMEOUT] ?: 50,
                maxBufferFrames = prefs[KEY_MAX_BUFFER] ?: 5000,
                autoConnectOnUsbAttach = prefs[KEY_AUTO_CONNECT] ?: true,
                keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
                clearInputAfterSend = prefs[KEY_CLEAR_AFTER_SEND] ?: false,
                vibrateOnError = prefs[KEY_VIBRATE_ON_ERROR] ?: true,
                plotProfileId = selectedPlotProfile(prefs),
            )
            val next = transform(current)
            prefs[KEY_APP_LANGUAGE] = next.appLanguage.name
            prefs[KEY_THEME_MODE] = next.themeMode.name
            prefs[KEY_CHARSET] = next.charset.name
            prefs[KEY_FONT_SIZE] = next.terminalFontSizeSp
            prefs[KEY_FRAME_TIMEOUT] = next.frameTimeoutMs
            prefs[KEY_MAX_BUFFER] = next.maxBufferFrames
            prefs[KEY_AUTO_CONNECT] = next.autoConnectOnUsbAttach
            prefs[KEY_KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[KEY_CLEAR_AFTER_SEND] = next.clearInputAfterSend
            prefs[KEY_VIBRATE_ON_ERROR] = next.vibrateOnError
            prefs.remove(KEY_PLOT_PROTOCOL)
            val plotProfileId = next.plotProfileId
            if (plotProfileId == null) prefs.remove(KEY_PLOT_PROFILE) else prefs[KEY_PLOT_PROFILE] = plotProfileId
        }
    }

    suspend fun resetTerminalDefaults() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CHARSET)
            prefs.remove(KEY_FONT_SIZE)
            prefs.remove(KEY_FRAME_TIMEOUT)
            prefs.remove(KEY_MAX_BUFFER)
            prefs.remove(KEY_CLEAR_AFTER_SEND)
            prefs.remove(KEY_VIBRATE_ON_ERROR)
        }
    }

    private fun selectedPlotProfile(prefs: Preferences): Long? =
        prefs[KEY_PLOT_PROFILE] ?: when (prefs[KEY_PLOT_PROTOCOL]) {
            "CSV_ONLY" -> BUILTIN_PLOT_COLUMNS
            "KEY_VALUE_ONLY" -> BUILTIN_PLOT_KEYS
            else -> null
        }

    private companion object {
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_CHARSET = stringPreferencesKey("charset")
        val KEY_FONT_SIZE = floatPreferencesKey("font_size")
        val KEY_FRAME_TIMEOUT = intPreferencesKey("frame_timeout")
        val KEY_MAX_BUFFER = intPreferencesKey("max_buffer")
        val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect_usb")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_CLEAR_AFTER_SEND = booleanPreferencesKey("clear_after_send")
        val KEY_VIBRATE_ON_ERROR = booleanPreferencesKey("vibrate_on_error")
        val KEY_PLOT_PROTOCOL = stringPreferencesKey("plot_protocol")
        val KEY_PLOT_PROFILE = longPreferencesKey("plot_profile_id")
    }
}
