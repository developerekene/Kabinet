package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.FileItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    files: List<FileItem>,
    onCategorySelected: (FileCategory) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onCloudSyncClick: () -> Unit,
    onVaultClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val totalSize = files.sumOf { it.size }
    val usedStorageGB = String.format(Locale.getDefault(), "%.1f", totalSize / (1024.0 * 1024.0 * 1024.0) + 42.5)
    val freeStorageGB = "185.3"
    val cloudSyncedCount = files.count { it.isCloudSynced }
    val lockedCount = files.count { it.isLocked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header / Welcome banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kabinet Storage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$usedStorageGB GB used of 228 GB total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 0.28f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageIcons.folder,
                                contentDescription = "Storage",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Access Cards Row (Cloud Sync, Vault, Favorites, Downloads)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    title = "Cloud Sync",
                    subtitle = "$cloudSyncedCount synced",
                    icon = Icons.Default.CloudSync,
                    modifier = Modifier.weight(1f),
                    onClick = onCloudSyncClick
                )
                QuickAccessCard(
                    title = "Secure Vault",
                    subtitle = "$lockedCount locked",
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f),
                    onClick = onVaultClick
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    title = "Favorites",
                    subtitle = "${files.count { it.isFavorite }} items",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                    onClick = onFavoritesClick
                )
                QuickAccessCard(
                    title = "Downloads",
                    subtitle = "${files.count { it.category == FileCategory.APKS || it.category == FileCategory.ARCHIVES }} items",
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f),
                    onClick = { onCategorySelected(FileCategory.APKS) }
                )
            }
        }

        // Categories section title
        item {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Category Grids
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryTile(
                        title = "Images",
                        count = files.count { it.category == FileCategory.IMAGES },
                        icon = Icons.Default.Image,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.IMAGES) }
                    )
                    CategoryTile(
                        title = "Videos",
                        count = files.count { it.category == FileCategory.VIDEOS },
                        icon = Icons.Default.Videocam,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.VIDEOS) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryTile(
                        title = "Audio",
                        count = files.count { it.category == FileCategory.AUDIO },
                        icon = Icons.Default.AudioFile,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.AUDIO) }
                    )
                    CategoryTile(
                        title = "Documents",
                        count = files.count { it.category == FileCategory.DOCUMENTS },
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.DOCUMENTS) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryTile(
                        title = "APKs",
                        count = files.count { it.category == FileCategory.APKS },
                        icon = Icons.Default.Android,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.APKS) }
                    )
                    CategoryTile(
                        title = "Archives",
                        count = files.count { it.category == FileCategory.ARCHIVES },
                        icon = Icons.Default.FolderZip,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(FileCategory.ARCHIVES) }
                    )
                }
            }
        }

        // Recent Files header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onCategorySelected(FileCategory.ALL) }) {
                    Text("View All")
                }
            }
        }

        // Recent Files list
        items(files.take(5)) { file ->
            FileListItem(file = file, onClick = { onFileClick(file) })
        }
    }
}

@Composable
fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryTile(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(file.dateModified))
    val sizeMb = String.format(Locale.getDefault(), "%.1f MB", file.size / (1024.0 * 1024.0))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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
                        tint = MaterialTheme.colorScheme.primary,
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
                    Text(
                        text = sizeMb,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (file.isCloudSynced) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

object imageIcons {
    val folder = Icons.Default.Folder
}
