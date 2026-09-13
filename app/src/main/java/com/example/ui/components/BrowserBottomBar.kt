package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActiveSheet
import com.example.engine.BrowserTab

@Composable
fun BrowserBottomBar(
    tab: BrowserTab,
    tabCount: Int,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleReader: () -> Unit,
    onToggleForceDark: () -> Unit,
    onSavePdf: () -> Unit,
    onSaveOffline: () -> Unit,
    onShare: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onOpenFindInPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                enabled = tab.canGoBack,
                modifier = Modifier.testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = if (tab.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            // Forward button
            IconButton(
                onClick = onForward,
                enabled = tab.canGoForward,
                modifier = Modifier.testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go Forward",
                    tint = if (tab.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            // Home button
            IconButton(
                onClick = onHome,
                modifier = Modifier.testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tabs Switcher button (badge showing count e.g. "3")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onTabsClick)
                    .testTag("nav_tabs_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Text(
                        text = if (tabCount > 99) "99+" else "$tabCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3-Dots Menu Button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("nav_more_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Browser Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.testTag("browser_dropdown_menu")
                ) {
                    // New Tab
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onNewTab(false)
                        },
                        modifier = Modifier.testTag("menu_new_tab")
                    )

                    // New Private Tab
                    DropdownMenuItem(
                        text = { Text("New Private Tab") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onNewTab(true)
                        },
                        modifier = Modifier.testTag("menu_new_private_tab")
                    )

                    HorizontalDivider()

                    // Bookmark Current Page
                    if (!tab.isStartPage) {
                        DropdownMenuItem(
                            text = { Text(if (isBookmarked) "Bookmarked" else "Add Bookmark") },
                            leadingIcon = {
                                Icon(
                                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleBookmark()
                            },
                            modifier = Modifier.testTag("menu_toggle_bookmark")
                        )
                    }

                    // Bookmarks List
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.BOOKMARKS)
                        },
                        modifier = Modifier.testTag("menu_bookmarks")
                    )

                    // History List
                    DropdownMenuItem(
                        text = { Text("History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.HISTORY)
                        },
                        modifier = Modifier.testTag("menu_history")
                    )

                    // Downloads Manager
                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.DOWNLOADS)
                        },
                        modifier = Modifier.testTag("menu_downloads")
                    )

                    // QR Code Scanner
                    DropdownMenuItem(
                        text = { Text("Scan QR Code") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.QR_SCANNER)
                        },
                        modifier = Modifier.testTag("menu_qr_scanner")
                    )

                    HorizontalDivider()

                    // Actions on current page
                    if (!tab.isStartPage) {
                        // Find in Page
                        DropdownMenuItem(
                            text = { Text("Find in Page") },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenFindInPage()
                            },
                            modifier = Modifier.testTag("menu_find_in_page")
                        )

                        // Force Dark Mode toggle
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Force Dark Mode")
                                    if (tab.isForceDarkMode) {
                                        Text("ON", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleForceDark()
                            },
                            modifier = Modifier.testTag("menu_force_dark")
                        )

                        // Desktop Site Toggle
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Desktop Site")
                                    if (tab.isDesktopMode) {
                                        Text("ON", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleDesktop()
                            },
                            modifier = Modifier.testTag("menu_desktop_mode")
                        )

                        // Save as PDF
                        DropdownMenuItem(
                            text = { Text("Save as PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSavePdf()
                            },
                            modifier = Modifier.testTag("menu_save_pdf")
                        )

                        // Save Offline (.mht archive)
                        DropdownMenuItem(
                            text = { Text("Save Offline Page") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSaveOffline()
                            },
                            modifier = Modifier.testTag("menu_save_offline")
                        )

                        // Share Current Page
                        DropdownMenuItem(
                            text = { Text("Share…") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            },
                            modifier = Modifier.testTag("menu_share")
                        )
                    }

                    HorizontalDivider()

                    // Shields Dashboard
                    DropdownMenuItem(
                        text = { Text("Nova Shields") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.SHIELDS)
                        },
                        modifier = Modifier.testTag("menu_shields")
                    )

                    // Clear Browsing Data
                    DropdownMenuItem(
                        text = { Text("Clear Data…") },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.CLEAR_DATA)
                        },
                        modifier = Modifier.testTag("menu_clear_data")
                    )

                    // Settings
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenSheet(ActiveSheet.SETTINGS)
                        },
                        modifier = Modifier.testTag("menu_settings")
                    )
                }
            }
        }
    }
}
