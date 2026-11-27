package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.platform.PlatformClock
import com.example.ui.model.AppLanguage
import com.example.ui.model.AppTab
import com.example.ui.model.AppThemeMode
import com.example.ui.model.CharsetEncoding
import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.LineEnding
import com.example.ui.model.MacroColorTag

private const val ABOUT_COMPANY_NAME = "DSD TECH · 东莞市德士德科技有限公司\nDongGuan DESHIDE TECHNOLOGY CO.,LTD"

private fun supportedLanguage(languageTag: String): AppLanguage? {
    val parts = languageTag.lowercase().replace('_', '-').split('-')
    return when (parts.first()) {
        "en" -> AppLanguage.EN
        "zh" -> when {
            "hans" in parts -> AppLanguage.ZH_CN
            "hant" in parts || parts.any { it == "tw" || it == "hk" || it == "mo" } -> AppLanguage.ZH_TW
            else -> AppLanguage.ZH_CN
        }
        "de" -> AppLanguage.DE
        "fr" -> AppLanguage.FR
        "es" -> AppLanguage.ES
        "it" -> AppLanguage.IT
        "pt" -> AppLanguage.PT
        "ja" -> AppLanguage.JA
        else -> null
    }
}

fun preferredSystemLanguageTag(languageTags: Iterable<String>): String =
    languageTags.firstOrNull { supportedLanguage(it) != null } ?: "en"

fun resolveEffectiveLanguage(preference: AppLanguage, languageTag: String = "en"): AppLanguage =
    if (preference == AppLanguage.SYSTEM) supportedLanguage(languageTag) ?: AppLanguage.EN else preference

fun resolveAppStrings(
    preference: AppLanguage,
    languageTag: String = "en",
): AppStrings {
    return when (resolveEffectiveLanguage(preference, languageTag)) {
        AppLanguage.ZH_CN -> ZhCnAppStrings
        AppLanguage.ZH_TW -> ZhTwAppStrings
        AppLanguage.DE -> DeAppStrings
        AppLanguage.FR -> FrAppStrings
        AppLanguage.ES -> EsAppStrings
        AppLanguage.IT -> ItAppStrings
        AppLanguage.PT -> PtAppStrings
        AppLanguage.JA -> JaAppStrings
        AppLanguage.SYSTEM, AppLanguage.EN -> EnAppStrings
    }
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { ZhCnAppStrings }

interface AppStrings {
    val isChinese: Boolean
    // Navigation Tabs
    val tabTerminal: String
    val tabPlotter: String
    val tabLogs: String
    val tabSettings: String
    val tabAbout: String
    // Connection Status & Common
    val statusDisconnected: String
    val statusConnecting: String
    val statusConnected: String
    val commonCancel: String
    val commonClose: String
    val commonConfirm: String
    val commonGotIt: String
    val commonClear: String
    val commonClearSearch: String
    // Enums
    val lineEndingNone: String
    val checksumNone: String
    val checksumNoneShort: String
    val checksumSum8: String
    val checksumXor8: String
    val endianLe: String
    val endianBe: String
    val colorCobalt: String
    val colorEmerald: String
    val colorAmber: String
    val colorViolet: String
    val charsetGbk: String
    // AppLanguage labels
    val langSectionTitle: String
    val langOptionSystem: String
    // AppThemeMode labels
    val themeSectionTitle: String
    val themeOptionSystem: String
    val themeOptionLight: String
    val themeOptionDark: String
    // TopBar
    val channelUsbOtg: String
    val topBarUsbConfigDesc: String
    val topBarNoUsbDetected: String
    val topBarAdjustParamsDesc: String
    val topBarDisconnectDesc: String
    val topBarConnectDesc: String
    val topBarDisconnect: String
    val topBarConnect: String
    val topBarSettingsDesc: String
    val navBackDesc: String
    val topBarIdleBanner: String
    // SerialConfigBottomSheet
    val sheetMultiBanner: (Int) -> String
    val sheetLockedDeviceBanner: (String, Int) -> String
    val sheetGenericMultiBanner: (Int) -> String
    val sheetTitleSelectUsb: String
    val sheetTitleConfigUsb: String
    val sheetSupportedChipsSubtitle: String
    val sheetUsbDeviceLockedNotice: String
    val sheetCloseDesc: String
    val sheetAvailableDevices: (Int) -> String
    val sheetRefreshScan: String
    val sheetNoDevicesTitle: String
    val sheetNoDevicesSubtitle: String
    val sheetBadgeActive: String
    val sheetBadgeGranted: String
    val sheetBadgeNeedsPerm: String
    val sheetSelectedDesc: String
    val sheetUnselectedDesc: String
    val sheetPresetsHeader: String
    val sheetBaudRateHeader: String
    val sheetCustomChip: String
    val sheetCustomBaudLabel: String
    val sheetCustomBaudPlaceholder: String
    val sheetCustomBaudRangeHint: String
    val sheetDataBits: String
    val sheetStopBits: String
    val sheetParity: String
    val sheetDtrPullHigh: String
    val sheetRtsPullHigh: String
    val sheetPresetName: String
    val sheetSavePresetButton: String
    val sheetCtaApplyConnected: String
    val sheetCtaConnectDevice: (String) -> String
    val sheetCtaSaveOffline: String
    // Terminal Screen
    val termDisplay: String
    val termDisplayFormat: String
    val termTimestamps: String
    val termAutoScroll: String
    val termShowTrafficCounters: String
    val termFilter: String
    val termFilterActive: String
    val termHighlight: String
    val termSaveRecording: String
    val termRecord: String
    val termDtrState: (Boolean) -> String
    val termRtsState: (Boolean) -> String
    val termDtrShort: String
    val termRtsShort: String
    val termResetCounters: String
    val termClear: String
    val termClearDesc: String
    val termJumpToBottom: String
    val termClearFilterBtn: String
    val termEmptyConnectBtn: String
    val termClearInputDesc: String
    val termPayloadBytesBadge: (Int) -> String
    val termRepeatControls: String
    val termRepeatIntervalSummary: (String) -> String
    val termRepeatStartBtn: String
    val termRepeatStopBtn: String
    val termMacroPanelCount: (Int) -> String
    val termFilterPlaceholder: String
    val termFilterClearDesc: String
    val termHighlightPlaceholder: String
    val termQuickHighlightLabel: String
    val termEmptyTitle: String
    val termNoFilterMatch: (String) -> String
    val termEmptySubtitle: String
    val termMacroPanel: String
    val termExpandMacroDesc: String
    val termFormatChip: (String) -> String
    val termToggleFormatDesc: String
    val termLineEndingChip: (String) -> String
    val termLineEndingMenuItem: (String) -> String
    val termChecksumNoneChip: String
    val termRepeatTimesSuffix: String
    val termRepeatConfigDesc: String
    val termSendFileChip: String
    val termSendingFileBanner: (String, Long, Long, Int) -> String
    val termStopFileSend: String
    val termToggleEndianDesc: String
    val termChecksumTailLabel: (String) -> String
    val termHexInputHint: String
    val termAsciiInputHint: String
    val termSendDesc: String
    val termSendButton: String
    val termHistoryDesc: String
    val termHistoryButton: String
    // Frame Inspector Dialog
    val inspectorTitle: String
    val inspectorTimestamp: (String) -> String
    val inspectorHexDumpTitle: String
    val inspectorAsciiTitle: String
    val inspectorDiagTitle: String
    val inspectorModbusPass: (String) -> String
    val inspectorModbusFail: (String, String) -> String
    val inspectorRowModbusLe: String
    val inspectorRowCcittBe: String
    val inspectorRowSum8: String
    val inspectorRowXor8: String
    val inspectorCopyToTx: String
    // Repeat Config Dialog
    val repeatDialogTitle: String
    val repeatIntervalLabel: String
    val repeatMaxCountLabel: String
    val repeatUnlimitedLabel: String
    val repeatCountUnit: (Int) -> String
    val repeatProgressLabel: (Int, String) -> String
    // File Send Dialog
    val fileDialogTitle: String
    val fileDialogSize: (Long) -> String
    val fileDialogLineModeTitle: String
    val fileDialogLineModeSubtitle: String
    val fileDialogChunkSizeTitle: String
    val fileDialogDelayTitle: String
    val fileDialogStartSend: String
    // History Sheet
    val historySheetTitle: String
    val historySheetSubtitle: String
    val historyClearDesc: String
    val historyClearBtn: String
    val historySearchPlaceholder: String
    val historyShowMore: String
    val historyEmpty: String
    val historyNoMatch: (String) -> String
    val historyUseCount: (String, Int) -> String
    val historyPromoteDesc: String
    val historyResendDesc: String
    // Macros Screen
    val macroGroupAll: String
    val macroAllGroups: String
    val macroGroupPickerDesc: String
    val macroUngrouped: String
    val macroCreateGroup: String
    val macroNewGroupName: String
    val macroNamedGroupLabel: (String) -> String
    val macroMoreButton: String
    val macroImportJsonDesc: String
    val macroExportJsonDesc: String
    val macroExportAllLabel: String
    val macroAddDesc: String
    val macroAddBtn: String
    val macroCollapseDesc: String
    val macroEditDesc: (String) -> String
    val macroSendDesc: (String) -> String
    val macroEditorNewTitle: String
    val macroEditorEditTitle: String
    val macroEditorDeleteDesc: String
    val macroEditorNameLabel: String
    val macroEditorNameHint: String
    val macroEditorGroupLabel: String
    val macroEditorPayloadLabel: String
    val macroEditorPayloadHint: String
    val macroEditorFormatLabel: String
    val macroEditorLineEndingLabel: String
    val macroEditorColorTagLabel: String
    val macroEditorSaveBtn: String
    // Plotter Screen
    val plotPauseDesc: String
    val plotResumeDesc: String
    val plotStartBtn: String
    val plotPauseBtn: String
    val plotResumeBtn: String
    val plotAutoY: String
    val plotZoomInDesc: String
    val plotZoomOutDesc: String
    val plotWindowPoints: (Int) -> String
    val plotClearDesc: String
    val plotStatusHold: String
    val plotStatusLive: String
    val plotEmptyTitle: String
    val plotEmptySubtitle: String
    val plotBackToTerminalBtn: String
    val plotNoChannelSelected: String
    val plotChannelsSection: String
    val plotMore: String
    val plotFixedYRange: String
    val plotWindowLabel: String
    val plotSwitchProfileHint: String
    val plotChooseProfileHint: String
    val plotStartHint: String
    val plotWaitingForUsb: String
    val plotWaitingForNewData: String
    val plotShowChannels: String
    val plotMinimum: String
    val plotMaximum: String
    val plotChooseProfile: String
    val plotProfileColumns: String
    val plotProfileKeys: String
    val plotProfileNew: String
    val plotProfileEdit: String
    val plotProfileDelete: String
    val plotProfileDeleteConfirm: String
    val plotProfileSaveFailed: String
    val plotProfileSelectionFailed: String
    val plotProfileDeleteFailed: String
    val plotProfileName: String
    val plotProfileTextRules: String
    val plotProfileFields: String
    val plotProfilePreview: String
    val plotProfileNameRequired: String
    val plotProfileFieldNameRequired: String
    val plotProfileDuplicateName: String
    val plotProfileWidth: String
    val plotProfileFormat: String
    val plotFormatColumns: String
    val plotFormatBinary: String
    val plotProfileBoundary: String
    val plotBoundaryLine: String
    val plotBoundaryFrame: String
    val plotProfileDelimiter: String
    val plotDelimiterComma: String
    val plotDelimiterSpace: String
    val plotDelimiterTab: String
    val plotDelimiterSemicolon: String
    val plotProfileKeySeparator: String
    val plotProfileHeader: String
    val plotProfileTrailer: String
    val plotProfileBinaryHint: String
    val plotProfileAddField: String
    val plotProfileRemoveField: String
    val plotProfileFieldName: String
    val plotProfileUnit: String
    val plotProfileColumn: String
    val plotProfileKey: String
    val plotProfileOffset: String
    val plotProfileNumberType: String
    val plotProfileByteOrder: String
    val plotByteOrderLittle: String
    val plotByteOrderBig: String
    val plotProfileScale: String
    val plotProfileBias: String
    val plotProfileSampleText: String
    val plotProfileSampleHex: String
    val plotProfileInvalid: String
    val plotProfileClearSample: String
    val plotProfileSampleInvalid: String
    val plotProfileSave: String
    val plotOverloaded: String
    fun plotTimeSpan(milliseconds: Long, seconds: String): String
    // Logs Screen
    val logsStartRecordBtn: String
    val logsLastSevenDays: String
    val logsToday: String
    val logsAllTime: String
    val logsCustomRange: String
    val logsRangeInputHint: String
    val logsRangeStart: String
    val logsRangeEnd: String
    val logsRangeInvalid: String
    val logsApplyRange: String
    val logsEmptyRange: String
    val logsMoreActions: String
    val logsLoadFailed: String
    val logsRetry: String
    val logsOpenFailed: String
    val logsDeleteFailed: String
    val logsHighlightActive: String
    val logsNoFrames: String
    val logsMatchedFrames: (Int, Int) -> String
    val logsEmptyTitle: String
    val logsEmptySubtitle: String
    val logsFramesCount: (Int) -> String
    val logsEmailBtn: String
    val logsShareDesc: String
    val logsDeleteCardDesc: String
    val logsDeleteConfirmTitle: String
    val logsDeleteConfirmBody: (String) -> String
    val logsDeleteConfirmBtn: String
    val logsExportTxtBtn: String
    val logsExportCsvBtn: String
    val emailDialogTitle: String
    val emailDialogSessionSummary: (String, Int, String) -> String
    val emailDialogFormatLabel: String
    val emailDialogFormatTxt: String
    val emailDialogFormatCsv: String
    val emailDialogPrivacyNotice: String
    val emailDialogConfirmBtn: String
    val logViewerFilterPlaceholder: String
    // Settings Screen
    val settingsTerminalSection: String
    val settingsAdvanced: String
    val settingsCharsetDetail: String
    val settingsResetTerminalDetail: String
    val settingsCharsetLabel: String
    val settingsFontSizeLabel: String
    val settingsFontCompact: String
    val settingsFontNormal: String
    val settingsFontLarge: String
    val settingsFrameTimeoutTitle: String
    val settingsFrameTimeoutSubtitle: String
    val settingsMaxBufferTitle: String
    val settingsMaxBufferSubtitle: String
    val settingsMaxBufferOption: (Int) -> String
    val settingsAutoConnectTitle: String
    val settingsKeepScreenOnTitle: String
    val settingsClearAfterSendTitle: String
    val settingsVibrateTitle: String
    val settingsAboutCardTitle: String
    val settingsResetDefaultsBtn: String
    // About Screen
    val aboutTagline: String
    val aboutChipsTitle: String
    val aboutUsbScope: String
    val aboutPrivacyTitle: String
    val aboutPrivacyBody: String
    val aboutPrivacyPolicyLink: String
    val aboutWiringBtn: String
    val aboutChangelogBtn: String
    val aboutWiringTip1: String
    val aboutWiringTip2: String
    val aboutWiringTip3: String
    val aboutWiringTip4: String
    val aboutChangelogVersion: String
    val aboutCompanyName: String get() = ABOUT_COMPANY_NAME
    val aboutChangelogBody: String
    // MainActivity Intents & Snackbars
    val shareLogChooserTitle: String
    val noShareAppNotice: String
    val emailClientChooserTitle: String
    val noEmailFallbackNotice: String
    val emailFallbackChooserTitle: (String) -> String
    val noEmailOrShareAppNotice: String
    // LogExporter
    val exportTxtHeaderTitle: String
    val exportTxtSession: String
    val exportTxtDevice: String
    val exportTxtConfig: String
    val exportTxtTime: String
    val exportTxtFrames: String
    val exportTxtSize: String
    val exportOpenStreamError: String
    val emailSubject: (String) -> String
    val emailBody: (String, String, String, String, Int, String, String) -> String
    // ViewModel & System Frames / Notices
    val vmSysUsbAttached: (String, String) -> String
    val vmSysUsbDetached: String
    val vmNoticeUsbPermDenied: String
    val vmNoticeConnected: (String, String) -> String
    val vmSysPortOpened: (String, String) -> String
    val vmSysDisconnected: (String) -> String
    val vmSysError: (String) -> String
    val vmNoticeNoUsbFound: String
    val vmNoticeFoundDevices: (Int) -> String
    val vmUserDisconnectedReason: String
    val vmNoticePlugUsbFirst: String
    val vmNoticeOpenPortFailed: String
    val vmNoticeParamsUpdated: (String) -> String
    val vmSysParamsSwitched: (String) -> String
    val vmNoticeParamsFailed: String
    val vmNoticeParamsSavedOffline: (String) -> String
    val vmCustomPresetDefaultName: (Int) -> String
    val vmNoticePresetSaved: (String) -> String
    val vmNoticeTerminalCleared: String
    val vmNoticeCopiedToTx: String
    val vmNoticeRecordingStarted: String
    val vmNoticeRecordingEmpty: String
    val vmSessionTitlePrefix: String
    val vmOfflineDeviceSummary: String
    val vmNoticeLogSaved: (Int) -> String
    val vmNoticeCountersReset: String
    val vmPinHighLabel: String
    val vmPinLowLabel: String
    val vmPinShortHigh: String
    val vmPinShortLow: String
    val vmSysPinChanged: (String, String) -> String
    val vmNoticePinChanged: (String, String) -> String
    val vmNoticePinFailed: (String) -> String
    val vmNoticePinPreset: (String, String) -> String
    val vmNoticeRepeatStopped: (Int) -> String
    val vmNoticeConnectBeforeRepeat: String
    val vmNoticeEnterRepeatPayload: String
    val vmRepeatUnlimitedHint: String
    val vmRepeatLimitHint: (Int) -> String
    val vmNoticeRepeatStarted: (Int, String) -> String
    val vmNoticeRepeatFinished: (Int) -> String
    val vmNoticeConnectBeforeSend: String
    val vmNoticeTxPayloadEmpty: String
    val vmNoticeSendFormatError: String
    val vmNoticeWriteFailed: String
    val vmNoticeFileEmpty: String
    val vmNoticeConnectBeforeFileSend: String
    val vmSysFileSendStart: (String, Long) -> String
    val vmFileReadError: String
    val vmFileChunkWriteError: String
    val vmSysFileSendFailed: (String, Long) -> String
    val vmSysFileSendDone: (String, Long) -> String
    val vmNoticeFileSendDone: (String) -> String
    val vmNoticeFileSendCancelled: (String) -> String
    val vmSysFileSendCancelled: (String) -> String
    val vmNoticeHistoryRefilled: String
    val vmNoticeHistoryFormatError: String
    val vmNoticeHistoryMissingSentBytes: String
    val vmNoticeHistoryResent: String
    val vmNoticeHistoryResendFailed: String
    val vmNoticeHistoryCleared: String
    val vmNoticeConnectBeforeMacro: (String) -> String
    val vmNoticeMacroFormatInvalid: String
    val vmNoticeMacroNeedsResave: String
    val vmNoticeMacroSendFailed: String
    val vmMacroFrameTag: (String) -> String
    val vmMacroDefaultTitle: String
    val vmNoticeMacroPayloadEmpty: String
    val vmNoticeMacroSaved: (String) -> String
    val vmNoticeMacroDeleted: String
    val vmNoticeMacroExportEmpty: String
    val vmMacroWriteJsonError: String
    val vmNoticeMacroExported: (Int) -> String
    val vmNoticeMacroExportFailed: String
    val vmMacroReadFileError: String
    val vmNoticeMacroReadFailed: String
    val vmNoticeMacroJsonInvalid: String
    val vmNoticeMacroImported: (Int) -> String
    val vmNoticeMacroAllExist: String
    val vmNoticePlotCleared: String
    val vmNoticeLogDeleted: String
    val vmNoticeExportedFormat: (String) -> String
    val vmNoticeExportFailed: String
    val vmNoticeSharePrepFailed: String
    val vmNoticeSettingsReset: String
    // Relative Time
    val relJustNow: String
    val relMinutesAgo: (Long) -> String
    val relHoursAgo: (Long) -> String
}

object ZhCnAppStrings : AppStrings {
    override val isChinese = true
    override val tabTerminal = "终端"
    override val tabPlotter = "可视化"
    override val tabLogs = "日志库"
    override val tabSettings = "设置"
    override val tabAbout = "关于"
    override val statusDisconnected = "未连接"
    override val statusConnecting = "连接中"
    override val statusConnected = "已连接"
    override val commonCancel = "取消"
    override val commonClose = "关闭"
    override val commonConfirm = "确定"
    override val commonGotIt = "知道了"
    override val commonClear = "清空"
    override val commonClearSearch = "清除搜索"
    override val lineEndingNone = "无"
    override val checksumNone = "无校验"
    override val checksumNoneShort = "无"
    override val checksumSum8 = "Sum8 累加和"
    override val checksumXor8 = "XOR8 异或"
    override val endianLe = "低字节在前 (LO-HI)"
    override val endianBe = "高字节在前 (HI-LO)"
    override val colorCobalt = "钴蓝"
    override val colorEmerald = "翡翠绿"
    override val colorAmber = "琥珀橙"
    override val colorViolet = "紫罗兰"
    override val charsetGbk = "GBK (中文工控)"
    override val langSectionTitle = "语言"
    override val langOptionSystem = "跟随系统"
    override val themeSectionTitle = "外观"
    override val themeOptionSystem = "跟随系统"
    override val themeOptionLight = "日间仪表"
    override val themeOptionDark = "深色护眼"
    override val channelUsbOtg = "USB OTG"
    override val topBarUsbConfigDesc = "USB 串口配置"
    override val topBarNoUsbDetected = "未检测到 USB 串口设备"
    override val topBarAdjustParamsDesc = "调整连接与通道参数"
    override val topBarDisconnectDesc = "断开连接"
    override val topBarConnectDesc = "建立连接"
    override val topBarDisconnect = "断开"
    override val topBarConnect = "连接"
    override val topBarSettingsDesc = "打开设置"
    override val navBackDesc = "返回"
    override val topBarIdleBanner = "○ 待机未连接 · 点击左侧参数卡配置连接，或直接点击连接"
    override val sheetMultiBanner: (Int) -> String = { count -> "检测到 $count 个 USB 串口设备，请点选目标设备并确认参数后进入终端" }
    override val sheetLockedDeviceBanner: (String, Int) -> String = { chip, count -> "已锁定新接入设备 $chip，当前共检测到 $count 个 USB 串口设备，请确认目标设备后连接" }
    override val sheetGenericMultiBanner: (Int) -> String = { count -> "检测到 $count 个 USB 串口设备，请点选目标设备进入终端" }
    override val sheetTitleSelectUsb = "选择 USB 串口设备"
    override val sheetTitleConfigUsb = "连接与通道配置"
    override val sheetSupportedChipsSubtitle = "单通道模式 · USB 支持 CH340 / CP210x / FTDI / PL2303 / CDC"
    override val sheetUsbDeviceLockedNotice = "请先断开当前 USB 串口，再切换设备。"
    override val sheetCloseDesc = "关闭面板"
    override val sheetAvailableDevices: (Int) -> String = { count -> "可用 USB 串口设备 ($count)" }
    override val sheetRefreshScan = "刷新扫描"
    override val sheetNoDevicesTitle = "当前未检测到 USB OTG 串口设备"
    override val sheetNoDevicesSubtitle = "请通过 Type-C OTG 转接线插入支持的 USB 串口设备，授权后即可打开串口。"
    override val sheetBadgeActive = "调试中"
    override val sheetBadgeGranted = "已授权"
    override val sheetBadgeNeedsPerm = "需授权"
    override val sheetSelectedDesc = "已选中"
    override val sheetUnselectedDesc = "未选中"
    override val sheetPresetsHeader = "串口预设"
    override val sheetBaudRateHeader = "波特率"
    override val sheetCustomChip = "自定义"
    override val sheetCustomBaudLabel = "自定义波特率"
    override val sheetCustomBaudPlaceholder = "如 74880、250000、1500000"
    override val sheetCustomBaudRangeHint = "范围 300–2,000,000"
    override val sheetDataBits = "数据位"
    override val sheetStopBits = "停止位"
    override val sheetParity = "校验位"
    override val sheetDtrPullHigh = "DTR 引脚拉高"
    override val sheetRtsPullHigh = "RTS 引脚拉高"
    override val sheetPresetName = "预设名称"
    override val sheetSavePresetButton = "保存预设"
    override val sheetCtaApplyConnected = "应用参数"
    override val sheetCtaConnectDevice: (String) -> String = { driver -> "连接 $driver" }
    override val sheetCtaSaveOffline = "保存串口参数"
    override val termDisplay = "显示"
    override val termDisplayFormat = "报文格式"
    override val termTimestamps = "时间戳"
    override val termAutoScroll = "自动滚屏"
    override val termShowTrafficCounters = "显示收发数据量"
    override val termFilter = "筛选"
    override val termFilterActive = "已筛选"
    override val termHighlight = "高亮"
    override val termSaveRecording = "保存录制"
    override val termRecord = "录制"
    override val termDtrState: (Boolean) -> String = { on -> if (on) "DTR:高" else "DTR:低" }
    override val termRtsState: (Boolean) -> String = { on -> if (on) "RTS:高" else "RTS:低" }
    override val termDtrShort = "DTR"
    override val termRtsShort = "RTS"
    override val termResetCounters = "计数清零"
    override val termClear = "清空"
    override val termClearDesc = "清空终端"
    override val termJumpToBottom = "回到底部"
    override val termClearFilterBtn = "清除过滤条件"
    override val termEmptyConnectBtn = "连接设备或配置通道"
    override val termClearInputDesc = "清空输入"
    override val termPayloadBytesBadge: (Int) -> String = { n -> "${n} B" }
    override val termRepeatControls = "定时"
    override val termRepeatIntervalSummary: (String) -> String = { "间隔 $it" }
    override val termRepeatStartBtn = "定时发送"
    override val termRepeatStopBtn = "停止定时发送"
    override val termMacroPanelCount: (Int) -> String = { count -> "快捷指令 ($count)" }
    override val termFilterPlaceholder = "筛选文本或 HEX"
    override val termFilterClearDesc = "清除筛选"
    override val termHighlightPlaceholder = "高亮关键字"
    override val termQuickHighlightLabel = "快捷高亮:"
    override val termEmptyTitle = "终端缓冲区为空"
    override val termNoFilterMatch: (String) -> String = { kw -> "无匹配「$kw」的报文" }
    override val termEmptySubtitle = "连接串口设备后，可在下方输入 ASCII/HEX 或点击快捷指令发送"
    override val termMacroPanel = "快捷指令"
    override val termExpandMacroDesc = "展开快捷指令"
    override val termFormatChip: (String) -> String = { fmt -> "编码:$fmt" }
    override val termToggleFormatDesc = "切换发送编码"
    override val termLineEndingChip: (String) -> String = { end -> "行尾:$end" }
    override val termLineEndingMenuItem: (String) -> String = { end -> "行尾符: $end" }
    override val termChecksumNoneChip = "校验:无"
    override val termRepeatTimesSuffix = "次"
    override val termRepeatConfigDesc = "设置定时发送参数"
    override val termSendFileChip = "发文件"
    override val termSendingFileBanner: (String, Long, Long, Int) -> String = { name, sent, total, pct -> "发送文件: $name ($sent/$total B · $pct%)" }
    override val termStopFileSend = "停止"
    override val termToggleEndianDesc = "切换校验字节序"
    override val termChecksumTailLabel: (String) -> String = { hex -> "帧尾: $hex" }
    override val termHexInputHint = "输入十六进制如 01 03 00 00"
    override val termAsciiInputHint = "输入 ASCII 指令如 AT+RST"
    override val termSendDesc = "发送指令"
    override val termSendButton = "发送"
    override val termHistoryDesc = "发送历史指令"
    override val termHistoryButton = "历史"
    override val inspectorTitle = "报文帧详情与校验诊断"
    override val inspectorTimestamp: (String) -> String = { ts -> "时间: $ts" }
    override val inspectorHexDumpTitle = "HEX 原始字节流"
    override val inspectorAsciiTitle = "文本解码视图"
    override val inspectorDiagTitle = "快速帧校验诊断"
    override val inspectorModbusPass: (String) -> String = { actual -> "CRC16-Modbus (LE) 验帧通过: $actual" }
    override val inspectorModbusFail: (String, String) -> String = { actual, expected -> "CRC16-Modbus 末2字节不匹配 (实际 $actual / 期望 $expected)" }
    override val inspectorRowModbusLe = "整帧 CRC16-Modbus (LE)"
    override val inspectorRowCcittBe = "整帧 CRC16-CCITT (BE)"
    override val inspectorRowSum8 = "整帧 Sum8 累加和"
    override val inspectorRowXor8 = "整帧 XOR8 异或"
    override val inspectorCopyToTx = "回填发送框"
    override val repeatDialogTitle = "定时发送参数配置"
    override val repeatIntervalLabel = "发送间隔毫秒 (50 - 60000)"
    override val repeatMaxCountLabel = "发送次数上限 (0 表示无限循环)"
    override val repeatUnlimitedLabel = "无限"
    override val repeatCountUnit: (Int) -> String = { cnt -> "$cnt 次" }
    override val repeatProgressLabel: (Int, String) -> String = { sent, limit -> "已发送 $sent / $limit" }
    override val fileDialogTitle = "本地文件分包发送"
    override val fileDialogSize: (Long) -> String = { bytes -> "文件大小：$bytes B" }
    override val fileDialogLineModeTitle = "按文本行发送（AT 脚本模式）"
    override val fileDialogLineModeSubtitle = "关闭则按固定字节分片发送（固件/HEX）"
    override val fileDialogChunkSizeTitle = "单包字节大小"
    override val fileDialogDelayTitle = "分包 / 逐行间隔延时"
    override val fileDialogStartSend = "开始发送"
    override val historySheetTitle = "发送历史指令（最近去重）"
    override val historySheetSubtitle = "点击行回填修改 · 右侧可添加为快捷指令或立即重发"
    override val historyClearDesc = "清空历史"
    override val historyClearBtn = "清空"
    override val historySearchPlaceholder = "过滤历史发送指令..."
    override val historyShowMore = "查看更多"
    override val historyEmpty = "暂无发送历史记录"
    override val historyNoMatch: (String) -> String = { q -> "无匹配「$q」的历史指令" }
    override val historyUseCount: (String, Int) -> String = { time, count -> "$time · ${count}次" }
    override val historyPromoteDesc = "添加为快捷指令"
    override val historyResendDesc = "立即重发"
    override val macroGroupAll = "全部"
    override val macroAllGroups = "全部分组"
    override val macroGroupPickerDesc = "选择快捷指令分组"
    override val macroUngrouped = "未分组"
    override val macroCreateGroup = "新建分组…"
    override val macroNewGroupName = "新分组名称"
    override val macroNamedGroupLabel: (String) -> String = { "$it（命名分组）" }
    override val macroMoreButton = "更多"
    override val macroImportJsonDesc = "导入快捷指令"
    override val macroExportJsonDesc = "导出快捷指令"
    override val macroExportAllLabel = "导出全部快捷指令"
    override val macroAddDesc = "新建快捷指令"
    override val macroAddBtn = "新建"
    override val macroCollapseDesc = "关闭快捷指令面板"
    override val macroEditDesc: (String) -> String = { title -> "编辑指令 $title" }
    override val macroSendDesc: (String) -> String = { title -> "发送 $title" }
    override val macroEditorNewTitle = "新建快捷指令"
    override val macroEditorEditTitle = "编辑快捷指令"
    override val macroEditorDeleteDesc = "删除指令"
    override val macroEditorNameLabel = "指令名称"
    override val macroEditorNameHint = "如：读取温度寄存器"
    override val macroEditorGroupLabel = "分组"
    override val macroEditorPayloadLabel = "指令内容"
    override val macroEditorPayloadHint = "如：AA 55 01 03 00 00 00 02"
    override val macroEditorFormatLabel = "编码格式"
    override val macroEditorLineEndingLabel = "行尾结束符"
    override val macroEditorColorTagLabel = "标签色"
    override val macroEditorSaveBtn = "保存指令"
    override val plotPauseDesc = "暂停采样"
    override val plotResumeDesc = "继续采样"
    override val plotStartBtn = "启动"
    override val plotPauseBtn = "暂停"
    override val plotResumeBtn = "继续"
    override val plotAutoY = "自动纵轴"
    override val plotZoomInDesc = "减少窗口点数"
    override val plotZoomOutDesc = "增加窗口点数"
    override val plotWindowPoints: (Int) -> String = { pts -> "${pts}点" }
    override val plotClearDesc = "清空波形"
    override val plotStatusHold = "已暂停"
    override val plotStatusLive = "实时"
    override val plotEmptyTitle = "等待数据"
    override val plotEmptySubtitle = "等待符合当前配置的接收数据。"
    override val plotBackToTerminalBtn = "前往终端发指令"
    override val plotNoChannelSelected = "请至少开启一个通道以查看波形"
    override val plotChannelsSection = "通道"
    override val plotMore = "更多"
    override val plotFixedYRange = "固定范围 0–60"
    override val plotWindowLabel = "窗口点数"
    override val plotSwitchProfileHint = "切换配置会清空当前波形"
    override val plotChooseProfileHint = "选择解析配置，再点「启动」开始绘图。"
    override val plotStartHint = "点「启动」，开始绘制接下来接收的数据。"
    override val plotWaitingForUsb = "等待 USB 连接"
    override val plotWaitingForNewData = "等待新数据"
    override val plotShowChannels = "显示通道"
    override val plotMinimum = "最小"
    override val plotMaximum = "最大"
    override val plotChooseProfile = "选择解析配置"
    override val plotProfileColumns = "数值列（CSV）"
    override val plotProfileKeys = "键值对"
    override val plotProfileNew = "新建配置"
    override val plotProfileEdit = "编辑配置"
    override val plotProfileDelete = "删除配置"
    override val plotProfileDeleteConfirm = "删除此配置？"
    override val plotProfileSaveFailed = "保存失败，请检查配置名称是否重复并重试。"
    override val plotProfileSelectionFailed = "无法记住所选配置，请重新选择。"
    override val plotProfileDeleteFailed = "无法删除配置，请重试。"
    override val plotProfileName = "配置名称"
    override val plotProfileTextRules = "文本解析设置"
    override val plotProfileFields = "绘图字段"
    override val plotProfilePreview = "样例与预览（选填）"
    override val plotProfileNameRequired = "请填写配置名称。"
    override val plotProfileFieldNameRequired = "请为每个字段填写名称。"
    override val plotProfileDuplicateName = "此配置名称已存在。"
    override val plotProfileWidth = "完整帧长度（字节）"
    override val plotProfileFormat = "数据格式"
    override val plotFormatColumns = "文本分列"
    override val plotFormatBinary = "二进制"
    override val plotProfileBoundary = "记录边界"
    override val plotBoundaryLine = "换行 LF／CRLF"
    override val plotBoundaryFrame = "串口接收帧"
    override val plotProfileDelimiter = "字段分隔符"
    override val plotDelimiterComma = "逗号"
    override val plotDelimiterSpace = "空白字符"
    override val plotDelimiterTab = "制表符"
    override val plotDelimiterSemicolon = "分号"
    override val plotProfileKeySeparator = "键值分隔符"
    override val plotProfileHeader = "帧头（HEX）"
    override val plotProfileTrailer = "帧尾（HEX）"
    override val plotProfileBinaryHint = "帧头、完整帧长度、帧尾至少填写两项。HEX 字节用空格分隔。"
    override val plotProfileAddField = "添加字段"
    override val plotProfileRemoveField = "移除字段"
    override val plotProfileFieldName = "字段名称"
    override val plotProfileUnit = "单位（可选）"
    override val plotProfileColumn = "列号（从 1 开始）"
    override val plotProfileKey = "键名"
    override val plotProfileOffset = "字节偏移（从 0 开始）"
    override val plotProfileNumberType = "数值类型"
    override val plotProfileByteOrder = "字节序"
    override val plotByteOrderLittle = "小端"
    override val plotByteOrderBig = "大端"
    override val plotProfileScale = "倍率"
    override val plotProfileBias = "偏移量"
    override val plotProfileSampleText = "样例文本"
    override val plotProfileSampleHex = "样例报文（HEX）"
    override val plotProfileInvalid = "请填写有效的配置和字段。"
    override val plotProfileClearSample = "清空样例"
    override val plotProfileSampleInvalid = "样例未解析出完整记录，请修正或清空样例。"
    override val plotProfileSave = "保存"
    override val plotOverloaded = "来不及解析接收数据，已暂停采样。"
    override fun plotTimeSpan(milliseconds: Long, seconds: String): String =
        if (milliseconds < 1000) "$milliseconds ms" else "约 $seconds 秒"
    override val logsStartRecordBtn = "录制"
    override val logsLastSevenDays = "最近 7 天"
    override val logsToday = "今天"
    override val logsAllTime = "全部时间"
    override val logsCustomRange = "自定义时间段"
    override val logsRangeInputHint = "输入日期 YYYY-MM-DD，也可加时间 HH:mm。"
    override val logsRangeStart = "开始时间"
    override val logsRangeEnd = "结束时间"
    override val logsRangeInvalid = "请检查日期格式和起止时间顺序。"
    override val logsApplyRange = "应用"
    override val logsEmptyRange = "这个时间段暂无录制"
    override val logsMoreActions = "更多操作"
    override val logsLoadFailed = "日志加载失败"
    override val logsRetry = "重试"
    override val logsOpenFailed = "无法打开这条录制"
    override val logsDeleteFailed = "删除录制失败"
    override val logsHighlightActive = "已高亮"
    override val logsNoFrames = "没有匹配的报文"
    override val logsMatchedFrames: (Int, Int) -> String = { matched, total -> "$matched / $total 帧" }
    override val logsEmptyTitle = "暂无已录制的会话日志"
    override val logsEmptySubtitle = "点「录制」保存串口报文"
    override val logsFramesCount: (Int) -> String = { count -> "$count 帧报文" }
    override val logsEmailBtn = "发邮件"
    override val logsShareDesc = "系统分享日志"
    override val logsDeleteCardDesc = "删除日志会话"
    override val logsDeleteConfirmTitle = "删除录制会话？"
    override val logsDeleteConfirmBody: (String) -> String = { title -> "确定要永久删除「$title」吗？此操作无法撤销。" }
    override val logsDeleteConfirmBtn = "确认删除"
    override val logsExportTxtBtn = "导出 TXT"
    override val logsExportCsvBtn = "导出 CSV"
    override val emailDialogTitle = "通过系统邮件发送日志"
    override val emailDialogSessionSummary: (String, Int, String) -> String = { title, count, size -> "$title（$count 帧 · $size）" }
    override val emailDialogFormatLabel = "选择附件格式"
    override val emailDialogFormatTxt = "TXT"
    override val emailDialogFormatCsv = "CSV"
    override val emailDialogPrivacyNotice = "diSerial 会打开邮件应用。如果没有可用的邮件应用，则打开系统分享。你选择的应用可临时读取附件。diSerial 不会读取邮件账号或上传日志。发送前请核对收件人，并检查日志是否含有密钥、密码等敏感信息。"
    override val emailDialogConfirmBtn = "打开邮件客户端"
    override val logViewerFilterPlaceholder = "过滤帧内容"
    override val settingsTerminalSection = "终端设置"
    override val settingsAdvanced = "高级"
    override val settingsCharsetDetail = "用于文字收发。设备使用 GBK 时可在这里切换。"
    override val settingsResetTerminalDetail = "将恢复文字编码、终端字号、发送后清空、接收分帧间隔、终端缓存和错误振动的默认值。"
    override val settingsCharsetLabel = "文字编码"
    override val settingsFontSizeLabel = "终端字号"
    override val settingsFontCompact = "小"
    override val settingsFontNormal = "标准"
    override val settingsFontLarge = "大"
    override val settingsFrameTimeoutTitle = "接收分帧间隔"
    override val settingsFrameTimeoutSubtitle = "空闲超过该间隔即判定为一帧结束"
    override val settingsMaxBufferTitle = "终端缓存"
    override val settingsMaxBufferSubtitle = "终端只保留最近的报文。录制中的日志继续保存。"
    override val settingsMaxBufferOption: (Int) -> String = { n -> "$n 帧" }
    override val settingsAutoConnectTitle = "插入 USB 时自动连接"
    override val settingsKeepScreenOnTitle = "屏幕常亮"
    override val settingsClearAfterSendTitle = "发送后清空输入框"
    override val settingsVibrateTitle = "错误振动"
    override val settingsAboutCardTitle = "关于 diSerial"
    override val settingsResetDefaultsBtn = "恢复终端默认设置"
    override val aboutTagline = "USB 串口终端与数据可视化"
    override val aboutChipsTitle = "USB 兼容性"
    override val aboutUsbScope = "Android 首版适用于以下系列的部分单口适配器，兼容性以具体型号为准。"
    override val aboutPrivacyTitle = "隐私与数据"
    override val aboutPrivacyBody = "串口录制、设备预设、发送历史和快捷指令保存在设备上。在 Android 上，这些数据库内容不参与自动云备份。你可以手动导出或分享日志。"
    override val aboutPrivacyPolicyLink = "查看完整隐私政策（英文）"
    override val aboutWiringBtn = "接线与排障"
    override val aboutChangelogBtn = "版本说明"
    override val aboutWiringTip1 = "设备未识别时，检查手机的 USB OTG 支持和转接线。部分手机需要在系统设置中打开 OTG。"
    override val aboutWiringTip2 = "使用 TTL 串口时，先核对两端电平。TX 接对端 RX，双方 GND 共地。"
    override val aboutWiringTip3 = "遇到乱码，先核对波特率、数据位、校验位和停止位，再检查「终端设置」中的文字编码。"
    override val aboutWiringTip4 = "使用 Modbus RTU 等协议时，按设备说明核对串口参数和帧尾校验。"
    override val aboutChangelogVersion = "v1.0.0"
    override val aboutChangelogBody = "• USB 串口终端支持收发、HEX 帧尾校验和快捷指令。\n• 串口数据可视化可按 CSV、键值对或二进制配置实时绘制接收数据。\n• 日志库支持录制、按时间查找和导出。"
    override val shareLogChooserTitle = "分享串口日志"
    override val noShareAppNotice = "当前系统暂无可用的分享应用"
    override val emailClientChooserTitle = "选择邮件客户端发送日志"
    override val noEmailFallbackNotice = "未检测到邮件客户端，已切换为系统分享"
    override val emailFallbackChooserTitle: (String) -> String = { ext -> "发送串口日志 ($ext)" }
    override val noEmailOrShareAppNotice = "当前系统暂无可用的邮件或分享应用"
    override val exportTxtHeaderTitle = "# diSerial 串口通信日志报告"
    override val exportTxtSession = "# 会话"
    override val exportTxtDevice = "# 设备"
    override val exportTxtConfig = "# 参数"
    override val exportTxtTime = "# 时间"
    override val exportTxtFrames = "# 帧数"
    override val exportTxtSize = "大小"
    override val exportOpenStreamError = "无法打开目标文件输出流"
    override val emailSubject: (String) -> String = { title -> "diSerial 串口日志：$title" }
    override val emailBody: (String, String, String, String, Int, String, String) -> String = { title, device, cfg, time, count, size, ext ->
        buildString {
            appendLine("您好，附件为 diSerial 导出的串口通信日志（$ext 格式）。")
            appendLine()
            appendLine("会话名称：$title")
            appendLine("串口设备：$device")
            appendLine("通信参数：$cfg")
            appendLine("录制时间：$time")
            appendLine("报文统计：$count 帧 · $size")
            appendLine()
            appendLine("提示：发出前请核对收件人邮箱，并确认串口报文中不包含敏感密钥或配网口令。")
        }
    }
    override val vmSysUsbAttached: (String, String) -> String = { chip, vidPid -> "检测到 USB 串口设备: $chip ($vidPid)" }
    override val vmSysUsbDetached = "USB 设备已移除"
    override val vmNoticeUsbPermDenied = "未获得 USB 访问权限"
    override val vmNoticeConnected: (String, String) -> String = { chip, cfg -> "已连接 $chip ($cfg)" }
    override val vmSysPortOpened: (String, String) -> String = { chip, cfg -> "串口已打开: $chip · $cfg" }
    override val vmSysDisconnected: (String) -> String = { reason -> "连接已断开: $reason" }
    override val vmSysError: (String) -> String = { msg -> "错误: $msg" }
    override val vmNoticeNoUsbFound = "未扫描到 USB OTG 串口设备，请检查转接线与 OTG 供电"
    override val vmNoticeFoundDevices: (Int) -> String = { count -> "已识别 $count 个 USB 串口设备" }
    override val vmUserDisconnectedReason = "已主动断开串口连接"
    override val vmNoticePlugUsbFirst = "请先插入支持的 USB OTG 串口设备"
    override val vmNoticeOpenPortFailed = "串口打开失败"
    override val vmNoticeParamsUpdated: (String) -> String = { cfg -> "串口参数已热更新：$cfg" }
    override val vmSysParamsSwitched: (String) -> String = { cfg -> "串口参数已切换: $cfg" }
    override val vmNoticeParamsFailed = "参数设置失败"
    override val vmNoticeParamsSavedOffline: (String) -> String = { cfg -> "参数已保存 ($cfg)，插入设备后即可连接" }
    override val vmCustomPresetDefaultName: (Int) -> String = { baud -> "自定义 $baud" }
    override val vmNoticePresetSaved: (String) -> String = { name -> "已保存串口预设「$name」" }
    override val vmNoticeTerminalCleared = "终端显示已清空"
    override val vmNoticeCopiedToTx = "已将该帧回填至发送输入框"
    override val vmNoticeRecordingStarted = "已开始录制通信日志"
    override val vmNoticeRecordingEmpty = "本次录制未捕获到通信报文"
    override val vmSessionTitlePrefix = "串口会话"
    override val vmOfflineDeviceSummary = "离线终端缓存"
    override val vmNoticeLogSaved: (Int) -> String = { count -> "已保存 $count 帧报文到本地日志库" }
    override val vmNoticeCountersReset = "RX / TX 字节统计已清零"
    override val vmPinHighLabel = "置高 (ON)"
    override val vmPinLowLabel = "拉低 (OFF)"
    override val vmPinShortHigh = "高"
    override val vmPinShortLow = "低"
    override val vmSysPinChanged: (String, String) -> String = { pin, stateLabel -> "$pin 引脚已$stateLabel" }
    override val vmNoticePinChanged: (String, String) -> String = { pin, stateLabel -> "$pin 已$stateLabel" }
    override val vmNoticePinFailed: (String) -> String = { pin -> "$pin 控制失败" }
    override val vmNoticePinPreset: (String, String) -> String = { pin, shortState -> "已预设 $pin=$shortState，连接时生效" }
    override val vmNoticeRepeatStopped: (Int) -> String = { count -> "已停止定时循环发送（累计 $count 次）" }
    override val vmNoticeConnectBeforeRepeat = "请先连接 USB 串口设备后再开启定时发送"
    override val vmNoticeEnterRepeatPayload = "请先输入要循环发送的指令内容"
    override val vmRepeatUnlimitedHint = "，无限循环"
    override val vmRepeatLimitHint: (Int) -> String = { max -> "，上限 $max 次" }
    override val vmNoticeRepeatStarted: (Int, String) -> String = { ms, limit -> "已开启定时发送（每 ${ms}ms$limit）" }
    override val vmNoticeRepeatFinished: (Int) -> String = { count -> "定时发送已完成（共发送 $count 次）" }
    override val vmNoticeConnectBeforeSend = "请先连接 USB 串口设备"
    override val vmNoticeTxPayloadEmpty = "发送内容不能为空"
    override val vmNoticeSendFormatError = "发送格式错误"
    override val vmNoticeWriteFailed = "串口写入失败"
    override val vmNoticeFileEmpty = "所选文件为空或无法读取"
    override val vmNoticeConnectBeforeFileSend = "请先连接 USB 串口设备再发送文件"
    override val vmSysFileSendStart: (String, Long) -> String = { name, bytes -> "开始发送文件: $name ($bytes B)" }
    override val vmFileReadError = "无法读取文件内容"
    override val vmFileChunkWriteError = "文件分包写入失败"
    override val vmSysFileSendFailed: (String, Long) -> String = { msg, sent -> "文件发送中断: $msg (已发送 $sent B)" }
    override val vmSysFileSendDone: (String, Long) -> String = { name, sent -> "文件发送完成: $name (共发送 $sent B)" }
    override val vmNoticeFileSendDone: (String) -> String = { name -> "文件「$name」发送完成" }
    override val vmNoticeFileSendCancelled: (String) -> String = { name -> "已取消发送「$name」" }
    override val vmSysFileSendCancelled: (String) -> String = { name -> "已手动取消文件发送: $name" }
    override val vmNoticeHistoryRefilled = "已回填历史指令及发送设置"
    override val vmNoticeHistoryFormatError = "历史指令格式无效"
    override val vmNoticeHistoryMissingSentBytes = "这条历史没有记录完整报文，请确认并重发后再保存为快捷指令"
    override val vmNoticeHistoryResent = "已重发历史指令"
    override val vmNoticeHistoryResendFailed = "重发失败"
    override val vmNoticeHistoryCleared = "发送历史指令已清空"
    override val vmNoticeConnectBeforeMacro: (String) -> String = { title -> "请先连接 USB 串口设备，再发送快捷指令「$title」" }
    override val vmNoticeMacroFormatInvalid = "请输入有效的十六进制报文，例如 01 03 00 00"
    override val vmNoticeMacroNeedsResave = "请打开这条旧快捷指令并重新保存完整报文"
    override val vmNoticeMacroSendFailed = "快捷指令发送失败"
    override val vmMacroFrameTag: (String) -> String = { title -> "[快捷指令:$title]" }
    override val vmMacroDefaultTitle = "自定义指令"
    override val vmNoticeMacroPayloadEmpty = "快捷指令内容不能为空"
    override val vmNoticeMacroSaved: (String) -> String = { title -> "已保存快捷指令「$title」" }
    override val vmNoticeMacroDeleted = "已删除快捷指令"
    override val vmNoticeMacroExportEmpty = "没有可导出的快捷指令"
    override val vmMacroWriteJsonError = "无法写入目标 JSON 文件"
    override val vmNoticeMacroExported: (Int) -> String = { count -> "已导出 $count 条快捷指令到 JSON 文件" }
    override val vmNoticeMacroExportFailed = "快捷指令导出失败"
    override val vmMacroReadFileError = "无法读取所选文件"
    override val vmNoticeMacroReadFailed = "文件读取失败"
    override val vmNoticeMacroJsonInvalid = "无法解析快捷指令 JSON"
    override val vmNoticeMacroImported: (Int) -> String = { count -> "已导入 $count 条新快捷指令" }
    override val vmNoticeMacroAllExist = "所选文件中的快捷指令已全部存在"
    override val vmNoticePlotCleared = "波形缓冲已清空"
    override val vmNoticeLogDeleted = "已删除日志会话"
    override val vmNoticeExportedFormat: (String) -> String = { ext -> "已导出为 $ext 文件" }
    override val vmNoticeExportFailed = "导出失败"
    override val vmNoticeSharePrepFailed = "创建分享文件失败"
    override val vmNoticeSettingsReset = "已恢复终端默认设置"
    override val relJustNow = "刚刚"
    override val relMinutesAgo: (Long) -> String = { m -> "${m}分钟前" }
    override val relHoursAgo: (Long) -> String = { h -> "${h}小时前" }
}

object EnAppStrings : AppStrings {
    override val isChinese = false
    override val tabTerminal = "Terminal"
    override val tabPlotter = "Visualization"
    override val tabLogs = "Logs"
    override val tabSettings = "Settings"
    override val tabAbout = "About"
    override val statusDisconnected = "Disconnected"
    override val statusConnecting = "Connecting"
    override val statusConnected = "Connected"
    override val commonCancel = "Cancel"
    override val commonClose = "Close"
    override val commonConfirm = "OK"
    override val commonGotIt = "Got it"
    override val commonClear = "Clear"
    override val commonClearSearch = "Clear search"
    override val lineEndingNone = "None"
    override val checksumNone = "No Checksum"
    override val checksumNoneShort = "None"
    override val checksumSum8 = "Sum8 Checksum"
    override val checksumXor8 = "XOR8 Checksum"
    override val endianLe = "Little-Endian (LO-HI)"
    override val endianBe = "Big-Endian (HI-LO)"
    override val colorCobalt = "Cobalt"
    override val colorEmerald = "Emerald"
    override val colorAmber = "Amber"
    override val colorViolet = "Violet"
    override val charsetGbk = "GBK (Chinese PLC)"
    override val langSectionTitle = "Language"
    override val langOptionSystem = "Follow System"
    override val themeSectionTitle = "Appearance"
    override val themeOptionSystem = "System"
    override val themeOptionLight = "Daylight"
    override val themeOptionDark = "Dark"
    override val channelUsbOtg = "USB OTG"
    override val topBarUsbConfigDesc = "USB Serial Config"
    override val topBarNoUsbDetected = "No USB Serial Device"
    override val topBarAdjustParamsDesc = "Adjust Channel & Port Settings"
    override val topBarDisconnectDesc = "Disconnect"
    override val topBarConnectDesc = "Connect"
    override val topBarDisconnect = "Disconnect"
    override val topBarConnect = "Connect"
    override val topBarSettingsDesc = "Open Settings"
    override val navBackDesc = "Back"
    override val topBarIdleBanner = "○ Idle · Tap the card on the left to configure the connection, or tap Connect"
    override val sheetMultiBanner: (Int) -> String = { count -> "$count USB serial devices detected. Pick a device and confirm parameters." }
    override val sheetLockedDeviceBanner: (String, Int) -> String = { chip, count -> "Locked onto attached $chip ($count USB serial devices detected). Confirm device to connect." }
    override val sheetGenericMultiBanner: (Int) -> String = { count -> "$count USB serial devices detected. Select target device to enter terminal." }
    override val sheetTitleSelectUsb = "Select USB Serial Device"
    override val sheetTitleConfigUsb = "Connection & Channel Config"
    override val sheetSupportedChipsSubtitle = "Single-channel mode · USB supports CH340 / CP210x / FTDI / PL2303 / CDC"
    override val sheetUsbDeviceLockedNotice = "Disconnect the current USB serial connection before switching devices."
    override val sheetCloseDesc = "Close Sheet"
    override val sheetAvailableDevices: (Int) -> String = { count -> "Available USB Devices ($count)" }
    override val sheetRefreshScan = "Rescan"
    override val sheetNoDevicesTitle = "No USB OTG Serial Device Detected"
    override val sheetNoDevicesSubtitle = "Plug in a supported USB serial adapter via a Type-C OTG cable, then grant USB access."
    override val sheetBadgeActive = "Active"
    override val sheetBadgeGranted = "Granted"
    override val sheetBadgeNeedsPerm = "Permission"
    override val sheetSelectedDesc = "Selected"
    override val sheetUnselectedDesc = "Unselected"
    override val sheetPresetsHeader = "Serial preset"
    override val sheetBaudRateHeader = "Baud Rate"
    override val sheetCustomChip = "Custom"
    override val sheetCustomBaudLabel = "Custom baud rate"
    override val sheetCustomBaudPlaceholder = "e.g. 74880, 250000, 1500000"
    override val sheetCustomBaudRangeHint = "Range 300–2,000,000"
    override val sheetDataBits = "Data Bits"
    override val sheetStopBits = "Stop Bits"
    override val sheetParity = "Parity"
    override val sheetDtrPullHigh = "DTR High"
    override val sheetRtsPullHigh = "RTS High"
    override val sheetPresetName = "Preset name"
    override val sheetSavePresetButton = "Save preset"
    override val sheetCtaApplyConnected = "Apply settings"
    override val sheetCtaConnectDevice: (String) -> String = { driver -> "Connect $driver" }
    override val sheetCtaSaveOffline = "Save settings"
    override val termDisplay = "Display"
    override val termDisplayFormat = "Frame format"
    override val termTimestamps = "Time"
    override val termAutoScroll = "Auto-scroll"
    override val termShowTrafficCounters = "Show RX/TX totals"
    override val termFilter = "Filter"
    override val termFilterActive = "Filter active"
    override val termHighlight = "Highlight"
    override val termSaveRecording = "Save Log"
    override val termRecord = "Rec"
    override val termDtrState: (Boolean) -> String = { on -> if (on) "DTR:HI" else "DTR:LO" }
    override val termRtsState: (Boolean) -> String = { on -> if (on) "RTS:HI" else "RTS:LO" }
    override val termDtrShort = "DTR"
    override val termRtsShort = "RTS"
    override val termResetCounters = "Zero Bytes"
    override val termClear = "Clear"
    override val termClearDesc = "Clear Terminal"
    override val termJumpToBottom = "Latest"
    override val termClearFilterBtn = "Clear Filter"
    override val termEmptyConnectBtn = "Connect / Configure Channel"
    override val termClearInputDesc = "Clear input"
    override val termPayloadBytesBadge: (Int) -> String = { n -> "${n} B" }
    override val termRepeatControls = "Repeat"
    override val termRepeatIntervalSummary: (String) -> String = { "Interval $it" }
    override val termRepeatStartBtn = "Start repeat"
    override val termRepeatStopBtn = "Stop repeat"
    override val termMacroPanelCount: (Int) -> String = { count -> "Shortcuts ($count)" }
    override val termFilterPlaceholder = "Filter text or HEX..."
    override val termFilterClearDesc = "Clear filter"
    override val termHighlightPlaceholder = "Highlight keyword"
    override val termQuickHighlightLabel = "Quick highlight:"
    override val termEmptyTitle = "Terminal Buffer Empty"
    override val termNoFilterMatch: (String) -> String = { kw -> "No frames matching \"$kw\"" }
    override val termEmptySubtitle = "Connect to a serial device, then enter ASCII/HEX or tap a shortcut to send it"
    override val termMacroPanel = "Shortcuts"
    override val termExpandMacroDesc = "Open shortcuts"
    override val termFormatChip: (String) -> String = { fmt -> "Fmt:$fmt" }
    override val termToggleFormatDesc = "Switch TX format"
    override val termLineEndingChip: (String) -> String = { end -> "EOL:$end" }
    override val termLineEndingMenuItem: (String) -> String = { end -> "Line Ending: $end" }
    override val termChecksumNoneChip = "CRC:None"
    override val termRepeatTimesSuffix = "x"
    override val termRepeatConfigDesc = "Configure timed repeat"
    override val termSendFileChip = "Send File"
    override val termSendingFileBanner: (String, Long, Long, Int) -> String = { name, sent, total, pct -> "Sending: $name ($sent/$total B · $pct%)" }
    override val termStopFileSend = "Stop"
    override val termToggleEndianDesc = "Switch checksum endianness"
    override val termChecksumTailLabel: (String) -> String = { hex -> "Tail: $hex" }
    override val termHexInputHint = "Enter HEX e.g. 01 03 00 00"
    override val termAsciiInputHint = "Enter ASCII e.g. AT+RST"
    override val termSendDesc = "Send frame"
    override val termSendButton = "Send"
    override val termHistoryDesc = "TX history"
    override val termHistoryButton = "History"
    override val inspectorTitle = "Frame Inspector & Checksum Diagnostics"
    override val inspectorTimestamp: (String) -> String = { ts -> "Time: $ts" }
    override val inspectorHexDumpTitle = "HEX Raw Bytes"
    override val inspectorAsciiTitle = "Decoded Text View"
    override val inspectorDiagTitle = "Checksum Verification"
    override val inspectorModbusPass: (String) -> String = { actual -> "CRC16-Modbus (LE) Verified: $actual" }
    override val inspectorModbusFail: (String, String) -> String = { actual, expected -> "CRC16-Modbus Tail Mismatch (Actual $actual / Expected $expected)" }
    override val inspectorRowModbusLe = "Full CRC16-Modbus (LE)"
    override val inspectorRowCcittBe = "Full CRC16-CCITT (BE)"
    override val inspectorRowSum8 = "Full Sum8 Checksum"
    override val inspectorRowXor8 = "Full XOR8 Checksum"
    override val inspectorCopyToTx = "Copy to TX"
    override val repeatDialogTitle = "Timed Repeat Configuration"
    override val repeatIntervalLabel = "Interval in ms (50 - 60000)"
    override val repeatMaxCountLabel = "Max Repeat Count (0 = unlimited)"
    override val repeatUnlimitedLabel = "Unlimited"
    override val repeatCountUnit: (Int) -> String = { cnt -> "$cnt times" }
    override val repeatProgressLabel: (Int, String) -> String = { sent, limit -> "Sent $sent / $limit" }
    override val fileDialogTitle = "Chunked Local File Send"
    override val fileDialogSize: (Long) -> String = { bytes -> "File size: $bytes B" }
    override val fileDialogLineModeTitle = "Send Line by Line (AT Script Mode)"
    override val fileDialogLineModeSubtitle = "Turn off for fixed-size binary/HEX chunking"
    override val fileDialogChunkSizeTitle = "Chunk Size"
    override val fileDialogDelayTitle = "Inter-chunk / Line Delay"
    override val fileDialogStartSend = "Start Send"
    override val historySheetTitle = "TX History (Deduplicated)"
    override val historySheetSubtitle = "Tap to refill · Add as shortcut or resend on the right"
    override val historyClearDesc = "Clear history"
    override val historyClearBtn = "Clear"
    override val historySearchPlaceholder = "Filter TX history..."
    override val historyShowMore = "Show more"
    override val historyEmpty = "No TX history recorded yet"
    override val historyNoMatch: (String) -> String = { q -> "No history matching \"$q\"" }
    override val historyUseCount: (String, Int) -> String = { time, count -> "$time · ${count}x" }
    override val historyPromoteDesc = "Add as shortcut"
    override val historyResendDesc = "Resend now"
    override val macroGroupAll = "All"
    override val macroAllGroups = "All groups"
    override val macroGroupPickerDesc = "Select shortcut group"
    override val macroUngrouped = "Ungrouped"
    override val macroCreateGroup = "New group…"
    override val macroNewGroupName = "New group name"
    override val macroNamedGroupLabel: (String) -> String = { "$it (named group)" }
    override val macroMoreButton = "More"
    override val macroImportJsonDesc = "Import Shortcuts"
    override val macroExportJsonDesc = "Export Shortcuts"
    override val macroExportAllLabel = "Export All Shortcuts"
    override val macroAddDesc = "New Shortcut"
    override val macroAddBtn = "New"
    override val macroCollapseDesc = "Close shortcuts panel"
    override val macroEditDesc: (String) -> String = { title -> "Edit shortcut $title" }
    override val macroSendDesc: (String) -> String = { title -> "Send $title" }
    override val macroEditorNewTitle = "New Shortcut"
    override val macroEditorEditTitle = "Edit Shortcut"
    override val macroEditorDeleteDesc = "Delete shortcut"
    override val macroEditorNameLabel = "Command Name"
    override val macroEditorNameHint = "e.g. Read Temp Reg"
    override val macroEditorGroupLabel = "Group"
    override val macroEditorPayloadLabel = "Payload"
    override val macroEditorPayloadHint = "e.g. AA 55 01 03 00 00 00 02"
    override val macroEditorFormatLabel = "Format"
    override val macroEditorLineEndingLabel = "Line Ending"
    override val macroEditorColorTagLabel = "Color Tag"
    override val macroEditorSaveBtn = "Save Shortcut"
    override val plotPauseDesc = "Pause sampling"
    override val plotResumeDesc = "Resume sampling"
    override val plotStartBtn = "Start"
    override val plotPauseBtn = "Pause"
    override val plotResumeBtn = "Resume"
    override val plotAutoY = "Auto Y axis"
    override val plotZoomInDesc = "Fewer samples"
    override val plotZoomOutDesc = "More samples"
    override val plotWindowPoints: (Int) -> String = { pts -> "${pts}pts" }
    override val plotClearDesc = "Clear waveform"
    override val plotStatusHold = "Paused"
    override val plotStatusLive = "Live"
    override val plotEmptyTitle = "Waiting for data"
    override val plotEmptySubtitle = "Waiting for incoming data that matches this profile."
    override val plotBackToTerminalBtn = "Go to Terminal"
    override val plotNoChannelSelected = "Enable at least one channel to view waveform"
    override val plotChannelsSection = "Channels"
    override val plotMore = "More"
    override val plotFixedYRange = "Fixed range 0–60"
    override val plotWindowLabel = "Window size"
    override val plotSwitchProfileHint = "Switching profiles clears the current waveform"
    override val plotChooseProfileHint = "Choose a parsing profile, then tap Start to plot data."
    override val plotStartHint = "Tap Start to plot incoming data."
    override val plotWaitingForUsb = "Waiting for USB connection"
    override val plotWaitingForNewData = "Waiting for new data"
    override val plotShowChannels = "Show channels"
    override val plotMinimum = "Min"
    override val plotMaximum = "Max"
    override val plotChooseProfile = "Choose a parsing profile"
    override val plotProfileColumns = "Numeric columns (CSV)"
    override val plotProfileKeys = "Key-value pairs"
    override val plotProfileNew = "New profile"
    override val plotProfileEdit = "Edit profile"
    override val plotProfileDelete = "Delete profile"
    override val plotProfileDeleteConfirm = "Delete this profile?"
    override val plotProfileSaveFailed = "Could not save. Check for a duplicate name and try again."
    override val plotProfileSelectionFailed = "Could not remember the selected profile. Choose it again."
    override val plotProfileDeleteFailed = "Could not delete the profile. Try again."
    override val plotProfileName = "Profile name"
    override val plotProfileTextRules = "Text parsing settings"
    override val plotProfileFields = "Plot fields"
    override val plotProfilePreview = "Sample and preview (optional)"
    override val plotProfileNameRequired = "Enter a profile name."
    override val plotProfileFieldNameRequired = "Give each field a name."
    override val plotProfileDuplicateName = "This profile name already exists."
    override val plotProfileWidth = "Full frame length (bytes)"
    override val plotProfileFormat = "Data format"
    override val plotFormatColumns = "Text columns"
    override val plotFormatBinary = "Binary"
    override val plotProfileBoundary = "Record boundary"
    override val plotBoundaryLine = "LF / CRLF"
    override val plotBoundaryFrame = "Serial receive frame"
    override val plotProfileDelimiter = "Field delimiter"
    override val plotDelimiterComma = "Comma"
    override val plotDelimiterSpace = "Whitespace"
    override val plotDelimiterTab = "Tab"
    override val plotDelimiterSemicolon = "Semicolon"
    override val plotProfileKeySeparator = "Key-value separator"
    override val plotProfileHeader = "Header (HEX)"
    override val plotProfileTrailer = "Trailer (HEX)"
    override val plotProfileBinaryHint = "Set at least two of header, full frame length and trailer. Separate HEX bytes with spaces."
    override val plotProfileAddField = "Add field"
    override val plotProfileRemoveField = "Remove field"
    override val plotProfileFieldName = "Field name"
    override val plotProfileUnit = "Unit (optional)"
    override val plotProfileColumn = "Column (starts at 1)"
    override val plotProfileKey = "Key name"
    override val plotProfileOffset = "Byte offset (starts at 0)"
    override val plotProfileNumberType = "Number type"
    override val plotProfileByteOrder = "Byte order"
    override val plotByteOrderLittle = "Little endian"
    override val plotByteOrderBig = "Big endian"
    override val plotProfileScale = "Scale"
    override val plotProfileBias = "Bias"
    override val plotProfileSampleText = "Sample text"
    override val plotProfileSampleHex = "Sample frame (HEX)"
    override val plotProfileInvalid = "Enter valid profile rules and fields."
    override val plotProfileClearSample = "Clear sample"
    override val plotProfileSampleInvalid = "No complete record could be parsed. Edit or clear the sample."
    override val plotProfileSave = "Save"
    override val plotOverloaded = "Sampling paused because parsing fell behind incoming data."
    override fun plotTimeSpan(milliseconds: Long, seconds: String): String =
        if (milliseconds < 1000) "$milliseconds ms" else "About $seconds s"
    override val logsStartRecordBtn = "Record"
    override val logsLastSevenDays = "Last 7 days"
    override val logsToday = "Today"
    override val logsAllTime = "All time"
    override val logsCustomRange = "Custom time range"
    override val logsRangeInputHint = "Enter YYYY-MM-DD, optionally followed by HH:mm."
    override val logsRangeStart = "Start"
    override val logsRangeEnd = "End"
    override val logsRangeInvalid = "Check the date format and time order."
    override val logsApplyRange = "Apply"
    override val logsEmptyRange = "No recordings in this range"
    override val logsMoreActions = "More actions"
    override val logsLoadFailed = "Could not load recordings"
    override val logsRetry = "Retry"
    override val logsOpenFailed = "Could not open this recording"
    override val logsDeleteFailed = "Could not delete recording"
    override val logsHighlightActive = "Highlight active"
    override val logsNoFrames = "No matching frames"
    override val logsMatchedFrames: (Int, Int) -> String = { matched, total -> "$matched of $total frames" }
    override val logsEmptyTitle = "No Recorded Sessions Yet"
    override val logsEmptySubtitle = "Tap Record to save serial frames"
    override val logsFramesCount: (Int) -> String = { count -> "$count frames" }
    override val logsEmailBtn = "Email"
    override val logsShareDesc = "System Share Log"
    override val logsDeleteCardDesc = "Delete session"
    override val logsDeleteConfirmTitle = "Delete Session Log?"
    override val logsDeleteConfirmBody: (String) -> String = { title -> "Permanently delete \"$title\"? This cannot be undone." }
    override val logsDeleteConfirmBtn = "Delete"
    override val logsExportTxtBtn = "Export TXT"
    override val logsExportCsvBtn = "Export CSV"
    override val emailDialogTitle = "Send Log via Email App"
    override val emailDialogSessionSummary: (String, Int, String) -> String = { title, count, size -> "$title ($count frames · $size)" }
    override val emailDialogFormatLabel = "Attachment Format"
    override val emailDialogFormatTxt = "TXT"
    override val emailDialogFormatCsv = "CSV"
    override val emailDialogPrivacyNotice = "diSerial opens an email app or the system share sheet if none is available. The selected app gets temporary read access to the attachment. diSerial does not access your email account or upload the log. Before sending, check the recipients and make sure the log contains no sensitive data such as keys or passwords."
    override val emailDialogConfirmBtn = "Open Email App"
    override val logViewerFilterPlaceholder = "Filter frames"
    override val settingsTerminalSection = "Terminal Settings"
    override val settingsAdvanced = "Advanced"
    override val settingsCharsetDetail = "Used for sending and receiving text. Choose GBK if your device uses it."
    override val settingsResetTerminalDetail = "Restore the defaults for text encoding, terminal text size, clearing input after sending, receive frame gap, terminal buffer, and error vibration."
    override val settingsCharsetLabel = "Text Encoding"
    override val settingsFontSizeLabel = "Terminal Text Size"
    override val settingsFontCompact = "Small"
    override val settingsFontNormal = "Standard"
    override val settingsFontLarge = "Large"
    override val settingsFrameTimeoutTitle = "Receive Frame Gap"
    override val settingsFrameTimeoutSubtitle = "Idle interval separating consecutive RX frames"
    override val settingsMaxBufferTitle = "Terminal Buffer"
    override val settingsMaxBufferSubtitle = "The terminal keeps recent frames. Active recordings continue to save frames."
    override val settingsMaxBufferOption: (Int) -> String = { n -> "$n frames" }
    override val settingsAutoConnectTitle = "Auto-connect on USB Attach"
    override val settingsKeepScreenOnTitle = "Keep Screen On"
    override val settingsClearAfterSendTitle = "Clear Input After Send"
    override val settingsVibrateTitle = "Vibrate on Error"
    override val settingsAboutCardTitle = "About diSerial"
    override val settingsResetDefaultsBtn = "Restore Terminal Defaults"
    override val aboutTagline = "USB serial terminal with data visualization"
    override val aboutChipsTitle = "USB Compatibility"
    override val aboutUsbScope = "The Android release targets selected single-port adapters in the families below. Compatibility varies by model."
    override val aboutPrivacyTitle = "Privacy and Data"
    override val aboutPrivacyBody = "Serial recordings, device presets, send history, and shortcuts are stored on your device. On Android, this database content is excluded from automatic cloud backup. You can manually export or share logs."
    override val aboutPrivacyPolicyLink = "Read the full privacy policy (English)"
    override val aboutWiringBtn = "Wiring and Troubleshooting"
    override val aboutChangelogBtn = "Release Notes"
    override val aboutWiringTip1 = "If an adapter is not detected, check USB OTG support and the cable. Some phones require enabling OTG in system settings."
    override val aboutWiringTip2 = "For TTL serial, check that both sides use compatible voltage levels. Connect TX to the other side’s RX and connect both grounds."
    override val aboutWiringTip3 = "If text appears garbled, check the baud rate, data bits, parity, and stop bits, then check the text encoding in Terminal Settings."
    override val aboutWiringTip4 = "For protocols such as Modbus RTU, check serial parameters and the frame checksum against the device manual."
    override val aboutChangelogVersion = "v1.0.0"
    override val aboutChangelogBody = "• USB serial terminal with send and receive, HEX frame checksums, and shortcuts.\n• Serial data visualization plots incoming data in real time using CSV, key-value, or binary parsing profiles.\n• Log recording, date filtering, and export."
    override val shareLogChooserTitle = "Share Serial Log"
    override val noShareAppNotice = "No sharing app available on this device"
    override val emailClientChooserTitle = "Choose Email App to Send Log"
    override val noEmailFallbackNotice = "No email client found; switched to system share"
    override val emailFallbackChooserTitle: (String) -> String = { ext -> "Send Serial Log ($ext)" }
    override val noEmailOrShareAppNotice = "No email or sharing app available on this device"
    override val exportTxtHeaderTitle = "# diSerial Serial Communication Log Report"
    override val exportTxtSession = "# Session"
    override val exportTxtDevice = "# Device"
    override val exportTxtConfig = "# Config"
    override val exportTxtTime = "# Time"
    override val exportTxtFrames = "# Frames"
    override val exportTxtSize = "Size"
    override val exportOpenStreamError = "Unable to open target output stream"
    override val emailSubject: (String) -> String = { title -> "diSerial Log: $title" }
    override val emailBody: (String, String, String, String, Int, String, String) -> String = { title, device, cfg, time, count, size, ext ->
        buildString {
            appendLine("Attached is the serial communication log exported from diSerial ($ext format).")
            appendLine()
            appendLine("Session : $title")
            appendLine("Device  : $device")
            appendLine("Config  : $cfg")
            appendLine("Time    : $time")
            appendLine("Summary : $count frames · $size")
            appendLine()
            appendLine("Note: Verify recipients and confirm the log contains no sensitive keys or passwords before sending.")
        }
    }
    override val vmSysUsbAttached: (String, String) -> String = { chip, vidPid -> "USB serial device detected: $chip ($vidPid)" }
    override val vmSysUsbDetached = "USB device detached"
    override val vmNoticeUsbPermDenied = "USB permission denied"
    override val vmNoticeConnected: (String, String) -> String = { chip, cfg -> "Connected $chip ($cfg)" }
    override val vmSysPortOpened: (String, String) -> String = { chip, cfg -> "Port opened: $chip · $cfg" }
    override val vmSysDisconnected: (String) -> String = { reason -> "Disconnected: $reason" }
    override val vmSysError: (String) -> String = { msg -> "Error: $msg" }
    override val vmNoticeNoUsbFound = "No USB OTG serial device found; check cable & OTG power"
    override val vmNoticeFoundDevices: (Int) -> String = { count -> "Detected $count USB serial device(s)" }
    override val vmUserDisconnectedReason = "Serial connection closed by user"
    override val vmNoticePlugUsbFirst = "Please plug in a supported USB OTG serial device first"
    override val vmNoticeOpenPortFailed = "Failed to open serial port"
    override val vmNoticeParamsUpdated: (String) -> String = { cfg -> "Port parameters updated: $cfg" }
    override val vmSysParamsSwitched: (String) -> String = { cfg -> "Port parameters switched: $cfg" }
    override val vmNoticeParamsFailed = "Failed to update port parameters"
    override val vmNoticeParamsSavedOffline: (String) -> String = { cfg -> "Parameters saved ($cfg); ready when device attaches" }
    override val vmCustomPresetDefaultName: (Int) -> String = { baud -> "Custom $baud" }
    override val vmNoticePresetSaved: (String) -> String = { name -> "Saved preset \"$name\"" }
    override val vmNoticeTerminalCleared = "Terminal display cleared"
    override val vmNoticeCopiedToTx = "Copied frame to TX input"
    override val vmNoticeRecordingStarted = "Started recording session log"
    override val vmNoticeRecordingEmpty = "No frames captured during recording"
    override val vmSessionTitlePrefix = "Serial Session"
    override val vmOfflineDeviceSummary = "Offline Terminal Buffer"
    override val vmNoticeLogSaved: (Int) -> String = { count -> "Saved $count frames to local log archive" }
    override val vmNoticeCountersReset = "RX / TX byte counters reset"
    override val vmPinHighLabel = "High (ON)"
    override val vmPinLowLabel = "Low (OFF)"
    override val vmPinShortHigh = "HIGH"
    override val vmPinShortLow = "LOW"
    override val vmSysPinChanged: (String, String) -> String = { pin, stateLabel -> "$pin set $stateLabel" }
    override val vmNoticePinChanged: (String, String) -> String = { pin, stateLabel -> "$pin set $stateLabel" }
    override val vmNoticePinFailed: (String) -> String = { pin -> "$pin control failed" }
    override val vmNoticePinPreset: (String, String) -> String = { pin, shortState -> "Preset $pin=$shortState; applies on connect" }
    override val vmNoticeRepeatStopped: (Int) -> String = { count -> "Timed repeat stopped ($count sent)" }
    override val vmNoticeConnectBeforeRepeat = "Connect USB serial device before starting timed repeat"
    override val vmNoticeEnterRepeatPayload = "Enter payload before starting timed repeat"
    override val vmRepeatUnlimitedHint = ", unlimited"
    override val vmRepeatLimitHint: (Int) -> String = { max -> ", max $max" }
    override val vmNoticeRepeatStarted: (Int, String) -> String = { ms, limit -> "Timed repeat started (every ${ms}ms$limit)" }
    override val vmNoticeRepeatFinished: (Int) -> String = { count -> "Timed repeat finished ($count sent)" }
    override val vmNoticeConnectBeforeSend = "Please connect a USB serial device first"
    override val vmNoticeTxPayloadEmpty = "Enter data to send."
    override val vmNoticeSendFormatError = "Invalid TX format"
    override val vmNoticeWriteFailed = "Serial write failed"
    override val vmNoticeFileEmpty = "Selected file is empty or unreadable"
    override val vmNoticeConnectBeforeFileSend = "Connect USB serial device before sending file"
    override val vmSysFileSendStart: (String, Long) -> String = { name, bytes -> "Starting file send: $name ($bytes B)" }
    override val vmFileReadError = "Unable to read file content"
    override val vmFileChunkWriteError = "Chunk write failed"
    override val vmSysFileSendFailed: (String, Long) -> String = { msg, sent -> "File send aborted: $msg ($sent B sent)" }
    override val vmSysFileSendDone: (String, Long) -> String = { name, sent -> "File send complete: $name ($sent B sent)" }
    override val vmNoticeFileSendDone: (String) -> String = { name -> "File \"$name\" sent completely" }
    override val vmNoticeFileSendCancelled: (String) -> String = { name -> "Cancelled sending \"$name\"" }
    override val vmSysFileSendCancelled: (String) -> String = { name -> "File send cancelled: $name" }
    override val vmNoticeHistoryRefilled = "Loaded command and send settings from history"
    override val vmNoticeHistoryFormatError = "Invalid history command format"
    override val vmNoticeHistoryMissingSentBytes = "This entry has no saved bytes. Check and resend it before adding it as a shortcut."
    override val vmNoticeHistoryResent = "Resent history command"
    override val vmNoticeHistoryResendFailed = "Resend failed"
    override val vmNoticeHistoryCleared = "TX history cleared"
    override val vmNoticeConnectBeforeMacro: (String) -> String = { title -> "Connect a USB serial device before sending shortcut \"$title\"" }
    override val vmNoticeMacroFormatInvalid = "Enter valid hex bytes, such as 01 03 00 00"
    override val vmNoticeMacroNeedsResave = "Edit this older shortcut and save the complete frame before sending."
    override val vmNoticeMacroSendFailed = "Shortcut send failed"
    override val vmMacroFrameTag: (String) -> String = { title -> "[Shortcut:$title]" }
    override val vmMacroDefaultTitle = "Custom Command"
    override val vmNoticeMacroPayloadEmpty = "Shortcut payload cannot be empty"
    override val vmNoticeMacroSaved: (String) -> String = { title -> "Saved shortcut \"$title\"" }
    override val vmNoticeMacroDeleted = "Deleted shortcut"
    override val vmNoticeMacroExportEmpty = "No shortcuts to export"
    override val vmMacroWriteJsonError = "Unable to write target JSON file"
    override val vmNoticeMacroExported: (Int) -> String = { count ->
        if (count == 1) "Exported 1 shortcut to JSON" else "Exported $count shortcuts to JSON"
    }
    override val vmNoticeMacroExportFailed = "Shortcut export failed"
    override val vmMacroReadFileError = "Unable to read selected file"
    override val vmNoticeMacroReadFailed = "File read failed"
    override val vmNoticeMacroJsonInvalid = "Failed to read shortcut JSON"
    override val vmNoticeMacroImported: (Int) -> String = { count ->
        if (count == 1) "Imported 1 new shortcut" else "Imported $count new shortcuts"
    }
    override val vmNoticeMacroAllExist = "All shortcuts in this file already exist"
    override val vmNoticePlotCleared = "Waveform buffer cleared"
    override val vmNoticeLogDeleted = "Deleted log session"
    override val vmNoticeExportedFormat: (String) -> String = { ext -> "Exported as $ext file" }
    override val vmNoticeExportFailed = "Export failed"
    override val vmNoticeSharePrepFailed = "Failed to prepare share file"
    override val vmNoticeSettingsReset = "Restored terminal defaults"
    override val relJustNow = "Just now"
    override val relMinutesAgo: (Long) -> String = { m -> "${m}m ago" }
    override val relHoursAgo: (Long) -> String = { h -> "${h}h ago" }
}

fun AppTab.localizedLabel(s: AppStrings): String = when (this) {
    AppTab.TERMINAL -> s.tabTerminal
    AppTab.PLOTTER -> s.tabPlotter
    AppTab.LOGS -> s.tabLogs
    AppTab.SETTINGS -> s.tabSettings
    AppTab.TERMINAL_SETTINGS -> s.settingsTerminalSection
    AppTab.ABOUT -> s.tabAbout
}

fun ConnectionStatus.localizedLabel(s: AppStrings): String = when (this) {
    ConnectionStatus.DISCONNECTED -> s.statusDisconnected
    ConnectionStatus.CONNECTING -> s.statusConnecting
    ConnectionStatus.CONNECTED -> s.statusConnected
}

fun LineEnding.localizedLabel(s: AppStrings): String = when (this) {
    LineEnding.NONE -> s.lineEndingNone
    LineEnding.CR, LineEnding.LF, LineEnding.CRLF -> label
}

fun ChecksumMode.localizedLabel(s: AppStrings): String = when (this) {
    ChecksumMode.NONE -> s.checksumNone
    ChecksumMode.CRC16_MODBUS, ChecksumMode.CRC16_CCITT -> label
    ChecksumMode.SUM8 -> s.checksumSum8
    ChecksumMode.XOR8 -> s.checksumXor8
}

fun ChecksumMode.localizedShortLabel(s: AppStrings): String = when (this) {
    ChecksumMode.NONE -> s.checksumNoneShort
    else -> shortLabel
}

fun ChecksumEndian.localizedLabel(s: AppStrings): String = when (this) {
    ChecksumEndian.LITTLE_ENDIAN -> s.endianLe
    ChecksumEndian.BIG_ENDIAN -> s.endianBe
}

fun MacroColorTag.localizedLabel(s: AppStrings): String = when (this) {
    MacroColorTag.COBALT -> s.colorCobalt
    MacroColorTag.EMERALD -> s.colorEmerald
    MacroColorTag.AMBER -> s.colorAmber
    MacroColorTag.VIOLET -> s.colorViolet
}

fun CharsetEncoding.localizedLabel(s: AppStrings): String = when (this) {
    CharsetEncoding.UTF_8, CharsetEncoding.ISO_8859_1 -> label
    CharsetEncoding.GBK -> s.charsetGbk
}

fun AppLanguage.localizedLabel(s: AppStrings): String = nativeName ?: s.langOptionSystem

fun AppThemeMode.localizedLabel(s: AppStrings): String = when (this) {
    AppThemeMode.SYSTEM -> s.themeOptionSystem
    AppThemeMode.LIGHT -> s.themeOptionLight
    AppThemeMode.DARK -> s.themeOptionDark
}

fun formatLocalizedRelativeTime(
    nowMs: Long,
    pastMs: Long,
    fallbackLabel: String,
    s: AppStrings,
): String {
    if (pastMs <= 0L) return fallbackLabel
    val diffSec = ((nowMs - pastMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> s.relJustNow
        diffSec < 3600L -> s.relMinutesAgo(diffSec / 60L)
        diffSec < 86400L -> s.relHoursAgo(diffSec / 3600L)
        else -> PlatformClock.formatMonthDayTime(pastMs)
    }
}
