package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.protocol.PlotConfiguration
import com.example.platform.formatDecimal
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.PlotChannel
import com.example.ui.model.PlotSamplingState
import com.example.ui.state.SerialUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.instrumentColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

private val RecentPlotSampleTimeout = 2.seconds

@Composable
fun PlotterScreen(
    state: SerialUiState,
    onSelectProfile: (Long) -> Unit,
    onSaveProfile: suspend (Long?, PlotConfiguration) -> Boolean,
    onDeleteProfile: (Long) -> Unit,
    onToggleChannel: (Int) -> Unit,
    onToggleSampling: () -> Unit,
    onToggleAutoScale: () -> Unit,
    onAdjustWindow: (Int) -> Unit,
    onClearPlot: () -> Unit,
    onGoToTerminal: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showChannels by remember { mutableStateOf(false) }
    val visibleChannels = remember(state.plotChannels, state.plotterWindowPoints) {
        state.plotChannels.filter { it.visible && it.samples.isNotEmpty() }.map { channel ->
            channel.copy(samples = channel.samples.takeLast(state.plotterWindowPoints))
        }
    }
    val minimumHeight = (320 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp

    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxOf(maxHeight, minimumHeight))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlotProfileBar(
                state = state,
                onSelect = onSelectProfile,
                onSave = onSaveProfile,
                onDelete = onDeleteProfile,
                onToggleSampling = onToggleSampling,
                onToggleAutoScale = onToggleAutoScale,
                onAdjustWindow = onAdjustWindow,
                onClearPlot = onClearPlot,
            )

            PlotWaveform(
                state = state,
                channels = visibleChannels,
                onGoToTerminal = onGoToTerminal,
                onShowChannels = { showChannels = true },
                modifier = Modifier.weight(1f),
            )

            if (state.plotChannels.isNotEmpty()) {
                PlotChannelSummary(state.plotChannels) { showChannels = true }
            }
        }
    }

    if (showChannels && state.plotChannels.isNotEmpty()) {
        PlotChannelSheet(
            channels = state.plotChannels,
            onToggleChannel = onToggleChannel,
            onDismiss = { showChannels = false },
        )
    }
}

@Composable
private fun PlotWaveform(
    state: SerialUiState,
    channels: List<PlotChannel>,
    onGoToTerminal: () -> Unit,
    onShowChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val lastSample = state.plotLastSampleAt
    val recentlyReceived by produceState(false, lastSample) {
        val remaining = lastSample?.let { RecentPlotSampleTimeout - it.elapsedNow() }
        value = remaining != null && remaining.isPositive()
        if (value && remaining != null) {
            delay(remaining)
            value = false
        }
    }
    val profile = state.activePlotProfile
    val connected = state.connectionStatus == ConnectionStatus.CONNECTED
    val live = connected && state.plotSampling == PlotSamplingState.RUNNING && recentlyReceived
    val status = when {
        profile == null || state.plotSampling == PlotSamplingState.IDLE -> null
        state.plotSampling == PlotSamplingState.PAUSED -> s.plotStatusHold
        !connected -> s.plotWaitingForUsb
        live -> s.plotStatusLive
        state.plotChannels.isEmpty() -> s.plotEmptyTitle
        else -> s.plotWaitingForNewData
    }
    val timeStart = channels.minOfOrNull { it.samples.first().timeMs } ?: 0L
    val timeEnd = channels.maxOfOrNull { it.samples.last().timeMs } ?: 0L

    val (yMin, yMax) = remember(channels, state.plotterAutoScale) {
        val points = channels.flatMap { it.samples }
        when {
            !state.plotterAutoScale -> 0.0 to 60.0
            points.isEmpty() -> 0.0 to 50.0
            else -> {
                val lo = points.minOf { it.value }
                val hi = points.maxOf { it.value }
                val padding = maxOf(0.12, hi * 0.12 - lo * 0.12, maxOf(abs(lo), abs(hi)) * 1e-12)
                (lo - padding).coerceAtLeast(-Double.MAX_VALUE) to (hi + padding).coerceAtMost(Double.MAX_VALUE)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("oscilloscope_canvas_box"),
        shape = RoundedCornerShape(12.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.border),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (channels.isNotEmpty()) {
                    OscilloscopeCanvas(
                        channels = channels,
                        channelColors = c.scopeChannelColors,
                        timeStart = timeStart,
                        timeEnd = timeEnd,
                        yMin = yMin,
                        yMax = yMax,
                        majorGridColor = c.scopeGrid,
                        minorGridColor = c.scopeGrid.copy(alpha = 0.55f),
                        pointHaloColor = c.surface,
                        modifier = Modifier.fillMaxSize().padding(start = 50.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .padding(start = 6.dp, top = 5.dp, bottom = 5.dp)
                            .width(41.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (index in 0..4) {
                            val fraction = index / 4.0
                            val tick = yMax * (1.0 - fraction) + yMin * fraction
                            Text(
                                text = formatPlotValue(tick, 1, 10_000.0),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = c.scopeAxis,
                                ),
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        when {
                            profile == null -> Text(s.plotChooseProfileHint, textAlign = TextAlign.Center)
                            state.plotSampling == PlotSamplingState.IDLE -> Text(s.plotStartHint, textAlign = TextAlign.Center)
                            state.plotChannels.isNotEmpty() -> {
                                Text(s.plotNoChannelSelected, textAlign = TextAlign.Center)
                                TextButton(onClick = onShowChannels, modifier = Modifier.testTag("plotter_show_channels")) {
                                    Text(s.plotShowChannels)
                                }
                            }
                            else -> {
                                Text(
                                    text = profile.builtInExample ?: s.plotEmptySubtitle,
                                    textAlign = TextAlign.Center,
                                    color = c.inkMuted,
                                )
                                TextButton(onClick = onGoToTerminal, modifier = Modifier.testTag("plotter_goto_terminal_button")) {
                                    Text(s.plotBackToTerminalBtn)
                                }
                            }
                        }
                    }
                }
            }

            if (status != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.testTag("plotter_status"),
                        color = if (live) c.rxEmerald else c.inkMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (channels.isNotEmpty()) {
                        val duration = timeEnd - timeStart
                        Text(
                            text = s.plotTimeSpan(duration, formatDecimal(duration / 1000.0, 1)),
                            modifier = Modifier.testTag("plotter_time_span"),
                            color = c.scopeAxis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotChannelSummary(channels: List<PlotChannel>, onShowChannels: () -> Unit) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    Row(
        modifier = Modifier.fillMaxWidth().testTag("plotter_channel_summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.asSequence().filter { it.visible }.take(2).forEach { channel ->
                Column(Modifier.weight(1f)) {
                    PlotChannelName(channel)
                    Text(
                        text = channel.reading(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TextButton(onClick = onShowChannels, modifier = Modifier.testTag("plotter_channels_button")) {
            Text("${s.plotChannelsSection} (${channels.size})", maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlotChannelSheet(channels: List<PlotChannel>, onToggleChannel: (Int) -> Unit, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("plotter_channels_sheet")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.plotChannelsSection, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("plotter_close_channels")) {
                    Icon(Icons.Rounded.Close, contentDescription = s.commonClose)
                }
            }
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                channels.forEachIndexed { index, channel ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("plot_channel_card_${channel.index}"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            PlotChannelName(channel)
                            Text(channel.reading(), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${s.plotMinimum} ${formatPlotValue(channel.minValue, 1)} · ${s.plotMaximum} ${formatPlotValue(channel.maxValue, 1)}",
                                color = c.inkMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = channel.visible,
                            onCheckedChange = { onToggleChannel(channel.index) },
                            modifier = Modifier
                                .testTag("plotter_channel_toggle_${channel.index}")
                                .semantics { contentDescription = channel.name },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotChannelName(channel: PlotChannel) {
    val c = MaterialTheme.instrumentColors
    val palette = c.scopeChannelColors
    val color = palette.getOrElse(channel.index % palette.size) { channel.color }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            channel.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = c.ink,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun PlotChannel.reading(): String = "${formatPlotValue(latestValue, 2)} $unit".trimEnd()

@Composable
private fun OscilloscopeCanvas(
    channels: List<PlotChannel>,
    channelColors: List<Color>,
    timeStart: Long,
    timeEnd: Long,
    yMin: Double,
    yMax: Double,
    majorGridColor: Color,
    minorGridColor: Color,
    pointHaloColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (i in 0..4) {
            val y = h * (i / 4f)
            drawLine(
                color = if (i == 0 || i == 2 || i == 4) majorGridColor else minorGridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (i == 0 || i == 4) null else dashEffect,
            )
        }

        for (i in 0..5) {
            val x = w * (i / 5f)
            drawLine(
                color = minorGridColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )
        }

        val scale = maxOf(1.0, abs(yMin), abs(yMax))
        val bottom = yMin / scale
        val range = (yMax / scale - bottom).coerceAtLeast(Double.MIN_VALUE)
        val duration = (timeEnd - timeStart).coerceAtLeast(1)

        channels.forEach { ch ->
            val traceColor = channelColors.getOrElse(ch.index % channelColors.size) { ch.color }
            val path = Path()
            var lastPoint = Offset.Zero

            ch.samples.forEachIndexed { idx, sample ->
                val x = ((sample.timeMs - timeStart).toDouble() / duration * w).toFloat()
                val normY = ((sample.value / scale - bottom) / range).coerceIn(0.0, 1.0).toFloat()
                val y = h - normY * h
                if (idx == 0 || sample.gapBefore) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                lastPoint = Offset(x, y)
            }

            drawPath(
                path = path,
                color = traceColor,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            drawCircle(
                color = pointHaloColor,
                radius = 4.5.dp.toPx(),
                center = lastPoint,
            )
            drawCircle(
                color = traceColor,
                radius = 3.dp.toPx(),
                center = lastPoint,
            )
        }
    }
}

private fun formatPlotValue(value: Double, decimals: Int, scientificThreshold: Double = 1e12): String {
    val magnitude = abs(value)
    if (magnitude == 0.0 || magnitude in 10.0.pow(-decimals)..<scientificThreshold) {
        return formatDecimal(value, decimals)
    }
    if (magnitude < 1e-300) return value.toString()
    val exponent = kotlin.math.floor(kotlin.math.log10(magnitude)).toInt().coerceAtMost(308)
    return "${formatDecimal(value / 10.0.pow(exponent.toDouble()), decimals)}e$exponent"
}
