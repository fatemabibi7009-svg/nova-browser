package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.engine.BrowserTab
import com.example.engine.SearchEngine
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.ShieldOrange
import com.example.ui.theme.WarningAmber

@Composable
fun Omnibox(
    tab: BrowserTab,
    isEditing: Boolean,
    textInput: String,
    searchEngine: SearchEngine,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onEditStart: () -> Unit,
    onEditCancel: () -> Unit,
    onTextChanged: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShieldClick: () -> Unit,
    modifier: Modifier = Modifier,
    onQrScannerClick: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Address bar container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isEditing
                        ) {
                            onEditStart()
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // SSL Lock or Search Icon
                        if (isEditing) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            val (lockIcon, lockTint) = when {
                                tab.isStartPage || tab.isNovaSearchPage -> Pair(Icons.Default.Search, MaterialTheme.colorScheme.primary)
                                tab.sslSecure -> Pair(Icons.Default.Lock, SecureGreen)
                                else -> Pair(Icons.Default.Warning, WarningAmber)
                            }
                            Icon(
                                imageVector = lockIcon,
                                contentDescription = "Security Status",
                                tint = lockTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // URL / Search Text Field
                        if (isEditing) {
                            BasicTextField(
                                value = textInput,
                                onValueChange = onTextChanged,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        focusManager.clearFocus()
                                        onSubmitUrl(textInput)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("omnibox_input_field")
                            )

                            if (textInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { onTextChanged("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        onEditCancel()
                                        onQrScannerClick()
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("omnibox_edit_qr_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR Code",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Display domain or prompt
                            Text(
                                text = when {
                                    tab.isStartPage -> "Search or type web address"
                                    tab.isNovaSearchPage -> "Nova Search: ${tab.novaSearchQuery}"
                                    else -> tab.domain
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (tab.isStartPage) FontWeight.Normal else FontWeight.Medium,
                                    color = if (tab.isStartPage) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (tab.isStartPage) {
                                IconButton(
                                    onClick = onQrScannerClick,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("omnibox_start_qr_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR Code",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Shield Button with live blocked count badge
                        if (!isEditing && !tab.isStartPage) {
                            IconButton(
                                onClick = onShieldClick,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("shield_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (tab.blockedCount > 0) {
                                            Badge(
                                                containerColor = ShieldOrange,
                                                contentColor = androidx.compose.ui.graphics.Color.White
                                            ) {
                                                Text(
                                                    text = "${tab.blockedCount}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Shields",
                                        tint = if (tab.blockedCount > 0) ShieldOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Reload / Stop button
                        if (!isEditing && !tab.isStartPage) {
                            IconButton(
                                onClick = { if (tab.isLoading) onStop() else onReload() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("reload_button")
                            ) {
                                Icon(
                                    imageVector = if (tab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (tab.isLoading) "Stop Loading" else "Reload Page",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // If editing, provide Cancel button
                AnimatedVisibility(visible = isEditing) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onEditCancel()
                            }
                            .testTag("omnibox_cancel_button")
                    )
                }
            }
        }

        // Animated Web Page Loading Progress Bar
        AnimatedVisibility(
            visible = tab.isLoading && tab.progress in 1..99,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { tab.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Suggestions Dropdown when typing
        if (isEditing && textInput.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    // Default Search Engine item
                    item {
                        SuggestionItem(
                            icon = Icons.Default.Search,
                            title = "Search with ${searchEngine.displayName}",
                            subtitle = textInput,
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitUrl(searchEngine.buildQuery(textInput))
                            }
                        )
                    }

                    // Direct Web Navigation item
                    item {
                        SuggestionItem(
                            icon = Icons.Default.Public,
                            title = "Go to website",
                            subtitle = if (textInput.startsWith("http")) textInput else "https://$textInput",
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitUrl(textInput)
                            }
                        )
                    }

                    // Matching Bookmarks
                    val matchedBookmarks = bookmarks.filter {
                        it.title.contains(textInput, ignoreCase = true) || it.url.contains(textInput, ignoreCase = true)
                    }.take(3)
                    items(matchedBookmarks) { bookmark ->
                        SuggestionItem(
                            icon = Icons.Default.Bookmark,
                            title = bookmark.title,
                            subtitle = bookmark.url,
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitUrl(bookmark.url)
                            }
                        )
                    }

                    // Matching History
                    val matchedHistory = history.filter {
                        it.title.contains(textInput, ignoreCase = true) || it.url.contains(textInput, ignoreCase = true)
                    }.take(3)
                    items(matchedHistory) { hist ->
                        SuggestionItem(
                            icon = Icons.Default.History,
                            title = hist.title,
                            subtitle = hist.url,
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitUrl(hist.url)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
