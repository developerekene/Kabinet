package com.example.model

enum class FileCategory {
    ALL,
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    APKS,
    ARCHIVES,
    DOWNLOADS,
    FAVORITES,
    VAULT,
    CLOUD
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val category: FileCategory,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val isCloudSynced: Boolean = false,
    val checksum: String = "SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    val contentSnippet: String = "" // For text/code preview
)
