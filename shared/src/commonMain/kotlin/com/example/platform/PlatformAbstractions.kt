package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

expect object PlatformClock {
    fun currentTimeMillis(): Long
    fun formatTimestampHmsMillis(epochMs: Long): String
    fun formatMonthDayTime(epochMs: Long): String
    fun formatMonthDayTimeSec(epochMs: Long): String
    fun formatHourMinute(epochMs: Long): String
    fun formatFileStamp(epochMs: Long): String
    fun formatLogDateTime(epochMs: Long): String
    fun parseLogDateTime(text: String): Long?
    fun startOfLogDay(epochMs: Long): Long
}

expect object PlatformTextCodec {
    fun encodeGbk(text: String): ByteArray
    fun decodeGbk(bytes: ByteArray): String
}

@Composable
expect fun rememberSystemLanguageTag(): String

internal expect val editorDialogProperties: DialogProperties

data class PlatformFileHandle(
    val rawUri: String,
    val displayName: String = "",
    val sizeBytes: Long = -1L,
)

interface PlatformFileGateway {
    suspend fun inspectFile(handle: PlatformFileHandle): PlatformFileHandle
    suspend fun readFileBytes(handle: PlatformFileHandle): Result<ByteArray>
    suspend fun readText(handle: PlatformFileHandle): Result<String>
    suspend fun writeText(handle: PlatformFileHandle, content: String): Result<Unit>
    suspend fun createShareTarget(fileName: String, content: String): Result<String>
}

interface PlatformFeedback {
    fun triggerErrorHaptic()
}
