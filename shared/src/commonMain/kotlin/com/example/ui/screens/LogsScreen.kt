package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExportFormat
import com.example.platform.PlatformClock
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.DisplayMode
import com.example.ui.model.LogTimeRange
import com.example.ui.model.SavedLogSession
import com.example.ui.model.SavedLogSummary
import com.example.ui.model.TerminalFrame
import com.example.ui.state.SerialUiState
import com.example.ui.theme.instrumentColors
import kotlinx.coroutines.flow.first

private data class LogEmailTarget(val id: Long, val title: String, val frameCount: Int, val sizeLabel: String)

@Composable
fun LogsScreen(
    state: SerialUiState,
    onTimeRangeChange: (LogTimeRange) -> Unit,
    onLoadMore: () -> Unit,
    onToggleRecordNow: () -> Unit,
    onInspectSession: (SavedLogSummary?) -> Unit,
    onExportSession: (ExportFormat?, Long) -> Unit,
    onEmailSession: (ExportFormat, Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var emailTarget by remember { mutableStateOf<LogEmailTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogTimeRangePicker(state.logTimeRange, onTimeRangeChange, Modifier.weight(1f))
            Button(
                onClick = onToggleRecordNow,
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRecordingSession) c.alertCrimson else c.cobalt,
                    contentColor = if (state.isRecordingSession) MaterialTheme.colorScheme.onError
                    else MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("logs_capture_button"),
            ) {
                Text(if (state.isRecordingSession) s.termSaveRecording else s.logsStartRecordBtn)
            }
        }
        HorizontalDivider(color = c.border)

        if (state.savedLogs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                when {
                    state.logsLoading -> CircularProgressIndicator()
                    state.logsLoadFailed -> LogRetry(onLoadMore)
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val all = state.logTimeRange == LogTimeRange.All
                        Text(if (all) s.logsEmptyTitle else s.logsEmptyRange, style = MaterialTheme.typography.titleMedium)
                        if (all) {
                            Text(s.logsEmptySubtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                        } else {
                            TextButton(onClick = { onTimeRangeChange(LogTimeRange.All) }) { Text(s.logsAllTime) }
                        }
                    }
                }
            }
        } else {
            key(state.logTimeRange) {
                val listState = rememberLazyListState()
                val loadMore by rememberUpdatedState(onLoadMore)
                LaunchedEffect(state.savedLogs.size, state.logsLoading, state.logsLoadFailed, state.logsHaveMore) {
                    if (!state.logsLoading && !state.logsLoadFailed && state.logsHaveMore) {
                        snapshotFlow {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            lastVisible >= (state.savedLogs.size - 6).coerceAtLeast(0)
                        }.first { it }
                        loadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("saved_logs_list"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(state.savedLogs, key = { it.id }) { session ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                modifier = Modifier.weight(1f)
                                    .clickable { onInspectSession(session) }
                                    .defaultMinSize(minHeight = 60.dp)
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .testTag("inspect_log_${session.id}"),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(session.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${session.deviceSummary.substringBefore(" (")} · ${s.logsFramesCount(session.frameCount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            LogSessionMenu(
                                tag = "log_more_${session.id}",
                                onExport = { onExportSession(it, session.id) },
                                onEmail = { emailTarget = LogEmailTarget(session.id, session.title, session.frameCount, session.sizeLabel) },
                                onDelete = { deleteTarget = session.id to session.title },
                            )
                        }
                        HorizontalDivider(color = c.border)
                    }
                    if (state.logsLoading || state.logsLoadFailed) {
                        item(key = "log_page_status") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                if (state.logsLoading) CircularProgressIndicator(Modifier.size(24.dp)) else LogRetry(onLoadMore)
                            }
                        }
                    }
                }
            }
        }
    }

    state.logBeingOpened?.let { session ->
        AlertDialog(
            onDismissRequest = { onInspectSession(null) },
            title = { Text(session.title) },
            text = { CircularProgressIndicator() },
            confirmButton = { TextButton(onClick = { onInspectSession(null) }) { Text(s.commonCancel) } },
        )
    }
    state.inspectingLog?.let { session ->
        LogSessionViewerSheet(
            session = session,
            highlightKeyword = state.highlightKeyword,
            onExport = { onExportSession(it, session.id) },
            onEmail = { emailTarget = LogEmailTarget(session.id, session.title, session.frameCount, session.sizeLabel) },
            onDelete = { deleteTarget = session.id to session.title },
            onDismiss = { onInspectSession(null) },
        )
    }
    emailTarget?.let { session ->
        EmailLogConfirmDialog(
            session = session,
            onConfirm = { format ->
                emailTarget = null
                onEmailSession(format, session.id)
            },
            onDismiss = { emailTarget = null },
        )
    }
    deleteTarget?.let { (id, title) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = c.surface,
            title = { Text(s.logsDeleteConfirmTitle) },
            text = { Text(s.logsDeleteConfirmBody(title)) },
            confirmButton = {
                Button(
                    onClick = { deleteTarget = null; onDeleteSession(id) },
                    colors = ButtonDefaults.buttonColors(containerColor = c.alertCrimson, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.testTag("confirm_delete_log_button"),
                ) { Text(s.logsDeleteConfirmBtn) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.commonCancel) } },
        )
    }
}

@Composable
private fun LogRetry(onRetry: () -> Unit) {
    val s = LocalAppStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.logsLoadFailed)
        TextButton(onClick = onRetry) { Text(s.logsRetry) }
    }
}

private fun LogTimeRange.label(strings: AppStrings): String = when (this) {
    LogTimeRange.LastSevenDays -> strings.logsLastSevenDays
    LogTimeRange.Today -> strings.logsToday
    LogTimeRange.All -> strings.logsAllTime
    is LogTimeRange.Custom -> {
        val from = PlatformClock.formatLogDateTime(fromInclusive)
        val until = PlatformClock.formatLogDateTime(untilExclusive - 1)
        "$from – $until"
    }
}

@Composable
private fun LogTimeRangePicker(range: LogTimeRange, onChange: (LogTimeRange) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("log_time_range"),
        ) {
            Text(range.label(s), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "recent" to LogTimeRange.LastSevenDays,
                "today" to LogTimeRange.Today,
                "all" to LogTimeRange.All,
            ).forEach { (tag, preset) ->
                DropdownMenuItem(
                    text = { Text(preset.label(s)) },
                    onClick = { expanded = false; onChange(preset) },
                    modifier = Modifier.testTag("log_range_$tag"),
                )
            }
            DropdownMenuItem(
                text = { Text(s.logsCustomRange) },
                onClick = { expanded = false; editing = true },
                modifier = Modifier.testTag("log_range_custom"),
            )
        }
    }
    if (editing) {
        CustomLogRangeDialog(
            current = range as? LogTimeRange.Custom,
            onConfirm = { editing = false; onChange(it) },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun CustomLogRangeDialog(
    current: LogTimeRange.Custom?,
    onConfirm: (LogTimeRange.Custom) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val today = remember { PlatformClock.formatLogDateTime(PlatformClock.currentTimeMillis()).substringBefore(' ') }
    var startText by rememberSaveable { mutableStateOf(current?.let { PlatformClock.formatLogDateTime(it.fromInclusive) } ?: today) }
    var endText by rememberSaveable { mutableStateOf(current?.let { PlatformClock.formatLogDateTime(it.untilExclusive - 1) } ?: today) }
    val start = remember(startText) { parseLogRangeInput(startText, "00:00") }
    val end = remember(endText) { parseLogRangeInput(endText, "23:59") }
    val valid = start != null && end != null && start <= end

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.logsCustomRange) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.logsRangeInputHint, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text(s.logsRangeStart) },
                    isError = start == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("log_range_start"),
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text(s.logsRangeEnd) },
                    isError = end == null || (start != null && start > end),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("log_range_end"),
                )
                if (!valid) Text(s.logsRangeInvalid, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (start != null && end != null) onConfirm(LogTimeRange.Custom(start, end + 60_000L)) },
                enabled = valid,
                modifier = Modifier.testTag("apply_log_range"),
            ) { Text(s.logsApplyRange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.commonCancel) } },
    )
}

private fun parseLogRangeInput(text: String, fallbackTime: String): Long? {
    val normalized = text.trim()
    val dateTime = if (normalized.length == 10) "$normalized $fallbackTime" else normalized
    return PlatformClock.parseLogDateTime(dateTime)
}

@Composable
private fun LogSessionMenu(
    tag: String,
    onExport: (ExportFormat?) -> Unit,
    onEmail: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(tag)) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = s.logsMoreActions, tint = c.inkMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(s.logsExportTxtBtn) },
                onClick = { expanded = false; onExport(ExportFormat.TXT) },
                modifier = Modifier.testTag("log_menu_txt"),
            )
            DropdownMenuItem(
                text = { Text(s.logsExportCsvBtn) },
                onClick = { expanded = false; onExport(ExportFormat.CSV) },
                modifier = Modifier.testTag("log_menu_csv"),
            )
            DropdownMenuItem(
                text = { Text(s.logsShareDesc) },
                onClick = { expanded = false; onExport(null) },
                modifier = Modifier.testTag("log_menu_share"),
            )
            DropdownMenuItem(
                text = { Text(s.logsEmailBtn) },
                onClick = { expanded = false; onEmail() },
                modifier = Modifier.testTag("log_menu_email"),
            )
            HorizontalDivider(color = c.border)
            DropdownMenuItem(
                text = { Text(s.logsDeleteCardDesc, color = c.alertCrimson) },
                onClick = { expanded = false; onDelete() },
                modifier = Modifier.testTag("log_menu_delete"),
            )
        }
    }
}

@Composable
private fun EmailLogConfirmDialog(
    session: LogEmailTarget,
    onConfirm: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var selectedFormat by remember { mutableStateOf(ExportFormat.TXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                text = s.emailDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = c.ink,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = s.emailDialogSessionSummary(session.title, session.frameCount, session.sizeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink,
                )

                Text(
                    text = s.emailDialogFormatLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ExportFormat.TXT to s.emailDialogFormatTxt,
                        ExportFormat.CSV to s.emailDialogFormatCsv,
                    ).forEach { (fmt, label) ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.cobaltContainer,
                                selectedLabelColor = c.cobaltOnContainer,
                            ),
                            modifier = Modifier.testTag("email_format_${fmt.extension}"),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.cobaltContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, c.cobalt.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = c.cobalt,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = s.emailDialogPrivacyNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.cobaltOnContainer,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedFormat) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.cobalt,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("confirm_email_log_button"),
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.emailDialogConfirmBtn)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(s.commonCancel)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSessionViewerSheet(
    session: SavedLogSession,
    highlightKeyword: String,
    onExport: (ExportFormat?) -> Unit,
    onEmail: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filtersExpanded by rememberSaveable(session.id) { mutableStateOf(false) }
    var localFilter by rememberSaveable(session.id) { mutableStateOf("") }
    var localHighlight by rememberSaveable(session.id) { mutableStateOf(highlightKeyword) }
    var inspectingFrame by remember(session.id) { mutableStateOf<TerminalFrame?>(null) }

    val matchingFrames = remember(session.frames, localFilter) {
        if (localFilter.isBlank()) session.frames else session.frames.filter {
            it.asciiText.contains(localFilter, ignoreCase = true) || it.hexText.contains(localFilter, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp).navigationBarsPadding().testTag("log_detail_sheet"),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium)
                    Text(session.deviceSummary, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                }
                LogSessionMenu("log_detail_more", onExport, onEmail, onDelete)
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_log_detail")) {
                    Icon(Icons.Rounded.Close, contentDescription = s.commonClose)
                }
            }
            Text(
                "${session.fullTimeRange} · ${session.configSummary} · ${session.sizeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
            HorizontalDivider(Modifier.padding(top = 6.dp), color = c.border)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (localFilter.isBlank()) s.logsFramesCount(session.frameCount)
                    else s.logsMatchedFrames(matchingFrames.size, session.frameCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
                TextButton(
                    onClick = { filtersExpanded = !filtersExpanded },
                    modifier = Modifier.testTag("toggle_log_frame_filters"),
                ) {
                    Text(when {
                        localFilter.isNotBlank() -> s.termFilterActive
                        localHighlight.isNotBlank() -> s.logsHighlightActive
                        else -> s.termFilter
                    })
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            if (filtersExpanded) {
                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = localFilter,
                        onValueChange = { localFilter = it },
                        label = { Text(s.logViewerFilterPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("log_frame_filter"),
                    )
                    OutlinedTextField(
                        value = localHighlight,
                        onValueChange = { localHighlight = it },
                        label = { Text(s.termHighlightPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("log_frame_highlight"),
                    )
                }
            }
            if (matchingFrames.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(s.logsNoFrames)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("log_frame_list"),
                ) {
                    items(matchingFrames, key = { it.id }) { frame ->
                        TerminalFrameRow(
                            frame = frame,
                            displayMode = DisplayMode.MIXED,
                            showTimestamp = true,
                            highlightKeyword = localHighlight,
                            onClick = { inspectingFrame = frame },
                        )
                    }
                }
            }
        }
    }
    inspectingFrame?.let { frame ->
        FrameInspectorDialog(frame = frame, onCopyToTx = null, onDismiss = { inspectingFrame = null })
    }
}
