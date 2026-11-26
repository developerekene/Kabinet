package com.devekene.kabinet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devekene.kabinet.data.FileRepository
import com.devekene.kabinet.model.FileCategory
import com.devekene.kabinet.model.FileItem
import com.devekene.kabinet.ui.viewmodel.SortOption
import com.devekene.kabinet.ui.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileListScreen(
    files: List<FileItem>,
    currentCategory: FileCategory,
    searchQuery: String,
    sortOption: SortOption,
    viewMode: ViewMode,
    selectedItemIds: Set<String>,
    isLoading: Boolean,
    onCategoryChange: (FileCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onViewModeToggle: () -> Unit,
    onConnectFolderClick: () -> Unit,
    onPickFilesClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onItemSelected: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onHideSelectedInVault: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onHideInVault: (FileItem) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onOpenExternally: (FileItem) -> Unit,
    onPropertiesClick: (FileItem) -> Unit,
    onClearSelection: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    val browseCategories = remember {
        listOf(
            FileCategory.ALL,
            FileCategory.IMAGES,
            FileCategory.VIDEOS,
            FileCategory.AUDIO,
            FileCategory.DOCUMENTS,
            FileCategory.APKS,
            FileCategory.ARCHIVES,
            FileCategory.DOWNLOADS,
            FileCategory.FAVORITES
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search local files by name, type...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("files_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    browseCategories.forEach { category ->
                        FilterChip(
                            selected = currentCategory == category,
                            onClick = { onCategoryChange(category) },
                            label = { Text(category.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentCategory.displayName} (${files.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onConnectFolderClick,
                            modifier = Modifier.testTag("connect_folder_button")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Connect Folder")
                        }
                        IconButton(
                            onClick = onPickFilesClick,
                            modifier = Modifier.testTag("pick_files_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Local File")
                        }

                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp))
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

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

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
                        IconButton(onClick = onHideSelectedInVault) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "Hide in Vault", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (files.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
                        text = "No local files found here",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connect a device folder or add files to browse, preview, delete, or hide them in your Secure Vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onConnectFolderClick, shape = RoundedCornerShape(12.dp)) {
                            Text("Connect Folder")
                        }
                        OutlinedButton(onClick = onPickFilesClick, shape = RoundedCornerShape(12.dp)) {
                            Text("Pick File")
                        }
                    }
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
                            onHideInVault = { onHideInVault(file) },
                            onDeleteClick = { onDeleteFile(file) },
                            onOpenExternally = { onOpenExternally(file) },
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
    onHideInVault: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenExternally: () -> Unit,
    onPropertiesClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(file.dateModified))
    val formattedSize = FileRepository.formatBytes(file.size)
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
                    Text(text = formattedSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (file.isFavorite) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Open With...") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        onClick = { showMenu = false; onOpenExternally() }
                    )
                    DropdownMenuItem(
                        text = { Text("Hide in Secure Vault") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                        onClick = { showMenu = false; onHideInVault() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (file.isFavorite) "Remove Favorite" else "Add to Favorites") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = { showMenu = false; onToggleFavorite() }
                    )
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = { showMenu = false; onPropertiesClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onDeleteClick() }
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
    val formattedSize = FileRepository.formatBytes(file.size)

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
                if (file.isFavorite) {
                    Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                    text = formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
