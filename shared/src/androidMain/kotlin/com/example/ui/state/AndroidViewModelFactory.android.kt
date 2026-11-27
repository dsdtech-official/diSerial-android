package com.example.ui.state

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AndroidDatabaseProvider
import com.example.data.local.AndroidSettingsDataStoreProvider
import com.example.data.local.SerialRepository
import com.example.data.local.SettingsRepository
import com.example.platform.AndroidFeedback
import com.example.platform.AndroidFileGateway
import com.example.platform.AndroidUsbTransportBridge
import com.example.platform.resolveSystemLanguageTag

class AndroidSerialViewModelFactory(
    context: Context,
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SerialUiViewModel::class.java)) {
            lateinit var vm: SerialUiViewModel
            val bridge = AndroidUsbTransportBridge(
                context = appContext,
                frameGapMsProvider = { vm.uiState.value.settings.frameTimeoutMs },
            )
            val database = AndroidDatabaseProvider.getInstance(appContext)
            val repository = SerialRepository(database)
            val settingsRepository = SettingsRepository(
                AndroidSettingsDataStoreProvider.getInstance(appContext),
            )
            vm = SerialUiViewModel(
                repository = repository,
                settingsRepository = settingsRepository,
                transportBridge = bridge,
                fileGateway = AndroidFileGateway(appContext),
                feedback = AndroidFeedback(appContext),
                systemLanguageTagProvider = { resolveSystemLanguageTag(appContext) },
            )
            return vm as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

fun SerialUiViewModel.handleLaunchedUsbDevice(rawDevice: UsbDevice) {
    val androidBridge = transportBridge as? AndroidUsbTransportBridge ?: return
    val targetId = androidBridge.resolveDeviceKey(rawDevice) ?: return
    handleLaunchedUsbDeviceKey(targetId)
}
