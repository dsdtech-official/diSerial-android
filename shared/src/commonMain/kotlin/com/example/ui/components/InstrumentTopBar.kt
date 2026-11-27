package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.ConnectionStatus
import com.example.ui.model.SerialPortConfig
import com.example.ui.model.UsbDeviceItem
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.instrumentColors

@Composable
fun InstrumentTopBar(
    connectionStatus: ConnectionStatus,
    selectedDevice: UsbDeviceItem?,
    config: SerialPortConfig,
    onOpenConfigSheet: () -> Unit,
    onToggleConnect: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    val ledColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            ConnectionStatus.CONNECTED -> c.rxEmerald
            ConnectionStatus.CONNECTING -> c.txAmber
            ConnectionStatus.DISCONNECTED -> c.sysSlate
        },
        label = "status_led_color",
    )

    Surface(
        color = c.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, if (isConnected) c.rxEmerald.copy(alpha = 0.45f) else c.border),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("device_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(role = Role.Button, onClick = onOpenConfigSheet)
                                .testTag("usb_config_pill")
                                .semantics { contentDescription = s.topBarAdjustParamsDesc }
                                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val titleText = selectedDevice?.let { it.shortModel ?: it.driverType } ?: s.topBarNoUsbDetected
                            val primaryTag = config.compactSummary
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = c.ink,
                                    modifier = Modifier.weight(1f).testTag("device_card_title"),
                                    maxLines = if (selectedDevice == null) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val metaStyle = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = c.inkMuted,
                                fontSize = 10.5.sp,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(ledColor),
                                )
                                Text(
                                    text = primaryTag,
                                    style = metaStyle.copy(color = c.cobalt, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                // Connect / Disconnect Action Button (>= 48dp touch target)
                Button(
                    onClick = onToggleConnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) c.alertCrimsonBg else c.cobalt,
                        contentColor = if (isConnected) c.alertCrimson else MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = if (isConnected) BorderStroke(1.dp, c.alertCrimson.copy(alpha = 0.45f)) else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("toggle_connect_button"),
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                        contentDescription = if (isConnected) s.topBarDisconnectDesc else s.topBarConnectDesc,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) s.topBarDisconnect else s.topBarConnect,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Top-Right Quick Settings Entry (>= 48dp touch target)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenSettings)
                        .testTag("top_bar_settings_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = s.topBarSettingsDesc,
                            tint = c.ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Show subtle guidance banner ONLY when disconnected or pending (saves vertical height during active debugging!)
            AnimatedVisibility(visible = !isConnected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.txAmberBg)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.topBarIdleBanner,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = c.txAmber,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = c.border)
        }
    }
}
