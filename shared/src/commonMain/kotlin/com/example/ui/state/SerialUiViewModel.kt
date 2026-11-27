package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExportFormat
import com.example.data.local.LogExporter
import com.example.data.local.SerialRepository
import com.example.data.local.SettingsRepository
import com.example.domain.protocol.MacroJsonCodec
import com.example.domain.protocol.PlotConfiguration
import com.example.domain.protocol.PlotProfile
import com.example.domain.protocol.PlotRecord
import com.example.domain.protocol.PlotStreamParser
import com.example.domain.protocol.builtInPlotProfiles
import com.example.domain.protocol.ProtocolEngine
import com.example.platform.PlatformClock
import com.example.platform.PlatformFeedback
import com.example.platform.PlatformFileGateway
import com.example.platform.PlatformFileHandle
import com.example.transport.SerialTransportBridge
import com.example.transport.TransportEvent
import com.example.transport.TransportPermissionPendingException
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.resolveAppStrings
import com.example.ui.model.AppLanguage
import com.example.ui.model.AppSettingsState
import com.example.ui.model.AppTab
import com.example.ui.model.AppThemeMode
import com.example.ui.model.CharsetEncoding
import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.DisplayMode
import com.example.ui.model.FileTransferDraft
import com.example.ui.model.FileTransferProgress
import com.example.ui.model.FrameDirection
import com.example.ui.model.LineEnding
import com.example.ui.model.LogTimeRange
import com.example.ui.model.MacroColorTag
import com.example.ui.model.MacroCommandItem
import com.example.ui.model.MacroGroupFilter
import com.example.ui.model.PlotChannel
import com.example.ui.model.PlotSample
import com.example.ui.model.PlotSamplingState
import com.example.ui.model.SavedLogSession
import com.example.ui.model.SavedLogSummary
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.TerminalFrame
import com.example.ui.model.TxFormat
import com.example.ui.model.TxHistoryItem
import com.example.ui.model.UsbDeviceItem
import com.example.ui.theme.ScopeChannelColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val LOG_PAGE_SIZE = 50
private const val PLOT_QUEUE_SIZE = 32
private const val PLOT_CHANNEL_LIMIT = 8
private const val PLOT_SAMPLE_LIMIT = 300

data class SerialUiState(
    val activeTab: AppTab = AppTab.TERMINAL,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val availableDevices: List<UsbDeviceItem> = emptyList(),
    val selectedDevice: UsbDeviceItem? = null,
    val configPresets: List<SerialPortConfig> = emptyList(),
    val currentConfig: SerialPortConfig = SerialPortConfig(),
    val rxByteCount: Long = 0L,
    val txByteCount: Long = 0L,
    val showConfigSheet: Boolean = false,
    val smartPickerPromptReason: String? = null,
    // Terminal State
    val displayMode: DisplayMode = DisplayMode.MIXED,
    val showTimestamps: Boolean = true,
    val autoScroll: Boolean = true,
    val showTrafficCounters: Boolean = false,
    val showFilterBar: Boolean = false,
    val filterKeyword: String = "",
    val highlightKeyword: String = "ERROR",
    val isRecordingSession: Boolean = false,
    val frames: List<TerminalFrame> = emptyList(),
    val inspectingFrame: TerminalFrame? = null,
    // TX Console State
    val txInput: String = "",
    val txFormat: TxFormat = TxFormat.HEX,
    val lineEnding: LineEnding = LineEnding.CRLF,
    val checksumMode: ChecksumMode = ChecksumMode.NONE,
    val txChecksumEndian: ChecksumEndian = ChecksumEndian.LITTLE_ENDIAN,
    val repeatEnabled: Boolean = false,
    val repeatIntervalMs: Int = 1000,
    val repeatMaxCount: Int = 0,
    val repeatSentCount: Int = 0,
    val fileTransferDraft: FileTransferDraft? = null,
    val fileTransferProgress: FileTransferProgress? = null,
    // Command History State
    val txHistory: List<TxHistoryItem> = emptyList(),
    val showHistorySheet: Boolean = false,
    val historySearchQuery: String = "",
    // Macros State (Integrated inside Terminal)
    val macroPanelExpanded: Boolean = false,
    val macros: List<MacroCommandItem> = emptyList(),
    val selectedMacroGroup: MacroGroupFilter = MacroGroupFilter.All,
    val editingMacro: MacroCommandItem? = null,
    val showMacroEditor: Boolean = false,
    // Plotter State
    val plotChannels: List<PlotChannel> = emptyList(),
    val plotSampling: PlotSamplingState = PlotSamplingState.IDLE,
    val plotterAutoScale: Boolean = true,
    val plotterWindowPoints: Int = 40,
    val plotProfiles: List<PlotProfile> = builtInPlotProfiles,
    val plotRunId: Long = 0,
    internal val plotLastSampleAt: TimeMark? = null,
    internal val pendingPlotGaps: Set<String> = emptySet(),
    // Logs State
    val savedLogs: List<SavedLogSummary> = emptyList(),
    val logTimeRange: LogTimeRange = LogTimeRange.LastSevenDays,
    val logsLoading: Boolean = false,
    val logsLoadFailed: Boolean = false,
    val logsHaveMore: Boolean = true,
    val logBeingOpened: SavedLogSummary? = null,
    val inspectingLog: SavedLogSession? = null,
    // Settings State
    val settings: AppSettingsState = AppSettingsState(),
    // Transient UI Banner / Snackbar message
    val uiNotice: String? = null,
) {
    val activePlotProfile: PlotProfile?
        get() = plotProfiles.firstOrNull { it.id == settings.plotProfileId }

    val checksumPreview: String
        get() = ProtocolEngine.computePreviewLabel(
            input = txInput,
            format = txFormat,
            lineEnding = lineEnding,
            checksumMode = checksumMode,
            endian = txChecksumEndian,
            charset = settings.charset,
        )

    val namedMacroGroups: List<String>
        get() = macros.map { it.group.trim() }.filter { it.isNotEmpty() }.distinct()

    val macroGroups: List<MacroGroupFilter>
        get() = buildList {
            add(MacroGroupFilter.All)
            if (macros.any { it.group.isBlank() }) add(MacroGroupFilter.Ungrouped)
            namedMacroGroups.mapTo(this) { MacroGroupFilter.Named(it) }
        }
}

class SerialUiViewModel(
    private val repository: SerialRepository,
    private val settingsRepository: SettingsRepository,
    internal val transportBridge: SerialTransportBridge,
    private val fileGateway: PlatformFileGateway,
    private val feedback: PlatformFeedback,
    private val systemLanguageTagProvider: () -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SerialUiState())
    val uiState: StateFlow<SerialUiState> = _uiState.asStateFlow()

    private var nextFrameId = 1L
    private var repeatJob: Job? = null
    private var fileSendJob: Job? = null
    private var recordingStartedAtMs: Long = 0L
    private val recordingBuffer = mutableListOf<TerminalFrame>()
    private var logPageJob: Job? = null
    private var logInspectJob: Job? = null
    private var logActionJob: Job? = null
    private var logQueryNowMs = PlatformClock.currentTimeMillis()
    private var settingsLoaded = false
    private var autoPromptedMultiUsbSignature: String? = null
    private var pendingLaunchedDeviceId: String? = null
    private val plotClock = TimeSource.Monotonic.markNow()
    private val plotInput = Channel<PlotInput>(PLOT_QUEUE_SIZE)

    private data class PlotInput(
        val run: Long,
        val profile: PlotProfile,
        val charset: CharsetEncoding,
        val timeMs: Long,
        val bytes: ByteArray,
    )

    init {
        viewModelScope.launch(Dispatchers.Default) { processPlotInput() }
        observePersistence()
        observeTransportStreams()
    }

    private fun observePersistence() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val isFirstLoad = !settingsLoaded
                settingsLoaded = true
                _uiState.update { it.reconcilePlot(it.copy(settings = settings)) }
                if (isFirstLoad) {
                    val launchId = pendingLaunchedDeviceId
                    evaluateSmartPickerAndAutoConnect(
                        triggeredByUsbAttachOrLaunch = launchId != null,
                        explicitLaunchedDeviceId = launchId,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.profilesFlow.collect { profiles ->
                _uiState.update { state ->
                    val current = if (state.configPresets.isEmpty() && profiles.isNotEmpty()) {
                        profiles.first()
                    } else {
                        state.currentConfig
                    }
                    state.copy(configPresets = profiles, currentConfig = current)
                }
            }
        }
        viewModelScope.launch {
            repository.macrosFlow.collect { macros ->
                _uiState.update { state ->
                    val validGroup = state.selectedMacroGroup.takeIf { filter ->
                        filter == MacroGroupFilter.All || macros.any { filter.matches(it.group) }
                    } ?: MacroGroupFilter.All
                    state.copy(macros = macros, selectedMacroGroup = validGroup)
                }
            }
        }
        viewModelScope.launch {
            repository.plotProfilesFlow.collect { profiles ->
                _uiState.update { it.reconcilePlot(it.copy(plotProfiles = builtInPlotProfiles + profiles)) }
            }
        }
        viewModelScope.launch {
            repository.historyFlow.collect { history ->
                _uiState.update { it.copy(txHistory = history) }
            }
        }
    }

    private fun observeTransportStreams() {
        viewModelScope.launch {
            transportBridge.usbDevices.collect { devices ->
                val targetLaunchId = pendingLaunchedDeviceId
                _uiState.update { state ->
                    val refreshedSelection = devices.firstOrNull { it.id == state.selectedDevice?.id }
                    val selected = if (state.connectionStatus == ConnectionStatus.DISCONNECTED) {
                        targetLaunchId?.let { id -> devices.firstOrNull { it.id == id } }
                            ?: refreshedSelection
                            ?: devices.singleOrNull()
                    } else {
                        refreshedSelection ?: state.selectedDevice
                    }
                    state.copy(availableDevices = devices, selectedDevice = selected)
                }
                if (settingsLoaded) {
                    evaluateSmartPickerAndAutoConnect(
                        triggeredByUsbAttachOrLaunch = false,
                        explicitLaunchedDeviceId = targetLaunchId,
                    )
                }
            }
        }

        viewModelScope.launch {
            transportBridge.rxFrames.collect { rawBytes ->
                if (rawBytes.isEmpty()) return@collect
                val stateSnapshot = _uiState.value
                enqueuePlotInput(stateSnapshot, rawBytes)
                val decodedText = ProtocolEngine.decodeRxText(rawBytes, stateSnapshot.settings.charset)
                val rxFrame = TerminalFrame(
                    id = nextFrameId++,
                    timestamp = nowTimestamp(),
                    direction = FrameDirection.RX,
                    rawBytes = rawBytes,
                    asciiText = decodedText,
                )

                if (stateSnapshot.isRecordingSession) {
                    recordingBuffer.add(rxFrame)
                }

                _uiState.update { state ->
                    val updatedFrames = (state.frames + rxFrame).takeLast(state.settings.maxBufferFrames)
                    state.copy(
                        frames = updatedFrames,
                        rxByteCount = state.rxByteCount + rawBytes.size,
                    )
                }
            }
        }

        viewModelScope.launch {
            transportBridge.events.collect { event ->
                val s = activeStrings()
                when (event) {
                    is TransportEvent.DeviceAttached -> {
                        val isDisconnected = _uiState.value.connectionStatus == ConnectionStatus.DISCONNECTED
                        _uiState.update { state ->
                            val selected = if (isDisconnected) {
                                event.device
                            } else {
                                state.selectedDevice
                            }
                            state.copy(selectedDevice = selected)
                        }
                        appendSystemFrame(s.vmSysUsbAttached(event.device.chipName, event.device.vidPid))
                        if (settingsLoaded) {
                            evaluateSmartPickerAndAutoConnect(
                                triggeredByUsbAttachOrLaunch = true,
                                explicitLaunchedDeviceId = event.device.id,
                            )
                        }
                    }

                    is TransportEvent.DeviceDetached -> {
                        if (pendingLaunchedDeviceId == event.deviceId) {
                            pendingLaunchedDeviceId = null
                        }
                        appendSystemFrame(s.vmSysUsbDetached)
                    }

                    is TransportEvent.PermissionResult -> {
                        if (!event.granted) {
                            _uiState.update {
                                it.copy(
                                    connectionStatus = ConnectionStatus.DISCONNECTED,
                                    uiNotice = s.vmNoticeUsbPermDenied,
                                )
                            }
                            triggerErrorHaptic()
                        }
                    }

                    is TransportEvent.Connected -> {
                        _uiState.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.CONNECTED,
                                selectedDevice = event.device,
                                uiNotice = s.vmNoticeConnected(event.device.chipName, event.config.compactSummary),
                            )
                        }
                        appendSystemFrame(s.vmSysPortOpened(event.device.chipName, event.config.compactSummary))
                    }

                    is TransportEvent.Disconnected -> {
                        stopRepeatJob()
                        cancelFileSendJob()
                        _uiState.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.DISCONNECTED,
                                repeatEnabled = false,
                                fileTransferProgress = null,
                                plotRunId = it.plotRunId + 1,
                                plotLastSampleAt = null,
                                uiNotice = event.reason,
                            )
                        }
                        appendSystemFrame(s.vmSysDisconnected(event.reason))
                        if (event.unexpected) {
                            triggerErrorHaptic()
                        }
                    }

                    is TransportEvent.Error -> {
                        _uiState.update { state ->
                            val nextStatus = if (state.connectionStatus == ConnectionStatus.CONNECTING) {
                                ConnectionStatus.DISCONNECTED
                            } else {
                                state.connectionStatus
                            }
                            state.copy(connectionStatus = nextStatus, uiNotice = event.message)
                        }
                        appendSystemFrame(s.vmSysError(event.message))
                        triggerErrorHaptic()
                    }
                }
            }
        }
    }

    fun handleLaunchedUsbDeviceKey(targetId: String) {
        if (targetId.isBlank()) return
        if (_uiState.value.connectionStatus != ConnectionStatus.DISCONNECTED) return
        pendingLaunchedDeviceId = targetId
        val devices = transportBridge.refreshUsbDevices()
        val matched = devices.firstOrNull { it.id == targetId }
        _uiState.update { state ->
            state.copy(
                activeTab = AppTab.TERMINAL,
                availableDevices = devices,
                selectedDevice = matched ?: state.selectedDevice,
            )
        }
        if (settingsLoaded) {
            evaluateSmartPickerAndAutoConnect(
                triggeredByUsbAttachOrLaunch = true,
                explicitLaunchedDeviceId = targetId,
            )
        }
    }

    private fun evaluateSmartPickerAndAutoConnect(
        triggeredByUsbAttachOrLaunch: Boolean,
        explicitLaunchedDeviceId: String? = null,
    ) {
        val state = _uiState.value
        val decision = SmartDevicePickerPolicy.evaluate(
            devices = state.availableDevices,
            connectionStatus = state.connectionStatus,
            autoConnectOnAttach = state.settings.autoConnectOnUsbAttach,
            isSheetAlreadyOpen = state.showConfigSheet,
            lastPromptedSignature = autoPromptedMultiUsbSignature,
            launchedDeviceId = explicitLaunchedDeviceId,
            strings = activeStrings(),
        )
        autoPromptedMultiUsbSignature = decision.nextSignature
        if (explicitLaunchedDeviceId != null && state.availableDevices.any { it.id == explicitLaunchedDeviceId }) {
            pendingLaunchedDeviceId = null
        }

        if (decision.shouldOpenPicker) {
            _uiState.update {
                it.copy(
                    showConfigSheet = true,
                    smartPickerPromptReason = decision.promptReason,
                )
            }
        } else if (triggeredByUsbAttachOrLaunch && decision.shouldAutoConnectSingleDevice) {
            connectSelectedDevice()
        }
    }

    fun selectTab(tab: AppTab) {
        val previous = _uiState.value.activeTab
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == AppTab.LOGS && previous != AppTab.LOGS) {
            reloadLogLibrary()
        } else if (previous == AppTab.LOGS && tab != AppTab.LOGS) {
            inspectLog(null)
        }
    }

    fun toggleConfigSheet(show: Boolean) {
        if (show) {
            transportBridge.refreshUsbDevices()
        }
        _uiState.update {
            it.copy(
                showConfigSheet = show,
                smartPickerPromptReason = if (show) it.smartPickerPromptReason else null,
            )
        }
    }

    fun refreshUsbDevices() {
        val list = transportBridge.refreshUsbDevices()
        val s = activeStrings()
        _uiState.update {
            it.copy(
                uiNotice = if (list.isEmpty()) {
                    s.vmNoticeNoUsbFound
                } else {
                    s.vmNoticeFoundDevices(list.size)
                },
            )
        }
    }

    fun toggleConnection() {
        val state = _uiState.value
        val s = activeStrings()
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            transportBridge.disconnect(s.vmUserDisconnectedReason, unexpected = false)
        } else {
            connectSelectedDevice()
        }
    }

    private fun connectSelectedDevice() {
        val devices = transportBridge.refreshUsbDevices()
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.DISCONNECTED) return
        val s = activeStrings()
        val device = devices.firstOrNull { it.id == state.selectedDevice?.id } ?: devices.singleOrNull()
        if (device == null) {
            _uiState.update {
                it.copy(
                    availableDevices = devices,
                    selectedDevice = null,
                    showConfigSheet = true,
                    uiNotice = if (devices.isEmpty()) s.vmNoticePlugUsbFirst else s.sheetGenericMultiBanner(devices.size),
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                availableDevices = devices,
                selectedDevice = device,
                connectionStatus = ConnectionStatus.CONNECTING,
            )
        }
        viewModelScope.launch {
            val result = transportBridge.connectUsb(device.id, state.currentConfig)
            result.onFailure { err ->
                if (err !is TransportPermissionPendingException) {
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.DISCONNECTED,
                            uiNotice = err.message ?: s.vmNoticeOpenPortFailed,
                        )
                    }
                    triggerErrorHaptic()
                }
            }
        }
    }

    fun selectDevice(device: UsbDeviceItem) {
        if (_uiState.value.connectionStatus != ConnectionStatus.DISCONNECTED) return
        _uiState.update { it.copy(selectedDevice = device) }
        if (!device.isPermissionGranted) {
            transportBridge.requestUsbPermission(device.id)
        }
    }

    fun applyConfigAndClose(config: SerialPortConfig) {
        val shouldSwitchToTerminal = _uiState.value.selectedDevice != null
        val s = activeStrings()
        _uiState.update {
            it.copy(
                currentConfig = config,
                showConfigSheet = false,
                smartPickerPromptReason = null,
                activeTab = if (shouldSwitchToTerminal) AppTab.TERMINAL else it.activeTab,
            )
        }
        val state = _uiState.value
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModelScope.launch {
                transportBridge.updateUsbParameters(config)
                    .onSuccess {
                        _uiState.update { it.copy(uiNotice = s.vmNoticeParamsUpdated(config.compactSummary)) }
                        appendSystemFrame(s.vmSysParamsSwitched(config.compactSummary))
                    }
                    .onFailure { err ->
                        _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeParamsFailed) }
                        triggerErrorHaptic()
                    }
            }
        } else if (state.selectedDevice != null) {
            connectSelectedDevice()
        } else {
            _uiState.update {
                it.copy(uiNotice = s.vmNoticeParamsSavedOffline(config.compactSummary))
            }
        }
    }

    fun saveConfigAsPreset(name: String, config: SerialPortConfig) {
        val s = activeStrings()
        val cleanName = name.trim().ifEmpty { s.vmCustomPresetDefaultName(config.baudRate) }
        val newPreset = config.copy(presetName = cleanName)
        viewModelScope.launch {
            repository.saveProfile(cleanName, newPreset)
            _uiState.update {
                it.copy(
                    currentConfig = newPreset,
                    uiNotice = s.vmNoticePresetSaved(cleanName),
                )
            }
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
    }

    fun toggleTimestamps() {
        _uiState.update { it.copy(showTimestamps = !it.showTimestamps) }
    }

    fun toggleAutoScroll() {
        _uiState.update { it.copy(autoScroll = !it.autoScroll) }
    }

    fun toggleTrafficCounters() {
        _uiState.update { it.copy(showTrafficCounters = !it.showTrafficCounters) }
    }

    fun toggleFilterBar() {
        _uiState.update { it.copy(showFilterBar = !it.showFilterBar) }
    }

    fun updateFilterKeyword(keyword: String) {
        _uiState.update { it.copy(filterKeyword = keyword) }
    }

    fun updateHighlightKeyword(keyword: String) {
        _uiState.update { it.copy(highlightKeyword = keyword) }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(frames = emptyList(), uiNotice = activeStrings().vmNoticeTerminalCleared) }
    }

    fun inspectFrame(frame: TerminalFrame?) {
        _uiState.update { it.copy(inspectingFrame = frame) }
    }

    fun copyFrameToTx(frame: TerminalFrame) {
        val s = activeStrings()
        _uiState.update { state ->
            val useHex = state.txFormat == TxFormat.HEX || frame.rawBytes.any { (it.toInt() and 0xFF) < 0x20 }
            val text = if (useHex) frame.hexText else frame.asciiText
            state.copy(
                txFormat = if (useHex) TxFormat.HEX else TxFormat.ASCII,
                txInput = text,
                inspectingFrame = null,
                activeTab = AppTab.TERMINAL,
                uiNotice = s.vmNoticeCopiedToTx,
            )
        }
    }

    fun toggleRecording() {
        val state = _uiState.value
        val s = activeStrings()
        if (!state.isRecordingSession) {
            recordingBuffer.clear()
            recordingStartedAtMs = PlatformClock.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isRecordingSession = true,
                    uiNotice = s.vmNoticeRecordingStarted,
                )
            }
        } else {
            val framesToSave = if (recordingBuffer.isNotEmpty()) {
                recordingBuffer.toList()
            } else {
                state.frames
            }
            recordingBuffer.clear()
            _uiState.update { it.copy(isRecordingSession = false) }
            if (framesToSave.isEmpty()) {
                _uiState.update { it.copy(uiNotice = s.vmNoticeRecordingEmpty) }
                return
            }
            val nowMs = PlatformClock.currentTimeMillis()
            val title = "${s.vmSessionTitlePrefix} ${PlatformClock.formatMonthDayTimeSec(nowMs)}"
            val deviceSummary = state.selectedDevice?.let { "${it.chipName} (${it.vidPid})" } ?: s.vmOfflineDeviceSummary
            viewModelScope.launch {
                repository.saveSessionLog(
                    title = title,
                    deviceSummary = deviceSummary,
                    configSummary = state.currentConfig.compactSummary,
                    startedAtMs = if (recordingStartedAtMs > 0L) recordingStartedAtMs else nowMs,
                    endedAtMs = nowMs,
                    frames = framesToSave,
                )
                if (_uiState.value.activeTab == AppTab.LOGS) reloadLogLibrary()
                _uiState.update { it.copy(uiNotice = s.vmNoticeLogSaved(framesToSave.size)) }
            }
        }
    }

    fun resetByteCounters() {
        _uiState.update {
            it.copy(
                rxByteCount = 0L,
                txByteCount = 0L,
                uiNotice = activeStrings().vmNoticeCountersReset,
            )
        }
    }

    fun toggleDtr() {
        val next = !_uiState.value.currentConfig.dtrEnabled
        val s = activeStrings()
        _uiState.update { it.copy(currentConfig = it.currentConfig.copy(dtrEnabled = next)) }
        if (_uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModelScope.launch {
                transportBridge.setDtr(next)
                    .onSuccess {
                        val label = if (next) s.vmPinHighLabel else s.vmPinLowLabel
                        appendSystemFrame(s.vmSysPinChanged("DTR", label))
                        _uiState.update { it.copy(uiNotice = s.vmNoticePinChanged("DTR", label)) }
                    }
                    .onFailure { err ->
                        _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticePinFailed("DTR")) }
                        triggerErrorHaptic()
                    }
            }
        } else {
            val shortState = if (next) s.vmPinShortHigh else s.vmPinShortLow
            _uiState.update { it.copy(uiNotice = s.vmNoticePinPreset("DTR", shortState)) }
        }
    }

    fun toggleRts() {
        val next = !_uiState.value.currentConfig.rtsEnabled
        val s = activeStrings()
        _uiState.update { it.copy(currentConfig = it.currentConfig.copy(rtsEnabled = next)) }
        if (_uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModelScope.launch {
                transportBridge.setRts(next)
                    .onSuccess {
                        val label = if (next) s.vmPinHighLabel else s.vmPinLowLabel
                        appendSystemFrame(s.vmSysPinChanged("RTS", label))
                        _uiState.update { it.copy(uiNotice = s.vmNoticePinChanged("RTS", label)) }
                    }
                    .onFailure { err ->
                        _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticePinFailed("RTS")) }
                        triggerErrorHaptic()
                    }
            }
        } else {
            val shortState = if (next) s.vmPinShortHigh else s.vmPinShortLow
            _uiState.update { it.copy(uiNotice = s.vmNoticePinPreset("RTS", shortState)) }
        }
    }

    fun updateTxInput(text: String) {
        val formatted = if (_uiState.value.txFormat == TxFormat.HEX) {
            ProtocolEngine.formatHexInputLive(text)
        } else {
            text
        }
        _uiState.update { it.copy(txInput = formatted) }
    }

    fun setTxFormat(format: TxFormat) {
        _uiState.update { state ->
            val updatedInput = if (format == TxFormat.HEX && state.txInput.isNotBlank()) {
                val asHex = ProtocolEngine.formatHexInputLive(state.txInput)
                asHex.ifEmpty { state.txInput }
            } else {
                state.txInput
            }
            state.copy(txFormat = format, txInput = updatedInput)
        }
    }

    fun setLineEnding(ending: LineEnding) {
        _uiState.update { it.copy(lineEnding = ending) }
    }

    fun setChecksumMode(mode: ChecksumMode) {
        _uiState.update { if (it.txFormat == TxFormat.HEX) it.copy(checksumMode = mode) else it }
    }

    fun toggleTxChecksumEndian() {
        _uiState.update { state ->
            if (state.txFormat != TxFormat.HEX) return@update state
            val next = if (state.txChecksumEndian == ChecksumEndian.LITTLE_ENDIAN) {
                ChecksumEndian.BIG_ENDIAN
            } else {
                ChecksumEndian.LITTLE_ENDIAN
            }
            state.copy(txChecksumEndian = next)
        }
    }

    fun toggleRepeatSend() {
        val state = _uiState.value
        val s = activeStrings()
        if (state.repeatEnabled) {
            stopRepeatJob()
            _uiState.update { it.copy(repeatEnabled = false, uiNotice = s.vmNoticeRepeatStopped(it.repeatSentCount)) }
            return
        }
        if (state.connectionStatus != ConnectionStatus.CONNECTED) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeConnectBeforeRepeat) }
            return
        }
        if (state.txInput.isBlank()) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeEnterRepeatPayload) }
            return
        }
        val maxLimit = state.repeatMaxCount.coerceAtLeast(0)
        val limitLabel = if (maxLimit > 0) s.vmRepeatLimitHint(maxLimit) else s.vmRepeatUnlimitedHint
        _uiState.update {
            it.copy(
                repeatEnabled = true,
                repeatSentCount = 0,
                uiNotice = s.vmNoticeRepeatStarted(state.repeatIntervalMs, limitLabel),
            )
        }
        repeatJob = viewModelScope.launch {
            var sent = 0
            while (isActive && _uiState.value.repeatEnabled) {
                val ok = sendCurrentConsoleFrameInternal(preserveInput = true)
                if (!ok) {
                    _uiState.update { it.copy(repeatEnabled = false) }
                    break
                }
                sent++
                _uiState.update { it.copy(repeatSentCount = sent) }
                val configuredMax = _uiState.value.repeatMaxCount
                if (configuredMax > 0 && sent >= configuredMax) {
                    val doneStrings = activeStrings()
                    _uiState.update {
                        it.copy(
                            repeatEnabled = false,
                            uiNotice = doneStrings.vmNoticeRepeatFinished(sent),
                        )
                    }
                    break
                }
                delay(_uiState.value.repeatIntervalMs.toLong())
            }
        }
    }

    fun updateRepeatSettings(intervalMs: Int, maxCount: Int) {
        _uiState.update {
            it.copy(
                repeatIntervalMs = intervalMs.coerceIn(50, 60000),
                repeatMaxCount = maxCount.coerceIn(0, 100000),
            )
        }
    }

    fun sendPreviewFrame() {
        viewModelScope.launch {
            sendCurrentConsoleFrameInternal(preserveInput = false)
        }
    }

    private suspend fun sendCurrentConsoleFrameInternal(preserveInput: Boolean = false): Boolean {
        val state = _uiState.value
        val s = activeStrings()
        if (state.connectionStatus != ConnectionStatus.CONNECTED) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeConnectBeforeSend) }
            return false
        }
        val cleanInput = state.txInput.trim()
        val packetResult = ProtocolEngine.buildTxPacket(
            input = state.txInput,
            format = state.txFormat,
            lineEnding = state.lineEnding,
            checksumMode = state.checksumMode,
            endian = state.txChecksumEndian,
            skipBytes = 0,
            charset = state.settings.charset,
        )
        val packet = packetResult.getOrElse {
            val message = when {
                cleanInput.isEmpty() -> s.vmNoticeTxPayloadEmpty
                state.txFormat == TxFormat.HEX -> s.vmNoticeMacroFormatInvalid
                else -> s.vmNoticeSendFormatError
            }
            _uiState.update { it.copy(uiNotice = message) }
            triggerErrorHaptic()
            return false
        }

        val writeRes = writeCommandFrame(packet.fullBytes)
        return writeRes.fold(
            onSuccess = {
                val display = if (packet.checksumHex.isNotEmpty()) {
                    "$cleanInput [${packet.checksumHex}]"
                } else {
                    cleanInput
                }
                val txFrame = TerminalFrame(
                    id = nextFrameId++,
                    timestamp = nowTimestamp(),
                    direction = FrameDirection.TX,
                    rawBytes = packet.fullBytes,
                    asciiText = display,
                )
                if (state.isRecordingSession) {
                    recordingBuffer.add(txFrame)
                }
                val effectiveEnding = if (state.txFormat == TxFormat.HEX) LineEnding.NONE else state.lineEnding
                repository.recordCommandHistory(
                    payload = cleanInput,
                    txFormat = state.txFormat,
                    lineEnding = effectiveEnding,
                    checksumMode = state.checksumMode,
                    checksumEndian = state.txChecksumEndian,
                    sentBytes = packet.fullBytes,
                )
                _uiState.update { st ->
                    st.copy(
                        frames = (st.frames + txFrame).takeLast(st.settings.maxBufferFrames),
                        txByteCount = st.txByteCount + packet.fullBytes.size,
                        txInput = if (st.settings.clearInputAfterSend && !preserveInput) "" else st.txInput,
                    )
                }
                true
            },
            onFailure = { err ->
                _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeWriteFailed) }
                triggerErrorHaptic()
                false
            },
        )
    }

    private suspend fun writeCommandFrame(bytes: ByteArray): Result<Unit> =
        transportBridge.write(bytes).mapCatching { written ->
            check(written == bytes.size) { activeStrings().vmNoticeWriteFailed }
        }

    // Chunked Local File Transfer
    fun prepareFileSend(handle: PlatformFileHandle) {
        viewModelScope.launch {
            val inspected = fileGateway.inspectFile(handle)
            val size = inspected.sizeBytes
            if (size <= 0L) {
                _uiState.update { it.copy(uiNotice = activeStrings().vmNoticeFileEmpty) }
                return@launch
            }
            val name = inspected.displayName.ifEmpty { "serial_payload.bin" }
            val suggestLineMode = name.endsWith(".txt", ignoreCase = true) ||
                name.endsWith(".at", ignoreCase = true) ||
                name.endsWith(".log", ignoreCase = true)
            _uiState.update {
                it.copy(
                    fileTransferDraft = FileTransferDraft(
                        uriString = inspected.rawUri,
                        fileName = name,
                        totalBytes = size,
                        chunkSizeBytes = 256,
                        chunkDelayMs = 20,
                        sendLineByLine = suggestLineMode,
                    ),
                )
            }
        }
    }

    fun dismissFileSendDraft() {
        _uiState.update { it.copy(fileTransferDraft = null) }
    }

    fun startFileSend(chunkSizeBytes: Int, chunkDelayMs: Int, sendLineByLine: Boolean) {
        val draft = _uiState.value.fileTransferDraft ?: return
        val s = activeStrings()
        if (_uiState.value.connectionStatus != ConnectionStatus.CONNECTED) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeConnectBeforeFileSend) }
            return
        }
        cancelFileSendJob()
        val safeChunk = chunkSizeBytes.coerceIn(32, 4096)
        val safeDelay = chunkDelayMs.coerceIn(0, 5000)
        _uiState.update {
            it.copy(
                fileTransferDraft = null,
                fileTransferProgress = FileTransferProgress(draft.fileName, 0L, draft.totalBytes),
            )
        }
        appendSystemFrame(s.vmSysFileSendStart(draft.fileName, draft.totalBytes))

        fileSendJob = viewModelScope.launch {
            var totalSent = 0L
            var failedMessage: String? = null
            val handle = PlatformFileHandle(
                rawUri = draft.uriString,
                displayName = draft.fileName,
                sizeBytes = draft.totalBytes,
            )
            val fileBytes = fileGateway.readFileBytes(handle).getOrElse { err ->
                _uiState.update {
                    it.copy(
                        fileTransferProgress = null,
                        uiNotice = err.message ?: s.vmNoticeMacroReadFailed,
                    )
                }
                triggerErrorHaptic()
                return@launch
            }

            if (sendLineByLine) {
                val charset = _uiState.value.settings.charset
                val suffix = _uiState.value.lineEnding.suffix
                val lines = ProtocolEngine.decodeBytes(fileBytes, charset).split(Regex("\r?\n"))
                for (line in lines) {
                    if (!isActive) break
                    if (line.isEmpty()) continue
                    val payload = ProtocolEngine.encodeText(line + suffix, charset)
                    val writeRes = transportBridge.write(payload)
                    if (writeRes.isFailure) {
                        failedMessage = writeRes.exceptionOrNull()?.message ?: s.vmFileChunkWriteError
                        break
                    }
                    totalSent += payload.size
                    _uiState.update { st ->
                        st.copy(
                            txByteCount = st.txByteCount + payload.size,
                            fileTransferProgress = FileTransferProgress(
                                fileName = draft.fileName,
                                sentBytes = totalSent.coerceAtMost(draft.totalBytes),
                                totalBytes = draft.totalBytes,
                            ),
                        )
                    }
                    if (safeDelay > 0) delay(safeDelay.toLong())
                }
            } else {
                var offset = 0
                while (isActive && offset < fileBytes.size) {
                    val end = (offset + safeChunk).coerceAtMost(fileBytes.size)
                    val slice = fileBytes.copyOfRange(offset, end)
                    val writeRes = transportBridge.write(slice)
                    if (writeRes.isFailure) {
                        failedMessage = writeRes.exceptionOrNull()?.message ?: s.vmFileChunkWriteError
                        break
                    }
                    offset = end
                    totalSent = offset.toLong()
                    _uiState.update { st ->
                        st.copy(
                            txByteCount = st.txByteCount + slice.size,
                            fileTransferProgress = FileTransferProgress(
                                fileName = draft.fileName,
                                sentBytes = totalSent,
                                totalBytes = fileBytes.size.toLong(),
                            ),
                        )
                    }
                    if (safeDelay > 0 && offset < fileBytes.size) delay(safeDelay.toLong())
                }
            }

            val doneStrings = activeStrings()
            _uiState.update { it.copy(fileTransferProgress = null) }
            if (failedMessage != null) {
                appendSystemFrame(doneStrings.vmSysFileSendFailed(failedMessage, totalSent))
                _uiState.update { it.copy(uiNotice = failedMessage) }
                triggerErrorHaptic()
            } else {
                appendSystemFrame(doneStrings.vmSysFileSendDone(draft.fileName, totalSent))
                _uiState.update { it.copy(uiNotice = doneStrings.vmNoticeFileSendDone(draft.fileName)) }
            }
        }
    }

    fun cancelFileSend() {
        val currentName = _uiState.value.fileTransferProgress?.fileName
        val s = activeStrings()
        cancelFileSendJob()
        _uiState.update {
            it.copy(
                fileTransferProgress = null,
                uiNotice = if (currentName != null) s.vmNoticeFileSendCancelled(currentName) else null,
            )
        }
        if (currentName != null) {
            appendSystemFrame(s.vmSysFileSendCancelled(currentName))
        }
    }

    // Command History Actions
    fun toggleHistorySheet(show: Boolean) {
        _uiState.update { it.copy(showHistorySheet = show) }
    }

    fun updateHistorySearchQuery(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun refillFromHistory(item: TxHistoryItem) {
        _uiState.update {
            it.copy(
                txInput = item.payload,
                txFormat = item.txFormat,
                lineEnding = if (item.txFormat == TxFormat.ASCII) item.lineEnding else it.lineEnding,
                checksumMode = if (item.txFormat == TxFormat.HEX) item.checksumMode else it.checksumMode,
                txChecksumEndian = if (item.txFormat == TxFormat.HEX) item.checksumEndian else it.txChecksumEndian,
                showHistorySheet = false,
                uiNotice = activeStrings().vmNoticeHistoryRefilled,
            )
        }
    }

    fun resendFromHistory(item: TxHistoryItem) {
        val state = _uiState.value
        val s = activeStrings()
        if (state.connectionStatus != ConnectionStatus.CONNECTED) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeConnectBeforeSend) }
            return
        }
        viewModelScope.launch {
            val packet = ProtocolEngine.buildTxPacket(
                input = item.payload,
                format = item.txFormat,
                lineEnding = item.lineEnding,
                checksumMode = item.checksumMode,
                endian = item.checksumEndian,
                skipBytes = 0,
                charset = state.settings.charset,
            ).getOrElse {
                _uiState.update { it.copy(uiNotice = s.vmNoticeHistoryFormatError) }
                return@launch
            }

            writeCommandFrame(packet.fullBytes).onSuccess {
                val display = if (packet.checksumHex.isNotEmpty()) {
                    "${item.payload} [${packet.checksumHex}]"
                } else {
                    item.payload
                }
                val txFrame = TerminalFrame(
                    id = nextFrameId++,
                    timestamp = nowTimestamp(),
                    direction = FrameDirection.TX,
                    rawBytes = packet.fullBytes,
                    asciiText = display,
                )
                if (_uiState.value.isRecordingSession) {
                    recordingBuffer.add(txFrame)
                }
                repository.recordCommandHistory(
                    payload = item.payload,
                    txFormat = item.txFormat,
                    lineEnding = item.lineEnding,
                    checksumMode = item.checksumMode,
                    checksumEndian = item.checksumEndian,
                    sentBytes = packet.fullBytes,
                )
                _uiState.update { st ->
                    st.copy(
                        frames = (st.frames + txFrame).takeLast(st.settings.maxBufferFrames),
                        txByteCount = st.txByteCount + packet.fullBytes.size,
                        showHistorySheet = false,
                        uiNotice = s.vmNoticeHistoryResent,
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeHistoryResendFailed) }
                triggerErrorHaptic()
            }
        }
    }

    fun promoteHistoryToMacro(item: TxHistoryItem) {
        val s = activeStrings()
        val sentHex = item.sentHex
        if (sentHex.isNullOrEmpty()) {
            _uiState.update { it.copy(showHistorySheet = false, uiNotice = s.vmNoticeHistoryMissingSentBytes) }
            return
        }
        val draft = MacroCommandItem(
            id = 0L,
            title = "",
            group = "",
            payload = sentHex,
            txFormat = TxFormat.HEX,
            lineEnding = LineEnding.NONE,
            checksumMode = ChecksumMode.NONE,
            colorTag = MacroColorTag.COBALT,
        )
        _uiState.update {
            it.copy(
                showHistorySheet = false,
                editingMacro = draft,
                showMacroEditor = true,
            )
        }
    }

    fun clearTxHistory() {
        viewModelScope.launch {
            repository.clearCommandHistory()
            _uiState.update { it.copy(uiNotice = activeStrings().vmNoticeHistoryCleared) }
        }
    }

    // Macro Actions
    fun toggleMacroPanel(show: Boolean) {
        _uiState.update { it.copy(macroPanelExpanded = show) }
    }

    fun selectMacroGroup(group: MacroGroupFilter) {
        _uiState.update { it.copy(selectedMacroGroup = group) }
    }

    fun fireMacro(macro: MacroCommandItem) {
        val state = _uiState.value
        val s = activeStrings()
        if (state.connectionStatus != ConnectionStatus.CONNECTED) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeConnectBeforeMacro(macro.title)) }
            return
        }
        if (!macro.hasFixedFrame) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeMacroNeedsResave) }
            return
        }
        val bytes = ProtocolEngine.parseHexString(macro.payload)
        if (bytes == null || bytes.isEmpty()) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeMacroFormatInvalid) }
            triggerErrorHaptic()
            return
        }
        viewModelScope.launch {
            writeCommandFrame(bytes).onSuccess {
                val txFrame = TerminalFrame(
                    id = nextFrameId++,
                    timestamp = nowTimestamp(),
                    direction = FrameDirection.TX,
                    rawBytes = bytes,
                    asciiText = "${s.vmMacroFrameTag(macro.title)} ${macro.payload}",
                )
                if (_uiState.value.isRecordingSession) {
                    recordingBuffer.add(txFrame)
                }
                repository.recordCommandHistory(
                    payload = macro.payload,
                    txFormat = TxFormat.HEX,
                    lineEnding = LineEnding.NONE,
                    checksumMode = ChecksumMode.NONE,
                    checksumEndian = ChecksumEndian.LITTLE_ENDIAN,
                    sentBytes = bytes,
                )
                _uiState.update { st ->
                    st.copy(
                        frames = (st.frames + txFrame).takeLast(st.settings.maxBufferFrames),
                        txByteCount = st.txByteCount + bytes.size,
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeMacroSendFailed) }
                triggerErrorHaptic()
            }
        }
    }

    fun openMacroEditor(macro: MacroCommandItem?) {
        val draft = if (macro != null && !macro.hasFixedFrame) {
            macro.toFixedFrame(_uiState.value.settings.charset) ?: macro
        } else {
            macro
        }
        _uiState.update { it.copy(editingMacro = draft, showMacroEditor = true) }
    }

    fun closeMacroEditor() {
        _uiState.update { it.copy(editingMacro = null, showMacroEditor = false) }
    }

    fun saveMacro(
        id: Long?,
        title: String,
        group: String,
        payload: String,
        txFormat: TxFormat,
        lineEnding: LineEnding,
        colorTag: MacroColorTag,
    ) {
        val s = activeStrings()
        val cleanTitle = title.trim().ifEmpty { s.vmMacroDefaultTitle }
        val cleanGroup = group.trim()
        if (payload.isBlank()) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeMacroPayloadEmpty) }
            return
        }
        val item = MacroCommandItem(
            id = id ?: 0L,
            title = cleanTitle,
            group = cleanGroup,
            payload = payload,
            txFormat = txFormat,
            lineEnding = lineEnding,
            checksumMode = ChecksumMode.NONE,
            colorTag = colorTag,
        ).toFixedFrame(_uiState.value.settings.charset) ?: run {
            _uiState.update { it.copy(uiNotice = s.vmNoticeMacroFormatInvalid) }
            return
        }
        viewModelScope.launch {
            repository.upsertMacro(item)
            _uiState.update {
                it.copy(
                    editingMacro = null,
                    showMacroEditor = false,
                    uiNotice = s.vmNoticeMacroSaved(cleanTitle),
                )
            }
        }
    }

    private fun MacroCommandItem.toFixedFrame(charset: CharsetEncoding): MacroCommandItem? {
        val packet = ProtocolEngine.buildTxPacket(
            input = payload,
            format = txFormat,
            lineEnding = lineEnding,
            checksumMode = checksumMode,
            endian = checksumEndian,
            skipBytes = checksumSkipBytes,
            charset = charset,
        ).getOrNull() ?: return null
        return copy(
            payload = ProtocolEngine.formatHex(packet.fullBytes),
            txFormat = TxFormat.HEX,
            lineEnding = LineEnding.NONE,
            checksumMode = ChecksumMode.NONE,
            checksumEndian = ChecksumEndian.LITTLE_ENDIAN,
            checksumSkipBytes = 0,
        )
    }

    fun deleteMacro(id: Long) {
        viewModelScope.launch {
            repository.deleteMacro(id)
            _uiState.update {
                it.copy(
                    editingMacro = null,
                    showMacroEditor = false,
                    uiNotice = activeStrings().vmNoticeMacroDeleted,
                )
            }
        }
    }

    fun exportMacros(target: PlatformFileHandle) {
        val macros = _uiState.value.macros
        val s = activeStrings()
        if (macros.isEmpty()) {
            _uiState.update { it.copy(uiNotice = s.vmNoticeMacroExportEmpty) }
            return
        }
        viewModelScope.launch {
            val json = MacroJsonCodec.encodeToJson(macros)
            fileGateway.writeText(target, json)
                .onSuccess {
                    _uiState.update { it.copy(uiNotice = s.vmNoticeMacroExported(macros.size)) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeMacroExportFailed) }
                }
        }
    }

    fun importMacros(source: PlatformFileHandle) {
        val charset = _uiState.value.settings.charset
        viewModelScope.launch {
            val s = activeStrings()
            val rawText = fileGateway.readText(source).getOrElse { err ->
                _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeMacroReadFailed) }
                return@launch
            }
            val parsed = MacroJsonCodec.decodeFromJson(rawText).getOrElse {
                _uiState.update { it.copy(uiNotice = s.vmNoticeMacroJsonInvalid) }
                return@launch
            }
            val fixed = parsed.mapNotNull { it.toFixedFrame(charset) }
            if (fixed.isEmpty()) {
                _uiState.update { it.copy(uiNotice = s.vmNoticeMacroJsonInvalid) }
                return@launch
            }
            val insertedCount = repository.importMacros(fixed)
            val msg = if (insertedCount > 0) {
                s.vmNoticeMacroImported(insertedCount)
            } else {
                s.vmNoticeMacroAllExist
            }
            _uiState.update { it.copy(uiNotice = msg) }
        }
    }

    // Plotter Actions
    fun selectPlotProfile(id: Long) {
        if (_uiState.value.plotProfiles.none { it.id == id }) return
        viewModelScope.launch { storePlotSelection(id) }
    }

    suspend fun savePlotProfile(id: Long?, configuration: PlotConfiguration): Boolean = try {
        val savedId = repository.savePlotProfile(id, configuration)
        storePlotSelection(savedId)
        true
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    private suspend fun storePlotSelection(id: Long) {
        try {
            settingsRepository.update { it.copy(plotProfileId = id) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            postNotice(activeStrings().plotProfileSelectionFailed)
        }
    }

    fun deletePlotProfile(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            try {
                repository.deletePlotProfile(id)
                settingsRepository.update { settings ->
                    if (settings.plotProfileId == id) settings.copy(plotProfileId = null) else settings
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                postNotice(activeStrings().plotProfileDeleteFailed)
            }
        }
    }

    private fun SerialUiState.reconcilePlot(next: SerialUiState): SerialUiState =
        if (activePlotProfile != next.activePlotProfile ||
            (activePlotProfile?.isBuiltIn == true && settings.charset != next.settings.charset)
        ) {
            next.copy(
                plotSampling = if (next.activePlotProfile == null) PlotSamplingState.IDLE else next.plotSampling,
                plotChannels = emptyList(),
                pendingPlotGaps = emptySet(),
                plotRunId = plotRunId + 1,
                plotLastSampleAt = null,
            )
        } else next

    private fun enqueuePlotInput(state: SerialUiState, bytes: ByteArray) {
        val profile = state.activePlotProfile ?: return
        if (state.plotSampling != PlotSamplingState.RUNNING) return
        val input = PlotInput(state.plotRunId, profile, state.settings.charset, plotClock.elapsedNow().inWholeMilliseconds, bytes)
        if (plotInput.trySend(input).isFailure) {
            val notice = activeStrings().plotOverloaded
            _uiState.update {
                if (it.plotRunId == input.run) {
                    it.copy(plotSampling = PlotSamplingState.PAUSED, plotRunId = it.plotRunId + 1, uiNotice = notice)
                } else it
            }
        }
    }

    private suspend fun processPlotInput() {
        var run = -1L
        var parser: PlotStreamParser? = null
        var pendingGap = false
        for (first in plotInput) {
            delay(40)
            val batch = buildList {
                add(first)
                repeat(PLOT_QUEUE_SIZE) { add(plotInput.tryReceive().getOrNull() ?: return@buildList) }
            }
            val activeRun = _uiState.value.plotRunId
            val records = mutableListOf<Pair<Long, PlotRecord>>()
            for (input in batch) {
                if (input.run != activeRun) continue
                if (run != input.run) {
                    pendingGap = true
                    parser = PlotStreamParser(input.profile)
                    run = input.run
                }
                parser?.feed(input.bytes, input.charset) { record ->
                    records += input.timeMs to record.copy(gapBefore = record.gapBefore || pendingGap)
                    pendingGap = false
                }
            }
            if (records.isEmpty()) continue
            _uiState.update { state ->
                if (state.plotRunId == activeRun && state.plotSampling == PlotSamplingState.RUNNING) {
                    state.withPlotRecords(records)
                } else state
            }
        }
    }

    private fun SerialUiState.withPlotRecords(records: List<Pair<Long, PlotRecord>>): SerialUiState {
        val channels = plotChannels.associateByTo(linkedMapOf()) { it.key }
        val samples = mutableMapOf<String, MutableList<PlotSample>>()
        val gaps = pendingPlotGaps.toMutableSet()
        for ((timeMs, record) in records) {
            if (record.gapBefore) gaps.addAll(channels.keys)
            for (value in record.values) {
                if (value.key !in channels && channels.size == PLOT_CHANNEL_LIMIT) continue
                channels.getOrPut(value.key) {
                    val index = channels.size
                    val color = ScopeChannelColors[index % ScopeChannelColors.size]
                    PlotChannel(index, value.name, value.unit, color, true, emptyList(), value.key)
                }
                samples.getOrPut(value.key) { mutableListOf() }
                    .add(PlotSample(timeMs, value.value, gaps.remove(value.key)))
            }
        }
        val updated = channels.values.map { channel ->
            val added = samples[channel.key] ?: return@map channel
            channel.copy(samples = (channel.samples + added).takeLast(PLOT_SAMPLE_LIMIT))
        }
        return copy(
            plotChannels = updated,
            pendingPlotGaps = gaps,
            plotLastSampleAt = if (samples.isEmpty()) plotLastSampleAt else TimeSource.Monotonic.markNow(),
        )
    }

    fun togglePlotChannel(index: Int) {
        _uiState.update { state ->
            state.copy(
                plotChannels = state.plotChannels.map {
                    if (it.index == index) it.copy(visible = !it.visible) else it
                },
            )
        }
    }

    fun togglePlotSampling() {
        _uiState.update { state ->
            if (state.activePlotProfile == null) return@update state
            val sampling = when (state.plotSampling) {
                PlotSamplingState.IDLE, PlotSamplingState.PAUSED -> PlotSamplingState.RUNNING
                PlotSamplingState.RUNNING -> PlotSamplingState.PAUSED
            }
            state.copy(plotSampling = sampling, plotRunId = state.plotRunId + 1, plotLastSampleAt = null)
        }
    }

    fun togglePlotterAutoScale() {
        _uiState.update { it.copy(plotterAutoScale = !it.plotterAutoScale) }
    }

    fun adjustPlotterWindow(delta: Int) {
        _uiState.update { state ->
            state.copy(plotterWindowPoints = (state.plotterWindowPoints + delta).coerceIn(10, 120))
        }
    }

    fun clearPlotSamples() {
        _uiState.update { state ->
            state.copy(
                plotChannels = emptyList(),
                pendingPlotGaps = emptySet(),
                plotRunId = state.plotRunId + 1,
                plotLastSampleAt = null,
                uiNotice = activeStrings().vmNoticePlotCleared,
            )
        }
    }

    // Logs & SAF Export Actions
    fun changeLogTimeRange(range: LogTimeRange) {
        reloadLogLibrary(range)
    }

    private fun reloadLogLibrary(range: LogTimeRange = _uiState.value.logTimeRange) {
        logPageJob?.cancel()
        logQueryNowMs = PlatformClock.currentTimeMillis()
        _uiState.update {
            it.copy(
                logTimeRange = range,
                savedLogs = emptyList(),
                logsLoading = false,
                logsLoadFailed = false,
                logsHaveMore = true,
            )
        }
        loadMoreLogs()
    }

    fun loadMoreLogs() {
        val state = _uiState.value
        if (state.logsLoading || !state.logsHaveMore) return
        val now = logQueryNowMs
        val (from, until) = when (val range = state.logTimeRange) {
            LogTimeRange.All -> Long.MIN_VALUE to Long.MAX_VALUE
            LogTimeRange.LastSevenDays -> (now - 7 * 24 * 60 * 60 * 1000L) to (now + 1)
            LogTimeRange.Today -> PlatformClock.startOfLogDay(now) to (now + 1)
            is LogTimeRange.Custom -> range.fromInclusive to range.untilExclusive
        }
        _uiState.update { it.copy(logsLoading = true, logsLoadFailed = false) }
        logPageJob = viewModelScope.launch {
            val result = runCatching {
                repository.loadLogPage(from, until, state.savedLogs.lastOrNull(), LOG_PAGE_SIZE + 1)
            }
            ensureActive()
            _uiState.update { current ->
                val page = result.getOrNull()
                current.copy(
                    savedLogs = if (page == null) current.savedLogs else current.savedLogs + page.take(LOG_PAGE_SIZE),
                    logsLoading = false,
                    logsLoadFailed = page == null,
                    logsHaveMore = page == null || page.size > LOG_PAGE_SIZE,
                )
            }
        }
    }

    fun inspectLog(summary: SavedLogSummary?) {
        logInspectJob?.cancel()
        _uiState.update { it.copy(logBeingOpened = summary, inspectingLog = null) }
        if (summary == null) return
        logInspectJob = viewModelScope.launch {
            val session = runCatching { repository.loadLogSession(summary.id) }.getOrNull()
            ensureActive()
            _uiState.update {
                it.copy(
                    logBeingOpened = null,
                    inspectingLog = session,
                    uiNotice = if (session == null) activeStrings().logsOpenFailed else it.uiNotice,
                )
            }
        }
    }

    fun withSavedLog(id: Long, onLoaded: (SavedLogSession) -> Unit) {
        if (logActionJob?.isActive == true) return
        logActionJob = viewModelScope.launch {
            val session = _uiState.value.inspectingLog?.takeIf { it.id == id }
                ?: runCatching { repository.loadLogSession(id) }.getOrNull()
            ensureActive()
            if (session == null) postNotice(activeStrings().logsOpenFailed) else onLoaded(session)
        }
    }

    fun deleteLogSession(id: Long) {
        viewModelScope.launch {
            val deleted = runCatching { repository.deleteSessionLog(id) }.isSuccess
            ensureActive()
            if (!deleted) {
                postNotice(activeStrings().logsDeleteFailed)
                return@launch
            }
            if (_uiState.value.inspectingLog?.id == id || _uiState.value.logBeingOpened?.id == id) inspectLog(null)
            logPageJob?.cancel()
            _uiState.update { state ->
                state.copy(
                    savedLogs = state.savedLogs.filterNot { it.id == id },
                    logsLoading = false,
                )
            }
            if (_uiState.value.activeTab == AppTab.LOGS && _uiState.value.savedLogs.isEmpty()) loadMoreLogs()
            postNotice(activeStrings().vmNoticeLogDeleted)
        }
    }

    fun exportSession(target: PlatformFileHandle, session: SavedLogSession, format: ExportFormat) {
        viewModelScope.launch {
            val s = activeStrings()
            val content = LogExporter.formatContent(
                session = session,
                format = format,
                strings = s,
            )
            fileGateway.writeText(target, content)
                .onSuccess {
                    _uiState.update { it.copy(uiNotice = s.vmNoticeExportedFormat(format.extension.uppercase())) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeExportFailed) }
                }
        }
    }

    fun shareSession(
        session: SavedLogSession,
        format: ExportFormat = ExportFormat.TXT,
        onTargetReady: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val s = activeStrings()
            val fileName = LogExporter.buildSuggestedFileName(session, format)
            val content = LogExporter.formatContent(
                session = session,
                format = format,
                strings = s,
            )
            fileGateway.createShareTarget(fileName, content)
                .onSuccess { targetUri ->
                    onTargetReady(targetUri)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(uiNotice = err.message ?: s.vmNoticeSharePrepFailed) }
                }
        }
    }

    // Settings Actions (Persisted in DataStore)
    fun updateAppLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.update { it.copy(appLanguage = language) } }
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.update { it.copy(themeMode = themeMode) } }
    }

    fun updateCharset(charset: CharsetEncoding) {
        viewModelScope.launch { settingsRepository.update { it.copy(charset = charset) } }
    }

    fun updateTerminalFontSize(fontSizeSp: Float) {
        viewModelScope.launch { settingsRepository.update { it.copy(terminalFontSizeSp = fontSizeSp) } }
    }

    fun updateFrameTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch { settingsRepository.update { it.copy(frameTimeoutMs = timeoutMs) } }
    }

    fun updateMaxBufferFrames(maxFrames: Int) {
        viewModelScope.launch { settingsRepository.update { it.copy(maxBufferFrames = maxFrames) } }
    }

    fun toggleAutoConnectUsb() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(autoConnectOnUsbAttach = !it.autoConnectOnUsbAttach) }
        }
    }

    fun toggleKeepScreenOn() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(keepScreenOn = !it.keepScreenOn) }
        }
    }

    fun toggleClearInputAfterSend() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(clearInputAfterSend = !it.clearInputAfterSend) }
        }
    }

    fun toggleVibrateOnError() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(vibrateOnError = !it.vibrateOnError) }
        }
    }

    fun resetTerminalSettingsToDefault() {
        viewModelScope.launch {
            settingsRepository.resetTerminalDefaults()
            postNotice(activeStrings().vmNoticeSettingsReset)
        }
    }

    fun postNotice(message: String) {
        _uiState.update { it.copy(uiNotice = message) }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(uiNotice = null) }
    }

    private fun activeStrings(): AppStrings {
        return resolveAppStrings(_uiState.value.settings.appLanguage, systemLanguageTagProvider())
    }

    private fun appendSystemFrame(message: String) {
        val sysFrame = TerminalFrame(
            id = nextFrameId++,
            timestamp = nowTimestamp(),
            direction = FrameDirection.SYS,
            rawBytes = message.encodeToByteArray(),
            asciiText = message,
        )
        if (_uiState.value.isRecordingSession) {
            recordingBuffer.add(sysFrame)
        }
        _uiState.update { state ->
            state.copy(frames = (state.frames + sysFrame).takeLast(state.settings.maxBufferFrames))
        }
    }

    private fun stopRepeatJob() {
        repeatJob?.cancel()
        repeatJob = null
    }

    private fun cancelFileSendJob() {
        fileSendJob?.cancel()
        fileSendJob = null
    }

    private fun triggerErrorHaptic() {
        if (_uiState.value.settings.vibrateOnError) {
            feedback.triggerErrorHaptic()
        }
    }

    private fun nowTimestamp(): String =
        PlatformClock.formatTimestampHmsMillis(PlatformClock.currentTimeMillis())

    override fun onCleared() {
        super.onCleared()
        plotInput.cancel()
        stopRepeatJob()
        cancelFileSendJob()
        transportBridge.release()
    }
}
