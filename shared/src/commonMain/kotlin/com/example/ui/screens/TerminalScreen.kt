package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.protocol.ProtocolEngine
import com.example.platform.formatByteSizeCompact
import com.example.platform.formatDecimal
import com.example.ui.components.TxHistoryBottomSheet
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedShortLabel
import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.DisplayMode
import com.example.ui.model.FileTransferDraft
import com.example.ui.model.FrameDirection
import com.example.ui.model.LineEnding
import com.example.ui.model.MacroColorTag
import com.example.ui.model.MacroCommandItem
import com.example.ui.model.MacroGroupFilter
import com.example.ui.model.PreviewChecksumHelper
import com.example.ui.model.TerminalFrame
import com.example.ui.model.TxFormat
import com.example.ui.model.TxHistoryItem
import com.example.ui.state.SerialUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTerminalStyle
import com.example.ui.theme.instrumentColors
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    state: SerialUiState,
    isWideLayout: Boolean,
    compactSender: Boolean,
    snackbarHostState: SnackbarHostState,
    onOpenConfigSheet: () -> Unit = {},
    onDisplayModeChange: (DisplayMode) -> Unit,
    onToggleTimestamps: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onToggleTrafficCounters: () -> Unit,
    onResetByteCounters: () -> Unit,
    onToggleFilterBar: () -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onHighlightKeywordChange: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onClearTerminal: () -> Unit,
    onInspectFrame: (TerminalFrame?) -> Unit,
    onCopyFrameToTx: (TerminalFrame) -> Unit,
    onTxInputChange: (String) -> Unit,
    onTxInputFocusChanged: (Boolean) -> Unit,
    onTxFormatChange: (TxFormat) -> Unit,
    onLineEndingChange: (LineEnding) -> Unit,
    onChecksumModeChange: (ChecksumMode) -> Unit,
    onToggleTxChecksumEndian: () -> Unit,
    onToggleRepeatSend: () -> Unit,
    onRepeatSettingsChange: (Int, Int) -> Unit,
    onSendFrame: () -> Unit,
    onPickFileToSend: (() -> Unit)?,
    onStartFileSend: (Int, Int, Boolean) -> Unit,
    onDismissFileSendDraft: () -> Unit,
    onCancelFileSend: () -> Unit,
    onFireMacro: (MacroCommandItem) -> Unit,
    onToggleMacroPanel: (Boolean) -> Unit,
    onSelectMacroGroup: (MacroGroupFilter) -> Unit,
    onOpenMacroEditor: (MacroCommandItem?) -> Unit,
    onCloseMacroEditor: () -> Unit,
    onSaveMacro: (Long?, String, String, String, TxFormat, LineEnding, MacroColorTag) -> Unit,
    onDeleteMacro: (Long) -> Unit,
    onImportMacros: () -> Unit,
    onExportMacros: () -> Unit,
    onToggleHistorySheet: (Boolean) -> Unit,
    onHistorySearchChange: (String) -> Unit,
    onRefillHistory: (TxHistoryItem) -> Unit,
    onResendHistory: (TxHistoryItem) -> Unit,
    onPromoteHistoryToMacro: (TxHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showIntervalDialog by remember { mutableStateOf(false) }
    var repeatControlsExpanded by remember { mutableStateOf(false) }
    val repeatControlsVisible = repeatControlsExpanded || state.repeatEnabled
    val showSidebar = isWideLayout && !compactSender
    LaunchedEffect(state.repeatEnabled) {
        if (state.repeatEnabled) repeatControlsExpanded = true
    }
    var showHighlightOptions by remember { mutableStateOf(false) }

    val filteredFrames by remember(state.frames, state.filterKeyword) {
        derivedStateOf {
            if (state.filterKeyword.isBlank()) {
                state.frames
            } else {
                state.frames.filter {
                    it.asciiText.contains(state.filterKeyword, ignoreCase = true) ||
                        it.hexText.contains(state.filterKeyword, ignoreCase = true)
                }
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 1
        }
    }

    val payloadByteCount = remember(state.txInput, state.txFormat, state.lineEnding, state.settings.charset) {
        if (state.txInput.isBlank()) {
            0
        } else {
            val effectiveEnding = if (state.txFormat == TxFormat.HEX) LineEnding.NONE else state.lineEnding
            ProtocolEngine.parsePayloadBytes(
                input = if (state.txFormat == TxFormat.HEX) state.txInput.trim() else state.txInput,
                format = state.txFormat,
                lineEnding = effectiveEnding,
                charset = state.settings.charset,
            )?.size ?: 0
        }
    }

    LaunchedEffect(filteredFrames.size, filteredFrames.lastOrNull()?.id, state.autoScroll) {
        if (state.autoScroll && filteredFrames.isNotEmpty()) {
            listState.animateScrollToItem(filteredFrames.lastIndex)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(if (showSidebar) 0.6f else 1f)
                .fillMaxHeight(),
        ) {
            if (!compactSender) {
                Surface(color = c.surface) {
                    Column {
                        TerminalToolbar(
                            state = state,
                            onDisplayModeChange = onDisplayModeChange,
                            onToggleTimestamps = onToggleTimestamps,
                            onToggleAutoScroll = onToggleAutoScroll,
                            onToggleTrafficCounters = onToggleTrafficCounters,
                            onResetByteCounters = onResetByteCounters,
                            onToggleFilterBar = onToggleFilterBar,
                            onToggleRecording = onToggleRecording,
                            onClearTerminal = onClearTerminal,
                        )

                        // Collapsible Keyword Filter & Highlight Bar
                        AnimatedVisibility(visible = state.showFilterBar) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(c.surfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = state.filterKeyword,
                                        onValueChange = onFilterKeywordChange,
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = {
                                            if (state.filterKeyword.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { onFilterKeywordChange("") },
                                                    modifier = Modifier.testTag("clear_terminal_filter_button"),
                                                ) {
                                                    Icon(Icons.Rounded.Close, contentDescription = s.termFilterClearDesc, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        placeholder = { Text(s.termFilterPlaceholder, style = MaterialTheme.typography.bodySmall) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("terminal_filter_input"),
                                    )
                                    TextButton(
                                        onClick = { showHighlightOptions = !showHighlightOptions },
                                        modifier = Modifier.testTag("terminal_highlight_options"),
                                    ) {
                                        Text(s.termHighlight)
                                    }
                                }
                                AnimatedVisibility(visible = showHighlightOptions) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedTextField(
                                            value = state.highlightKeyword,
                                            onValueChange = onHighlightKeywordChange,
                                            placeholder = { Text(s.termHighlightPlaceholder, style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("terminal_highlight_input"),
                                        )
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(s.termQuickHighlightLabel, style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
                                            listOf("ERROR", "WARN", "OK", "01 03").forEach { preset ->
                                                FilterChip(
                                                    selected = state.highlightKeyword.equals(preset, ignoreCase = true),
                                                    onClick = {
                                                        onHighlightKeywordChange(if (state.highlightKeyword == preset) "" else preset)
                                                    },
                                                    label = { Text(preset, fontFamily = JetBrainsMonoFontFamily) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = c.border)
                    }
                }

                // 2. High-Contrast Terminal Stream Area (Tap any frame to inspect CRC & bytes)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .border(1.dp, c.border, RoundedCornerShape(12.dp)),
                ) {
                    if (filteredFrames.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = c.cobalt.copy(alpha = 0.7f),
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (state.frames.isEmpty()) s.termEmptyTitle else s.termNoFilterMatch(state.filterKeyword),
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.termEmptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.inkMuted,
                                textAlign = TextAlign.Center,
                            )
                            if (state.frames.isEmpty() && state.connectionStatus == ConnectionStatus.DISCONNECTED) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onOpenConfigSheet,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.cobalt,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("empty_connect_button"),
                                ) {
                                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.termEmptyConnectBtn)
                                }
                            } else if (state.frames.isNotEmpty() && state.filterKeyword.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { onFilterKeywordChange("") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("empty_clear_filter_button"),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.termClearFilterBtn)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("terminal_frame_list"),
                        ) {
                            items(filteredFrames, key = { it.id }) { frame ->
                                TerminalFrameRow(
                                    frame = frame,
                                    displayMode = state.displayMode,
                                    showTimestamp = state.showTimestamps,
                                    highlightKeyword = state.highlightKeyword,
                                    fontSizeSp = state.settings.terminalFontSizeSp,
                                    onClick = { onInspectFrame(frame) },
                                )
                            }
                        }

                        // Floating Jump-to-Latest Pill when user scrolls up
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isAtBottom,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = c.cobalt,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (!state.autoScroll) {
                                            onToggleAutoScroll()
                                        }
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(filteredFrames.lastIndex)
                                        }
                                    }
                                    .testTag("jump_to_latest_button"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = s.termJumpToBottom,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = s.termJumpToBottom,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isWideLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TerminalShortcutButton(
                            label = s.termMacroPanelCount(state.macros.size),
                            icon = Icons.Rounded.GridView,
                            onClick = { onToggleMacroPanel(true) },
                            modifier = Modifier
                                .testTag("toggle_macro_panel_button")
                                .semantics { selected = state.macroPanelExpanded },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandLess,
                                    contentDescription = s.termExpandMacroDesc,
                                    tint = c.cobalt,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                        TerminalShortcutButton(
                            label = s.termHistoryButton,
                            icon = Icons.Rounded.History,
                            onClick = { onToggleHistorySheet(true) },
                            modifier = Modifier
                                .testTag("open_tx_history_button")
                                .semantics { contentDescription = s.termHistoryDesc },
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // 4. Bottom TX Console Dock (Anti-Mis-Tap Split Repeat/Config, Payload Byte Counter, Multi-Line Input + Clear)
            Surface(
                color = c.surface,
                border = BorderStroke(1.dp, c.border),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (compactSender) 6.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!compactSender) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("tx_option_list"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Single-tap ASCII <-> HEX Format Toggle Chip
                                FilterChip(
                                    selected = state.txFormat == TxFormat.HEX,
                                    onClick = {
                                        val nextFormat = if (state.txFormat == TxFormat.HEX) TxFormat.ASCII else TxFormat.HEX
                                        onTxFormatChange(nextFormat)
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapHoriz,
                                            contentDescription = s.termToggleFormatDesc,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = s.termFormatChip(state.txFormat.label),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = c.cobaltContainer,
                                        selectedLabelColor = c.cobaltOnContainer,
                                    ),
                                    modifier = Modifier.testTag("tx_format_toggle_chip"),
                                )

                                // Line Ending Dropdown Chip (only applicable in ASCII mode)
                                AnimatedVisibility(visible = state.txFormat == TxFormat.ASCII) {
                                    LineEndingMenuChip(
                                        current = state.lineEnding,
                                        onSelect = onLineEndingChange,
                                    )
                                }

                                if (state.txFormat == TxFormat.HEX) {
                                    ChecksumMenuChip(
                                        current = state.checksumMode,
                                        previewBytes = state.checksumPreview,
                                        onSelect = onChecksumModeChange,
                                    )
                                }

                                if (onPickFileToSend != null) {
                                    FilterChip(
                                        selected = state.fileTransferProgress != null,
                                        onClick = onPickFileToSend,
                                        leadingIcon = {
                                            Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                        },
                                        label = { Text(s.termSendFileChip, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = c.cobaltContainer,
                                            selectedLabelColor = c.cobaltOnContainer,
                                        ),
                                        modifier = Modifier.testTag("pick_file_to_send_chip"),
                                    )
                                }
                            }
                            FilterChip(
                                selected = repeatControlsVisible,
                                enabled = !state.repeatEnabled,
                                onClick = { repeatControlsExpanded = !repeatControlsExpanded },
                                label = { Text(s.termRepeatControls, style = MaterialTheme.typography.labelMedium) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (repeatControlsVisible) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                },
                                modifier = Modifier.testTag("toggle_repeat_controls"),
                            )
                        }

                        // Active File Transfer Progress Banner
                        AnimatedVisibility(visible = onPickFileToSend != null && state.fileTransferProgress != null) {
                            state.fileTransferProgress?.let { progress ->
                                val pct = (progress.fraction * 100).toInt()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = c.cobaltContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, c.cobalt.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = s.termSendingFileBanner(progress.fileName, progress.sentBytes, progress.totalBytes, pct),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    color = c.cobaltOnContainer,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            TextButton(
                                                onClick = onCancelFileSend,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("cancel_file_send_button"),
                                            ) {
                                                Text(s.termStopFileSend, color = c.alertCrimson, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress.fraction },
                                            color = c.cobalt,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Status Bar: Live Checksum Tail (when enabled) + Payload Byte Count Badge
                        val hasChecksumPreview = state.txFormat == TxFormat.HEX && state.checksumPreview.isNotEmpty()
                        val showByteBadge = state.txInput.isNotBlank() && payloadByteCount > 0
                        if (hasChecksumPreview || showByteBadge) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(c.cobaltContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (hasChecksumPreview) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = state.checksumMode.localizedLabel(s),
                                            style = MaterialTheme.typography.labelSmall.copy(color = c.cobaltOnContainer),
                                        )
                                        if (state.checksumMode.is16Bit) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = c.surface,
                                                border = BorderStroke(1.dp, c.cobalt.copy(alpha = 0.4f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable(onClick = onToggleTxChecksumEndian)
                                                    .testTag("toggle_tx_checksum_endian"),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.SwapHoriz,
                                                        contentDescription = s.termToggleEndianDesc,
                                                        tint = c.cobalt,
                                                        modifier = Modifier.size(12.dp),
                                                    )
                                                    Text(
                                                        text = state.txChecksumEndian.shortLabel,
                                                        style = MonoBadgeStyle.copy(color = c.cobalt, fontSize = 10.sp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = s.termChecksumTailLabel(state.checksumPreview),
                                            style = MonoBadgeStyle.copy(color = c.cobalt),
                                        )
                                    }
                                } else {
                                    Text(
                                        text = state.txFormat.label,
                                        style = MonoBadgeStyle.copy(color = c.cobaltOnContainer, fontSize = 10.5.sp),
                                    )
                                }

                                if (showByteBadge) {
                                    Text(
                                        text = s.termPayloadBytesBadge(payloadByteCount),
                                        style = MonoBadgeStyle.copy(color = c.cobalt, fontSize = 10.5.sp),
                                    )
                                }
                            }
                        }

                        if (repeatControlsVisible) {
                            TerminalRepeatControls(
                                running = state.repeatEnabled,
                                intervalMs = state.repeatIntervalMs,
                                onToggle = onToggleRepeatSend,
                                onOpenSettings = { showIntervalDialog = true },
                            )
                        }
                    }

                    // Input and send, with history kept beside them on wide layouts.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = state.txInput,
                            onValueChange = onTxInputChange,
                            placeholder = {
                                Text(
                                    text = if (state.txFormat == TxFormat.HEX) s.termHexInputHint else s.termAsciiInputHint,
                                    style = MonoTerminalStyle.copy(color = c.inkMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = if (state.txInput.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { onTxInputChange("") },
                                        modifier = Modifier.testTag("clear_tx_input_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = s.termClearInputDesc,
                                            tint = c.inkMuted,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else null,
                            textStyle = MonoTerminalStyle.copy(color = c.ink),
                            singleLine = false,
                            maxLines = if (compactSender) 2 else 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendFrame() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = c.cobalt,
                                unfocusedBorderColor = c.border,
                                focusedContainerColor = c.surfaceElevated,
                                unfocusedContainerColor = c.surfaceElevated,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .onFocusChanged { onTxInputFocusChanged(it.isFocused) }
                                .testTag("tx_input_field"),
                        )

                        Button(
                            onClick = onSendFrame,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.cobalt,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("send_frame_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = s.termSendDesc,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(s.termSendButton, style = MaterialTheme.typography.labelLarge)
                        }

                        if (compactSender && state.repeatEnabled) {
                            IconButton(
                                onClick = onToggleRepeatSend,
                                modifier = Modifier.size(52.dp).testTag("toggle_repeat_send"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = s.termRepeatStopBtn,
                                    tint = c.alertCrimson,
                                )
                            }
                        }

                        if (isWideLayout || compactSender) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = c.surfaceElevated,
                                border = BorderStroke(1.dp, c.border),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleHistorySheet(true) }
                                    .testTag("open_tx_history_button"),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = s.termHistoryDesc,
                                        tint = c.cobalt,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = s.termHistoryButton,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = c.ink,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wide Screen Side-by-Side Macro Pad Panel
        if (showSidebar) {
            VerticalDivider(color = c.border)
            EmbeddedMacroPanel(
                macros = state.macros,
                macroGroups = state.macroGroups,
                selectedGroup = state.selectedMacroGroup,
                onSelectGroup = onSelectMacroGroup,
                onFireMacro = onFireMacro,
                onOpenEditor = onOpenMacroEditor,
                onImportMacros = onImportMacros,
                onExportMacros = onExportMacros,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
            )
        }
    }

    if (showIntervalDialog) {
        RepeatConfigDialog(
            currentMs = state.repeatIntervalMs,
            currentMaxCount = state.repeatMaxCount,
            sentCount = state.repeatSentCount.takeIf { state.repeatEnabled },
            onConfirm = { intervalMs, maxCount ->
                onRepeatSettingsChange(intervalMs, maxCount)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false },
        )
    }

    if (onPickFileToSend != null) {
        state.fileTransferDraft?.let { draft ->
            FileSendConfigDialog(
                draft = draft,
                onConfirm = onStartFileSend,
                onDismiss = onDismissFileSendDraft,
            )
        }
    }

    state.inspectingFrame?.let { frame ->
        FrameInspectorDialog(
            frame = frame,
            onCopyToTx = { onCopyFrameToTx(frame) },
            onDismiss = { onInspectFrame(null) },
        )
    }

    if (!isWideLayout && state.macroPanelExpanded) {
        MacroPanelBottomSheet(
            macroCount = state.macros.size,
            snackbarHostState = snackbarHostState,
            onDismiss = { onToggleMacroPanel(false) },
        ) {
            EmbeddedMacroPanel(
                macros = state.macros,
                macroGroups = state.macroGroups,
                selectedGroup = state.selectedMacroGroup,
                onSelectGroup = onSelectMacroGroup,
                onFireMacro = onFireMacro,
                onOpenEditor = onOpenMacroEditor,
                onImportMacros = onImportMacros,
                onExportMacros = onExportMacros,
                compact = true,
            )
        }
    } else if (state.showHistorySheet) {
        TxHistoryBottomSheet(
            historyItems = state.txHistory,
            searchQuery = state.historySearchQuery,
            onSearchQueryChange = onHistorySearchChange,
            onRefillItem = onRefillHistory,
            onResendItem = onResendHistory,
            onPromoteToMacro = onPromoteHistoryToMacro,
            onClearHistory = onClearHistory,
            onDismiss = { onToggleHistorySheet(false) },
        )
    }

    if (state.showMacroEditor) {
        MacroEditorDialog(
            initial = state.editingMacro,
            defaultGroup = (state.selectedMacroGroup as? MacroGroupFilter.Named)?.name.orEmpty(),
            namedGroups = state.namedMacroGroups,
            onDismiss = onCloseMacroEditor,
            onSave = { title, group, payload, fmt, ending, color ->
                onSaveMacro(state.editingMacro?.id, title, group, payload, fmt, ending, color)
            },
            onDelete = { state.editingMacro?.id?.let(onDeleteMacro) },
        )
    }
}

@Composable
fun TerminalFrameRow(
    frame: TerminalFrame,
    displayMode: DisplayMode,
    showTimestamp: Boolean,
    highlightKeyword: String,
    fontSizeSp: Float = 12.5f,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.instrumentColors
    val hexDisplay = remember(frame.id, frame.rawBytes) {
        frame.hexText
    }
    val isHighlighted = highlightKeyword.isNotBlank() && (
        frame.asciiText.contains(highlightKeyword, ignoreCase = true) ||
            hexDisplay.contains(highlightKeyword, ignoreCase = true) ||
            frame.hexText.contains(highlightKeyword, ignoreCase = true)
        )

    val (badgeText, badgeFg, rowBg, borderCol) = when (frame.direction) {
        FrameDirection.RX -> Quad("RX", c.rxEmerald, if (isHighlighted) c.highlightYellowBg else c.rxEmeraldBg.copy(alpha = 0.55f), c.rxEmeraldBorder)
        FrameDirection.TX -> Quad("TX", c.txAmber, if (isHighlighted) c.highlightYellowBg else c.txAmberBg.copy(alpha = 0.65f), c.txAmberBorder)
        FrameDirection.SYS -> Quad("SYS", c.sysSlate, if (isHighlighted) c.highlightYellowBg else c.sysSlateBg, c.border)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = if (isHighlighted) c.txAmber else borderCol,
                shape = RoundedCornerShape(8.dp),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("terminal_frame_${frame.id}"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = badgeFg.copy(alpha = 0.14f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = badgeText,
                    style = MonoBadgeStyle.copy(color = badgeFg),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            if (showTimestamp) {
                Text(
                    text = frame.timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = if (isHighlighted) c.highlightYellowInk else c.inkMuted,
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${frame.rawBytes.size} B",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = c.inkMuted,
                ),
            )
        }

        when (displayMode) {
            DisplayMode.ASCII -> {
                Text(
                    text = frame.asciiText,
                    style = MonoTerminalStyle.copy(
                        fontSize = fontSizeSp.sp,
                        color = if (isHighlighted) c.highlightYellowInk else c.ink,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
            DisplayMode.HEX -> {
                Text(
                    text = hexDisplay,
                    style = MonoTerminalStyle.copy(
                        fontSize = fontSizeSp.sp,
                        color = if (isHighlighted) c.highlightYellowInk else c.ink,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            DisplayMode.MIXED -> {
                Text(
                    text = hexDisplay,
                    style = MonoTerminalStyle.copy(
                        color = c.cobalt,
                        fontSize = (fontSizeSp - 1.0f).coerceAtLeast(10.0f).sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = frame.asciiText,
                    style = MonoTerminalStyle.copy(
                        fontSize = fontSizeSp.sp,
                        color = if (isHighlighted) c.highlightYellowInk else c.ink,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
fun FrameInspectorDialog(
    frame: TerminalFrame,
    onCopyToTx: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val report = remember(frame.id, frame.rawBytes) {
        PreviewChecksumHelper.inspectFrameChecksums(frame.rawBytes)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.inspectorTitle, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Text(
                    text = "${frame.direction.name} · ${frame.rawBytes.size}B",
                    style = MonoBadgeStyle.copy(color = c.cobalt),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = s.inspectorTimestamp(frame.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(color = c.inkMuted),
                )

                // HEX Dump Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.inspectorHexDumpTitle, style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
                        Text(
                            text = frame.hexText,
                            style = MonoTerminalStyle.copy(color = c.cobalt, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }

                // ASCII Text Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.inspectorAsciiTitle, style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
                        Text(
                            text = frame.asciiText,
                            style = MonoTerminalStyle.copy(color = c.ink),
                        )
                    }
                }

                // Checksum Verification Diagnostics (Zero permanent clutter, surfaced on tap!)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.surface,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.inspectorDiagTitle, style = MaterialTheme.typography.labelMedium, color = c.ink)

                        if (report.modbusTailMatched != null) {
                            val matched = report.modbusTailMatched
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (matched) c.rxEmeraldBg else c.alertCrimsonBg,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = if (matched) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (matched) c.rxEmerald else c.alertCrimson,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (matched) {
                                            s.inspectorModbusPass(report.actualTail2.orEmpty())
                                        } else {
                                            s.inspectorModbusFail(report.actualTail2.orEmpty(), report.expectedModbusTail.orEmpty())
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (matched) c.rxEmerald else c.alertCrimson,
                                        ),
                                    )
                                }
                            }
                        }

                        ChecksumInfoRow(s.inspectorRowModbusLe, report.fullModbusLe)
                        ChecksumInfoRow(s.inspectorRowCcittBe, report.fullCcittBe)
                        ChecksumInfoRow(s.inspectorRowSum8, report.fullSum8)
                        ChecksumInfoRow(s.inspectorRowXor8, report.fullXor8)
                    }
                }
            }
        },
        confirmButton = {
            if (onCopyToTx != null) {
                OutlinedButton(
                    onClick = onCopyToTx,
                    modifier = Modifier.testTag("copy_frame_to_tx_button"),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.inspectorCopyToTx)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.commonClose)
            }
        },
    )
}

@Composable
private fun ChecksumInfoRow(label: String, value: String) {
    val c = MaterialTheme.instrumentColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        Text(value, style = MonoBadgeStyle.copy(color = c.ink))
    }
}

@Composable
private fun TerminalShortcutButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.instrumentColors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.border),
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = c.cobalt, modifier = Modifier.size(15.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(color = c.ink, fontWeight = FontWeight.Bold),
            )
            trailingContent?.invoke()
        }
    }
}

private val TerminalToolbarSpacing = 6.dp
private val TerminalCounterResetSize = 48.dp

@Composable
private fun TerminalToolbar(
    state: SerialUiState,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onToggleTimestamps: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onToggleTrafficCounters: () -> Unit,
    onResetByteCounters: () -> Unit,
    onToggleFilterBar: () -> Unit,
    onToggleRecording: () -> Unit,
    onClearTerminal: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val rx = if (state.showTrafficCounters) "↓${formatByteSizeCompact(state.rxByteCount)}" else ""
        val tx = if (state.showTrafficCounters) "↑${formatByteSizeCompact(state.txByteCount)}" else ""
        val countersBesideButtons = if (state.showTrafficCounters) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val style = MaterialTheme.typography.labelSmall
            val labelWidth = with(density) {
                (measurer.measure(rx, style).size.width + measurer.measure(tx, style).size.width).toDp()
            }
            // Keep at least the button width available on a 320 dp terminal at normal font size.
            val minimumButtonRowWidth = 300.dp * density.fontScale.coerceAtLeast(1f)
            maxWidth >= minimumButtonRowWidth + labelWidth + TerminalCounterResetSize + TerminalToolbarSpacing * 3
        } else {
            false
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TerminalToolbarSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(TerminalToolbarSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TerminalDisplayMenu(
                        current = state.displayMode,
                        showTimestamps = state.showTimestamps,
                        autoScroll = state.autoScroll,
                        onSelect = onDisplayModeChange,
                        onToggleTimestamps = onToggleTimestamps,
                        onToggleAutoScroll = onToggleAutoScroll,
                        showTrafficCounters = state.showTrafficCounters,
                        onToggleTrafficCounters = onToggleTrafficCounters,
                        modifier = Modifier.weight(1f),
                    )

                    val filterActive = state.filterKeyword.isNotBlank()
                    Box(modifier = Modifier.weight(1f)) {
                        TerminalToolbarButton(
                            label = s.termFilter,
                            onClick = onToggleFilterBar,
                            selected = state.showFilterBar,
                            containerColor = if (state.showFilterBar) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (state.showFilterBar) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_filter_bar")
                                .semantics {
                                    if (filterActive) stateDescription = s.termFilterActive
                                },
                        )
                        if (filterActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 2.dp)
                                    .size(8.dp)
                                    .background(c.cobalt, CircleShape)
                                    .border(1.dp, c.surface, CircleShape)
                                    .testTag("terminal_filter_active_indicator"),
                            )
                        }
                    }

                    TerminalToolbarButton(
                        label = if (state.isRecordingSession) s.termSaveRecording else s.termRecord,
                        onClick = onToggleRecording,
                        selected = state.isRecordingSession,
                        containerColor = if (state.isRecordingSession) c.alertCrimsonBg else Color.Transparent,
                        contentColor = if (state.isRecordingSession) c.alertCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).testTag("toggle_record_session"),
                    )

                    TerminalToolbarButton(
                        label = s.termClear,
                        onClick = onClearTerminal,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_terminal_button")
                            .semantics { contentDescription = s.termClearDesc },
                    )
                }
                if (countersBesideButtons) {
                    TerminalTrafficCounters(rx, tx, onResetByteCounters)
                }
            }
            if (state.showTrafficCounters && !countersBesideButtons) {
                TerminalTrafficCounters(rx, tx, onResetByteCounters)
            }
        }
    }
}

@Composable
private fun TerminalTrafficCounters(
    rx: String,
    tx: String,
    onReset: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val style = MaterialTheme.typography.labelSmall.copy(color = c.inkMuted)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TerminalToolbarSpacing, Alignment.End),
        modifier = Modifier.testTag("terminal_traffic_counters"),
    ) {
        Text(rx, style = style, modifier = Modifier.align(Alignment.CenterVertically).testTag("terminal_rx_byte_count"))
        Text(tx, style = style, modifier = Modifier.align(Alignment.CenterVertically).testTag("terminal_tx_byte_count"))
        IconButton(
            onClick = onReset,
            modifier = Modifier.size(TerminalCounterResetSize).testTag("reset_byte_counters_compact"),
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = s.termResetCounters, tint = c.inkMuted)
        }
    }
}

@Composable
private fun TerminalToolbarButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Button
            if (selected != null) this.selected = selected
        },
        shape = FilterChipDefaults.shape,
        color = containerColor,
        contentColor = contentColor,
        border = if (containerColor == Color.Transparent) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 32.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TerminalDisplayMenu(
    current: DisplayMode,
    showTimestamps: Boolean,
    autoScroll: Boolean,
    onSelect: (DisplayMode) -> Unit,
    onToggleTimestamps: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    showTrafficCounters: Boolean,
    onToggleTrafficCounters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TerminalToolbarButton(
            label = s.termDisplay,
            selected = expanded,
            onClick = { expanded = true },
            containerColor = if (expanded) c.cobalt else Color.Transparent,
            contentColor = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display_mode_menu_chip")
                .semantics { stateDescription = current.label },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 216.dp),
        ) {
            Text(
                text = s.termDisplayFormat,
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            listOf(DisplayMode.MIXED, DisplayMode.HEX, DisplayMode.ASCII).forEach { mode ->
                val isSelected = current == mode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.label,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) c.cobalt else c.ink,
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = c.cobalt,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else null,
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    modifier = Modifier
                        .testTag("display_mode_${mode.name.lowercase()}")
                        .semantics { selected = isSelected },
                )
            }
            HorizontalDivider()
            TerminalDisplaySwitch(s.termTimestamps, showTimestamps, onToggleTimestamps, "toggle_timestamps")
            TerminalDisplaySwitch(s.termAutoScroll, autoScroll, onToggleAutoScroll, "toggle_autoscroll")
            TerminalDisplaySwitch(s.termShowTrafficCounters, showTrafficCounters, onToggleTrafficCounters, "toggle_traffic_counters")
        }
    }
}

@Composable
private fun TerminalDisplaySwitch(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .testTag(tag)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LineEndingMenuChip(
    current: LineEnding,
    onSelect: (LineEnding) -> Unit,
) {
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = current != LineEnding.NONE,
            onClick = { expanded = true },
            label = { Text(s.termLineEndingChip(current.localizedLabel(s)), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) },
            modifier = Modifier.testTag("line_ending_chip"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LineEnding.entries.forEach { ending ->
                DropdownMenuItem(
                    text = { Text(s.termLineEndingMenuItem(ending.localizedLabel(s)), fontFamily = JetBrainsMonoFontFamily) },
                    onClick = {
                        onSelect(ending)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ChecksumMenuChip(
    current: ChecksumMode,
    previewBytes: String,
    onSelect: (ChecksumMode) -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = current != ChecksumMode.NONE,
            onClick = { expanded = true },
            leadingIcon = {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
            },
            label = {
                Text(
                    text = if (current == ChecksumMode.NONE) s.termChecksumNoneChip else "${current.localizedShortLabel(s)}${if (previewBytes.isNotEmpty()) " ($previewBytes)" else ""}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = c.rxEmeraldBg,
                selectedLabelColor = c.rxEmerald,
            ),
            modifier = Modifier.testTag("checksum_mode_chip"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ChecksumMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.localizedLabel(s)) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TerminalRepeatControls(
    running: Boolean,
    intervalMs: Int,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val interval = if (intervalMs < 1000) {
        "$intervalMs ms"
    } else {
        formatDecimal(intervalMs / 1000.0, 3).trimEnd('0').trimEnd('.') + " s"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = running,
            onClick = onToggle,
            leadingIcon = {
                Icon(
                    imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            },
            label = {
                Text(
                    text = if (running) s.termRepeatStopBtn else s.termRepeatStartBtn,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = c.alertCrimsonBg,
                selectedLabelColor = c.alertCrimson,
                selectedLeadingIconColor = c.alertCrimson,
            ),
            modifier = Modifier.weight(1f).testTag("toggle_repeat_send"),
        )
        FilterChip(
            selected = false,
            onClick = onOpenSettings,
            label = {
                Text(
                    text = s.termRepeatIntervalSummary(interval),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            trailingIcon = {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("repeat_config_button")
                .semantics { contentDescription = s.termRepeatConfigDesc },
        )
    }
}

@Composable
private fun RepeatConfigDialog(
    currentMs: Int,
    currentMaxCount: Int,
    sentCount: Int?,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var intervalText by remember { mutableStateOf(currentMs.toString()) }
    var countText by remember { mutableStateOf(currentMaxCount.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(s.repeatDialogTitle, color = c.ink) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (sentCount != null) {
                    val limit = if (currentMaxCount == 0) s.repeatUnlimitedLabel else currentMaxCount.toString()
                    Text(
                        text = s.repeatProgressLabel(sentCount, limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(s.repeatIntervalLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("repeat_interval_input"),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(200, 500, 1000, 3000).forEach { preset ->
                        FilterChip(
                            selected = intervalText == preset.toString(),
                            onClick = { intervalText = preset.toString() },
                            label = { Text("${preset}ms") },
                        )
                    }
                }

                HorizontalDivider(color = c.border)

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(s.repeatMaxCountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("repeat_max_count_input"),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        0 to s.repeatUnlimitedLabel,
                        5 to s.repeatCountUnit(5),
                        10 to s.repeatCountUnit(10),
                        50 to s.repeatCountUnit(50),
                        100 to s.repeatCountUnit(100),
                    ).forEach { (cnt, label) ->
                        FilterChip(
                            selected = countText == cnt.toString(),
                            onClick = { countText = cnt.toString() },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        intervalText.toIntOrNull() ?: currentMs,
                        countText.toIntOrNull() ?: currentMaxCount,
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.cobalt,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag("confirm_repeat_config_button"),
            ) {
                Text(s.commonConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.commonCancel)
            }
        },
    )
}

@Composable
private fun FileSendConfigDialog(
    draft: FileTransferDraft,
    onConfirm: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var chunkSize by remember(draft) { mutableStateOf(draft.chunkSizeBytes) }
    var delayMs by remember(draft) { mutableStateOf(draft.chunkDelayMs) }
    var lineByLine by remember(draft) { mutableStateOf(draft.sendLineByLine) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(s.fileDialogTitle, color = c.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = draft.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = s.fileDialogSize(draft.totalBytes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = c.cobalt,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(s.fileDialogLineModeTitle, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Text(s.fileDialogLineModeSubtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                    }
                    Switch(
                        checked = lineByLine,
                        onCheckedChange = { lineByLine = it },
                        modifier = Modifier.testTag("switch_file_line_by_line"),
                    )
                }

                if (!lineByLine) {
                    Text(s.fileDialogChunkSizeTitle, style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(64, 128, 256, 512, 1024).forEach { size ->
                            FilterChip(
                                selected = chunkSize == size,
                                onClick = { chunkSize = size },
                                label = { Text("${size}B", fontFamily = JetBrainsMonoFontFamily) },
                            )
                        }
                    }
                }

                Text(s.fileDialogDelayTitle, style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(0, 10, 20, 50, 100).forEach { delayVal ->
                        FilterChip(
                            selected = delayMs == delayVal,
                            onClick = { delayMs = delayVal },
                            label = { Text("${delayVal}ms", fontFamily = JetBrainsMonoFontFamily) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(chunkSize, delayMs, lineByLine) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.cobalt,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag("start_file_send_button"),
            ) {
                Text(s.fileDialogStartSend)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.commonCancel)
            }
        },
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
