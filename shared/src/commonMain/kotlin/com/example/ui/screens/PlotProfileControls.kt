package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.protocol.BUILTIN_PLOT_COLUMNS
import com.example.domain.protocol.BUILTIN_PLOT_KEYS
import com.example.domain.protocol.MAX_PLOT_SAMPLE_TEXT
import com.example.domain.protocol.PlotBoundary
import com.example.domain.protocol.PlotByteOrder
import com.example.domain.protocol.PlotConfiguration
import com.example.domain.protocol.PlotDelimiter
import com.example.domain.protocol.PlotField
import com.example.domain.protocol.PlotFormat
import com.example.domain.protocol.PlotNumberType
import com.example.domain.protocol.PlotProfile
import com.example.domain.protocol.previewPlot
import com.example.platform.editorDialogProperties
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.PlotSamplingState
import com.example.ui.state.SerialUiState
import kotlinx.coroutines.launch

private fun PlotProfile.label(strings: AppStrings): String = when (id) {
    BUILTIN_PLOT_COLUMNS -> strings.plotProfileColumns
    BUILTIN_PLOT_KEYS -> strings.plotProfileKeys
    else -> configuration.name
}

internal val PlotProfile.builtInExample: String?
    get() = when (id) {
        BUILTIN_PLOT_COLUMNS -> "23.5, 48.2"
        BUILTIN_PLOT_KEYS -> "T:29.5, H:61.0"
        else -> null
    }

@Composable
internal fun PlotProfileBar(
    state: SerialUiState,
    onSelect: (Long) -> Unit,
    onSave: suspend (Long?, PlotConfiguration) -> Boolean,
    onDelete: (Long) -> Unit,
    onToggleSampling: () -> Unit,
    onToggleAutoScale: () -> Unit,
    onAdjustWindow: (Int) -> Unit,
    onClearPlot: () -> Unit,
) {
    val s = LocalAppStrings.current
    val active = state.activePlotProfile
    val hasWaveform = state.plotChannels.isNotEmpty()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlotProfile?>(null) }
    var deleting by remember { mutableStateOf<PlotProfile?>(null) }
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().testTag("plotter_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlotChoice<PlotProfile?>(
            label = null,
            current = active,
            options = state.plotProfiles,
            optionLabel = { it?.label(s) ?: s.plotChooseProfile },
            onSelect = { it?.let { profile -> onSelect(profile.id) } },
            modifier = Modifier.weight(1f),
            tag = "plot_profile_picker",
            compact = true,
            menuHint = s.plotSwitchProfileHint.takeIf { hasWaveform },
        )
        Button(
            onClick = onToggleSampling,
            enabled = active != null,
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = Modifier.testTag("plotter_pause_button"),
        ) {
            Text(
                when (state.plotSampling) {
                    PlotSamplingState.IDLE -> s.plotStartBtn
                    PlotSamplingState.RUNNING -> s.plotPauseBtn
                    PlotSamplingState.PAUSED -> s.plotResumeBtn
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
        Box {
            IconButton(onClick = { showActions = true }, modifier = Modifier.testTag("plotter_more")) {
                Icon(Icons.Rounded.MoreVert, contentDescription = s.plotMore)
            }
            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                DropdownMenuItem(
                    text = { Text(s.plotProfileNew) },
                    onClick = { showActions = false; editing = null; showEditor = true },
                    modifier = Modifier.testTag("plot_profile_new"),
                )
                if (active != null && !active.isBuiltIn) {
                    DropdownMenuItem(
                        text = { Text(s.plotProfileEdit) },
                        onClick = { showActions = false; editing = active; showEditor = true },
                    )
                    DropdownMenuItem(
                        text = { Text(s.plotProfileDelete) },
                        onClick = { showActions = false; deleting = active },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(s.plotAutoY)
                            if (!state.plotterAutoScale) {
                                Text(s.plotFixedYRange, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    trailingIcon = { Checkbox(checked = state.plotterAutoScale, onCheckedChange = null) },
                    onClick = onToggleAutoScale,
                    modifier = Modifier.testTag("plotter_autoscale_chip"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(s.plotWindowLabel, style = MaterialTheme.typography.bodyMedium)
                    IconButton(
                        onClick = { onAdjustWindow(-5) },
                        enabled = state.plotterWindowPoints > 10,
                        modifier = Modifier.testTag("plotter_zoom_in"),
                    ) { Icon(Icons.Rounded.Remove, contentDescription = s.plotZoomInDesc) }
                    Text(s.plotWindowPoints(state.plotterWindowPoints), modifier = Modifier.testTag("plotter_window_count"))
                    IconButton(
                        onClick = { onAdjustWindow(5) },
                        enabled = state.plotterWindowPoints < 120,
                        modifier = Modifier.testTag("plotter_zoom_out"),
                    ) { Icon(Icons.Rounded.Add, contentDescription = s.plotZoomOutDesc) }
                }
                DropdownMenuItem(
                    text = { Text(s.plotClearDesc) },
                    onClick = { showActions = false; onClearPlot() },
                    enabled = hasWaveform,
                    modifier = Modifier.testTag("plotter_clear_button"),
                )
            }
        }
    }

    if (showEditor) {
        PlotProfileEditor(editing, state.plotProfiles, onSave) { showEditor = false }
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(s.plotProfileDeleteConfirm) },
            text = { Text(profile.label(s)) },
            confirmButton = {
                TextButton(onClick = { onDelete(profile.id); deleting = null }) { Text(s.plotProfileDelete) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.commonCancel) } },
        )
    }
}

@Composable
private fun PlotProfileEditor(
    initial: PlotProfile?,
    profiles: List<PlotProfile>,
    onSave: suspend (Long?, PlotConfiguration) -> Boolean,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var draft by remember(initial) { mutableStateOf(PlotProfileDraft(initial?.configuration ?: PlotConfiguration())) }
    var currentField by remember(initial) { mutableStateOf(0) }
    var sample by remember(initial) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val rules = draft.rules
    val configuration = remember(draft) { draft.toConfiguration()?.takeIf { it.isValid() } }
    val hasDuplicateName = profiles.any {
        !it.isBuiltIn && it.id != initial?.id && it.configuration.name.equals(rules.name.trim(), ignoreCase = true)
    }
    val preview = remember(configuration, sample) {
        if (sample.isBlank()) null else configuration?.let { previewPlot(it, sample) }
    }
    val invalidSample = configuration != null && sample.isNotBlank() && preview == null
    val canSave = configuration != null && !hasDuplicateName && !invalidSample
    val guidance = when {
        saveFailed -> s.plotProfileSaveFailed
        hasDuplicateName -> s.plotProfileDuplicateName
        rules.name.isBlank() -> s.plotProfileNameRequired
        draft.fields.any { it.field.name.isBlank() } -> s.plotProfileFieldNameRequired
        configuration == null -> s.plotProfileInvalid
        invalidSample -> s.plotProfileSampleInvalid
        else -> null
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = editorDialogProperties,
    ) {
        Surface(Modifier.fillMaxSize().testTag("plot_profile_editor")) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                    Text(
                        if (initial == null) s.plotProfileNew else s.plotProfileEdit,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("plot_profile_form"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlotChoice(
                            s.plotProfileFormat, rules.format, PlotFormat.entries, { it.label(s) },
                            onSelect = {
                                if (it != rules.format) {
                                    draft = draft.copy(rules = rules.copy(format = it))
                                    sample = ""
                                }
                            },
                            tag = "plot_profile_format",
                        )
                        PlotInput(s.plotProfileName, rules.name, isError = hasDuplicateName) {
                            draft = draft.copy(rules = rules.copy(name = it.take(80)))
                            saveFailed = false
                        }
                        if (rules.format == PlotFormat.BINARY) {
                            Text(s.plotProfileBinaryHint, style = MaterialTheme.typography.bodySmall)
                            PlotInput(s.plotProfileHeader, rules.header) { draft = draft.copy(rules = rules.copy(header = it)) }
                            PlotInput(s.plotProfileWidth, draft.length, keyboardType = KeyboardType.Number) {
                                draft = draft.copy(length = it.take(6))
                            }
                            PlotInput(s.plotProfileTrailer, rules.trailer) { draft = draft.copy(rules = rules.copy(trailer = it)) }
                        } else {
                            PlotTextRules(rules) { draft = draft.copy(rules = it) }
                        }

                        HorizontalDivider()
                        Text(s.plotProfileFields, style = MaterialTheme.typography.titleMedium)
                        if (draft.fields.size > 1) {
                            PlotChoice(
                                label = null,
                                current = currentField,
                                options = draft.fields.indices.toList(),
                                optionLabel = { index ->
                                    "${index + 1}. ${draft.fields[index].field.name.ifBlank { s.plotProfileFieldName }}"
                                },
                                onSelect = { currentField = it },
                                tag = "plot_profile_field_picker",
                            )
                        }
                        key(currentField) {
                            PlotFieldEditor(draft.fields[currentField], rules.format) { update ->
                                draft = draft.copy(fields = draft.fields.mapIndexed { index, field ->
                                    if (index == currentField) update else field
                                })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (draft.fields.size < 4) {
                                TextButton(
                                    onClick = {
                                        draft = draft.copy(fields = draft.fields + PlotFieldDraft(PlotField("")))
                                        currentField = draft.fields.lastIndex
                                    },
                                    modifier = Modifier.weight(1f).testTag("plot_profile_add_field"),
                                ) { Text(s.plotProfileAddField) }
                            }
                            if (draft.fields.size > 1) {
                                TextButton(
                                    onClick = {
                                        val remaining = draft.fields.filterIndexed { index, _ -> index != currentField }
                                        currentField = currentField.coerceAtMost(remaining.lastIndex)
                                        draft = draft.copy(fields = remaining)
                                    },
                                    modifier = Modifier.weight(1f).testTag("plot_profile_remove_field"),
                                ) { Text(s.plotProfileRemoveField) }
                            }
                        }

                        HorizontalDivider()
                        Text(s.plotProfilePreview, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = sample,
                            onValueChange = { sample = it.take(MAX_PLOT_SAMPLE_TEXT) },
                            label = { Text(if (rules.format == PlotFormat.BINARY) s.plotProfileSampleHex else s.plotProfileSampleText) },
                            placeholder = { Text(rules.samplePlaceholder()) },
                            trailingIcon = if (sample.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { sample = "" },
                                        modifier = Modifier.testTag("plot_profile_clear_sample"),
                                    ) {
                                        Icon(Icons.Rounded.Clear, contentDescription = s.plotProfileClearSample)
                                    }
                                }
                            } else null,
                            maxLines = 4,
                            isError = invalidSample,
                            modifier = Modifier.fillMaxWidth().testTag("plot_profile_sample"),
                        )
                        if (preview != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    preview.values.joinToString("\n") { "${it.name} = ${it.value} ${it.unit}".trimEnd() },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        if (guidance != null) {
                            Text(
                                guidance,
                                modifier = Modifier.testTag("plot_profile_guidance"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (saveFailed || hasDuplicateName || invalidSample) {
                                    MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            TextButton(enabled = !saving, onClick = onDismiss) { Text(s.commonCancel) }
                            Button(
                                enabled = canSave && !saving,
                                modifier = Modifier.testTag("plot_profile_save"),
                                onClick = {
                                    val valid = configuration ?: return@Button
                                    saving = true
                                    saveFailed = false
                                    scope.launch {
                                        val saved = onSave(initial?.id, valid)
                                        saving = false
                                        if (saved) onDismiss() else saveFailed = true
                                    }
                                },
                            ) { Text(s.plotProfileSave) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotTextRules(rules: PlotConfiguration, onChange: (PlotConfiguration) -> Unit) {
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    val boundaryLabel: (PlotBoundary) -> String = {
        if (it == PlotBoundary.LINE) s.plotBoundaryLine else s.plotBoundaryFrame
    }
    val summary = listOfNotNull(
        boundaryLabel(rules.boundary),
        rules.delimiter.label(s),
        rules.keySeparator.takeIf { rules.format == PlotFormat.KEY_VALUE },
    ).joinToString(" · ")
    PlotDisclosure(s.plotProfileTextRules, summary, expanded, "plot_profile_text_rules") { expanded = !expanded }
    if (expanded) {
        PlotChoice(s.plotProfileBoundary, rules.boundary, PlotBoundary.entries, boundaryLabel, {
            onChange(rules.copy(boundary = it))
        }, tag = "plot_profile_boundary")
        PlotChoice(s.plotProfileDelimiter, rules.delimiter, PlotDelimiter.entries, { it.label(s) }, {
            onChange(rules.copy(delimiter = it))
        }, tag = "plot_profile_delimiter")
        if (rules.format == PlotFormat.KEY_VALUE) {
            PlotChoice(s.plotProfileKeySeparator, rules.keySeparator, listOf("=", ":"), { it }, {
                onChange(rules.copy(keySeparator = it))
            }, tag = "plot_profile_key_separator")
        }
    }
}

@Composable
private fun PlotFieldEditor(draft: PlotFieldDraft, format: PlotFormat, onChange: (PlotFieldDraft) -> Unit) {
    val s = LocalAppStrings.current
    var advanced by remember { mutableStateOf(false) }
    val field = draft.field
    PlotInput(s.plotProfileFieldName, field.name) { onChange(draft.copy(field = field.copy(name = it.take(60)))) }
    when (format) {
        PlotFormat.COLUMNS -> PlotInput(s.plotProfileColumn, draft.column, keyboardType = KeyboardType.Number) {
            onChange(draft.copy(column = it.take(6)))
        }
        PlotFormat.KEY_VALUE -> PlotInput(s.plotProfileKey, field.key) { onChange(draft.copy(field = field.copy(key = it))) }
        PlotFormat.BINARY -> {
            PlotInput(s.plotProfileOffset, draft.offset, keyboardType = KeyboardType.Number) {
                onChange(draft.copy(offset = it.take(6)))
            }
            PlotChoice(s.plotProfileNumberType, field.numberType, PlotNumberType.entries, { it.name }, {
                onChange(draft.copy(field = field.copy(numberType = it)))
            })
            if (field.numberType.width > 1) {
                PlotChoice(s.plotProfileByteOrder, field.byteOrder, PlotByteOrder.entries, {
                    if (it == PlotByteOrder.LITTLE_ENDIAN) s.plotByteOrderLittle else s.plotByteOrderBig
                }, { onChange(draft.copy(field = field.copy(byteOrder = it))) })
            }
        }
    }
    val advancedSummary = listOfNotNull(
        field.unit.takeIf { it.isNotBlank() },
        draft.scale.takeIf { it.toDoubleOrNull() != 1.0 }?.let { "${s.plotProfileScale} $it" },
        draft.bias.takeIf { it.toDoubleOrNull() != 0.0 }?.let { "${s.plotProfileBias} $it" },
    ).joinToString(" · ").ifBlank { null }
    PlotDisclosure(s.settingsAdvanced, advancedSummary, advanced, "plot_profile_field_advanced") { advanced = !advanced }
    if (advanced) {
        PlotInput(s.plotProfileUnit, field.unit) { onChange(draft.copy(field = field.copy(unit = it.take(24)))) }
        PlotInput(s.plotProfileScale, draft.scale) { onChange(draft.copy(scale = it)) }
        PlotInput(s.plotProfileBias, draft.bias) { onChange(draft.copy(bias = it)) }
    }
}

@Composable
private fun PlotDisclosure(label: String, summary: String?, expanded: Boolean, tag: String, onToggle: () -> Unit) {
    TextButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (!expanded && summary != null) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            if (expanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun PlotInput(
    label: String,
    value: String,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(MAX_PLOT_SAMPLE_TEXT)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

private fun PlotConfiguration.samplePlaceholder(): String = when (format) {
    PlotFormat.COLUMNS -> "23.5${delimiter.character}48.2"
    PlotFormat.KEY_VALUE -> "T${keySeparator}29.5${delimiter.character}H${keySeparator}61.0"
    PlotFormat.BINARY -> "AA 2C 01 55"
}

@Composable
private fun <T> PlotChoice(
    label: String?,
    current: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    tag: String? = null,
    compact: Boolean = false,
    menuHint: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().then(if (tag == null) Modifier else Modifier.testTag(tag)),
            contentPadding = if (compact) PaddingValues(horizontal = 8.dp) else androidx.compose.material3.ButtonDefaults.ContentPadding,
        ) {
            Column(Modifier.weight(1f)) {
                if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    optionLabel(current),
                    style = if (compact) MaterialTheme.typography.labelMedium else LocalTextStyle.current,
                    maxLines = if (compact) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = if (compact) Modifier.size(20.dp) else Modifier,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (menuHint != null) {
                Text(
                    menuHint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider()
            }
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(optionLabel(item), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { expanded = false; onSelect(item) },
                )
            }
        }
    }
}

private data class PlotProfileDraft(
    val rules: PlotConfiguration,
    val length: String = rules.frameLength?.toString().orEmpty(),
    val fields: List<PlotFieldDraft> = rules.fields.ifEmpty { listOf(PlotField("")) }.map(::PlotFieldDraft),
) {
    fun toConfiguration(): PlotConfiguration? {
        val binary = rules.format == PlotFormat.BINARY
        return rules.copy(
            name = rules.name.trim(),
            frameLength = if (binary && length.isNotBlank()) length.trim().toIntOrNull() ?: return null else null,
            fields = fields.map { it.toField(rules.format) ?: return null },
        )
    }
}

private data class PlotFieldDraft(
    val field: PlotField,
    val column: String = field.column.toString(),
    val offset: String = field.offset.toString(),
    val scale: String = field.scale.toString(),
    val bias: String = field.bias.toString(),
) {
    fun toField(format: PlotFormat): PlotField? {
        return field.copy(
            name = field.name.trim(),
            unit = field.unit.trim(),
            key = field.key.trim(),
            column = if (format == PlotFormat.COLUMNS) column.trim().toIntOrNull() ?: return null else field.column,
            offset = if (format == PlotFormat.BINARY) offset.trim().toIntOrNull() ?: return null else field.offset,
            scale = scale.trim().toDoubleOrNull() ?: return null,
            bias = bias.trim().toDoubleOrNull() ?: return null,
        )
    }
}

private fun PlotFormat.label(s: AppStrings): String = when (this) {
    PlotFormat.COLUMNS -> s.plotFormatColumns
    PlotFormat.KEY_VALUE -> s.plotProfileKeys
    PlotFormat.BINARY -> s.plotFormatBinary
}

private fun PlotDelimiter.label(s: AppStrings): String = when (this) {
    PlotDelimiter.COMMA -> s.plotDelimiterComma
    PlotDelimiter.SPACE -> s.plotDelimiterSpace
    PlotDelimiter.TAB -> s.plotDelimiterTab
    PlotDelimiter.SEMICOLON -> s.plotDelimiterSemicolon
}
