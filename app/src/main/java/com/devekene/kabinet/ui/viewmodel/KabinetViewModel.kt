package com.devekene.kabinet.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devekene.kabinet.data.FileRepository
import com.devekene.kabinet.model.DeviceStorageInfo
import com.devekene.kabinet.model.FileCategory
import com.devekene.kabinet.model.FileItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    NAME, DATE, SIZE
}

enum class ViewMode {
    LIST, GRID
}

class KabinetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application.applicationContext)

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _vaultFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val vaultFiles: StateFlow<List<FileItem>> = _vaultFiles.asStateFlow()

    private val _deviceStorageInfo = MutableStateFlow(repository.getDeviceStorageInfo())
    val deviceStorageInfo: StateFlow<DeviceStorageInfo> = _deviceStorageInfo.asStateFlow()

    private val _connectedFolderCount = MutableStateFlow(repository.getConnectedFolderUris().size)
    val connectedFolderCount: StateFlow<Int> = _connectedFolderCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentCategory = MutableStateFlow(FileCategory.ALL)
    val currentCategory: StateFlow<FileCategory> = _currentCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _selectedFileForPreview = MutableStateFlow<FileItem?>(null)
    val selectedFileForPreview: StateFlow<FileItem?> = _selectedFileForPreview.asStateFlow()

    private val _previewTextContent = MutableStateFlow("")
    val previewTextContent: StateFlow<String> = _previewTextContent.asStateFlow()

    private val _selectedFileForProperties = MutableStateFlow<FileItem?>(null)
    val selectedFileForProperties: StateFlow<FileItem?> = _selectedFileForProperties.asStateFlow()

    private val _propertiesChecksum = MutableStateFlow("")
    val propertiesChecksum: StateFlow<String> = _propertiesChecksum.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _statusMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessage: SharedFlow<String> = _statusMessage.asSharedFlow()

    val filteredFiles: StateFlow<List<FileItem>> = combine(
        _files,
        _currentCategory,
        _searchQuery,
        _sortOption
    ) { allFiles, category, query, sort ->
        var result = allFiles.asSequence()

        when (category) {
            FileCategory.ALL -> {}
            FileCategory.IMAGES -> result = result.filter { it.category == FileCategory.IMAGES }
            FileCategory.VIDEOS -> result = result.filter { it.category == FileCategory.VIDEOS }
            FileCategory.AUDIO -> result = result.filter { it.category == FileCategory.AUDIO }
            FileCategory.DOCUMENTS -> result = result.filter { it.category == FileCategory.DOCUMENTS }
            FileCategory.APKS -> result = result.filter { it.category == FileCategory.APKS }
            FileCategory.ARCHIVES -> result = result.filter { it.category == FileCategory.ARCHIVES }
            FileCategory.DOWNLOADS -> result = result.filter {
                it.category == FileCategory.DOWNLOADS || it.path.contains("/Download", ignoreCase = true)
            }
            FileCategory.FAVORITES -> result = result.filter { it.isFavorite }
            FileCategory.VAULT -> {}
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.mimeType.contains(query, ignoreCase = true) ||
                    it.path.contains(query, ignoreCase = true)
            }
        }

        val sorted = when (sort) {
            SortOption.NAME -> result.sortedBy { it.name.lowercase() }
            SortOption.DATE -> result.sortedByDescending { it.dateModified }
            SortOption.SIZE -> result.sortedByDescending { it.size }
        }

        sorted.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _deviceStorageInfo.value = repository.getDeviceStorageInfo()
            _connectedFolderCount.value = repository.getConnectedFolderUris().size
            _files.value = repository.loadLocalFiles()
            _vaultFiles.value = repository.loadVaultFiles()
            _isLoading.value = false
        }
    }

    fun onFolderConnected(treeUri: Uri) {
        repository.persistFolderPermission(treeUri)
        refreshAll()
        _statusMessage.tryEmit("Device folder connected")
    }

    fun importLocalFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            var added = 0
            for (uri in uris) {
                if (repository.importUriToLocalWorkspace(uri).isSuccess) {
                    added++
                }
            }
            refreshAll()
            if (added > 0) {
                _statusMessage.tryEmit("Added $added file(s) to Kabinet")
            } else {
                _statusMessage.tryEmit("Could not access selected file(s)")
            }
        }
    }

    fun hideUrisDirectlyInVault(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            var hiddenCount = 0
            for (uri in uris) {
                if (repository.hideUriDirectlyInVault(uri).isSuccess) {
                    hiddenCount++
                }
            }
            refreshAll()
            if (hiddenCount > 0) {
                _statusMessage.tryEmit("Moved $hiddenCount file(s) to Secure Vault")
            } else {
                _statusMessage.tryEmit("Failed to hide selected file(s)")
            }
        }
    }

    fun hideFileInVault(file: FileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.hideFileInVault(file)
            if (result.isSuccess) {
                _selectedItemIds.value = _selectedItemIds.value - file.id
                if (_selectedFileForPreview.value?.id == file.id) {
                    _selectedFileForPreview.value = null
                }
                refreshAll()
                _statusMessage.tryEmit("\"${file.name}\" hidden in Secure Vault")
            } else {
                _isLoading.value = false
                _statusMessage.tryEmit("Unable to hide \"${file.name}\"")
            }
        }
    }

    fun hideSelectedFilesInVault() {
        val ids = _selectedItemIds.value
        if (ids.isEmpty()) return
        val targets = _files.value.filter { ids.contains(it.id) }
        viewModelScope.launch {
            _isLoading.value = true
            var count = 0
            for (file in targets) {
                if (repository.hideFileInVault(file).isSuccess) {
                    count++
                }
            }
            _selectedItemIds.value = emptySet()
            refreshAll()
            _statusMessage.tryEmit("Hidden $count file(s) in Secure Vault")
        }
    }

    fun restoreFromVault(vaultItem: FileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.restoreFromVault(vaultItem)
            if (result.isSuccess) {
                if (_selectedFileForPreview.value?.id == vaultItem.id) {
                    _selectedFileForPreview.value = null
                }
                refreshAll()
                _statusMessage.tryEmit("Restored \"${vaultItem.name}\" to device storage")
            } else {
                _isLoading.value = false
                _statusMessage.tryEmit("Could not restore \"${vaultItem.name}\"")
            }
        }
    }

    fun setCategory(category: FileCategory) {
        _currentCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun unlockVault(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            val deleted = repository.deleteFile(file)
            _selectedItemIds.value = _selectedItemIds.value - file.id
            if (_selectedFileForPreview.value?.id == file.id) {
                _selectedFileForPreview.value = null
            }
            refreshAll()
            _statusMessage.tryEmit(
                if (deleted) "Deleted \"${file.name}\"" else "Could not delete \"${file.name}\""
            )
        }
    }

    fun deleteSelectedFiles() {
        val ids = _selectedItemIds.value
        if (ids.isEmpty()) return
        val targets = _files.value.filter { ids.contains(it.id) }
        viewModelScope.launch {
            var count = 0
            for (file in targets) {
                if (repository.deleteFile(file)) {
                    count++
                }
            }
            _selectedItemIds.value = emptySet()
            refreshAll()
            _statusMessage.tryEmit("Deleted $count file(s)")
        }
    }

    fun toggleFavorite(id: String) {
        val updatedFavs = repository.toggleFavorite(id)
        _files.value = _files.value.map {
            if (it.id == id) it.copy(isFavorite = updatedFavs.contains(id)) else it
        }
    }

    fun setFileForPreview(file: FileItem?) {
        _selectedFileForPreview.value = file
        _previewTextContent.value = ""
        if (file != null && FileRepository.isTextPreviewable(file)) {
            viewModelScope.launch {
                _previewTextContent.value = repository.readTextPreview(file)
            }
        }
    }

    fun setFileForProperties(file: FileItem?) {
        _selectedFileForProperties.value = file
        _propertiesChecksum.value = file?.checksum.orEmpty()
        if (file != null && file.checksum.isBlank()) {
            _propertiesChecksum.value = "Calculating..."
            viewModelScope.launch {
                _propertiesChecksum.value = repository.computeSha256(file)
            }
        }
    }

    fun toggleItemSelected(id: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) current - id else current + id
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }
}
