package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ZhCnAppStrings
import com.example.ui.model.SavedLogSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun LogExporter.writeToUri(
    context: Context,
    uri: Uri,
    session: SavedLogSession,
    format: ExportFormat,
    strings: AppStrings = ZhCnAppStrings,
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val content = formatContent(session, format, strings)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error(strings.exportOpenStreamError)
    }
}

suspend fun LogExporter.createShareUri(
    context: Context,
    session: SavedLogSession,
    format: ExportFormat = ExportFormat.TXT,
    strings: AppStrings = ZhCnAppStrings,
): Result<Uri> = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile) oldFile.delete()
        }
        val file = File(dir, buildSuggestedFileName(session, format))
        file.writeText(formatContent(session, format, strings), Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
