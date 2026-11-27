package com.example.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.transport.TransportPermissionPendingException
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.UsbDeviceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

sealed interface UsbSerialEvent {
    data class PermissionResult(val deviceId: String, val granted: Boolean) : UsbSerialEvent
    data class DeviceAttached(val device: UsbDeviceItem) : UsbSerialEvent
    data class DeviceDetached(val deviceId: String) : UsbSerialEvent
    data class Connected(val device: UsbDeviceItem, val config: SerialPortConfig) : UsbSerialEvent
    data class Disconnected(val reason: String, val unexpected: Boolean) : UsbSerialEvent
    data class Error(val message: String) : UsbSerialEvent
}

class UsbSerialManager(
    private val context: Context,
    private val frameGapMsProvider: () -> Int,
) {
    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _devices = MutableStateFlow<List<UsbDeviceItem>>(emptyList())
    val devices: StateFlow<List<UsbDeviceItem>> = _devices.asStateFlow()

    private val _rxFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val rxFrames: SharedFlow<ByteArray> = _rxFrames.asSharedFlow()

    private val _events = MutableSharedFlow<UsbSerialEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<UsbSerialEvent> = _events.asSharedFlow()

    private val writeMutex = Mutex()
    @Volatile
    private var activeDriver: SerialPortDriver? = null
    private var activeDeviceId: String? = null
    private var readerJob: Job? = null
    private var pendingConnectConfig: SerialPortConfig? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

            when (action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    refreshDevices()
                    if (device != null) {
                        val id = deviceKey(device)
                        _events.tryEmit(UsbSerialEvent.PermissionResult(id, granted))
                        if (granted && pendingConnectConfig != null) {
                            val cfg = pendingConnectConfig!!
                            pendingConnectConfig = null
                            scope.launch { connect(id, cfg) }
                        } else if (!granted) {
                            pendingConnectConfig = null
                            _events.tryEmit(UsbSerialEvent.Error("USB 串口权限被拒绝"))
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    refreshDevices()
                    device?.let { dev ->
                        toDeviceItem(dev)?.let { item ->
                            _events.tryEmit(UsbSerialEvent.DeviceAttached(item))
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val detachedId = device?.let(::deviceKey)
                    refreshDevices()
                    if (detachedId != null) {
                        _events.tryEmit(UsbSerialEvent.DeviceDetached(detachedId))
                        if (detachedId == activeDeviceId) {
                            disconnect(reason = "USB 设备已拔出", unexpected = true)
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            context,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        refreshDevices()
    }

    fun refreshDevices(): List<UsbDeviceItem> {
        val manager = usbManager ?: run {
            _devices.value = emptyList()
            return emptyList()
        }
        val list = manager.deviceList.values
            .mapNotNull { toDeviceItem(it) }
            .sortedBy { it.portPath }
        _devices.value = list
        return list
    }

    fun requestPermission(deviceId: String, autoConnectConfig: SerialPortConfig? = null): Boolean {
        val manager = usbManager ?: return false
        val rawDevice = findRawDevice(deviceId) ?: return false
        if (manager.hasPermission(rawDevice)) {
            if (autoConnectConfig != null) {
                scope.launch { connect(deviceId, autoConnectConfig) }
            }
            return true
        }
        pendingConnectConfig = autoConnectConfig
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
        manager.requestPermission(rawDevice, pi)
        return true
    }

    suspend fun connect(deviceId: String, config: SerialPortConfig): Result<UsbDeviceItem> =
        withContext(Dispatchers.IO) {
            val manager = usbManager
                ?: return@withContext Result.failure(IOException("当前设备不支持 Android USB Host"))
            val rawDevice = findRawDevice(deviceId)
                ?: return@withContext Result.failure(IOException("未找到指定的 USB 串口设备"))
            val chip = UsbChipCatalog.probe(rawDevice)
                ?: return@withContext Result.failure(IOException("暂不支持的 USB 设备类型"))

            if (!manager.hasPermission(rawDevice)) {
                requestPermission(deviceId, autoConnectConfig = config)
                return@withContext Result.failure(TransportPermissionPendingException("正在请求 USB 硬件访问权限..."))
            }

            disconnectInternal(emitEvent = false)

            val connection = manager.openDevice(rawDevice)
                ?: return@withContext Result.failure(IOException("无法打开 USB 设备连接"))

            val driver = UsbChipCatalog.createDriver(chip.driverType, rawDevice)
            try {
                driver.open(connection)
                driver.setParameters(config)
                activeDriver = driver
                activeDeviceId = deviceId
                startReaderLoop(driver)

                val item = toDeviceItem(rawDevice)!!
                _events.tryEmit(UsbSerialEvent.Connected(item, config))
                Result.success(item)
            } catch (e: Exception) {
                runCatching { driver.close() }
                activeDriver = null
                activeDeviceId = null
                val msg = e.message ?: "串口打开失败"
                _events.tryEmit(UsbSerialEvent.Error(msg))
                Result.failure(e)
            }
        }

    suspend fun write(bytes: ByteArray): Result<Int> {
        val driver = activeDriver ?: return Result.failure(IOException("串口未连接"))
        return writeMutex.withLock {
            withContext(Dispatchers.IO) {
                // A queued write belongs to the driver that was active when it was submitted.
                if (activeDriver !== driver) {
                    return@withContext Result.failure(IOException("USB 串口连接已变更，请重新发送"))
                }
                runCatching {
                    driver.write(bytes, timeoutMs = 1000)
                }.onFailure { err ->
                    _events.tryEmit(UsbSerialEvent.Error(err.message ?: "数据写入失败"))
                }
            }
        }
    }

    suspend fun updateParameters(config: SerialPortConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val driver = activeDriver ?: return@withContext Result.success(Unit)
        runCatching {
            driver.setParameters(config)
        }
    }

    suspend fun setDtr(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val driver = activeDriver ?: return@withContext Result.success(Unit)
        runCatching {
            driver.setDtr(enabled)
        }
    }

    suspend fun setRts(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val driver = activeDriver ?: return@withContext Result.success(Unit)
        runCatching {
            driver.setRts(enabled)
        }
    }

    fun disconnect(reason: String = "已主动断开串口连接", unexpected: Boolean = false) {
        disconnectInternal(emitEvent = true, reason = reason, unexpected = unexpected)
    }

    fun release() {
        disconnectInternal(emitEvent = false)
        runCatching { context.unregisterReceiver(usbReceiver) }
        scope.cancel()
    }

    private fun startReaderLoop(driver: SerialPortDriver) {
        readerJob?.cancel()
        readerJob = scope.launch {
            val chunk = ByteArray(1024)
            val accumulator = ByteArrayOutputStream()
            var lastByteAtMs = 0L

            while (isActive) {
                try {
                    val readCount = driver.read(chunk, timeoutMs = 20)
                    val now = System.currentTimeMillis()
                    if (readCount > 0) {
                        accumulator.write(chunk, 0, readCount)
                        lastByteAtMs = now
                        if (accumulator.size() >= 4096) {
                            _rxFrames.tryEmit(accumulator.toByteArray())
                            accumulator.reset()
                        }
                    } else if (accumulator.size() > 0) {
                        val gapMs = frameGapMsProvider().coerceIn(10, 500)
                        if (now - lastByteAtMs >= gapMs) {
                            _rxFrames.tryEmit(accumulator.toByteArray())
                            accumulator.reset()
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        if (accumulator.size() > 0) {
                            _rxFrames.tryEmit(accumulator.toByteArray())
                            accumulator.reset()
                        }
                        disconnect(reason = e.message ?: "串口读取异常中断", unexpected = true)
                    }
                    break
                }
            }
        }
    }

    private fun disconnectInternal(
        emitEvent: Boolean,
        reason: String = "",
        unexpected: Boolean = false,
    ) {
        readerJob?.cancel()
        readerJob = null
        val driver = activeDriver
        activeDriver = null
        activeDeviceId = null
        if (driver != null) {
            runCatching { driver.close() }
            if (emitEvent) {
                _events.tryEmit(UsbSerialEvent.Disconnected(reason, unexpected))
            }
        }
    }

    private fun findRawDevice(deviceId: String): UsbDevice? {
        return usbManager?.deviceList?.values?.firstOrNull { deviceKey(it) == deviceId }
    }

    private fun toDeviceItem(device: UsbDevice): UsbDeviceItem? {
        val chip = UsbChipCatalog.probe(device) ?: return null
        val hasPerm = usbManager?.hasPermission(device) == true
        val productName = device.productName?.takeIf { it.isNotBlank() }
        return UsbDeviceItem(
            id = deviceKey(device),
            chipName = chip.chipName,
            driverType = chip.driverType.displayName,
            vidPid = "%04X:%04X".format(device.vendorId, device.productId),
            portPath = device.deviceName,
            isPermissionGranted = hasPerm,
            productName = productName,
            shortModel = UsbChipCatalog.shortModelFromProductName(chip.driverType, productName),
        )
    }

    fun resolveDeviceKey(device: UsbDevice): String? {
        return if (UsbChipCatalog.probe(device) != null) deviceKey(device) else null
    }

    private fun deviceKey(device: UsbDevice): String {
        return "${device.vendorId}:${device.productId}:${device.deviceName}"
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.aistudio.serialscope.USB_PERMISSION"
    }
}
