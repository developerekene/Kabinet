package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FileCategory
import com.example.ui.screens.*
import com.example.ui.theme.KabinetTheme
import com.example.ui.viewmodel.KabinetViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Files : Screen("files", "Files", Icons.Default.Folder)
    object Cloud : Screen("cloud", "Cloud", Icons.Default.CloudSync)
    object Vault : Screen("vault", "Vault", Icons.Default.Lock)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KabinetTheme {
                val viewModel: KabinetViewModel = viewModel()
                val files by viewModel.files.collectAsState()
                val filteredFiles by viewModel.filteredFiles.collectAsState()
                val currentCategory by viewModel.currentCategory.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val sortOption by viewModel.sortOption.collectAsState()
                val viewMode by viewModel.viewMode.collectAsState()
                val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
                val previewFile by viewModel.selectedFileForPreview.collectAsState()
                val propertiesFile by viewModel.selectedFileForProperties.collectAsState()
                val selectedItemIds by viewModel.selectedItemIds.collectAsState()

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(Screen.Home, Screen.Files, Screen.Cloud, Screen.Vault)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Home -> {
                                HomeScreen(
                                    files = files,
                                    onCategorySelected = { category ->
                                        viewModel.setCategory(category)
                                        currentScreen = Screen.Files
                                    },
                                    onFileClick = { file -> viewModel.setFileForPreview(file) },
                                    onCloudSyncClick = { currentScreen = Screen.Cloud },
                                    onVaultClick = { currentScreen = Screen.Vault },
                                    onFavoritesClick = {
                                        viewModel.setCategory(FileCategory.FAVORITES)
                                        currentScreen = Screen.Files
                                    }
                                )
                            }
                            Screen.Files -> {
                                FileListScreen(
                                    files = filteredFiles,
                                    currentCategory = currentCategory,
                                    searchQuery = searchQuery,
                                    sortOption = sortOption,
                                    viewMode = viewMode,
                                    selectedItemIds = selectedItemIds,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onSortOptionChange = { viewModel.setSortOption(it) },
                                    onViewModeToggle = { viewModel.toggleViewMode() },
                                    onFileClick = { file -> viewModel.setFileForPreview(file) },
                                    onFileLongClick = { file -> viewModel.toggleItemSelected(file.id) },
                                    onItemSelected = { id -> viewModel.toggleItemSelected(id) },
                                    onDeleteSelected = { viewModel.deleteSelectedFiles() },
                                    onSyncSelected = { viewModel.syncAllToCloud() },
                                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                    onToggleLock = { id -> viewModel.toggleLock(id) },
                                    onPropertiesClick = { file -> viewModel.setFileForProperties(file) },
                                    onClearSelection = { viewModel.clearSelection() }
                                )
                            }
                            Screen.Cloud -> {
                                CloudSyncScreen(
                                    files = files,
                                    onSyncAll = { viewModel.syncAllToCloud() },
                                    onToggleCloudSync = { id -> viewModel.toggleCloudSync(id) },
                                    onFileClick = { file -> viewModel.setFileForPreview(file) }
                                )
                            }
                            Screen.Vault -> {
                                VaultScreen(
                                    files = files,
                                    isVaultUnlocked = isVaultUnlocked,
                                    onUnlockVault = { viewModel.unlockVault(it) },
                                    onFileClick = { file -> viewModel.setFileForPreview(file) },
                                    onToggleLock = { id -> viewModel.toggleLock(id) }
                                )
                            }
                        }
                    }

                    // Dialogs
                    previewFile?.let { file ->
                        FilePreviewDialog(file = file, onDismiss = { viewModel.setFileForPreview(null) })
                    }

                    propertiesFile?.let { file ->
                        FilePropertiesDialog(file = file, onDismiss = { viewModel.setFileForProperties(null) })
                    }
                }
            }
        }
    }
}
