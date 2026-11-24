package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    files: List<FileItem>,
    currentCategory: FileCategory,
    searchQuery: String,
    sortOption: SortOption,
    viewMode: ViewMode,
    selectedItemIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onViewModeToggle: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onItemSelected: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onSyncSelected: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onPropertiesClick: (FileItem) -> Unit,
    onClearSelection: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar Controls: Search & View Mode & Sort
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files by name, type...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentCategory.name} (${files.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Sort Button
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = sortOption.name.lowercase().replaceFirstChar { it.uppercase() })
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Date") },
                                    onClick = {
                                        onSortOptionChange(SortOption.DATE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Name") },
                                    onClick = {
                                        onSortOptionChange(SortOption.NAME)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Size") },
                                    onClick = {
                                        onSortOptionChange(SortOption.SIZE)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Grid / List Toggle
                        IconButton(onClick = onViewModeToggle) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Toggle View Mode"
                            )
                        }
                    }
                }
            }
        }

        // Contextual Action Bar when items are selected
        AnimatedVisibility(visible = selectedItemIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedItemIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onSyncSelected) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // File Content List or Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (files.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No files found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.id }) { file ->
                        FileRowItem(
                            file = file,
                            isSelected = selectedItemIds.contains(file.id),
                            onClick = {
                                if (selectedItemIds.isNotEmpty()) {
                                    onItemSelected(file.id)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onLongClick = { onFileLongClick(file) },
                            onToggleFavorite = { onToggleFavorite(file.id) },
                            onToggleLock = { onToggleLock(file.id) },
                            onPropertiesClick = { onPropertiesClick(file) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(files, key = { it.id }) { file ->
                        FileGridItem(
                            file = file,
                            isSelected = selectedItemIds.contains(file.id),
                            onClick = {
                                if (selectedItemIds.isNotEmpty()) {
                                    onItemSelected(file.id)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onLongClick = { onFileLongClick(file) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
    onPropertiesClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(file.dateModified))
    val sizeMb = String.format(Locale.getDefault(), "%.1f MB", file.size / (1024.0 * 1024.0))
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (file.category) {
                            FileCategory.IMAGES -> Icons.Default.Image
                            FileCategory.VIDEOS -> Icons.Default.Videocam
                            FileCategory.AUDIO -> Icons.Default.AudioFile
                            FileCategory.DOCUMENTS -> Icons.Default.Description
                            FileCategory.APKS -> Icons.Default.Android
                            FileCategory.ARCHIVES -> Icons.Default.FolderZip
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = file.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = sizeMb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (file.isFavorite) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (file.isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        onClick = { showMenu = false; onPropertiesClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (file.isFavorite) "Unfavorite" else "Favorite") },
                        onClick = { showMenu = false; onToggleFavorite() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (file.isLocked) "Unlock Vault" else "Lock in Vault") },
                        onClick = { showMenu = false; onToggleLock() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val sizeMb = String.format(Locale.getDefault(), "%.1f MB", file.size / (1024.0 * 1024.0))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (file.category) {
                                FileCategory.IMAGES -> Icons.Default.Image
                                FileCategory.VIDEOS -> Icons.Default.Videocam
                                FileCategory.AUDIO -> Icons.Default.AudioFile
                                FileCategory.DOCUMENTS -> Icons.Default.Description
                                FileCategory.APKS -> Icons.Default.Android
                                FileCategory.ARCHIVES -> Icons.Default.FolderZip
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = file.name,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (file.isLocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                } else if (file.isCloudSynced) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }

            Column {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sizeMb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
