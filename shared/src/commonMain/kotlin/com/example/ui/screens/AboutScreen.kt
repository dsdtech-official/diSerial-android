package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.shared.resources.Res
import com.example.shared.resources.ic_launcher_art
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.instrumentColors
import org.jetbrains.compose.resources.painterResource

private enum class AboutDetail { USB, WIRING, RELEASE_NOTES, PRIVACY }

val LocalPrivacyPolicyUrl = staticCompositionLocalOf<String?> { null }

@Composable
fun AboutScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    var openDetail by rememberSaveable { mutableStateOf<AboutDetail?>(null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_art),
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium).testTag("about_app_icon"),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("diSerial", style = MaterialTheme.typography.headlineSmall, color = c.ink)
                if (appVersion.isNotBlank()) {
                    Text("v$appVersion", style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
                }
                Text(s.aboutTagline, style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
            }
        }

        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.surface) {
            Column {
                SettingsLinkRow(s.aboutChipsTitle, { openDetail = AboutDetail.USB }, "about_usb_button")
                SettingsDivider()
                SettingsLinkRow(s.aboutWiringBtn, { openDetail = AboutDetail.WIRING }, "about_wiring_guide_button")
                SettingsDivider()
                SettingsLinkRow(s.aboutChangelogBtn, { openDetail = AboutDetail.RELEASE_NOTES }, "about_changelog_button")
                SettingsDivider()
                SettingsLinkRow(s.aboutPrivacyTitle, { openDetail = AboutDetail.PRIVACY }, "about_privacy_button")
            }
        }

        SelectionContainer {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.aboutCompanyName, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Text("www.deshide.com", style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
                Text("dsd_tech@outlook.com", style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
            }
        }
    }

    openDetail?.let { detail ->
        AboutDetailDialog(detail, onClose = { openDetail = null })
    }
}

@Composable
private fun AboutDetailDialog(detail: AboutDetail, onClose: () -> Unit) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val title = when (detail) {
        AboutDetail.USB -> s.aboutChipsTitle
        AboutDetail.WIRING -> s.aboutWiringBtn
        AboutDetail.RELEASE_NOTES -> s.aboutChangelogBtn
        AboutDetail.PRIVACY -> s.aboutPrivacyTitle
    }
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("about_detail_dialog"),
        containerColor = c.surface,
        title = { Text(title, color = c.ink) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (detail) {
                    AboutDetail.USB -> {
                        Text(s.aboutUsbScope)
                        Text("WCH CH340 / CH341 / CH343 / CH9102\nSilicon Labs CP210x\nFTDI FT232 / FT230X / FT231X\nProlific PL2303\nUSB CDC-ACM")
                    }
                    AboutDetail.WIRING -> {
                        Text(s.aboutWiringTip1)
                        Text(s.aboutWiringTip2)
                        Text(s.aboutWiringTip3)
                        Text(s.aboutWiringTip4)
                    }
                    AboutDetail.RELEASE_NOTES -> {
                        Text(s.aboutChangelogVersion, style = MaterialTheme.typography.titleSmall, color = c.ink)
                        Text(s.aboutChangelogBody)
                    }
                    AboutDetail.PRIVACY -> {
                        Text(s.aboutPrivacyBody)
                        LocalPrivacyPolicyUrl.current?.let { url ->
                            PrivacyPolicyLink(url, s.aboutPrivacyPolicyLink)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose, modifier = Modifier.testTag("about_detail_close")) {
                Text(s.commonClose)
            }
        },
    )
}

@Composable
private fun PrivacyPolicyLink(url: String, label: String) {
    val uriHandler = LocalUriHandler.current
    var showAddress by rememberSaveable(url) { mutableStateOf(false) }

    TextButton(
        onClick = {
            showAddress = try {
                uriHandler.openUri(url)
                false
            } catch (_: IllegalArgumentException) {
                true
            }
        },
        modifier = Modifier.testTag("about_privacy_policy_link"),
    ) {
        Text(label)
    }
    if (showAddress) {
        SelectionContainer { Text(url) }
    }
}
