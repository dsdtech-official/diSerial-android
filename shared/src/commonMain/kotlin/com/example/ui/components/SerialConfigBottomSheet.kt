package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.ParityMode
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.UsbDeviceItem
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.instrumentColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialConfigBottomSheet(
    devices: List<UsbDeviceItem>,
    selectedDevice: UsbDeviceItem?,
    connectionStatus: ConnectionStatus,
    promptReason: String?,
    presets: List<SerialPortConfig>,
    currentConfig: SerialPortConfig,
    autoConnectOnUsbAttach: Boolean,
    onToggleAutoConnectUsb: () -> Unit,
    onSelectDevice: (UsbDeviceItem) -> Unit,
    onRefreshDevices: () -> Unit,
    onSavePreset: (String, SerialPortConfig) -> Unit,
    onApplyAndConnect: (SerialPortConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    val isDeviceSelectionLocked = connectionStatus != ConnectionStatus.DISCONNECTED

    val usbBannerMessage = when {
        !promptReason.isNullOrBlank() -> promptReason
        devices.size >= 2 -> s.sheetMultiBanner(devices.size)
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            devices.size >= 2 || promptReason != null -> s.sheetTitleSelectUsb
                            else -> s.sheetTitleConfigUsb
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.ink,
                    )
                    Text(
                        text = s.sheetSupportedChipsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_serial_config_sheet"),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = s.sheetCloseDesc, tint = c.inkMuted)
                }
            }

            UsbChannelLabel()

            AnimatedVisibility(visible = usbBannerMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.cobaltContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, c.cobalt.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Usb,
                            contentDescription = null,
                            tint = c.cobalt,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = usbBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.cobaltOnContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            UsbOtgConfigSection(
                devices = devices,
                selectedDevice = selectedDevice,
                isConnected = isConnected,
                isDeviceSelectionLocked = isDeviceSelectionLocked,
                presets = presets,
                currentConfig = currentConfig,
                onSelectDevice = onSelectDevice,
                onRefreshDevices = onRefreshDevices,
                onSavePreset = onSavePreset,
                onApplyAndConnect = onApplyAndConnect,
            )
            HorizontalDivider(color = c.border)
            UsbControlLine(
                label = s.settingsAutoConnectTitle,
                checked = autoConnectOnUsbAttach,
                onCheckedChange = { onToggleAutoConnectUsb() },
                tag = "switch_auto_connect_usb",
            )
        }
    }
}

@Composable
private fun UsbChannelLabel() {
    val colors = MaterialTheme.instrumentColors
    val contentColor = MaterialTheme.colorScheme.onPrimary
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.cobalt,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("channel_tab_usb"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Usb, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Text(LocalAppStrings.current.channelUsbOtg, color = contentColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun UsbOtgConfigSection(
    devices: List<UsbDeviceItem>,
    selectedDevice: UsbDeviceItem?,
    isConnected: Boolean,
    isDeviceSelectionLocked: Boolean,
    presets: List<SerialPortConfig>,
    currentConfig: SerialPortConfig,
    onSelectDevice: (UsbDeviceItem) -> Unit,
    onRefreshDevices: () -> Unit,
    onSavePreset: (String, SerialPortConfig) -> Unit,
    onApplyAndConnect: (SerialPortConfig) -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var draftConfig by remember(currentConfig) { mutableStateOf(currentConfig) }
    var presetNameInput by remember { mutableStateOf("") }

    val commonBaudRates = listOf(4800, 9600, 19200, 38400, 57600, 74880, 115200, 230400, 460800, 921600, 1000000)
    var showCustomBaudInput by remember(currentConfig) {
        mutableStateOf(currentConfig.baudRate !in commonBaudRates)
    }
    var customBaudText by remember(currentConfig) {
        mutableStateOf(currentConfig.baudRate.toString())
    }
    val customBaudRange = 300..2_000_000
    val parsedCustomBaud = customBaudText.toIntOrNull()
    val isBaudValid = !showCustomBaudInput || (parsedCustomBaud != null && parsedCustomBaud in customBaudRange)
    val selectedPreset = if (isBaudValid) presets.firstOrNull { it == draftConfig } else null

    // 1. Detected USB OTG Devices
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = s.sheetAvailableDevices(devices.size),
            style = MaterialTheme.typography.labelMedium,
            color = c.inkMuted,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onRefreshDevices,
            modifier = Modifier.testTag("refresh_usb_devices_button"),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(s.sheetRefreshScan)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isDeviceSelectionLocked && devices.any { it.id != selectedDevice?.id }) {
            Text(
                text = s.sheetUsbDeviceLockedNotice,
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
                modifier = Modifier.testTag("usb_device_switch_notice"),
            )
        }
        if (devices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = c.surfaceElevated,
                border = BorderStroke(1.dp, c.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = s.sheetNoDevicesTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                    )
                    Text(
                        text = s.sheetNoDevicesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
            }
        }
        devices.forEach { dev ->
            val isSelected = dev.id == selectedDevice?.id
            val displayName = dev.productName ?: dev.chipName
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) c.cobaltContainer.copy(alpha = 0.45f) else c.surfaceElevated,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) c.cobalt else c.border,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isDeviceSelectionLocked) { onSelectDevice(dev) }
                    .testTag("usb_device_${dev.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = null,
                        tint = if (isSelected) c.cobalt else c.inkMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink,
                        )
                        Text(
                            text = "${dev.driverType} · VID:PID ${dev.vidPid} · ${dev.portPath}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = c.inkMuted,
                            ),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected && isConnected) {
                            c.cobaltContainer
                        } else if (dev.isPermissionGranted) {
                            c.rxEmeraldBg
                        } else {
                            c.surface
                        },
                        border = BorderStroke(1.dp, c.border),
                    ) {
                        Text(
                            text = when {
                                isSelected && isConnected -> s.sheetBadgeActive
                                dev.isPermissionGranted -> s.sheetBadgeGranted
                                else -> s.sheetBadgeNeedsPerm
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when {
                                    isSelected && isConnected -> c.cobaltOnContainer
                                    dev.isPermissionGranted -> c.rxEmerald
                                    else -> c.inkMuted
                                },
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) s.sheetSelectedDesc else s.sheetUnselectedDesc,
                        tint = if (isSelected) c.cobalt else c.inkMuted,
                    )
                }
            }
        }
    }

    HorizontalDivider(color = c.border)

    if (presets.isNotEmpty()) {
        UsbConfigPicker(
            label = s.sheetPresetsHeader,
            valueLabel = selectedPreset?.presetName ?: s.sheetCustomChip,
            options = presets,
            optionLabel = { it.presetName },
            isSelected = { it == selectedPreset },
            onSelect = { preset ->
                draftConfig = preset
                showCustomBaudInput = preset.baudRate !in commonBaudRates
                customBaudText = preset.baudRate.toString()
            },
            tag = "usb_preset",
            optionKey = { it.presetName },
        )
    }

    UsbConfigPicker(
        label = s.sheetBaudRateHeader,
        valueLabel = if (showCustomBaudInput) s.sheetCustomChip else draftConfig.baudRate.toString(),
        options = commonBaudRates + null,
        optionLabel = { it?.toString() ?: s.sheetCustomChip },
        isSelected = { if (showCustomBaudInput) it == null else it == draftConfig.baudRate },
        onSelect = { baud ->
            showCustomBaudInput = baud == null
            if (baud != null) draftConfig = draftConfig.copy(baudRate = baud)
            customBaudText = draftConfig.baudRate.toString()
        },
        tag = "usb_baud",
        optionKey = { it?.toString() ?: "custom" },
    )
    if (showCustomBaudInput) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = s.sheetCustomBaudRangeHint,
                style = MaterialTheme.typography.bodySmall,
                color = if (isBaudValid) c.inkMuted else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            OutlinedTextField(
                value = customBaudText,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(7)
                    customBaudText = digits
                    digits.toIntOrNull()?.takeIf { it in customBaudRange }?.let { validBaud ->
                        draftConfig = draftConfig.copy(baudRate = validBaud)
                    }
                },
                label = { Text(s.sheetCustomBaudLabel) },
                placeholder = { Text(s.sheetCustomBaudPlaceholder) },
                isError = !isBaudValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("custom_baud_input"),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UsbConfigPicker(
            label = s.sheetDataBits,
            valueLabel = draftConfig.dataBits.toString(),
            options = listOf(5, 6, 7, 8),
            optionLabel = Int::toString,
            isSelected = { it == draftConfig.dataBits },
            onSelect = { draftConfig = draftConfig.copy(dataBits = it) },
            tag = "usb_data_bits",
            optionKey = Int::toString,
            modifier = Modifier.weight(1f),
        )
        UsbConfigPicker(
            label = s.sheetStopBits,
            valueLabel = draftConfig.stopBits,
            options = listOf("1", "1.5", "2"),
            optionLabel = { it },
            isSelected = { it == draftConfig.stopBits },
            onSelect = { draftConfig = draftConfig.copy(stopBits = it) },
            tag = "usb_stop_bits",
            optionKey = { it },
            modifier = Modifier.weight(1f),
        )
    }

    UsbConfigPicker(
        label = s.sheetParity,
        valueLabel = draftConfig.parity.label,
        options = ParityMode.entries,
        optionLabel = { it.label },
        isSelected = { it == draftConfig.parity },
        onSelect = { draftConfig = draftConfig.copy(parity = it) },
        tag = "usb_parity",
        optionKey = { it.name.lowercase() },
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.surfaceElevated,
        border = BorderStroke(1.dp, c.border),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
    ) {
        Column {
            UsbControlLine(
                label = s.sheetDtrPullHigh,
                checked = draftConfig.dtrEnabled,
                onCheckedChange = { draftConfig = draftConfig.copy(dtrEnabled = it) },
                tag = "switch_dtr",
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = c.border)
            UsbControlLine(
                label = s.sheetRtsPullHigh,
                checked = draftConfig.rtsEnabled,
                onCheckedChange = { draftConfig = draftConfig.copy(rtsEnabled = it) },
                tag = "switch_rts",
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = presetNameInput,
            onValueChange = { presetNameInput = it },
            label = { Text(s.sheetPresetName) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("preset_name_input"),
        )
        OutlinedButton(
            onClick = {
                onSavePreset(presetNameInput, draftConfig)
                presetNameInput = ""
            },
            enabled = isBaudValid,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("save_preset_button"),
        ) {
            Text(s.sheetSavePresetButton)
        }
    }

    val primaryCtaAction = when {
        selectedDevice != null && isConnected -> s.sheetCtaApplyConnected
        selectedDevice != null -> s.sheetCtaConnectDevice(selectedDevice.driverType)
        else -> s.sheetCtaSaveOffline
    }
    val primaryCtaLabel = if (isBaudValid) "$primaryCtaAction (${draftConfig.compactSummary})" else primaryCtaAction
    Button(
        onClick = { onApplyAndConnect(draftConfig) },
        enabled = isBaudValid,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.cobalt,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .testTag("apply_serial_config_button"),
    ) {
        Text(
            text = primaryCtaLabel,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun <T> UsbConfigPicker(
    label: String,
    valueLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    tag: String,
    optionKey: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.instrumentColors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("${tag}_picker"),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("${tag}_value"),
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val selectedOption = isSelected(option)
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (selectedOption) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null,
                    modifier = Modifier
                        .testTag("${tag}_${optionKey(option)}")
                        .semantics { selected = selectedOption },
                )
            }
        }
    }
}

@Composable
private fun UsbControlLine(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(tag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.instrumentColors.ink,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}
