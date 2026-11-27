package com.example.ui.state

import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ZhCnAppStrings
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.UsbDeviceItem

data class SmartDevicePickerDecision(
    val shouldOpenPicker: Boolean = false,
    val promptReason: String? = null,
    val nextSignature: String? = null,
    val shouldAutoConnectSingleDevice: Boolean = false,
)

object SmartDevicePickerPolicy {

    fun evaluate(
        devices: List<UsbDeviceItem>,
        connectionStatus: ConnectionStatus,
        autoConnectOnAttach: Boolean,
        isSheetAlreadyOpen: Boolean,
        lastPromptedSignature: String?,
        launchedDeviceId: String? = null,
        strings: AppStrings = ZhCnAppStrings,
    ): SmartDevicePickerDecision {
        if (devices.size < 2) {
            val shouldAutoConnect = devices.size == 1 &&
                connectionStatus == ConnectionStatus.DISCONNECTED &&
                autoConnectOnAttach
            return SmartDevicePickerDecision(
                shouldOpenPicker = false,
                promptReason = null,
                nextSignature = null,
                shouldAutoConnectSingleDevice = shouldAutoConnect,
            )
        }

        val signature = devices.map { it.id }.sorted().joinToString("|")
        if (connectionStatus != ConnectionStatus.DISCONNECTED) {
            return SmartDevicePickerDecision(
                shouldOpenPicker = false,
                promptReason = null,
                nextSignature = lastPromptedSignature,
                shouldAutoConnectSingleDevice = false,
            )
        }

        if (isSheetAlreadyOpen) {
            return SmartDevicePickerDecision(
                shouldOpenPicker = false,
                promptReason = null,
                nextSignature = signature,
                shouldAutoConnectSingleDevice = false,
            )
        }

        if (signature == lastPromptedSignature) {
            return SmartDevicePickerDecision(
                shouldOpenPicker = false,
                promptReason = null,
                nextSignature = signature,
                shouldAutoConnectSingleDevice = false,
            )
        }

        val lockedChip = launchedDeviceId?.let { targetId ->
            devices.firstOrNull { it.id == targetId }?.chipName
        }
        val reason = if (lockedChip != null) {
            strings.sheetLockedDeviceBanner(lockedChip, devices.size)
        } else {
            strings.sheetGenericMultiBanner(devices.size)
        }

        return SmartDevicePickerDecision(
            shouldOpenPicker = true,
            promptReason = reason,
            nextSignature = signature,
            shouldAutoConnectSingleDevice = false,
        )
    }
}
