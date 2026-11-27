package com.example.data.local

import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ZhCnAppStrings
import com.example.ui.model.SavedLogSession

enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    CSV("csv", "text/csv"),
}

object LogExporter {

    fun buildSuggestedFileName(session: SavedLogSession, format: ExportFormat): String {
        val safeTitle = session.title
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .ifEmpty { "serial_log" }
        return "diSerial_${safeTitle}.${format.extension}"
    }

    fun formatContent(
        session: SavedLogSession,
        format: ExportFormat,
        strings: AppStrings = ZhCnAppStrings,
    ): String {
        return when (format) {
            ExportFormat.TXT -> buildString {
                appendLine(strings.exportTxtHeaderTitle)
                appendLine("${strings.exportTxtSession}: ${session.title}")
                appendLine("${strings.exportTxtDevice}: ${session.deviceSummary}")
                appendLine("${strings.exportTxtConfig}: ${session.configSummary}")
                appendLine("${strings.exportTxtTime}: ${session.timeRange}")
                appendLine("${strings.exportTxtFrames}: ${session.frameCount} | ${strings.exportTxtSize}: ${session.sizeLabel}")
                appendLine("------------------------------------------------------------")
                session.frames.forEach { frame ->
                    val hex = frame.hexText
                    appendLine("[${frame.timestamp}] [${frame.direction.name}] (${frame.rawBytes.size}B)")
                    if (hex.isNotEmpty()) appendLine("  HEX : $hex")
                    if (frame.asciiText.isNotEmpty()) appendLine("  TEXT: ${frame.asciiText}")
                }
            }

            ExportFormat.CSV -> buildString {
                append('\uFEFF')
                appendLine("Seq,Timestamp,Direction,ByteLength,HEX,Text")
                session.frames.forEachIndexed { idx, frame ->
                    val hex = frame.hexText
                    append(idx + 1).append(',')
                    append(csvEscape(frame.timestamp)).append(',')
                    append(csvEscape(frame.direction.name)).append(',')
                    append(frame.rawBytes.size).append(',')
                    append(csvEscape(hex)).append(',')
                    appendLine(csvEscape(frame.asciiText))
                }
            }
        }
    }

    fun buildEmailSubject(
        session: SavedLogSession,
        strings: AppStrings = ZhCnAppStrings,
    ): String {
        return strings.emailSubject(session.title)
    }

    fun buildEmailBody(
        session: SavedLogSession,
        format: ExportFormat,
        strings: AppStrings = ZhCnAppStrings,
    ): String {
        return strings.emailBody(
            session.title,
            session.deviceSummary,
            session.configSummary,
            session.timeRange,
            session.frameCount,
            session.sizeLabel,
            format.extension.uppercase(),
        )
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
