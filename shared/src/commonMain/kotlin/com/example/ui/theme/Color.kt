package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Non-composable default brand/channel colors (used by data models & PlotParser)
val CobaltPrimary = Color(0xFF0052CC)
val RxEmerald = Color(0xFF047857)
val TxAmber = Color(0xFFB45309)

val ScopeChannelColors = listOf(
    Color(0xFF0052CC), // CH1 Cobalt
    Color(0xFFDC2626), // CH2 Crimson
    Color(0xFF059669), // CH3 Emerald
    Color(0xFFEA580C), // CH4 Signal Orange
    Color(0xFF7C3AED), // CH5 Violet
)

@Immutable
data class InstrumentPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val cobalt: Color,
    val cobaltPressed: Color,
    val cobaltContainer: Color,
    val cobaltOnContainer: Color,
    val rxEmerald: Color,
    val rxEmeraldBg: Color,
    val rxEmeraldBorder: Color,
    val txAmber: Color,
    val txAmberBg: Color,
    val txAmberBorder: Color,
    val alertCrimson: Color,
    val alertCrimsonBg: Color,
    val alertCrimsonBorder: Color,
    val sysSlate: Color,
    val sysSlateBg: Color,
    val highlightYellowBg: Color,
    val highlightYellowInk: Color,
    val scopeGrid: Color,
    val scopeAxis: Color,
    val scopeChannelColors: List<Color>,
)

// High-Luminance Field-Instrument Color Palette (Sunlight-Readable)
val LightInstrumentPalette = InstrumentPalette(
    isDark = false,
    background = Color(0xFFF3F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFE9EFF6),
    surfaceElevated = Color(0xFFF8FAFC),
    border = Color(0xFFCBD5E1),
    borderStrong = Color(0xFF94A3B8),
    ink = Color(0xFF0F172A),
    inkSecondary = Color(0xFF334155),
    inkMuted = Color(0xFF64748B),
    cobalt = Color(0xFF0052CC),
    cobaltPressed = Color(0xFF003D99),
    cobaltContainer = Color(0xFFDBEAFE),
    cobaltOnContainer = Color(0xFF002B6B),
    rxEmerald = Color(0xFF047857),
    rxEmeraldBg = Color(0xFFECFDF5),
    rxEmeraldBorder = Color(0xFFA7F3D0),
    txAmber = Color(0xFFB45309),
    txAmberBg = Color(0xFFFFFBEB),
    txAmberBorder = Color(0xFFFDE68A),
    alertCrimson = Color(0xFFDC2626),
    alertCrimsonBg = Color(0xFFFEF2F2),
    alertCrimsonBorder = Color(0xFFFECACA),
    sysSlate = Color(0xFF475569),
    sysSlateBg = Color(0xFFF1F5F9),
    highlightYellowBg = Color(0xFFFEF08A),
    highlightYellowInk = Color(0xFF713F12),
    scopeGrid = Color(0xFFCBD5E1),
    scopeAxis = Color(0xFF64748B),
    scopeChannelColors = ScopeChannelColors,
)

// Low-Glare Lab Bench Color Palette (Dark Mode)
val DarkInstrumentPalette = InstrumentPalette(
    isDark = true,
    background = Color(0xFF0B111B),
    surface = Color(0xFF131C2B),
    surfaceSubtle = Color(0xFF192436),
    surfaceElevated = Color(0xFF1B273A),
    border = Color(0xFF27364E),
    borderStrong = Color(0xFF3E5272),
    ink = Color(0xFFE6EDF7),
    inkSecondary = Color(0xFFC1CCE0),
    inkMuted = Color(0xFF8B9BB5),
    cobalt = Color(0xFF5BA3FF),
    cobaltPressed = Color(0xFF3B82F6),
    cobaltContainer = Color(0xFF173257),
    cobaltOnContainer = Color(0xFFBFDBFE),
    rxEmerald = Color(0xFF34D399),
    rxEmeraldBg = Color(0xFF0D2B23),
    rxEmeraldBorder = Color(0xFF145341),
    txAmber = Color(0xFFFBBF24),
    txAmberBg = Color(0xFF2D200B),
    txAmberBorder = Color(0xFF593E12),
    alertCrimson = Color(0xFFF87171),
    alertCrimsonBg = Color(0xFF34161A),
    alertCrimsonBorder = Color(0xFF68262D),
    sysSlate = Color(0xFF94A3B8),
    sysSlateBg = Color(0xFF182233),
    highlightYellowBg = Color(0xFF423007),
    highlightYellowInk = Color(0xFFFDE047),
    scopeGrid = Color(0xFF223047),
    scopeAxis = Color(0xFF8B9BB5),
    scopeChannelColors = listOf(
        Color(0xFF60A5FA), // CH1 Luminous Cobalt
        Color(0xFFF87171), // CH2 Luminous Coral
        Color(0xFF34D399), // CH3 Luminous Mint
        Color(0xFFFB923C), // CH4 Luminous Orange
        Color(0xFFA78BFA), // CH5 Luminous Violet
    ),
)
