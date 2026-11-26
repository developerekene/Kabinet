package com.devekene.kabinet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devekene.kabinet.model.FileCategory
import com.devekene.kabinet.model.FileItem
import com.devekene.kabinet.ui.screens.*
import com.devekene.kabinet.ui.theme.KabinetTheme
import com.devekene.kabinet.ui.viewmodel.KabinetViewModel
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Files : Screen("files", "Files", Icons.Default.Folder)
    object Vault : Screen("vault", "Vault", Icons.Default.Lock)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KabinetTheme {
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 350),
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        KabinetMainContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun KabinetMainContent() {
    val context = LocalContext.current
    val viewModel: KabinetViewModel = viewModel()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val vaultFiles by viewModel.vaultFiles.collectAsStateWithLifecycle()
    val filteredFiles by viewModel.filteredFiles.collectAsStateWithLifecycle()
    val deviceStorageInfo by viewModel.deviceStorageInfo.collectAsStateWithLifecycle()
    val connectedFolderCount by viewModel.connectedFolderCount.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentCategory by viewModel.currentCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val previewFile by viewModel.selectedFileForPreview.collectAsStateWithLifecycle()
    val previewText by viewModel.previewTextContent.collectAsStateWithLifecycle()
    val propertiesFile by viewModel.selectedFileForProperties.collectAsStateWithLifecycle()
    val propertiesChecksum by viewModel.propertiesChecksum.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Legacy storage permission request on Android 12 and below (API <= 32)
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.refreshAll()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val readGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!readGranted) {
                legacyStoragePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.statusMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val connectFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderConnected(uri)
        }
    }

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importLocalFiles(uris)
        }
    }

    val hideFilesInVaultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.hideUrisDirectlyInVault(uris)
        }
    }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val items = listOf(Screen.Home, Screen.Files, Screen.Vault)
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
                        vaultCount = vaultFiles.size,
                        connectedFolderCount = connectedFolderCount,
                        storageInfo = deviceStorageInfo,
                        onCategorySelected = { category ->
                            viewModel.setCategory(category)
                            currentScreen = Screen.Files
                        },
                        onFileClick = { file -> viewModel.setFileForPreview(file) },
                        onConnectFolderClick = { connectFolderLauncher.launch(null) },
                        onPickFilesClick = { pickFilesLauncher.launch(arrayOf("*/*")) },
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
                        isLoading = isLoading,
                        onCategoryChange = { viewModel.setCategory(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSortOptionChange = { viewModel.setSortOption(it) },
                        onViewModeToggle = { viewModel.toggleViewMode() },
                        onConnectFolderClick = { connectFolderLauncher.launch(null) },
                        onPickFilesClick = { pickFilesLauncher.launch(arrayOf("*/*")) },
                        onFileClick = { file -> viewModel.setFileForPreview(file) },
                        onFileLongClick = { file -> viewModel.toggleItemSelected(file.id) },
                        onItemSelected = { id -> viewModel.toggleItemSelected(id) },
                        onDeleteSelected = { viewModel.deleteSelectedFiles() },
                        onHideSelectedInVault = { viewModel.hideSelectedFilesInVault() },
                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                        onHideInVault = { file -> viewModel.hideFileInVault(file) },
                        onDeleteFile = { file -> viewModel.deleteFile(file) },
                        onOpenExternally = { file ->
                            openFileWithSystemApp(context, file) { err ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                            }
                        },
                        onPropertiesClick = { file -> viewModel.setFileForProperties(file) },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                }
                Screen.Vault -> {
                    VaultScreen(
                        vaultFiles = vaultFiles,
                        isVaultUnlocked = isVaultUnlocked,
                        onUnlockVault = { viewModel.unlockVault(it) },
                        onHideFilesFromDevice = { hideFilesInVaultLauncher.launch(arrayOf("*/*")) },
                        onFileClick = { file -> viewModel.setFileForPreview(file) },
                        onRestoreFile = { file -> viewModel.restoreFromVault(file) },
                        onDeleteVaultFile = { file -> viewModel.deleteFile(file) },
                        onPropertiesClick = { file -> viewModel.setFileForProperties(file) }
                    )
                }
            }
        }

        previewFile?.let { file ->
            FilePreviewDialog(
                file = file,
                textPreview = previewText,
                onOpenExternally = { target ->
                    openFileWithSystemApp(context, target) { err ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                    }
                },
                onHideOrRestore = { target ->
                    if (target.isLocked) {
                        viewModel.restoreFromVault(target)
                    } else {
                        viewModel.hideFileInVault(target)
                    }
                },
                onDismiss = { viewModel.setFileForPreview(null) }
            )
        }

        propertiesFile?.let { file ->
            FilePropertiesDialog(
                file = file,
                checksum = propertiesChecksum,
                onDismiss = { viewModel.setFileForProperties(null) }
            )
        }
    }
}

private fun openFileWithSystemApp(
    context: Context,
    file: FileItem,
    onError: (String) -> Unit
) {
    try {
        val uri: Uri = if (file.uriString != null) {
            Uri.parse(file.uriString)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(file.path)
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
    } catch (e: Exception) {
        onError("No installed app found to open ${file.name}")
    }
}
