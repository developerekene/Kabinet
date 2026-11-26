package com.devekene.kabinet.model

enum class FileCategory(val displayName: String) {
    ALL("All Files"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    APKS("APKs"),
    ARCHIVES("Archives"),
    DOWNLOADS("Downloads"),
    FAVORITES("Favorites"),
    VAULT("Secure Vault")
}

data class DeviceStorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val availableBytes: Long = 0L
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val uriString: String? = null,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val category: FileCategory,
    val isDirectory: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val vaultFileName: String? = null,
    val originalPath: String? = null,
    val checksum: String = "",
    val contentSnippet: String = ""
)
