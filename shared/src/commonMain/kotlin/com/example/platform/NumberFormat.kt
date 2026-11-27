package com.example.platform

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

private const val HEX_DIGITS = "0123456789ABCDEF"

fun formatHexByte(value: Byte): String {
    val v = value.toInt() and 0xFF
    return charArrayOf(HEX_DIGITS[v ushr 4], HEX_DIGITS[v and 0x0F]).concatToString()
}

fun formatHexInt(value: Int, width: Int): String {
    val chars = CharArray(width)
    var current = value
    for (i in width - 1 downTo 0) {
        chars[i] = HEX_DIGITS[current and 0x0F]
        current = current ushr 4
    }
    return chars.concatToString()
}

fun formatDecimal(value: Double, decimals: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value < 0) "-Inf" else "Inf"
    if (decimals <= 0) return value.roundToLong().toString()

    val negative = value < 0.0
    val scale = 10.0.pow(decimals)
    val scaled = abs(value * scale).roundToLong()
    val factor = scale.toLong()
    val intPart = scaled / factor
    val fracPart = (scaled % factor).toString().padStart(decimals, '0')
    val sign = if (negative && scaled != 0L) "-" else ""
    return "$sign$intPart.$fracPart"
}

fun formatDecimal(value: Float, decimals: Int): String = formatDecimal(value.toDouble(), decimals)

fun formatByteSizeCompact(bytes: Long): String {
    return when {
        bytes < 1024L -> "${bytes}B"
        bytes < 1024L * 1024L -> "${formatDecimal(bytes / 1024.0, 1)}K"
        else -> "${formatDecimal(bytes / (1024.0 * 1024.0), 2)}M"
    }
}

fun formatByteSizeLabel(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${formatDecimal(bytes / 1024.0, 1)} KB"
        else -> "${formatDecimal(bytes / (1024.0 * 1024.0), 2)} MB"
    }
}
