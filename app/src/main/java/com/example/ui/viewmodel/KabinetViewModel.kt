package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.FileRepository
import com.example.model.FileCategory
import com.example.model.FileItem
import kotlinx.coroutines.flow.*

enum class SortOption {
    NAME, DATE, SIZE
}

enum class ViewMode {
    LIST, GRID
}

class KabinetViewModel : ViewModel() {
    private val repository = FileRepository()

    val files: StateFlow<List<FileItem>> = repository.files

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

    private val _selectedFileForProperties = MutableStateFlow<FileItem?>(null)
    val selectedFileForProperties: StateFlow<FileItem?> = _selectedFileForProperties.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    val filteredFiles: StateFlow<List<FileItem>> = combine(
        repository.files,
        _currentCategory,
        _searchQuery,
        _sortOption,
        _isVaultUnlocked
    ) { allFiles, category, query, sort, vaultUnlocked ->
        var result = allFiles.asSequence()

        // Category filter
        when (category) {
            FileCategory.ALL -> {}
            FileCategory.IMAGES -> result = result.filter { it.category == FileCategory.IMAGES }
            FileCategory.VIDEOS -> result = result.filter { it.category == FileCategory.VIDEOS }
            FileCategory.AUDIO -> result = result.filter { it.category == FileCategory.AUDIO }
            FileCategory.DOCUMENTS -> result = result.filter { it.category == FileCategory.DOCUMENTS }
            FileCategory.APKS -> result = result.filter { it.category == FileCategory.APKS }
            FileCategory.ARCHIVES -> result = result.filter { it.category == FileCategory.ARCHIVES }
            FileCategory.FAVORITES -> result = result.filter { it.isFavorite }
            FileCategory.VAULT -> {
                if (!vaultUnlocked) {
                    // Hide or filter locked items if vault not unlocked
                }
                result = result.filter { it.isLocked }
            }
            FileCategory.CLOUD -> result = result.filter { it.isCloudSynced }
            else -> {}
        }

        // Search query filter
        if (query.isNotBlank()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) || it.mimeType.contains(query, ignoreCase = true) }
        }

        // Sorting
        val sorted = when (sort) {
            SortOption.NAME -> result.sortedBy { it.name.lowercase() }
            SortOption.DATE -> result.sortedByDescending { it.dateModified }
            SortOption.SIZE -> result.sortedByDescending { it.size }
        }

        sorted.toList()
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    fun deleteFile(id: String) {
        repository.deleteFile(id)
        _selectedItemIds.value = _selectedItemIds.value - id
    }

    fun deleteSelectedFiles() {
        for (id in _selectedItemIds.value) {
            repository.deleteFile(id)
        }
        _selectedItemIds.value = emptySet()
    }

    fun toggleFavorite(id: String) {
        repository.toggleFavorite(id)
    }

    fun toggleLock(id: String) {
        repository.toggleLock(id)
    }

    fun toggleCloudSync(id: String) {
        repository.toggleCloudSync(id)
    }

    fun syncAllToCloud() {
        repository.syncAllToCloud()
    }

    fun setFileForPreview(file: FileItem?) {
        _selectedFileForPreview.value = file
    }

    fun setFileForProperties(file: FileItem?) {
        _selectedFileForProperties.value = file
    }

    fun toggleItemSelected(id: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) current - id else current + id
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun addNewFile(name: String, path: String, size: Long, mimeType: String, category: FileCategory) {
        val newItem = FileItem(
            id = System.currentTimeMillis().toString(),
            name = name,
            path = path,
            size = size,
            dateModified = System.currentTimeMillis(),
            mimeType = mimeType,
            category = category,
            isCloudSynced = false
        )
        repository.addFile(newItem)
    }
}
