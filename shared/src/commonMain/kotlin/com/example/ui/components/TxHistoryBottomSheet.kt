package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.PlatformClock
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.formatLocalizedRelativeTime
import com.example.ui.model.ChecksumMode
import com.example.ui.model.TxFormat
import com.example.ui.model.TxHistoryItem
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTerminalStyle
import com.example.ui.theme.instrumentColors

private const val INITIAL_VISIBLE_HISTORY_COUNT = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxHistoryBottomSheet(
    historyItems: List<TxHistoryItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefillItem: (TxHistoryItem) -> Unit,
    onResendItem: (TxHistoryItem) -> Unit,
    onPromoteToMacro: (TxHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAll by rememberSaveable { mutableStateOf(false) }
    val isSearching = searchQuery.isNotBlank()
    val canShowMore = !isSearching && !showAll && historyItems.size > INITIAL_VISIBLE_HISTORY_COUNT
    val visibleItems = remember(historyItems, searchQuery, canShowMore) {
        when {
            isSearching -> historyItems.filter {
                it.payload.contains(searchQuery, ignoreCase = true) ||
                    it.txFormat.label.contains(searchQuery, ignoreCase = true)
            }
            canShowMore -> historyItems.take(INITIAL_VISIBLE_HISTORY_COUNT)
            else -> historyItems
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = c.cobalt,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text = s.historySheetTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink,
                        )
                        Text(
                            text = s.historySheetSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkMuted,
                        )
                    }
                }
                if (historyItems.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_tx_history_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = s.historyClearDesc,
                            tint = c.alertCrimson,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(s.historyClearBtn, color = c.alertCrimson)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = s.commonClearSearch, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                placeholder = { Text(s.historySearchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
            )

            HorizontalDivider(color = c.border)

            if (visibleItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = c.inkMuted,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (historyItems.isEmpty()) s.historyEmpty else s.historyNoMatch(searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkMuted,
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth().height(310.dp)) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("tx_history_list"),
                    ) {
                        items(visibleItems, key = { it.id }) { item ->
                            TxHistoryRow(
                                item = item,
                                onRefill = { onRefillItem(item) },
                                onPromote = { onPromoteToMacro(item) },
                                onResend = { onResendItem(item) },
                            )
                        }
                    }
                    if (canShowMore) {
                        TextButton(
                            onClick = { showAll = true },
                            modifier = Modifier.fillMaxWidth().testTag("show_more_tx_history"),
                        ) {
                            Text(s.historyShowMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxHistoryRow(
    item: TxHistoryItem,
    onRefill: () -> Unit,
    onPromote: () -> Unit,
    onResend: () -> Unit,
) {
    val s = LocalAppStrings.current
    val c = MaterialTheme.instrumentColors
    val timeLabel = remember(item.id, item.lastUsedAtMs, item.lastUsedLabel, s) {
        formatLocalizedRelativeTime(
            nowMs = PlatformClock.currentTimeMillis(),
            pastMs = item.lastUsedAtMs,
            fallbackLabel = item.lastUsedLabel,
            s = s,
        )
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.surfaceElevated,
        border = BorderStroke(1.dp, c.border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onRefill)
            .testTag("tx_history_item_${item.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(color = c.cobaltContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = item.txFormat.label,
                            style = MonoBadgeStyle.copy(color = c.cobaltOnContainer, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                    if (item.txFormat == TxFormat.HEX && item.checksumMode != ChecksumMode.NONE) {
                        val crcText = if (item.checksumMode.is16Bit) {
                            "${item.checksumMode.shortLabel}·${item.checksumEndian.shortLabel}"
                        } else {
                            item.checksumMode.shortLabel
                        }
                        Surface(color = c.rxEmeraldBg, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = crcText,
                                style = MonoBadgeStyle.copy(color = c.rxEmerald, fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(
                        text = s.historyUseCount(timeLabel, item.useCount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = c.inkMuted,
                            fontSize = 10.5.sp,
                        ),
                    )
                }

                Text(
                    text = item.payload,
                    style = MonoTerminalStyle.copy(color = c.ink),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onPromote,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("promote_history_${item.id}"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdd,
                    contentDescription = s.historyPromoteDesc,
                    tint = c.inkMuted,
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = onResend,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("resend_history_${item.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = s.historyResendDesc,
                    tint = c.cobalt,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
