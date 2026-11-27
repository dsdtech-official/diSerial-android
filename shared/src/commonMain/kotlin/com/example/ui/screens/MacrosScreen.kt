package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.protocol.ProtocolEngine
import com.example.platform.editorDialogProperties
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.model.LineEnding
import com.example.ui.model.MacroColorTag
import com.example.ui.model.MacroCommandItem
import com.example.ui.model.MacroGroupFilter
import com.example.ui.model.TxFormat
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTerminalStyle
import com.example.ui.theme.instrumentColors

@Composable
private fun MacroColorTag.themedAccent(): Color {
    val c = MaterialTheme.instrumentColors
    return when (this) {
        MacroColorTag.COBALT -> c.cobalt
        MacroColorTag.EMERALD -> c.rxEmerald
        MacroColorTag.AMBER -> c.txAmber
        MacroColorTag.VIOLET -> if (c.isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MacroPanelBottomSheet(
    macroCount: Int,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .navigationBarsPadding()
                .testTag("macro_panel_sheet"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.termMacroPanelCount(macroCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_macro_panel_button")) {
                    Icon(Icons.Rounded.Close, contentDescription = s.macroCollapseDesc)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun EmbeddedMacroPanel(
    macros: List<MacroCommandItem>,
    macroGroups: List<MacroGroupFilter>,
    selectedGroup: MacroGroupFilter,
    onSelectGroup: (MacroGroupFilter) -> Unit,
    onFireMacro: (MacroCommandItem) -> Unit,
    onOpenEditor: (MacroCommandItem?) -> Unit,
    onImportMacros: () -> Unit,
    onExportMacros: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = MaterialTheme.instrumentColors
    val visibleMacros = remember(macros, selectedGroup) {
        macros.filter { selectedGroup.matches(it.group) }
    }

    Surface(
        color = c.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalDivider(color = c.border)

            if (compact) {
                CompactMacroToolbar(
                    groups = macroGroups,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onAdd = { onOpenEditor(null) },
                    onImport = onImportMacros,
                    onExport = onExportMacros,
                )
            } else {
                WideMacroToolbar(
                    groups = macroGroups,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onAdd = { onOpenEditor(null) },
                    onImport = onImportMacros,
                    onExport = onExportMacros,
                )
            }

            // 2-Column Tactile Macro Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("macro_grid"),
            ) {
                items(visibleMacros, key = { it.id }) { macro ->
                    MacroPadCard(
                        macro = macro,
                        compact = compact,
                        onFire = { onFireMacro(macro) },
                        onEdit = { onOpenEditor(macro) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMacroToolbar(
    groups: List<MacroGroupFilter>,
    selectedGroup: MacroGroupFilter,
    onSelectGroup: (MacroGroupFilter) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    val s = LocalAppStrings.current
    var showGroups by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { showGroups = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = s.macroGroupPickerDesc }
                    .testTag("macro_group_dropdown"),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Text(
                    text = selectedGroup.label(s),
                    modifier = Modifier.weight(1f),
                    maxLines = if (selectedGroup is MacroGroupFilter.Named) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showGroups, onDismissRequest = { showGroups = false }) {
                groups.forEach { group ->
                    val isSelected = group == selectedGroup
                    DropdownMenuItem(
                        text = { Text(group.label(s)) },
                        onClick = {
                            onSelectGroup(group)
                            showGroups = false
                        },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                        modifier = Modifier
                            .semantics { selected = isSelected }
                            .testTag(group.testTag()),
                    )
                }
            }
        }

        NewMacroButton(onAdd, Modifier.defaultMinSize(minHeight = 48.dp))

        Box {
            TextButton(
                onClick = { showMore = true },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macro_more_button"),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(s.macroMoreButton, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                DropdownMenuItem(
                    text = { Text(s.macroImportJsonDesc) },
                    onClick = {
                        showMore = false
                        onImport()
                    },
                    modifier = Modifier.testTag("import_macros_button"),
                )
                DropdownMenuItem(
                    text = { Text(s.macroExportAllLabel) },
                    onClick = {
                        showMore = false
                        onExport()
                    },
                    modifier = Modifier.testTag("export_macros_button"),
                )
            }
        }
    }
}

@Composable
private fun WideMacroToolbar(
    groups: List<MacroGroupFilter>,
    selectedGroup: MacroGroupFilter,
    onSelectGroup: (MacroGroupFilter) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        ) {
            groups.forEach { group ->
                FilterChip(
                    selected = selectedGroup == group,
                    onClick = { onSelectGroup(group) },
                    label = { Text(group.label(s, all = s.macroGroupAll), fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.cobalt,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.testTag(group.testTag()),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = onImport,
                modifier = Modifier.size(38.dp).testTag("import_macros_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileOpen,
                    contentDescription = s.macroImportJsonDesc,
                    tint = c.cobalt,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = onExport,
                modifier = Modifier.size(38.dp).testTag("export_macros_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SaveAlt,
                    contentDescription = s.macroExportJsonDesc,
                    tint = c.cobalt,
                    modifier = Modifier.size(18.dp),
                )
            }
            NewMacroButton(onAdd, Modifier.defaultMinSize(minHeight = 40.dp))
        }
    }
}

@Composable
private fun NewMacroButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp),
        modifier = modifier.testTag("add_macro_button"),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = s.macroAddDesc, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(s.macroAddBtn, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MacroPadCard(
    macro: MacroCommandItem,
    compact: Boolean,
    onFire: () -> Unit,
    onEdit: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val accentColor = macro.colorTag.themedAccent()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.surfaceElevated,
        border = BorderStroke(1.dp, c.border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onFire)
            .testTag("macro_card_${macro.id}"),
    ) {
        val rowHeight = if (compact) Modifier.height(IntrinsicSize.Min) else Modifier
        val stripeHeight = if (compact) Modifier.fillMaxHeight() else Modifier.height(92.dp)
        val contentHeight = if (compact) Modifier.heightIn(min = 64.dp) else Modifier
        Row(modifier = Modifier.fillMaxWidth().then(rowHeight)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .then(stripeHeight)
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(contentHeight)
                    .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = macro.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("edit_macro_${macro.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = s.macroEditDesc(macro.title),
                            tint = c.inkMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (compact) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MacroTagBadge(
                            text = macro.txFormat.label,
                            bg = c.cobaltContainer,
                            fg = c.cobaltOnContainer,
                        )
                        MacroPayloadPreview(macro.payload, Modifier.weight(1f))
                        MacroSendIcon(macro.title, accentColor)
                    }
                } else {
                    MacroPayloadPreview(macro.payload)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MacroTagBadge(
                            text = macro.txFormat.label,
                            bg = c.cobaltContainer,
                            fg = c.cobaltOnContainer,
                        )
                        MacroSendIcon(macro.title, accentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPayloadPreview(payload: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.instrumentColors
    Text(
        text = payload,
        style = MonoTerminalStyle.copy(fontSize = 11.sp, color = c.inkMuted),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun MacroSendIcon(title: String, color: Color) {
    val s = LocalAppStrings.current
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.Send,
        contentDescription = s.macroSendDesc(title),
        tint = color,
        modifier = Modifier.size(14.dp),
    )
}

@Composable
private fun MacroTagBadge(text: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            style = MonoBadgeStyle.copy(color = fg, fontSize = 9.5.sp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

private fun MacroGroupFilter.label(s: AppStrings, all: String = s.macroAllGroups): String = when (this) {
    MacroGroupFilter.All -> all
    MacroGroupFilter.Ungrouped -> s.macroUngrouped
    is MacroGroupFilter.Named -> namedGroupLabel(name, s)
}

private fun MacroGroupFilter.testTag(): String = when (this) {
    MacroGroupFilter.All -> "macro_group_all"
    MacroGroupFilter.Ungrouped -> "macro_group_ungrouped"
    is MacroGroupFilter.Named -> "macro_group_named_$name"
}

private fun namedGroupLabel(name: String, s: AppStrings): String =
    if (name == s.macroAllGroups || name == s.macroGroupAll || name == s.macroUngrouped || name == s.macroCreateGroup) {
        s.macroNamedGroupLabel(name)
    } else {
        name
    }

@Composable
fun MacroEditorDialog(
    initial: MacroCommandItem?,
    defaultGroup: String,
    namedGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, TxFormat, LineEnding, MacroColorTag) -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var group by remember(initial) { mutableStateOf(initial?.group?.trim() ?: defaultGroup) }
    var newGroupName by remember(initial) { mutableStateOf<String?>(null) }
    var payload by remember(initial) { mutableStateOf(initial?.payload ?: "") }
    var txFormat by remember(initial) { mutableStateOf(initial?.txFormat ?: TxFormat.HEX) }
    var lineEnding by remember(initial) { mutableStateOf(initial?.lineEnding ?: LineEnding.NONE) }
    var colorTag by remember(initial) { mutableStateOf(initial?.colorTag ?: MacroColorTag.COBALT) }
    var payloadError by remember(initial) { mutableStateOf<String?>(null) }

    val isExisting = initial != null && initial.id != 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        properties = editorDialogProperties,
        containerColor = c.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isExisting) s.macroEditorEditTitle else s.macroEditorNewTitle,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                if (isExisting) {
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_macro_button")) {
                        Icon(Icons.Rounded.Delete, contentDescription = s.macroEditorDeleteDesc, tint = c.alertCrimson)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).testTag("macro_editor_form"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.macroEditorNameLabel) },
                    placeholder = { Text(s.macroEditorNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("macro_title_input"),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        s.macroEditorFormatLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkMuted,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    TxFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = txFormat == fmt,
                            onClick = {
                                txFormat = fmt
                                payloadError = null
                            },
                            label = { Text(fmt.label) },
                            modifier = Modifier.testTag("macro_format_${fmt.name.lowercase()}"),
                        )
                    }
                }
                OutlinedTextField(
                    value = payload,
                    onValueChange = {
                        payload = it
                        payloadError = null
                    },
                    isError = payloadError != null,
                    supportingText = payloadError?.let { message -> { Text(message) } },
                    label = { Text(s.macroEditorPayloadLabel) },
                    textStyle = MonoTerminalStyle,
                    placeholder = { Text(s.macroEditorPayloadHint) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("macro_payload_input"),
                )

                if (txFormat == TxFormat.ASCII) {
                    MacroEditorPicker(
                        label = s.macroEditorLineEndingLabel,
                        value = lineEnding,
                        options = LineEnding.entries,
                        optionLabel = { it.localizedLabel(s) },
                        onSelect = { lineEnding = it },
                        tag = "macro_line_ending",
                        optionKey = { it.name.lowercase() },
                    )
                }

                val draftGroup = newGroupName
                if (draftGroup == null) {
                    MacroEditorPicker<String?>(
                        label = s.macroEditorGroupLabel,
                        value = group,
                        options = listOf("") + (namedGroups + group).filter { it.isNotEmpty() }.distinct() + listOf(null),
                        optionLabel = { option ->
                            when (option) {
                                null -> s.macroCreateGroup
                                "" -> s.macroUngrouped
                                else -> namedGroupLabel(option, s)
                            }
                        },
                        onSelect = { chosen ->
                            if (chosen == null) newGroupName = "" else group = chosen
                        },
                        tag = "macro_editor_group",
                        optionKey = { option ->
                            when (option) {
                                null -> "new"
                                "" -> "ungrouped"
                                else -> "named_$option"
                            }
                        },
                    )
                } else {
                    OutlinedTextField(
                        value = draftGroup,
                        onValueChange = { newGroupName = it },
                        label = { Text(s.macroNewGroupName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("macro_new_group_input"),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { newGroupName = null },
                            modifier = Modifier.testTag("macro_cancel_new_group"),
                        ) { Text(s.commonCancel) }
                        TextButton(
                            onClick = {
                                group = draftGroup.trim()
                                newGroupName = null
                            },
                            enabled = draftGroup.isNotBlank(),
                            modifier = Modifier.testTag("macro_use_new_group"),
                        ) { Text(s.commonConfirm) }
                    }
                }
                MacroEditorPicker(
                    label = s.macroEditorColorTagLabel,
                    value = colorTag,
                    options = MacroColorTag.entries,
                    optionLabel = { it.localizedLabel(s) },
                    onSelect = { colorTag = it },
                    tag = "macro_color",
                    optionKey = { it.name.lowercase() },
                    leadingIcon = { tag ->
                        Box(Modifier.size(10.dp).clip(CircleShape).background(tag.themedAccent()))
                    },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = newGroupName == null,
                onClick = {
                    payloadError = when {
                        payload.isBlank() -> s.vmNoticeMacroPayloadEmpty
                        txFormat == TxFormat.HEX && ProtocolEngine.parseHexString(payload)?.isNotEmpty() != true ->
                            s.vmNoticeMacroFormatInvalid
                        else -> null
                    }
                    if (payloadError == null) onSave(title, group, payload, txFormat, lineEnding, colorTag)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.cobalt,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag("save_macro_dialog_button"),
            ) {
                Text(s.macroEditorSaveBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_macro_dialog_button")) {
                Text(s.commonCancel)
            }
        },
    )
}

@Composable
private fun <T> MacroEditorPicker(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
    optionKey: (T) -> String,
    leadingIcon: (@Composable (T) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val c = MaterialTheme.instrumentColors
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            modifier = Modifier.fillMaxWidth().testTag("${tag}_picker"),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.invoke(value)
                    Text(optionLabel(value), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val isSelected = option == value
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = leadingIcon?.let { icon -> { icon(option) } },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null,
                    modifier = Modifier
                        .semantics { selected = isSelected }
                        .testTag("${tag}_${optionKey(option)}"),
                )
            }
        }
    }
}
