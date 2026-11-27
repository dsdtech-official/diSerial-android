package com.example.domain.protocol

import com.example.ui.model.CharsetEncoding
import kotlinx.serialization.Serializable

@Serializable
enum class PlotFormat { COLUMNS, KEY_VALUE, BINARY }

@Serializable
enum class PlotDelimiter(val character: Char) { COMMA(','), SPACE(' '), TAB('\t'), SEMICOLON(';') }

@Serializable
enum class PlotBoundary { LINE, FRAME }

@Serializable
enum class PlotNumberType(val width: Int) {
    INT8(1), UINT8(1), INT16(2), UINT16(2), INT32(4), UINT32(4), FLOAT32(4),
}

@Serializable
enum class PlotByteOrder { LITTLE_ENDIAN, BIG_ENDIAN }

@Serializable
data class PlotField(
    val name: String,
    val unit: String = "",
    val column: Int = 1,
    val key: String = "",
    val offset: Int = 0,
    val numberType: PlotNumberType = PlotNumberType.UINT16,
    val byteOrder: PlotByteOrder = PlotByteOrder.LITTLE_ENDIAN,
    val scale: Double = 1.0,
    val bias: Double = 0.0,
)

@Serializable
data class PlotConfiguration(
    val name: String = "",
    val format: PlotFormat = PlotFormat.COLUMNS,
    val boundary: PlotBoundary = PlotBoundary.LINE,
    val delimiter: PlotDelimiter = PlotDelimiter.COMMA,
    val keySeparator: String = "=",
    val header: String = "",
    val frameLength: Int? = null,
    val trailer: String = "",
    val fields: List<PlotField> = emptyList(),
) {
    fun isValid(): Boolean {
        if (name.isBlank() || fields.size !in 1..4 ||
            fields.any { it.name.isBlank() || !it.scale.isFinite() || !it.bias.isFinite() } ||
            fields.map { it.name.trim().lowercase() }.distinct().size != fields.size
        ) return false
        return when (format) {
            PlotFormat.COLUMNS -> fields.all { it.column in 1..MAX_PLOT_RECORD }
            PlotFormat.KEY_VALUE -> keySeparator in listOf("=", ":") &&
                fields.all { it.key.isNotBlank() && it.key.none { char -> char !in ' '..'~' } }
            PlotFormat.BINARY -> {
                val head = parsePlotHex(header, 16) ?: return false
                val tail = parsePlotHex(trailer, MAX_PLOT_RECORD) ?: return false
                val bounds = listOf(head.isNotEmpty(), frameLength != null, tail.isNotEmpty())
                val maxLength = frameLength ?: MAX_PLOT_RECORD
                bounds.count { it } >= 2 &&
                    maxLength in 1..MAX_PLOT_RECORD && head.size + tail.size <= maxLength &&
                    fields.all { it.offset in 0..(maxLength - it.numberType.width) }
            }
        }
    }
}

const val BUILTIN_PLOT_COLUMNS = -1L
const val BUILTIN_PLOT_KEYS = -2L
private const val MAX_PLOT_RECORD = 4096
const val MAX_PLOT_SAMPLE_TEXT = 24_576

data class PlotProfile(val id: Long, val configuration: PlotConfiguration) {
    val isBuiltIn: Boolean get() = id == BUILTIN_PLOT_COLUMNS || id == BUILTIN_PLOT_KEYS
}

val builtInPlotProfiles = listOf(
    PlotProfile(BUILTIN_PLOT_COLUMNS, PlotConfiguration(format = PlotFormat.COLUMNS, boundary = PlotBoundary.FRAME)),
    PlotProfile(BUILTIN_PLOT_KEYS, PlotConfiguration(format = PlotFormat.KEY_VALUE, boundary = PlotBoundary.FRAME)),
)

data class PlotValue(val key: String, val name: String, val value: Double, val unit: String = "")

data class PlotRecord(val values: List<PlotValue>, val gapBefore: Boolean = false)

fun parsePlotHex(text: String, limit: Int = MAX_PLOT_RECORD): ByteArray? {
    if (text.isBlank()) return byteArrayOf()
    if (text.length > MAX_PLOT_SAMPLE_TEXT) return null
    val tokens = text.trim().split(Regex("\\s+"))
    if (tokens.size > limit) return null
    val bytes = ByteArray(tokens.size)
    for ((index, token) in tokens.withIndex()) {
        if (token.length != 2 || token.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) return null
        bytes[index] = (token.toIntOrNull(16) ?: return null).toByte()
    }
    return bytes
}

private val legacyKey = Regex("([A-Za-z0-9_\\u4e00-\\u9fa5]+)\\s*[:=]\\s*([-+]?\\d+(?:\\.\\d+)?)")
private val strictNumber = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")

/** Consumes one ordered RX stream. A new instance starts a new configuration or connection. */
class PlotStreamParser(private val profile: PlotProfile) {
    private val configuration = profile.configuration
    private val text = ByteArray(MAX_PLOT_RECORD)
    private var length = 0
    private var invalid = false
    private var sawCr = false
    private var gap = false
    private val binary = if (configuration.format == PlotFormat.BINARY) BinaryPlotStream(configuration) else null

    fun feed(bytes: ByteArray, charset: CharsetEncoding = CharsetEncoding.UTF_8, emit: (PlotRecord) -> Unit) {
        val binaryParser = binary
        when {
            binaryParser != null -> binaryParser.feed(bytes, emit)
            profile.isBuiltIn -> ProtocolEngine.decodeRxText(bytes, charset).split('\r', '\n').forEach { line ->
                if (line.isNotBlank()) accept(parseBuiltIn(line.trim()), emit)
            }
            configuration.boundary == PlotBoundary.FRAME -> {
                var end = bytes.size
                if (end > 0 && bytes[end - 1] == 10.toByte()) {
                    end--
                    if (end > 0 && bytes[end - 1] == 13.toByte()) end--
                }
                if (end > MAX_PLOT_RECORD || (0 until end).any { !asciiByte(bytes[it].toInt()) }) {
                    gap = true
                } else {
                    complete(bytes.decodeToString(0, end), emit)
                }
            }
            else -> bytes.forEach { byte ->
                val value = byte.toInt()
                if (value == 10) {
                    if (invalid) gap = true else complete(text.decodeToString(0, length), emit)
                    length = 0
                    invalid = false
                    sawCr = false
                } else {
                    if (sawCr) invalid = true
                    if (value == 13) sawCr = true
                    else if (!asciiByte(value) || length == MAX_PLOT_RECORD) invalid = true
                    else if (!invalid) text[length++] = byte
                }
            }
        }
    }

    private fun complete(line: String, emit: (PlotRecord) -> Unit) {
        if (line.isBlank()) return
        val columns = split(line, configuration.delimiter)
        val pairs = if (configuration.format == PlotFormat.KEY_VALUE) parsePairs(columns) else emptyMap()
        if (pairs == null) { gap = true; return }
        val values = configuration.fields.mapIndexed { index, field ->
            val token = when (configuration.format) {
                PlotFormat.COLUMNS -> columns.getOrNull(field.column - 1)
                PlotFormat.KEY_VALUE -> pairs[field.key]
                PlotFormat.BINARY -> null
            }?.trim()
            val number = token?.let(::parseTextNumber)
            val value = number?.let { converted(it, field) }
            if (value == null) { gap = true; return }
            PlotValue(index.toString(), field.name, value, field.unit)
        }
        accept(values, emit)
    }

    private fun parsePairs(columns: List<String>): Map<String, String>? {
        val pairs = mutableMapOf<String, String>()
        for (column in columns) {
            val part = column.indexOf(configuration.keySeparator)
            if (part < 0) return null
            val key = column.substring(0, part).trim()
            if (key.isEmpty() || pairs.containsKey(key)) return null
            pairs[key] = column.substring(part + 1)
        }
        return pairs
    }

    private fun parseBuiltIn(line: String): List<PlotValue> {
        if (profile.id == BUILTIN_PLOT_KEYS) {
            return legacyKey.findAll(line).mapNotNull {
                val name = it.groupValues[1]
                val value = it.groupValues[2].toDoubleOrNull()?.takeIf(Double::isFinite)
                value?.let { parsed -> PlotValue(name.lowercase(), name, parsed) }
            }.distinctBy { it.key }.take(8).toList()
        }
        val prefix = line.substringBefore(':')
        val possiblePayload = line.substringAfter(':', "").trimStart()
        val payload = if (',' !in prefix && possiblePayload.firstOrNull()?.let {
                it.isDigit() || it == '-' || it == '+'
            } == true
        ) possiblePayload else line
        val tokens = payload.split(',', ';', '\t', ' ').filter(String::isNotBlank)
        val numbers = tokens.map { token ->
            token.takeIf { strictNumber.matches(it) }?.toDoubleOrNull()?.takeIf(Double::isFinite)
                ?: return emptyList()
        }
        return numbers.take(8).mapIndexed { index, value ->
            val name = "CH${index + 1}"
            PlotValue(name, name, value)
        }
    }

    private fun accept(values: List<PlotValue>, emit: (PlotRecord) -> Unit) {
        if (values.isEmpty()) { gap = true; return }
        emit(PlotRecord(values, gap))
        gap = false
    }

    private fun split(line: String, delimiter: PlotDelimiter): List<String> =
        if (delimiter == PlotDelimiter.SPACE) line.split(' ', '\t').filter(String::isNotEmpty)
        else line.split(delimiter.character)

    private fun asciiByte(value: Int): Boolean = value == 9 || value in 32..126
}

private fun converted(number: Double, field: PlotField): Double? =
    (number * field.scale + field.bias).takeIf { number.isFinite() && it.isFinite() }

private fun parseTextNumber(token: String): Double? {
    if (!strictNumber.matches(token)) return null
    if (token.none { it == '.' || it == 'e' || it == 'E' }) {
        val integer = token.toLongOrNull() ?: return null
        if (integer !in -9_007_199_254_740_991..9_007_199_254_740_991) return null
    }
    return token.toDoubleOrNull()
}

private class BinaryPlotStream(private val configuration: PlotConfiguration) {
    private val header = requireNotNull(parsePlotHex(configuration.header, 16))
    private val trailer = requireNotNull(parsePlotHex(configuration.trailer))
    private val buffer = ByteArray(MAX_PLOT_RECORD)
    private var start = 0
    private var count = 0
    private var gap = false

    fun feed(bytes: ByteArray, emit: (PlotRecord) -> Unit) {
        for (byte in bytes) {
            if (count == buffer.size) {
                drop((count - (header.size - 1).coerceAtLeast(0)).coerceAtLeast(1))
            }
            buffer[(start + count) % buffer.size] = byte
            count++
            while (count > 0 && !prefixMatches()) drop(1)
            if (count < header.size) continue

            val length = configuration.frameLength
            val expected = length ?: count
            if (count < expected || count < header.size + trailer.size) continue
            if (trailer.indices.any { peek(expected - trailer.size + it) != trailer[it] }) {
                if (length != null) drop(1)
                continue
            }
            val values = readFields()
            if (values != null) emit(PlotRecord(values, gap))
            gap = values == null
            start = 0
            count = 0
        }
    }

    private fun readFields(): List<PlotValue>? = configuration.fields.mapIndexed { index, field ->
        val type = field.numberType
        if (field.offset + type.width > count) return null
        var bits = 0L
        for (position in 0 until type.width) {
            val from = if (field.byteOrder == PlotByteOrder.LITTLE_ENDIAN) position else type.width - 1 - position
            bits = bits or ((peek(field.offset + from).toLong() and 0xffL) shl (position * 8))
        }
        val raw = when (type) {
            PlotNumberType.INT8 -> bits.toByte().toDouble()
            PlotNumberType.INT16 -> bits.toShort().toDouble()
            PlotNumberType.INT32 -> bits.toInt().toDouble()
            PlotNumberType.FLOAT32 -> Float.fromBits(bits.toInt()).toDouble()
            else -> bits.toDouble()
        }
        val value = converted(raw, field) ?: return null
        PlotValue(index.toString(), field.name, value, field.unit)
    }

    private fun prefixMatches(): Boolean = (0 until minOf(count, header.size)).all { peek(it) == header[it] }

    private fun peek(index: Int): Byte = buffer[(start + index) % buffer.size]

    private fun drop(amount: Int) {
        start = (start + amount) % buffer.size
        count -= amount
        gap = true
    }
}

fun previewPlot(configuration: PlotConfiguration, sample: String): PlotRecord? {
    if (!configuration.isValid() || sample.length > MAX_PLOT_SAMPLE_TEXT) return null
    val parser = PlotStreamParser(PlotProfile(0, configuration))
    var first: PlotRecord? = null
    val emit: (PlotRecord) -> Unit = { if (first == null) first = it }
    when {
        configuration.format == PlotFormat.BINARY -> parser.feed(parsePlotHex(sample) ?: return null, emit = emit)
        configuration.boundary == PlotBoundary.FRAME -> sample.lineSequence().forEach {
            parser.feed(it.encodeToByteArray(), emit = emit)
        }
        else -> parser.feed((sample + "\n").encodeToByteArray(), emit = emit)
    }
    return first
}
