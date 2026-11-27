package com.example.data.local

import com.example.domain.protocol.ProtocolEngine
import com.example.domain.protocol.PlotConfiguration
import com.example.domain.protocol.PlotProfile
import com.example.ui.model.ChecksumEndian
import com.example.ui.model.ChecksumMode
import com.example.ui.model.FrameDirection
import com.example.ui.model.LineEnding
import com.example.ui.model.MacroColorTag
import com.example.ui.model.MacroCommandItem
import com.example.ui.model.ParityMode
import com.example.ui.model.SavedLogSession
import com.example.ui.model.SavedLogSummary
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.TerminalFrame
import com.example.ui.model.TxFormat
import com.example.ui.model.TxHistoryItem
import com.example.platform.PlatformClock
import com.example.platform.formatByteSizeLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SerialRepository(private val database: AppDatabase) {
    private val profileDao = database.profileDao()
    private val macroDao = database.macroDao()
    private val historyDao = database.historyDao()
    private val sessionLogDao = database.sessionLogDao()
    private val plotDao = database.plotConfigurationDao()

    val plotProfilesFlow: Flow<List<PlotProfile>> = plotDao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            runCatching { Json.decodeFromString<PlotConfiguration>(entity.rulesJson) }
                .getOrNull()?.takeIf { it.isValid() }?.let { PlotProfile(entity.id, it) }
        }
    }

    suspend fun savePlotProfile(id: Long?, configuration: PlotConfiguration): Long {
        require(configuration.isValid())
        val entity = PlotConfigurationEntity(id ?: 0, configuration.name, Json.encodeToString(configuration))
        if (id == null) return plotDao.insert(entity)
        check(plotDao.update(entity) == 1)
        return id
    }

    suspend fun deletePlotProfile(id: Long) = plotDao.delete(id)

    val profilesFlow: Flow<List<SerialPortConfig>> = profileDao.observeAll().map { entities ->
        entities.map { entity ->
            SerialPortConfig(
                presetName = entity.presetName,
                baudRate = entity.baudRate,
                dataBits = entity.dataBits,
                stopBits = entity.stopBits,
                parity = ParityMode.entries.firstOrNull { it.name == entity.parityName } ?: ParityMode.NONE,
                dtrEnabled = entity.dtrEnabled,
                rtsEnabled = entity.rtsEnabled,
            )
        }
    }

    val macrosFlow: Flow<List<MacroCommandItem>> = macroDao.observeAll().map { entities ->
        entities.map { entity ->
            MacroCommandItem(
                id = entity.id,
                title = entity.title,
                group = entity.groupName,
                payload = entity.payload,
                txFormat = TxFormat.entries.firstOrNull { it.name == entity.txFormatName } ?: TxFormat.HEX,
                lineEnding = LineEnding.entries.firstOrNull { it.name == entity.lineEndingName } ?: LineEnding.NONE,
                checksumMode = ChecksumMode.entries.firstOrNull { it.name == entity.checksumModeName } ?: ChecksumMode.NONE,
                checksumEndian = ChecksumEndian.entries.firstOrNull { it.name == entity.checksumEndianName }
                    ?: ChecksumEndian.LITTLE_ENDIAN,
                checksumSkipBytes = entity.checksumSkipBytes.coerceIn(0, 3),
                colorTag = MacroColorTag.entries.firstOrNull { it.name == entity.colorTagName } ?: MacroColorTag.COBALT,
            )
        }
    }

    val historyFlow: Flow<List<TxHistoryItem>> = historyDao.observeRecent().map { entities ->
        val now = PlatformClock.currentTimeMillis()
        entities.map { entity ->
            TxHistoryItem(
                id = entity.id,
                payload = entity.payload,
                txFormat = TxFormat.entries.firstOrNull { it.name == entity.txFormatName } ?: TxFormat.HEX,
                lineEnding = LineEnding.entries.firstOrNull { it.name == entity.lineEndingName } ?: LineEnding.NONE,
                checksumMode = ChecksumMode.entries.firstOrNull { it.name == entity.checksumModeName } ?: ChecksumMode.NONE,
                checksumEndian = ChecksumEndian.entries.firstOrNull { it.name == entity.checksumEndianName }
                    ?: ChecksumEndian.LITTLE_ENDIAN,
                useCount = entity.useCount,
                lastUsedLabel = formatRelativeTime(now, entity.lastUsedAtMs),
                lastUsedAtMs = entity.lastUsedAtMs,
                sentHex = entity.sentHex,
            )
        }
    }

    suspend fun loadLogPage(
        fromInclusive: Long,
        untilExclusive: Long,
        after: SavedLogSummary?,
        limit: Int,
    ): List<SavedLogSummary> = sessionLogDao.loadPage(
        fromInclusive = fromInclusive,
        untilExclusive = untilExclusive,
        beforeStart = after?.startedAtMs ?: Long.MAX_VALUE,
        beforeId = after?.id ?: Long.MAX_VALUE,
        limit = limit,
    ).map { it.toLogSummary() }

    suspend fun loadLogSession(id: Long): SavedLogSession? {
        val row = sessionLogDao.loadWithEntries(id) ?: return null
        val session = row.session
        return SavedLogSession(
            id = session.id,
            title = session.title,
            deviceSummary = session.deviceSummary,
            configSummary = session.configSummary,
            timeRange = "${PlatformClock.formatMonthDayTime(session.startedAtMs)} - ${PlatformClock.formatHourMinute(session.endedAtMs)}",
            frameCount = session.frameCount,
            sizeLabel = formatByteSizeLabel(session.totalBytes),
            frames = row.entries.sortedBy { it.seqIndex }.map { entry ->
                TerminalFrame(
                    id = entry.id,
                    timestamp = entry.timestampLabel,
                    direction = FrameDirection.entries.firstOrNull { it.name == entry.directionName }
                        ?: FrameDirection.SYS,
                    rawBytes = entry.rawBytes,
                    asciiText = entry.asciiText,
                )
            },
            fullTimeRange = "${PlatformClock.formatLogDateTime(session.startedAtMs)} – ${PlatformClock.formatLogDateTime(session.endedAtMs)}",
        )
    }

    private fun SessionLogEntity.toLogSummary() = SavedLogSummary(
        id = id,
        title = title,
        deviceSummary = deviceSummary,
        startedAtMs = startedAtMs,
        frameCount = frameCount,
        sizeLabel = formatByteSizeLabel(totalBytes),
    )

    suspend fun saveProfile(name: String, config: SerialPortConfig) {
        profileDao.insert(
            ConnectionProfileEntity(
                presetName = name,
                baudRate = config.baudRate,
                dataBits = config.dataBits,
                stopBits = config.stopBits,
                parityName = config.parity.name,
                dtrEnabled = config.dtrEnabled,
                rtsEnabled = config.rtsEnabled,
                sortOrder = 100,
            ),
        )
    }

    suspend fun upsertMacro(macro: MacroCommandItem): Long {
        return macroDao.upsert(
            MacroCommandEntity(
                id = if (macro.id > 0L) macro.id else 0L,
                title = macro.title,
                groupName = macro.group,
                payload = macro.payload,
                txFormatName = macro.txFormat.name,
                lineEndingName = macro.lineEnding.name,
                checksumModeName = macro.checksumMode.name,
                checksumEndianName = macro.checksumEndian.name,
                checksumSkipBytes = macro.checksumSkipBytes,
                colorTagName = macro.colorTag.name,
                sortOrder = 10,
            ),
        )
    }

    suspend fun deleteMacro(id: Long) {
        macroDao.deleteById(id)
    }

    suspend fun importMacros(items: List<MacroCommandItem>): Int {
        if (items.isEmpty()) return 0
        val existingKeys = macroDao.getAllOnce()
            .map { "${it.title}|${it.groupName}|${it.payload}|${it.txFormatName}" }
            .toMutableSet()
        val toInsert = items.mapNotNull { macro ->
            val key = "${macro.title}|${macro.group}|${macro.payload}|${macro.txFormat.name}"
            if (existingKeys.add(key)) {
                MacroCommandEntity(
                    id = 0L,
                    title = macro.title,
                    groupName = macro.group,
                    payload = macro.payload,
                    txFormatName = macro.txFormat.name,
                    lineEndingName = macro.lineEnding.name,
                    checksumModeName = macro.checksumMode.name,
                    checksumEndianName = macro.checksumEndian.name,
                    checksumSkipBytes = macro.checksumSkipBytes.coerceIn(0, 3),
                    colorTagName = macro.colorTag.name,
                    sortOrder = 20,
                )
            } else {
                null
            }
        }
        if (toInsert.isNotEmpty()) {
            macroDao.insertAll(toInsert)
        }
        return toInsert.size
    }

    suspend fun recordCommandHistory(
        payload: String,
        txFormat: TxFormat,
        lineEnding: LineEnding,
        checksumMode: ChecksumMode,
        checksumEndian: ChecksumEndian,
        sentBytes: ByteArray,
    ) {
        val effectiveChecksum = if (txFormat == TxFormat.HEX) checksumMode else ChecksumMode.NONE
        val effectiveEndian = if (txFormat == TxFormat.HEX) checksumEndian else ChecksumEndian.LITTLE_ENDIAN
        historyDao.record(
            CommandHistoryEntity(
                payload = payload,
                txFormatName = txFormat.name,
                lineEndingName = lineEnding.name,
                checksumModeName = effectiveChecksum.name,
                checksumEndianName = effectiveEndian.name,
                useCount = 1,
                lastUsedAtMs = PlatformClock.currentTimeMillis(),
                sentHex = ProtocolEngine.formatHex(sentBytes),
            ),
        )
    }

    suspend fun clearCommandHistory() {
        historyDao.clearAll()
    }

    suspend fun saveSessionLog(
        title: String,
        deviceSummary: String,
        configSummary: String,
        startedAtMs: Long,
        endedAtMs: Long,
        frames: List<TerminalFrame>,
    ): Long {
        val totalBytes = frames.sumOf { it.rawBytes.size.toLong() }
        val session = SessionLogEntity(
            title = title,
            deviceSummary = deviceSummary,
            configSummary = configSummary,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            frameCount = frames.size,
            totalBytes = totalBytes,
        )
        val entries = frames.mapIndexed { idx, frame ->
            SessionLogEntryEntity(
                sessionId = 0L,
                timestampLabel = frame.timestamp,
                directionName = frame.direction.name,
                rawBytes = frame.rawBytes,
                asciiText = frame.asciiText,
                seqIndex = idx,
            )
        }
        return sessionLogDao.saveSessionWithEntries(session, entries)
    }

    suspend fun deleteSessionLog(id: Long) {
        sessionLogDao.deleteSessionById(id)
    }

    private fun formatRelativeTime(nowMs: Long, pastMs: Long): String {
        val diffSec = ((nowMs - pastMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "刚刚"
            diffSec < 3600L -> "${diffSec / 60L}分钟前"
            diffSec < 86400L -> "${diffSec / 3600L}小时前"
            else -> PlatformClock.formatMonthDayTime(pastMs)
        }
    }
}
