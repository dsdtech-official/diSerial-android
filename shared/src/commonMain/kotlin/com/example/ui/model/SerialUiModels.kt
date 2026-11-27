package com.example.ui.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.RxEmerald
import com.example.ui.theme.TxAmber

enum class AppTab(val label: String) {
    TERMINAL("终端"),
    PLOTTER("可视化"),
    LOGS("日志库"),
    SETTINGS("设置"),
    TERMINAL_SETTINGS("终端设置"),
    ABOUT("关于");

    companion object {
        val primaryTabs: List<AppTab> = listOf(TERMINAL, PLOTTER, LOGS)
    }
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
}

enum class DisplayMode(val label: String) {
    ASCII("ASCII"),
    HEX("HEX"),
    MIXED("HEX+ASCII"),
}

enum class TxFormat(val label: String) {
    ASCII("ASCII"),
    HEX("HEX"),
}

enum class LineEnding(val label: String, val suffix: String) {
    NONE("无", ""),
    CR("\\r", "\r"),
    LF("\\n", "\n"),
    CRLF("\\r\\n", "\r\n"),
}

enum class ChecksumMode(val label: String, val shortLabel: String, val is16Bit: Boolean) {
    NONE("无校验", "无", false),
    CRC16_MODBUS("CRC16-Modbus", "CRC16", true),
    CRC16_CCITT("CRC16-CCITT", "CCITT", true),
    SUM8("Sum8 累加和", "SUM8", false),
    XOR8("XOR8 异或", "XOR8", false),
}

enum class ChecksumEndian(val label: String, val shortLabel: String) {
    LITTLE_ENDIAN("低字节在前 (LO-HI)", "LE"),
    BIG_ENDIAN("高字节在前 (HI-LO)", "BE"),
}

enum class ParityMode(val label: String) {
    NONE("None"),
    ODD("Odd"),
    EVEN("Even"),
    MARK("Mark"),
    SPACE("Space"),
}

enum class FrameDirection {
    RX,
    TX,
    SYS,
}

enum class MacroColorTag(val label: String, val accent: Color) {
    COBALT("钴蓝", CobaltPrimary),
    EMERALD("翡翠绿", RxEmerald),
    AMBER("琥珀橙", TxAmber),
    VIOLET("紫罗兰", Color(0xFF7C3AED)),
}

data class UsbDeviceItem(
    val id: String,
    val chipName: String,
    val driverType: String,
    val vidPid: String,
    val portPath: String,
    val isPermissionGranted: Boolean,
    val productName: String? = null,
    val shortModel: String? = null,
)

data class SerialPortConfig(
    val presetName: String = "Standard Debug 115200",
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: String = "1",
    val parity: ParityMode = ParityMode.NONE,
    val dtrEnabled: Boolean = false,
    val rtsEnabled: Boolean = false,
) {
    val compactSummary: String
        get() {
            val parityChar = parity.label.first()
            return "$baudRate · $dataBits$parityChar$stopBits"
        }
}

data class TerminalFrame(
    val id: Long,
    val timestamp: String,
    val direction: FrameDirection,
    val rawBytes: ByteArray,
    val asciiText: String,
) {
    val hexText: String
        get() = com.example.domain.protocol.ProtocolEngine.formatHex(rawBytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalFrame) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class MacroCommandItem(
    val id: Long,
    val title: String,
    val group: String,
    val payload: String,
    val txFormat: TxFormat,
    val lineEnding: LineEnding,
    val checksumMode: ChecksumMode,
    val checksumEndian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
    val checksumSkipBytes: Int = 0,
    val colorTag: MacroColorTag,
) {
    val hasFixedFrame: Boolean
        get() = txFormat == TxFormat.HEX && lineEnding == LineEnding.NONE && checksumMode == ChecksumMode.NONE
}

sealed interface MacroGroupFilter {
    data object All : MacroGroupFilter
    data object Ungrouped : MacroGroupFilter
    data class Named(val name: String) : MacroGroupFilter

    fun matches(group: String): Boolean = when (this) {
        All -> true
        Ungrouped -> group.isBlank()
        is Named -> group.trim() == name
    }
}

enum class PlotSamplingState { IDLE, RUNNING, PAUSED }

data class PlotSample(val timeMs: Long, val value: Double, val gapBefore: Boolean = false)

data class PlotChannel(
    val index: Int,
    val name: String,
    val unit: String,
    val color: Color,
    val visible: Boolean,
    val samples: List<PlotSample>,
    val key: String = name,
) {
    val latestValue: Double get() = samples.lastOrNull()?.value ?: 0.0
    val minValue: Double get() = samples.minOfOrNull { it.value } ?: 0.0
    val maxValue: Double get() = samples.maxOfOrNull { it.value } ?: 0.0
}

data class SavedLogSummary(
    val id: Long,
    val title: String,
    val deviceSummary: String,
    val startedAtMs: Long,
    val frameCount: Int,
    val sizeLabel: String,
)

sealed interface LogTimeRange {
    data object LastSevenDays : LogTimeRange
    data object Today : LogTimeRange
    data object All : LogTimeRange
    data class Custom(val fromInclusive: Long, val untilExclusive: Long) : LogTimeRange
}

data class SavedLogSession(
    val id: Long,
    val title: String,
    val deviceSummary: String,
    val configSummary: String,
    val timeRange: String,
    val frameCount: Int,
    val sizeLabel: String,
    val frames: List<TerminalFrame>,
    val fullTimeRange: String = timeRange,
)

enum class AppLanguage(val nativeName: String?) {
    SYSTEM(null),
    ZH_CN("简体中文"),
    EN("English"),
    ZH_TW("繁體中文（台灣）"),
    DE("Deutsch"),
    FR("Français"),
    ES("Español (España)"),
    IT("Italiano"),
    PT("Português (Portugal)"),
    JA("日本語"),
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class TxHistoryItem(
    val id: Long,
    val payload: String,
    val txFormat: TxFormat,
    val lineEnding: LineEnding,
    val checksumMode: ChecksumMode,
    val checksumEndian: ChecksumEndian,
    val useCount: Int,
    val lastUsedLabel: String,
    val lastUsedAtMs: Long = 0L,
    val sentHex: String? = null,
)

enum class CharsetEncoding(val label: String) {
    UTF_8("UTF-8"),
    GBK("GBK (中文工控)"),
    ISO_8859_1("ASCII / Latin-1"),
}

enum class HexSeparator(val label: String, val example: String) {
    SPACE("空格分隔", "AA 55 01"),
    NONE("紧凑无空格", "AA5501"),
    PREFIX_0X("0x 前缀", "0xAA 0x55"),
}

data class AppSettingsState(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val charset: CharsetEncoding = CharsetEncoding.UTF_8,
    val terminalFontSizeSp: Float = 12.5f,
    val frameTimeoutMs: Int = 50,
    val maxBufferFrames: Int = 5000,
    val autoConnectOnUsbAttach: Boolean = true,
    val keepScreenOn: Boolean = true,
    val clearInputAfterSend: Boolean = false,
    val vibrateOnError: Boolean = true,
    val plotProfileId: Long? = null,
)

data class FrameChecksumReport(
    val fullModbusLe: String,
    val fullCcittBe: String,
    val fullSum8: String,
    val fullXor8: String,
    val modbusTailMatched: Boolean?,
    val expectedModbusTail: String?,
    val actualTail2: String?,
    val sum8TailMatched: Boolean?,
)

data class FileTransferDraft(
    val uriString: String,
    val fileName: String,
    val totalBytes: Long,
    val chunkSizeBytes: Int = 256,
    val chunkDelayMs: Int = 20,
    val sendLineByLine: Boolean = false,
)

data class FileTransferProgress(
    val fileName: String,
    val sentBytes: Long,
    val totalBytes: Long,
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (sentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

object PreviewChecksumHelper {
    fun computePreviewBytes(
        input: String,
        format: TxFormat,
        lineEnding: LineEnding,
        checksum: ChecksumMode,
        endian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
        skipBytes: Int = 0,
    ): String {
        return com.example.domain.protocol.ProtocolEngine.computePreviewLabel(
            input = input,
            format = format,
            lineEnding = lineEnding,
            checksumMode = checksum,
            endian = endian,
            skipBytes = skipBytes,
        )
    }

    fun inspectFrameChecksums(rawBytes: ByteArray): FrameChecksumReport {
        return com.example.domain.protocol.ProtocolEngine.inspectFrameChecksums(rawBytes)
    }

    fun parsePayloadBytes(input: String, format: TxFormat, lineEnding: LineEnding): ByteArray {
        return com.example.domain.protocol.ProtocolEngine.parsePayloadBytes(
            input = input,
            format = format,
            lineEnding = lineEnding,
        ) ?: ByteArray(0)
    }
}
