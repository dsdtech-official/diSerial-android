package com.example.domain.protocol

import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.LineEnding
import com.example.ui.model.MacroColorTag
import com.example.ui.model.MacroCommandItem
import com.example.ui.model.TxFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class MacroJsonEntry(
    val title: String = "",
    val group: String = "",
    val payload: String = "",
    val txFormat: String = TxFormat.HEX.name,
    val lineEnding: String = LineEnding.NONE.name,
    val checksumMode: String = ChecksumMode.NONE.name,
    val checksumEndian: String = ChecksumEndian.LITTLE_ENDIAN.name,
    val checksumSkipBytes: Int = 0,
    val colorTag: String = MacroColorTag.COBALT.name,
)

@Serializable
private data class MacroJsonEnvelope(
    val app: String = "diSerial",
    val version: Int = 1,
    val count: Int = 0,
    val macros: List<MacroJsonEntry> = emptyList(),
)

object MacroJsonCodec {

    private const val SCHEMA_VERSION = 1

    private val jsonCodec = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeToJson(macros: List<MacroCommandItem>): String {
        val envelope = MacroJsonEnvelope(
            app = "diSerial",
            version = SCHEMA_VERSION,
            count = macros.size,
            macros = macros.map { macro ->
                MacroJsonEntry(
                    title = macro.title,
                    group = macro.group.takeUnless { it.isBlank() }.orEmpty(),
                    payload = macro.payload,
                    txFormat = macro.txFormat.name,
                    lineEnding = macro.lineEnding.name,
                    checksumMode = macro.checksumMode.name,
                    checksumEndian = macro.checksumEndian.name,
                    checksumSkipBytes = macro.checksumSkipBytes,
                    colorTag = macro.colorTag.name,
                )
            },
        )
        return jsonCodec.encodeToString(envelope)
    }

    fun decodeFromJson(rawJson: String): Result<List<MacroCommandItem>> = runCatching {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "宏配置文件为空" }

        val element = jsonCodec.parseToJsonElement(trimmed)
        val array: JsonArray = when (element) {
            is JsonObject -> {
                element["macros"]?.jsonArray
                    ?: throw IllegalArgumentException("JSON 中缺少 macros 字段")
            }
            is JsonArray -> element
            else -> throw IllegalArgumentException("无效的宏指令 JSON 格式")
        }

        val parsed = mutableListOf<MacroCommandItem>()
        for (i in array.indices) {
            val obj = runCatching { array[i].jsonObject }.getOrNull() ?: continue
            val payload = obj["payload"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (payload.isEmpty()) continue
            val title = obj["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                .ifEmpty { "导入指令 ${i + 1}" }
            val group = obj["group"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val txFormatRaw = obj["txFormat"]?.jsonPrimitive?.contentOrNull
            val txFormat = TxFormat.entries.firstOrNull {
                it.name.equals(txFormatRaw, ignoreCase = true)
            } ?: TxFormat.HEX
            val lineEndingRaw = obj["lineEnding"]?.jsonPrimitive?.contentOrNull
            val lineEnding = LineEnding.entries.firstOrNull {
                it.name.equals(lineEndingRaw, ignoreCase = true)
            } ?: LineEnding.NONE
            val checksumModeRaw = obj["checksumMode"]?.jsonPrimitive?.contentOrNull
            val checksumMode = ChecksumMode.entries.firstOrNull {
                it.name.equals(checksumModeRaw, ignoreCase = true)
            } ?: ChecksumMode.NONE
            val checksumEndianRaw = obj["checksumEndian"]?.jsonPrimitive?.contentOrNull
            val checksumEndian = ChecksumEndian.entries.firstOrNull {
                it.name.equals(checksumEndianRaw, ignoreCase = true)
            } ?: ChecksumEndian.LITTLE_ENDIAN
            val skipBytes = (obj["checksumSkipBytes"]?.jsonPrimitive?.intOrNull ?: 0).coerceIn(0, 3)
            val colorTagRaw = obj["colorTag"]?.jsonPrimitive?.contentOrNull
            val colorTag = MacroColorTag.entries.firstOrNull {
                it.name.equals(colorTagRaw, ignoreCase = true)
            } ?: MacroColorTag.COBALT

            parsed.add(
                MacroCommandItem(
                    id = 0L,
                    title = title,
                    group = group,
                    payload = payload,
                    txFormat = txFormat,
                    lineEnding = lineEnding,
                    checksumMode = checksumMode,
                    checksumEndian = checksumEndian,
                    checksumSkipBytes = skipBytes,
                    colorTag = colorTag,
                ),
            )
        }
        require(parsed.isNotEmpty()) { "未解析到有效的快捷宏指令" }
        parsed
    }
}
