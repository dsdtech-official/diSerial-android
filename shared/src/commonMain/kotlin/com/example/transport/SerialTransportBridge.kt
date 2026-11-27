package com.example.transport

import com.example.ui.model.SerialPortConfig
import com.example.ui.model.UsbDeviceItem
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class TransportPermissionPendingException(message: String) : Exception(message)

sealed interface TransportEvent {
    data class PermissionResult(val deviceId: String, val granted: Boolean) : TransportEvent
    data class DeviceAttached(val device: UsbDeviceItem) : TransportEvent
    data class DeviceDetached(val deviceId: String) : TransportEvent
    data class Connected(val device: UsbDeviceItem, val config: SerialPortConfig) : TransportEvent
    data class Disconnected(val reason: String, val unexpected: Boolean) : TransportEvent
    data class Error(val message: String) : TransportEvent
}

interface SerialTransportBridge {
    val usbDevices: StateFlow<List<UsbDeviceItem>>
    val rxFrames: SharedFlow<ByteArray>
    val events: SharedFlow<TransportEvent>

    fun refreshUsbDevices(): List<UsbDeviceItem>
    fun requestUsbPermission(deviceId: String, autoConnectConfig: SerialPortConfig? = null): Boolean
    suspend fun connectUsb(deviceId: String, config: SerialPortConfig): Result<UsbDeviceItem>
    suspend fun updateUsbParameters(config: SerialPortConfig): Result<Unit>
    suspend fun setDtr(enabled: Boolean): Result<Unit>
    suspend fun setRts(enabled: Boolean): Result<Unit>
    suspend fun write(bytes: ByteArray): Result<Int>
    fun disconnect(reason: String, unexpected: Boolean = false)
    fun release()
}
