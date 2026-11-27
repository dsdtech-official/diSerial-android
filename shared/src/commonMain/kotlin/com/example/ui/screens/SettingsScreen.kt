package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.model.AppLanguage
import com.example.ui.model.AppSettingsState
import com.example.ui.model.AppThemeMode
import com.example.ui.model.CharsetEncoding
import com.example.ui.theme.instrumentColors

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    appVersion: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onOpenTerminalSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    SettingsList(modifier) {
        SettingsChoiceRow(
            title = s.themeSectionTitle,
            selected = settings.themeMode,
            options = AppThemeMode.entries,
            label = { it.localizedLabel(s) },
            onSelect = onThemeModeChange,
            tag = "settings_theme",
        )
        SettingsDivider()
        SettingsChoiceRow(
            title = s.langSectionTitle,
            selected = settings.appLanguage,
            options = AppLanguage.entries,
            label = { it.localizedLabel(s) },
            onSelect = onLanguageChange,
            tag = "settings_language",
        )
        SettingsDivider()
        SettingsToggleRow(s.settingsKeepScreenOnTitle, settings.keepScreenOn, onToggleKeepScreenOn, "switch_keep_screen_on")
        SettingsDivider()
        SettingsLinkRow(s.settingsTerminalSection, onOpenTerminalSettings, "settings_open_terminal")
        SettingsDivider()
        SettingsLinkRow(
            title = s.settingsAboutCardTitle,
            value = appVersion.takeIf { it.isNotBlank() }?.let { "v$it" },
            onClick = onOpenAbout,
            tag = "settings_open_about_card",
        )
    }
}

@Composable
fun TerminalSettingsScreen(
    settings: AppSettingsState,
    onCharsetChange: (CharsetEncoding) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFrameTimeoutChange: (Int) -> Unit,
    onMaxBufferChange: (Int) -> Unit,
    onToggleClearAfterSend: () -> Unit,
    onToggleVibrateOnError: () -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val fontSizes = mapOf(11.0f to s.settingsFontCompact, 12.5f to s.settingsFontNormal, 14.0f to s.settingsFontLarge)

    SettingsList(modifier) {
        SettingsChoiceRow(
            title = s.settingsCharsetLabel,
            selected = settings.charset,
            options = CharsetEncoding.entries,
            label = { it.localizedLabel(s) },
            onSelect = onCharsetChange,
            tag = "settings_charset",
            detail = s.settingsCharsetDetail,
        )
        SettingsDivider()
        SettingsChoiceRow(
            title = s.settingsFontSizeLabel,
            selected = settings.terminalFontSizeSp,
            options = fontSizes.keys.toList(),
            label = { fontSizes[it] ?: "$it sp" },
            onSelect = onFontSizeChange,
            tag = "settings_font_size",
        )
        SettingsDivider()
        SettingsToggleRow(
            s.settingsClearAfterSendTitle, settings.clearInputAfterSend, onToggleClearAfterSend, "switch_clear_after_send",
        )
        SettingsDivider()
        SettingsRow(
            title = s.settingsAdvanced,
            modifier = Modifier.clickable { advancedExpanded = !advancedExpanded }.testTag("settings_terminal_advanced"),
        ) {
            Icon(
                if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.instrumentColors.inkMuted,
            )
        }
        if (advancedExpanded) {
            SettingsDivider()
            SettingsChoiceRow(
                title = s.settingsFrameTimeoutTitle,
                selected = settings.frameTimeoutMs,
                options = listOf(20, 50, 100, 200),
                label = { "$it ms" },
                onSelect = onFrameTimeoutChange,
                tag = "setting_frame_timeout_dropdown",
                detail = s.settingsFrameTimeoutSubtitle,
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = s.settingsMaxBufferTitle,
                selected = settings.maxBufferFrames,
                options = listOf(2000, 5000, 10000),
                label = s.settingsMaxBufferOption,
                onSelect = onMaxBufferChange,
                tag = "setting_max_buffer_dropdown",
                detail = s.settingsMaxBufferSubtitle,
            )
            SettingsDivider()
            SettingsToggleRow(s.settingsVibrateTitle, settings.vibrateOnError, onToggleVibrateOnError, "switch_vibrate_on_error")
            SettingsDivider()
            SettingsLinkRow(s.settingsResetDefaultsBtn, { confirmReset = true }, "reset_settings_button")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.settingsResetDefaultsBtn) },
            text = { Text(s.settingsResetTerminalDetail) },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }, modifier = Modifier.testTag("cancel_terminal_reset")) {
                    Text(s.commonCancel)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onResetDefaults()
                    },
                    modifier = Modifier.testTag("confirm_terminal_reset"),
                ) { Text(s.commonConfirm) }
            },
        )
    }
}

@Composable
private fun SettingsList(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.instrumentColors.surface,
        ) { Column { content() } }
    }
}

@Composable
private fun SettingsRow(title: String, modifier: Modifier, trailing: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.instrumentColors.ink)
        trailing()
    }
}

@Composable
internal fun SettingsLinkRow(title: String, onClick: () -> Unit, tag: String, value: String? = null) {
    val c = MaterialTheme.instrumentColors
    SettingsRow(title, Modifier.clickable(onClick = onClick).testTag(tag)) {
        if (value != null) {
            Text(
                text = value,
                modifier = Modifier.widthIn(max = 120.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkMuted,
                textAlign = TextAlign.End,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.inkMuted)
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, onToggle: () -> Unit, tag: String) {
    SettingsRow(title, Modifier.toggleable(checked, role = Role.Switch) { onToggle() }.testTag(tag)) {
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.instrumentColors.border)
}

@Composable
private fun <T> SettingsChoiceRow(
    title: String,
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
    detail: String? = null,
) {
    val s = LocalAppStrings.current
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    SettingsLinkRow(title, { dialogOpen = true }, tag, label(selected))
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    if (detail != null) Text(detail, modifier = Modifier.padding(bottom = 8.dp))
                    options.forEachIndexed { index, option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = {
                                    dialogOpen = false
                                    onSelect(option)
                                },
                            ).testTag("${tag}_option_$index"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(label(option), modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogOpen = false }) { Text(s.commonCancel) } },
        )
    }
}
