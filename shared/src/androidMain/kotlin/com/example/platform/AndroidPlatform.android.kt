package com.example.platform

import android.content.Context
import android.content.res.Configuration
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import com.example.ui.i18n.preferredSystemLanguageTag
import java.io.File
import com.example.data.usb.UsbSerialEvent
import com.example.data.usb.UsbSerialManager
import com.example.transport.SerialTransportBridge
import com.example.transport.TransportEvent
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.UsbDeviceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

internal actual val editorDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)

actual object PlatformClock {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun formatTimestampHmsMillis(epochMs: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(epochMs))

    actual fun formatMonthDayTime(epochMs: Long): String =
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

    actual fun formatMonthDayTimeSec(epochMs: Long): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMs))

    actual fun formatHourMinute(epochMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

    actual fun formatFileStamp(epochMs: Long): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(epochMs))

    actual fun formatLogDateTime(epochMs: Long): String = logDateFormatter().format(Date(epochMs))

    actual fun parseLogDateTime(text: String): Long? {
        val formatter = logDateFormatter()
        val date = runCatching { formatter.parse(text) }.getOrNull() ?: return null
        return date.time.takeIf { formatter.format(date) == text }
    }

    actual fun startOfLogDay(epochMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun logDateFormatter() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply {
        isLenient = false
    }
}

actual object PlatformTextCodec {
    private val gbkCharset: Charset by lazy {
        runCatching { Charset.forName("GBK") }.getOrElse { Charsets.UTF_8 }
    }

    actual fun encodeGbk(text: String): ByteArray = text.toByteArray(gbkCharset)

    actual fun decodeGbk(bytes: ByteArray): String = String(bytes, gbkCharset)
}

@Composable
actual fun rememberSystemLanguageTag(): String {
    val configuration = LocalConfiguration.current
    return remember(configuration) { languageTagForConfiguration(configuration) }
}

fun resolveSystemLanguageTag(context: Context): String =
    languageTagForConfiguration(context.resources.configuration)

private fun languageTagForConfiguration(configuration: Configuration): String {
    val locales = ConfigurationCompat.getLocales(configuration)
    val languageTags = List(locales.size()) { index -> locales[index]?.toLanguageTag().orEmpty() }
    return preferredSystemLanguageTag(languageTags.ifEmpty { listOf(Locale.getDefault().toLanguageTag()) })
}

class AndroidFileGateway(private val context: Context) : PlatformFileGateway {

    override suspend fun inspectFile(handle: PlatformFileHandle): PlatformFileHandle = withContext(Dispatchers.IO) {
        val uri = Uri.parse(handle.rawUri)
        var resolvedName = uri.lastPathSegment ?: handle.displayName.ifEmpty { "serial_payload.bin" }
        var resolvedSize = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    resolvedName = cursor.getString(nameIdx) ?: resolvedName
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    resolvedSize = cursor.getLong(sizeIdx)
                }
            }
        }
        if (resolvedSize <= 0L) {
            resolvedSize = context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buf = ByteArray(8192)
                while (true) {
                    val read = stream.read(buf)
                    if (read <= 0) break
                    total += read
                }
                total
            } ?: 0L
        }
        handle.copy(displayName = resolvedName, sizeBytes = resolvedSize)
    }

    override suspend fun readFileBytes(handle: PlatformFileHandle): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(handle.rawUri)
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to read file stream")
        }
    }

    override suspend fun readText(handle: PlatformFileHandle): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(handle.rawUri)
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: error("Unable to read text file")
        }
    }

    override suspend fun writeText(handle: PlatformFileHandle, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(handle.rawUri)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Unable to open output stream")
        }
    }

    override suspend fun createShareTarget(fileName: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles()?.forEach { oldFile ->
                if (oldFile.isFile) oldFile.delete()
            }
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
        }
    }
}

class AndroidFeedback(private val context: Context) : PlatformFeedback {
    override fun triggerErrorHaptic() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80L)
                }
            }
        }
    }
}

class AndroidUsbTransportBridge(
    context: Context,
    frameGapMsProvider: () -> Int,
) : SerialTransportBridge {
    private val usbManager = UsbSerialManager(context = context, frameGapMsProvider = frameGapMsProvider)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val usbDevices: StateFlow<List<UsbDeviceItem>> = usbManager.devices
    override val rxFrames: SharedFlow<ByteArray> = usbManager.rxFrames

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<TransportEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            usbManager.events.collect { ev ->
                val mapped = when (ev) {
                    is UsbSerialEvent.PermissionResult -> TransportEvent.PermissionResult(ev.deviceId, ev.granted)
                    is UsbSerialEvent.DeviceAttached -> TransportEvent.DeviceAttached(ev.device)
                    is UsbSerialEvent.DeviceDetached -> TransportEvent.DeviceDetached(ev.deviceId)
                    is UsbSerialEvent.Connected -> TransportEvent.Connected(ev.device, ev.config)
                    is UsbSerialEvent.Disconnected -> TransportEvent.Disconnected(ev.reason, ev.unexpected)
                    is UsbSerialEvent.Error -> TransportEvent.Error(ev.message)
                }
                _events.tryEmit(mapped)
            }
        }
    }

    fun resolveDeviceKey(rawDevice: UsbDevice): String? = usbManager.resolveDeviceKey(rawDevice)

    override fun refreshUsbDevices(): List<UsbDeviceItem> = usbManager.refreshDevices()

    override fun requestUsbPermission(deviceId: String, autoConnectConfig: SerialPortConfig?): Boolean =
        usbManager.requestPermission(deviceId, autoConnectConfig)

    override suspend fun connectUsb(deviceId: String, config: SerialPortConfig): Result<UsbDeviceItem> =
        usbManager.connect(deviceId, config)

    override suspend fun updateUsbParameters(config: SerialPortConfig): Result<Unit> =
        usbManager.updateParameters(config)

    override suspend fun setDtr(enabled: Boolean): Result<Unit> = usbManager.setDtr(enabled)

    override suspend fun setRts(enabled: Boolean): Result<Unit> = usbManager.setRts(enabled)

    override suspend fun write(bytes: ByteArray): Result<Int> = usbManager.write(bytes)

    override fun disconnect(reason: String, unexpected: Boolean) {
        usbManager.disconnect(reason, unexpected)
    }

    override fun release() {
        scope.cancel()
        usbManager.release()
    }
}
