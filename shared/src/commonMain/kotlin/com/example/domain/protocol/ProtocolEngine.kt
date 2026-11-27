package com.example.domain.protocol

import com.example.platform.PlatformTextCodec
import com.example.platform.formatHexByte
import com.example.ui.model.CharsetEncoding
import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.FrameChecksumReport
import com.example.ui.model.HexSeparator
import com.example.ui.model.LineEnding
import com.example.ui.model.TxFormat

data class TxPacket(
    val payloadBytes: ByteArray,
    val checksumBytes: ByteArray,
    val fullBytes: ByteArray,
    val checksumHex: String,
)

object ProtocolEngine {

    fun encodeText(text: String, charset: CharsetEncoding): ByteArray {
        return when (charset) {
            CharsetEncoding.UTF_8 -> text.encodeToByteArray()
            CharsetEncoding.GBK -> PlatformTextCodec.encodeGbk(text)
            CharsetEncoding.ISO_8859_1 -> ByteArray(text.length) { i ->
                val code = text[i].code
                if (code <= 0xFF) code.toByte() else '?'.code.toByte()
            }
        }
    }

    fun decodeBytes(bytes: ByteArray, charset: CharsetEncoding): String {
        if (bytes.isEmpty()) return ""
        return when (charset) {
            CharsetEncoding.UTF_8 -> bytes.decodeToString()
            CharsetEncoding.GBK -> PlatformTextCodec.decodeGbk(bytes)
            CharsetEncoding.ISO_8859_1 -> CharArray(bytes.size) { i ->
                (bytes[i].toInt() and 0xFF).toChar()
            }.concatToString()
        }
    }

    fun parseHexString(input: String): ByteArray? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ByteArray(0)

        val tokens = trimmed.replace(",", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        val out = ArrayList<Byte>(tokens.size * 2)
        for (token in tokens) {
            val clean = token.removePrefix("0x").removePrefix("0X")
            if (clean.isEmpty()) return null
            if (clean.length <= 2) {
                val value = clean.toIntOrNull(16) ?: return null
                if (value !in 0..255) return null
                out.add(value.toByte())
            } else {
                if (clean.length % 2 != 0) return null
                var i = 0
                while (i < clean.length) {
                    val byteVal = clean.substring(i, i + 2).toIntOrNull(16) ?: return null
                    out.add(byteVal.toByte())
                    i += 2
                }
            }
        }
        return out.toByteArray()
    }

    fun parsePayloadBytes(
        input: String,
        format: TxFormat,
        lineEnding: LineEnding,
        charset: CharsetEncoding = CharsetEncoding.UTF_8,
    ): ByteArray? {
        return when (format) {
            TxFormat.ASCII -> {
                if (input.isEmpty()) ByteArray(0)
                else encodeText(input + lineEnding.suffix, charset)
            }
            TxFormat.HEX -> parseHexString(input)
        }
    }

    fun buildTxPacket(
        input: String,
        format: TxFormat,
        lineEnding: LineEnding,
        checksumMode: ChecksumMode,
        endian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
        skipBytes: Int = 0,
        charset: CharsetEncoding = CharsetEncoding.UTF_8,
    ): Result<TxPacket> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("发送内容不能为空"))
        }

        val effectiveEnding = if (format == TxFormat.HEX) LineEnding.NONE else lineEnding
        val payloadBytes = parsePayloadBytes(
            input = if (format == TxFormat.HEX) trimmed else input,
            format = format,
            lineEnding = effectiveEnding,
            charset = charset,
        ) ?: return Result.failure(IllegalArgumentException("HEX 格式有误，请输入有效十六进制字节 (如 01 03 00 00)"))

        if (payloadBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("有效发送字节为空"))
        }

        if (format != TxFormat.HEX || checksumMode == ChecksumMode.NONE) {
            return Result.success(
                TxPacket(
                    payloadBytes = payloadBytes,
                    checksumBytes = ByteArray(0),
                    fullBytes = payloadBytes,
                    checksumHex = "",
                ),
            )
        }

        if (skipBytes >= payloadBytes.size) {
            return Result.failure(
                IllegalArgumentException("帧长度 (${payloadBytes.size}B) 小于等于跳过偏移 (${skipBytes}B)"),
            )
        }

        val slice = if (skipBytes > 0) {
            payloadBytes.copyOfRange(skipBytes, payloadBytes.size)
        } else {
            payloadBytes
        }

        val checksumBytes = computeChecksumBytes(slice, checksumMode, endian)
        val checksumHex = formatHex(checksumBytes, HexSeparator.SPACE)

        return Result.success(
            TxPacket(
                payloadBytes = payloadBytes,
                checksumBytes = checksumBytes,
                fullBytes = payloadBytes + checksumBytes,
                checksumHex = checksumHex,
            ),
        )
    }

    fun computePreviewLabel(
        input: String,
        format: TxFormat,
        lineEnding: LineEnding,
        checksumMode: ChecksumMode,
        endian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
        skipBytes: Int = 0,
        charset: CharsetEncoding = CharsetEncoding.UTF_8,
    ): String {
        if (format != TxFormat.HEX || checksumMode == ChecksumMode.NONE || input.isBlank()) return ""
        val effectiveEnding = if (format == TxFormat.HEX) LineEnding.NONE else lineEnding
        val allBytes = parsePayloadBytes(input, format, effectiveEnding, charset) ?: return "格式无效"
        if (allBytes.isEmpty()) return "格式无效"
        if (skipBytes >= allBytes.size) return "长度不足"
        val slice = if (skipBytes > 0) allBytes.copyOfRange(skipBytes, allBytes.size) else allBytes
        return formatHex(computeChecksumBytes(slice, checksumMode, endian), HexSeparator.SPACE)
    }

    fun computeChecksumBytes(
        bytes: ByteArray,
        checksum: ChecksumMode,
        endian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
    ): ByteArray {
        return when (checksum) {
            ChecksumMode.NONE -> ByteArray(0)
            ChecksumMode.SUM8 -> {
                val sum = bytes.fold(0) { acc, b -> (acc + (b.toInt() and 0xFF)) and 0xFF }
                byteArrayOf(sum.toByte())
            }
            ChecksumMode.XOR8 -> {
                val xor = bytes.fold(0) { acc, b -> acc xor (b.toInt() and 0xFF) }
                byteArrayOf(xor.toByte())
            }
            ChecksumMode.CRC16_MODBUS -> encode16Bit(crc16Modbus(bytes), endian)
            ChecksumMode.CRC16_CCITT -> encode16Bit(crc16Ccitt(bytes), endian)
        }
    }

    fun formatHex(bytes: ByteArray, separator: HexSeparator = HexSeparator.SPACE): String {
        if (bytes.isEmpty()) return ""
        return when (separator) {
            HexSeparator.SPACE -> bytes.joinToString(" ") { formatHexByte(it) }
            HexSeparator.NONE -> bytes.joinToString("") { formatHexByte(it) }
            HexSeparator.PREFIX_0X -> bytes.joinToString(" ") { "0x${formatHexByte(it)}" }
        }
    }

    fun formatHexInputLive(raw: String): String {
        val cleaned = raw
            .replace(Regex("0[xX]"), "")
            .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        if (cleaned.isEmpty()) return ""
        return cleaned.chunked(2).joinToString(" ")
    }

    fun decodeRxText(bytes: ByteArray, charset: CharsetEncoding): String {
        if (bytes.isEmpty()) return ""
        val decoded = decodeBytes(bytes, charset)
        val sb = StringBuilder(decoded.length)
        for (ch in decoded) {
            when {
                ch == '\r' || ch == '\n' || ch == '\t' -> sb.append(ch)
                ch.code < 0x20 || ch.code == 0x7F -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString().trimEnd('\r', '\n')
    }

    fun inspectFrameChecksums(rawBytes: ByteArray): FrameChecksumReport {
        val fullModbus = formatHex(computeChecksumBytes(rawBytes, ChecksumMode.CRC16_MODBUS, ChecksumEndian.LITTLE_ENDIAN))
        val fullCcitt = formatHex(computeChecksumBytes(rawBytes, ChecksumMode.CRC16_CCITT, ChecksumEndian.BIG_ENDIAN))
        val fullSum8 = formatHex(computeChecksumBytes(rawBytes, ChecksumMode.SUM8))
        val fullXor8 = formatHex(computeChecksumBytes(rawBytes, ChecksumMode.XOR8))

        val modbusTailMatched: Boolean?
        val expectedModbusTail: String?
        val actualTail2: String?

        if (rawBytes.size >= 3) {
            val payloadWithoutTail2 = rawBytes.copyOfRange(0, rawBytes.size - 2)
            val expected = formatHex(
                computeChecksumBytes(payloadWithoutTail2, ChecksumMode.CRC16_MODBUS, ChecksumEndian.LITTLE_ENDIAN),
            )
            val actual = "${formatHexByte(rawBytes[rawBytes.size - 2])} ${formatHexByte(rawBytes[rawBytes.size - 1])}"
            expectedModbusTail = expected
            actualTail2 = actual
            modbusTailMatched = (expected == actual)
        } else {
            expectedModbusTail = null
            actualTail2 = null
            modbusTailMatched = null
        }

        val sum8TailMatched = if (rawBytes.size >= 2) {
            val payloadWithoutTail1 = rawBytes.copyOfRange(0, rawBytes.size - 1)
            val expectedSum = formatHex(computeChecksumBytes(payloadWithoutTail1, ChecksumMode.SUM8))
            val actualSum = formatHexByte(rawBytes.last())
            expectedSum == actualSum
        } else {
            null
        }

        return FrameChecksumReport(
            fullModbusLe = fullModbus,
            fullCcittBe = fullCcitt,
            fullSum8 = fullSum8,
            fullXor8 = fullXor8,
            modbusTailMatched = modbusTailMatched,
            expectedModbusTail = expectedModbusTail,
            actualTail2 = actualTail2,
            sum8TailMatched = sum8TailMatched,
        )
    }

    fun crc16Modbus(bytes: ByteArray): Int {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xFFFF
    }

    fun crc16Ccitt(bytes: ByteArray): Int {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x1021) and 0xFFFF else (crc shl 1) and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    private fun encode16Bit(value: Int, endian: ChecksumEndian): ByteArray {
        val lo = (value and 0xFF).toByte()
        val hi = ((value ushr 8) and 0xFF).toByte()
        return if (endian == ChecksumEndian.LITTLE_ENDIAN) {
            byteArrayOf(lo, hi)
        } else {
            byteArrayOf(hi, lo)
        }
    }
}
