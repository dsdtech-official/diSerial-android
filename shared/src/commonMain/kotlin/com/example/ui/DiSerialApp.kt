package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ExportFormat
import com.example.data.local.LogExporter
import com.example.platform.PlatformClock
import com.example.platform.rememberSystemLanguageTag
import com.example.ui.components.InstrumentTopBar
import com.example.ui.components.SerialConfigBottomSheet
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.resolveAppStrings
import com.example.ui.model.AppTab
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.SavedLogSession
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.PlotterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.TerminalSettingsScreen
import com.example.ui.state.SerialUiState
import com.example.ui.state.SerialUiViewModel
import com.example.ui.theme.instrumentColors
import androidx.compose.ui.backhandler.BackHandler

@Composable
fun DiSerialApp(
    viewModel: SerialUiViewModel,
    appVersion: String,
    onPickFileToSend: (() -> Unit)? = null,
    onImportMacros: () -> Unit = {},
    onExportMacros: (suggestedFileName: String) -> Unit = {},
    onExportLogDocument: (session: SavedLogSession, format: ExportFormat, suggestedFileName: String) -> Unit = { _, _, _ -> },
    onShareLog: (session: SavedLogSession, format: ExportFormat) -> Unit = { _, _ -> },
    onEmailLog: (format: ExportFormat, session: SavedLogSession) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemLanguageTag = rememberSystemLanguageTag()
    val strings = remember(state.settings.appLanguage, systemLanguageTag) {
        resolveAppStrings(state.settings.appLanguage, systemLanguageTag)
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        DiSerialAppContent(
            state = state,
            viewModel = viewModel,
            appVersion = appVersion,
            onPickFileToSend = onPickFileToSend,
            onImportMacros = onImportMacros,
            onExportMacros = onExportMacros,
            onExportLogDocument = onExportLogDocument,
            onShareLog = onShareLog,
            onEmailLog = onEmailLog,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DiSerialAppContent(
    state: SerialUiState,
    viewModel: SerialUiViewModel,
    appVersion: String,
    onPickFileToSend: (() -> Unit)?,
    onImportMacros: () -> Unit,
    onExportMacros: (suggestedFileName: String) -> Unit,
    onExportLogDocument: (session: SavedLogSession, format: ExportFormat, suggestedFileName: String) -> Unit,
    onShareLog: (session: SavedLogSession, format: ExportFormat) -> Unit,
    onEmailLog: (format: ExportFormat, session: SavedLogSession) -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val snackbarHostState = remember { SnackbarHostState() }

    val handleExportOrShare: (ExportFormat?, Long) -> Unit = { format, id ->
        viewModel.withSavedLog(id) { session ->
            if (format == null) {
                onShareLog(session, ExportFormat.TXT)
            } else {
                onExportLogDocument(session, format, LogExporter.buildSuggestedFileName(session, format))
            }
        }
    }

    val triggerExportMacros: () -> Unit = {
        val stamp = PlatformClock.formatFileStamp(PlatformClock.currentTimeMillis())
        onExportMacros("diSerial_Shortcuts_${stamp}.json")
    }

    LaunchedEffect(state.uiNotice) {
        state.uiNotice?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeNotice()
        }
    }

    var lastPrimaryTab by remember { mutableStateOf(AppTab.TERMINAL) }
    var txInputFocused by remember { mutableStateOf(false) }
    LaunchedEffect(state.activeTab) {
        if (state.activeTab in AppTab.primaryTabs) {
            lastPrimaryTab = state.activeTab
        }
    }

    val secondaryTitle = when (state.activeTab) {
        AppTab.SETTINGS -> s.tabSettings
        AppTab.TERMINAL_SETTINGS -> s.settingsTerminalSection
        AppTab.ABOUT -> s.tabAbout
        else -> null
    }
    val isSecondaryScreen = secondaryTitle != null
    val navigateBack: () -> Unit = {
        when (state.activeTab) {
            AppTab.ABOUT, AppTab.TERMINAL_SETTINGS -> viewModel.selectTab(AppTab.SETTINGS)
            AppTab.SETTINGS -> viewModel.selectTab(lastPrimaryTab)
            else -> viewModel.selectTab(AppTab.TERMINAL)
        }
    }

    BackHandler(enabled = state.activeTab != AppTab.TERMINAL, onBack = navigateBack)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        val isWideLayout = maxWidth >= 640.dp
        val density = LocalDensity.current
        val keyboardHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val compactSender = state.activeTab == AppTab.TERMINAL && txInputFocused &&
            keyboardHeight > 0.dp && maxHeight - keyboardHeight < 280.dp

        if (isWideLayout) {
            Scaffold(
                containerColor = c.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.navigationBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = c.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding(),
                    ) {
                        AppTab.primaryTabs.forEach { tab ->
                            val selected = state.activeTab == tab
                            val tabLabel = tab.localizedLabel(s)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabLabel,
                                    )
                                },
                                label = { Text(tabLabel, maxLines = 2) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = c.cobalt,
                                    selectedTextColor = c.cobalt,
                                    indicatorColor = c.cobaltContainer,
                                    unselectedIconColor = c.inkMuted,
                                    unselectedTextColor = c.inkMuted,
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                            )
                        }
                    }
                    VerticalDivider(color = c.border)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .imePadding()
                            .let { if (compactSender) it.statusBarsPadding() else it },
                    ) {
                        if (secondaryTitle != null) {
                            SecondaryPageTopBar(
                                title = secondaryTitle,
                                connectionStatus = state.connectionStatus,
                                onBack = navigateBack,
                                onOpenConfigSheet = { viewModel.toggleConfigSheet(true) },
                            )
                        } else if (!compactSender) {
                            InstrumentTopBar(
                                connectionStatus = state.connectionStatus,
                                selectedDevice = state.selectedDevice,
                                config = state.currentConfig,
                                onOpenConfigSheet = { viewModel.toggleConfigSheet(true) },
                                onToggleConnect = viewModel::toggleConnection,
                                onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ActiveTabContent(
                                state = state,
                                snackbarHostState = snackbarHostState,
                                isWideLayout = true,
                                compactSender = compactSender,
                                onTxInputFocusChanged = { txInputFocused = it },
                                viewModel = viewModel,
                                appVersion = appVersion,
                                onExportOrShareLog = handleExportOrShare,
                                onEmailLog = onEmailLog,
                                onPickFileToSend = onPickFileToSend,
                                onImportMacros = onImportMacros,
                                onExportMacros = triggerExportMacros,
                            )
                        }
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = c.background,
                snackbarHost = {
                    if (state.activeTab != AppTab.TERMINAL || !state.macroPanelExpanded) {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!isSecondaryScreen) {
                        Column {
                            HorizontalDivider(color = c.border)
                            NavigationBar(
                                containerColor = c.surface,
                                windowInsets = WindowInsets.navigationBars,
                            ) {
                                AppTab.primaryTabs.forEach { tab ->
                                    val selected = state.activeTab == tab
                                    val tabLabel = tab.localizedLabel(s)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            Icon(
                                                imageVector = tabIcon(tab, selected),
                                                contentDescription = tabLabel,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tabLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 2,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = c.cobaltOnContainer,
                                            selectedTextColor = c.cobalt,
                                            indicatorColor = c.cobaltContainer,
                                            unselectedIconColor = c.inkMuted,
                                            unselectedTextColor = c.inkMuted,
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .let { if (isSecondaryScreen) it.navigationBarsPadding() else it }
                        .imePadding()
                        .let { if (compactSender) it.statusBarsPadding() else it },
                ) {
                    if (secondaryTitle != null) {
                        SecondaryPageTopBar(
                            title = secondaryTitle,
                            connectionStatus = state.connectionStatus,
                            onBack = navigateBack,
                            onOpenConfigSheet = { viewModel.toggleConfigSheet(true) },
                        )
                    } else if (!compactSender) {
                        InstrumentTopBar(
                            connectionStatus = state.connectionStatus,
                            selectedDevice = state.selectedDevice,
                            config = state.currentConfig,
                            onOpenConfigSheet = { viewModel.toggleConfigSheet(true) },
                            onToggleConnect = viewModel::toggleConnection,
                            onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActiveTabContent(
                            state = state,
                            snackbarHostState = snackbarHostState,
                            isWideLayout = false,
                            compactSender = compactSender,
                            onTxInputFocusChanged = { txInputFocused = it },
                            viewModel = viewModel,
                            appVersion = appVersion,
                            onExportOrShareLog = handleExportOrShare,
                            onEmailLog = onEmailLog,
                            onPickFileToSend = onPickFileToSend,
                            onImportMacros = onImportMacros,
                            onExportMacros = triggerExportMacros,
                        )
                    }
                }
            }
        }
    }

    if (state.showConfigSheet) {
        SerialConfigBottomSheet(
            devices = state.availableDevices,
            selectedDevice = state.selectedDevice,
            connectionStatus = state.connectionStatus,
            promptReason = state.smartPickerPromptReason,
            presets = state.configPresets,
            currentConfig = state.currentConfig,
            autoConnectOnUsbAttach = state.settings.autoConnectOnUsbAttach,
            onToggleAutoConnectUsb = viewModel::toggleAutoConnectUsb,
            onSelectDevice = viewModel::selectDevice,
            onRefreshDevices = viewModel::refreshUsbDevices,
            onSavePreset = viewModel::saveConfigAsPreset,
            onApplyAndConnect = viewModel::applyConfigAndClose,
            onDismiss = { viewModel.toggleConfigSheet(false) },
        )
    }
}

@Composable
private fun ActiveTabContent(
    state: SerialUiState,
    snackbarHostState: SnackbarHostState,
    isWideLayout: Boolean,
    compactSender: Boolean,
    onTxInputFocusChanged: (Boolean) -> Unit,
    viewModel: SerialUiViewModel,
    appVersion: String,
    onExportOrShareLog: (ExportFormat?, Long) -> Unit,
    onEmailLog: (ExportFormat, SavedLogSession) -> Unit,
    onPickFileToSend: (() -> Unit)?,
    onImportMacros: () -> Unit,
    onExportMacros: () -> Unit,
) {
    when (state.activeTab) {
        AppTab.TERMINAL -> TerminalScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            isWideLayout = isWideLayout,
            compactSender = compactSender,
            onOpenConfigSheet = { viewModel.toggleConfigSheet(true) },
            onDisplayModeChange = viewModel::setDisplayMode,
            onToggleTimestamps = viewModel::toggleTimestamps,
            onToggleAutoScroll = viewModel::toggleAutoScroll,
            onToggleTrafficCounters = viewModel::toggleTrafficCounters,
            onResetByteCounters = viewModel::resetByteCounters,
            onToggleFilterBar = viewModel::toggleFilterBar,
            onFilterKeywordChange = viewModel::updateFilterKeyword,
            onHighlightKeywordChange = viewModel::updateHighlightKeyword,
            onToggleRecording = viewModel::toggleRecording,
            onClearTerminal = viewModel::clearTerminal,
            onInspectFrame = viewModel::inspectFrame,
            onCopyFrameToTx = viewModel::copyFrameToTx,
            onTxInputChange = viewModel::updateTxInput,
            onTxInputFocusChanged = onTxInputFocusChanged,
            onTxFormatChange = viewModel::setTxFormat,
            onLineEndingChange = viewModel::setLineEnding,
            onChecksumModeChange = viewModel::setChecksumMode,
            onToggleTxChecksumEndian = viewModel::toggleTxChecksumEndian,
            onToggleRepeatSend = viewModel::toggleRepeatSend,
            onRepeatSettingsChange = viewModel::updateRepeatSettings,
            onSendFrame = viewModel::sendPreviewFrame,
            onPickFileToSend = onPickFileToSend,
            onStartFileSend = viewModel::startFileSend,
            onDismissFileSendDraft = viewModel::dismissFileSendDraft,
            onCancelFileSend = viewModel::cancelFileSend,
            onFireMacro = viewModel::fireMacro,
            onToggleMacroPanel = viewModel::toggleMacroPanel,
            onSelectMacroGroup = viewModel::selectMacroGroup,
            onOpenMacroEditor = viewModel::openMacroEditor,
            onCloseMacroEditor = viewModel::closeMacroEditor,
            onSaveMacro = viewModel::saveMacro,
            onDeleteMacro = viewModel::deleteMacro,
            onImportMacros = onImportMacros,
            onExportMacros = onExportMacros,
            onToggleHistorySheet = viewModel::toggleHistorySheet,
            onHistorySearchChange = viewModel::updateHistorySearchQuery,
            onRefillHistory = viewModel::refillFromHistory,
            onResendHistory = viewModel::resendFromHistory,
            onPromoteHistoryToMacro = viewModel::promoteHistoryToMacro,
            onClearHistory = viewModel::clearTxHistory,
        )

        AppTab.PLOTTER -> PlotterScreen(
            state = state,
            onSelectProfile = viewModel::selectPlotProfile,
            onSaveProfile = viewModel::savePlotProfile,
            onDeleteProfile = viewModel::deletePlotProfile,
            onToggleChannel = viewModel::togglePlotChannel,
            onToggleSampling = viewModel::togglePlotSampling,
            onToggleAutoScale = viewModel::togglePlotterAutoScale,
            onAdjustWindow = viewModel::adjustPlotterWindow,
            onClearPlot = viewModel::clearPlotSamples,
            onGoToTerminal = { viewModel.selectTab(AppTab.TERMINAL) },
        )

        AppTab.LOGS -> LogsScreen(
            state = state,
            onTimeRangeChange = viewModel::changeLogTimeRange,
            onLoadMore = viewModel::loadMoreLogs,
            onToggleRecordNow = viewModel::toggleRecording,
            onInspectSession = viewModel::inspectLog,
            onExportSession = onExportOrShareLog,
            onEmailSession = { format, id ->
                viewModel.withSavedLog(id) { session -> onEmailLog(format, session) }
            },
            onDeleteSession = viewModel::deleteLogSession,
        )

        AppTab.SETTINGS -> SettingsScreen(
            settings = state.settings,
            appVersion = appVersion,
            onLanguageChange = viewModel::updateAppLanguage,
            onThemeModeChange = viewModel::updateThemeMode,
            onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
            onOpenTerminalSettings = { viewModel.selectTab(AppTab.TERMINAL_SETTINGS) },
            onOpenAbout = { viewModel.selectTab(AppTab.ABOUT) },
        )

        AppTab.TERMINAL_SETTINGS -> TerminalSettingsScreen(
            settings = state.settings,
            onCharsetChange = viewModel::updateCharset,
            onFontSizeChange = viewModel::updateTerminalFontSize,
            onFrameTimeoutChange = viewModel::updateFrameTimeoutMs,
            onMaxBufferChange = viewModel::updateMaxBufferFrames,
            onToggleClearAfterSend = viewModel::toggleClearInputAfterSend,
            onToggleVibrateOnError = viewModel::toggleVibrateOnError,
            onResetDefaults = viewModel::resetTerminalSettingsToDefault,
        )

        AppTab.ABOUT -> AboutScreen(appVersion = appVersion)
    }
}

@Composable
private fun SecondaryPageTopBar(
    title: String,
    connectionStatus: ConnectionStatus,
    onBack: () -> Unit,
    onOpenConfigSheet: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val statusColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> c.rxEmerald
        ConnectionStatus.CONNECTING -> c.txAmber
        ConnectionStatus.DISCONNECTED -> c.sysSlate
    }
    Surface(
        color = c.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("secondary_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = s.navBackDesc,
                        tint = c.ink,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenConfigSheet)
                        .testTag("secondary_status_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                        Text(
                            text = connectionStatus.localizedLabel(s),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.inkMuted,
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = c.border)
        }
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector {
    return when (tab) {
        AppTab.TERMINAL -> if (selected) Icons.Rounded.Terminal else Icons.Outlined.Terminal
        AppTab.PLOTTER -> if (selected) Icons.Rounded.ShowChart else Icons.Outlined.ShowChart
        AppTab.LOGS -> if (selected) Icons.Rounded.Description else Icons.Outlined.Description
        AppTab.SETTINGS, AppTab.TERMINAL_SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
        AppTab.ABOUT -> if (selected) Icons.Rounded.Info else Icons.Outlined.Info
    }
}
