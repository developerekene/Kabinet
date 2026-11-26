package com.devekene.kabinet.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.devekene.kabinet.model.DeviceStorageInfo
import com.devekene.kabinet.model.FileCategory
import com.devekene.kabinet.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

class FileRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("kabinet_local_prefs", Context.MODE_PRIVATE)
    private val vaultDir = File(context.filesDir, "kabinet_vault")
    private val vaultIndexFile = File(context.filesDir, "kabinet_vault_index.json")

    init {
        ensureVaultDirectory()
    }

    private fun ensureVaultDirectory() {
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        val nomedia = File(vaultDir, ".nomedia")
        if (!nomedia.exists()) {
            runCatching { nomedia.createNewFile() }
        }
    }

    fun getDeviceStorageInfo(): DeviceStorageInfo {
        return try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val total = stat.blockSizeLong * stat.blockCountLong
            val available = stat.blockSizeLong * stat.availableBlocksLong
            val used = (total - available).coerceAtLeast(0L)
            DeviceStorageInfo(
                totalBytes = total,
                usedBytes = used,
                availableBytes = available
            )
        } catch (_: Exception) {
            DeviceStorageInfo()
        }
    }

    fun getConnectedFolderUris(): List<Uri> {
        val persisted = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
        return persisted
    }

    fun persistFolderPermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(id: String): Set<String> {
        val updated = getFavoriteIds().toMutableSet()
        if (updated.contains(id)) {
            updated.remove(id)
        } else {
            updated.add(id)
        }
        prefs.edit().putStringSet("favorite_ids", updated).apply()
        return updated
    }

    suspend fun loadLocalFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val favorites = getFavoriteIds()
        val collected = LinkedHashMap<String, FileItem>()

        // 1. Scan connected SAF tree URIs
        for (treeUri in getConnectedFolderUris()) {
            scanDocumentTree(treeUri, favorites, collected, maxDepth = 4)
        }

        // 2. Scan readable external storage roots
        val externalRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (externalRoot != null && externalRoot.exists() && externalRoot.canRead()) {
            scanDirectory(externalRoot, favorites, collected, maxDepth = 3)
        }

        // 3. Scan public directories if individually accessible
        val publicDirs = listOfNotNull(
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) }.getOrNull(),
            context.getExternalFilesDir(null)
        )
        for (dir in publicDirs) {
            if (dir.exists() && dir.canRead()) {
                scanDirectory(dir, favorites, collected, maxDepth = 3)
            }
        }

        // 4. Include manually imported local workspace files (if any)
        val localWorkspace = File(context.filesDir, "kabinet_local")
        if (localWorkspace.exists() && localWorkspace.canRead()) {
            scanDirectory(localWorkspace, favorites, collected, maxDepth = 2)
        }

        collected.values.sortedByDescending { it.dateModified }
    }

    private fun scanDirectory(
        dir: File,
        favorites: Set<String>,
        out: MutableMap<String, FileItem>,
        maxDepth: Int,
        depth: Int = 0
    ) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            val name = child.name
            if (name.startsWith(".") || name == "Android") continue
            if (child.isDirectory) {
                if (child.canRead()) {
                    scanDirectory(child, favorites, out, maxDepth, depth + 1)
                }
            } else if (child.isFile) {
                val id = child.absolutePath
                if (!out.containsKey(id)) {
                    val mime = guessMimeType(name)
                    val category = resolveCategory(name, mime, child.absolutePath)
                    out[id] = FileItem(
                        id = id,
                        name = name,
                        path = child.absolutePath,
                        uriString = null,
                        size = child.length(),
                        dateModified = child.lastModified(),
                        mimeType = mime,
                        category = category,
                        isDirectory = false,
                        isFavorite = favorites.contains(id),
                        isLocked = false
                    )
                }
            }
        }
    }

    private fun scanDocumentTree(
        treeUri: Uri,
        favorites: Set<String>,
        out: MutableMap<String, FileItem>,
        maxDepth: Int
    ) {
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return
        scanDocumentNode(treeUri, rootDocId, "/Device Folder", favorites, out, maxDepth, 0)
    }

    private fun scanDocumentNode(
        treeUri: Uri,
        parentDocId: String,
        parentDisplayPath: String,
        favorites: Set<String>,
        out: MutableMap<String, FileItem>,
        maxDepth: Int,
        depth: Int
    ) {
        if (depth > maxDepth) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unnamed" else "Unnamed"
                    if (name.startsWith(".")) continue
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "application/octet-stream" else "application/octet-stream"
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val modified = if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else System.currentTimeMillis()

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanDocumentNode(
                            treeUri = treeUri,
                            parentDocId = docId,
                            parentDisplayPath = "$parentDisplayPath/$name",
                            favorites = favorites,
                            out = out,
                            maxDepth = maxDepth,
                            depth = depth + 1
                        )
                    } else {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val key = docUri.toString()
                        if (!out.containsKey(key)) {
                            val resolvedMime = if (mime == "application/octet-stream") guessMimeType(name) else mime
                            val displayPath = "$parentDisplayPath/$name"
                            out[key] = FileItem(
                                id = key,
                                name = name,
                                path = displayPath,
                                uriString = key,
                                size = size,
                                dateModified = modified,
                                mimeType = resolvedMime,
                                category = resolveCategory(name, resolvedMime, displayPath),
                                isDirectory = false,
                                isFavorite = favorites.contains(key),
                                isLocked = false
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun loadVaultFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        ensureVaultDirectory()
        if (!vaultIndexFile.exists()) return@withContext emptyList()
        val raw = runCatching { vaultIndexFile.readText() }.getOrNull().orEmpty()
        if (raw.isBlank()) return@withContext emptyList()

        val items = mutableListOf<FileItem>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val vaultFileName = obj.getString("vaultFileName")
                val vaultFile = File(vaultDir, vaultFileName)
                if (vaultFile.exists()) {
                    val categoryName = obj.optString("category", FileCategory.DOCUMENTS.name)
                    val category = runCatching { FileCategory.valueOf(categoryName) }.getOrDefault(FileCategory.DOCUMENTS)
                    items.add(
                        FileItem(
                            id = obj.getString("id"),
                            name = obj.getString("originalName"),
                            path = vaultFile.absolutePath,
                            uriString = null,
                            size = obj.optLong("size", vaultFile.length()),
                            dateModified = obj.optLong("dateHidden", vaultFile.lastModified()),
                            mimeType = obj.optString("mimeType", "application/octet-stream"),
                            category = category,
                            isDirectory = false,
                            isFavorite = false,
                            isLocked = true,
                            vaultFileName = vaultFileName,
                            originalPath = obj.optString("originalPath", ""),
                            checksum = obj.optString("checksum", "")
                        )
                    )
                }
            }
        }
        items.sortedByDescending { it.dateModified }
    }

    private fun saveVaultIndex(items: List<FileItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalName", item.name)
                put("vaultFileName", item.vaultFileName ?: "")
                put("originalPath", item.originalPath ?: "")
                put("size", item.size)
                put("dateHidden", item.dateModified)
                put("mimeType", item.mimeType)
                put("category", item.category.name)
                put("checksum", item.checksum)
            }
            arr.put(obj)
        }
        vaultIndexFile.writeText(arr.toString())
    }

    suspend fun hideFileInVault(file: FileItem): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            ensureVaultDirectory()
            val vaultFileName = "vault_${UUID.randomUUID()}.kdat"
            val destFile = File(vaultDir, vaultFileName)

            val sourceUri = file.uriString?.let { Uri.parse(it) }
            val sourceFile = if (sourceUri == null) File(file.path) else null

            val digest = MessageDigest.getInstance("SHA-256")
            var copiedBytes = 0L

            val input = when {
                sourceUri != null -> context.contentResolver.openInputStream(sourceUri)
                    ?: throw IllegalStateException("Unable to read selected file.")
                sourceFile != null && sourceFile.exists() -> FileInputStream(sourceFile)
                else -> throw IllegalStateException("File not found on device.")
            }

            input.use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = inStream.read(buffer)
                        if (read <= 0) break
                        outStream.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copiedBytes += read
                    }
                    outStream.flush()
                }
            }

            val checksumHex = digest.digest().joinToString("") { "%02x".format(it) }

            // Delete original file so it is moved and hidden from public storage/gallery
            var removedOriginal = false
            if (sourceUri != null) {
                removedOriginal = runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
                }.getOrDefault(false)
                if (!removedOriginal) {
                    removedOriginal = runCatching {
                        context.contentResolver.delete(sourceUri, null, null) > 0
                    }.getOrDefault(false)
                }
            } else if (sourceFile != null) {
                removedOriginal = sourceFile.delete()
                if (removedOriginal) {
                    MediaScannerConnection.scanFile(context, arrayOf(sourceFile.absolutePath), null, null)
                }
            }

            val vaultItem = FileItem(
                id = "vault_${UUID.randomUUID()}",
                name = file.name,
                path = destFile.absolutePath,
                uriString = null,
                size = if (copiedBytes > 0) copiedBytes else file.size,
                dateModified = System.currentTimeMillis(),
                mimeType = file.mimeType,
                category = file.category,
                isDirectory = false,
                isFavorite = false,
                isLocked = true,
                vaultFileName = vaultFileName,
                originalPath = if (removedOriginal) file.path else "${file.path} (Copy protected by OS)",
                checksum = "SHA-256: $checksumHex"
            )

            val existing = loadVaultFiles().toMutableList()
            existing.add(0, vaultItem)
            saveVaultIndex(existing)

            vaultItem
        }
    }

    suspend fun hideUriDirectlyInVault(uri: Uri): Result<FileItem> = withContext(Dispatchers.IO) {
        val meta = queryUriMetadata(uri)
        hideFileInVault(meta)
    }

    suspend fun importUriToLocalWorkspace(uri: Uri): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val localWorkspace = File(context.filesDir, "kabinet_local")
            if (!localWorkspace.exists()) localWorkspace.mkdirs()

            val meta = queryUriMetadata(uri)
            val targetFile = uniqueFileInDir(localWorkspace, meta.name)

            context.contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(targetFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Cannot open selected file.")

            meta.copy(
                id = targetFile.absolutePath,
                path = targetFile.absolutePath,
                uriString = null,
                size = targetFile.length(),
                dateModified = targetFile.lastModified()
            )
        }
    }

    suspend fun restoreFromVault(vaultItem: FileItem): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val vaultFile = File(vaultDir, vaultItem.vaultFileName ?: "")
            if (!vaultFile.exists()) {
                throw IllegalStateException("Hidden file payload missing.")
            }

            // Try restoring to public Downloads/Kabinet_Restored, fallback to app external files dir
            val downloadsDir = runCatching {
                val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val target = File(pub, "Kabinet_Restored")
                if (!target.exists()) target.mkdirs()
                if (target.canWrite()) target else null
            }.getOrNull()

            val restoreDir = downloadsDir ?: run {
                val appRestore = context.getExternalFilesDir("Restored") ?: File(context.filesDir, "kabinet_local")
                if (!appRestore.exists()) appRestore.mkdirs()
                appRestore
            }

            val restoredFile = uniqueFileInDir(restoreDir, vaultItem.name)
            FileInputStream(vaultFile).use { inStream ->
                FileOutputStream(restoredFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            vaultFile.delete()
            val updated = loadVaultFiles().filter { it.id != vaultItem.id }
            saveVaultIndex(updated)

            MediaScannerConnection.scanFile(context, arrayOf(restoredFile.absolutePath), arrayOf(vaultItem.mimeType), null)
            restoredFile.absolutePath
        }
    }

    suspend fun deleteFile(file: FileItem): Boolean = withContext(Dispatchers.IO) {
        if (file.isLocked) {
            val vaultFile = File(vaultDir, file.vaultFileName ?: "")
            val deleted = !vaultFile.exists() || vaultFile.delete()
            val updated = loadVaultFiles().filter { it.id != file.id }
            saveVaultIndex(updated)
            deleted
        } else {
            val uri = file.uriString?.let { Uri.parse(it) }
            if (uri != null) {
                val deleted = runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                }.getOrDefault(false)
                deleted || runCatching { context.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
            } else {
                val target = File(file.path)
                val deleted = target.delete()
                if (deleted) {
                    MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                }
                deleted
            }
        }
    }

    suspend fun readTextPreview(file: FileItem, maxBytes: Int = 32 * 1024): String = withContext(Dispatchers.IO) {
        runCatching {
            val uri = file.uriString?.let { Uri.parse(it) }
            val input = if (uri != null) {
                context.contentResolver.openInputStream(uri)
            } else {
                val localFile = File(file.path)
                if (localFile.exists() && localFile.canRead()) FileInputStream(localFile) else null
            } ?: return@runCatching ""

            input.use { stream ->
                val buffer = ByteArray(maxBytes)
                val read = stream.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read, Charsets.UTF_8)
            }
        }.getOrDefault("")
    }

    suspend fun computeSha256(file: FileItem): String = withContext(Dispatchers.IO) {
        if (file.checksum.isNotBlank()) return@withContext file.checksum
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val uri = file.uriString?.let { Uri.parse(it) }
            val input = if (uri != null) {
                context.contentResolver.openInputStream(uri)
            } else {
                val localFile = File(file.path)
                if (localFile.exists() && localFile.canRead()) FileInputStream(localFile) else null
            } ?: return@runCatching "Unavailable"

            input.use { stream ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            "SHA-256: " + digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("Unavailable")
    }

    fun queryUriMetadata(uri: Uri): FileItem {
        val resolver: ContentResolver = context.contentResolver
        var displayName = "Selected_File"
        var size = 0L

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        displayName = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        }

        val mime = resolver.getType(uri) ?: guessMimeType(displayName)
        return FileItem(
            id = uri.toString(),
            name = displayName,
            path = uri.toString(),
            uriString = uri.toString(),
            size = size,
            dateModified = System.currentTimeMillis(),
            mimeType = mime,
            category = resolveCategory(displayName, mime, uri.toString())
        )
    }

    private fun uniqueFileInDir(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dotIdx = fileName.lastIndexOf('.')
        val base = if (dotIdx > 0) fileName.substring(0, dotIdx) else fileName
        val ext = if (dotIdx > 0) fileName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$counter$ext")
            counter++
        }
        return candidate
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "apk" -> "application/vnd.android.package-archive"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            "md", "kt", "kts", "java", "py", "js", "ts", "sh", "json", "xml", "yml", "yaml", "csv", "log" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun resolveCategory(name: String, mime: String, path: String): FileCategory {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val lowerMime = mime.lowercase(Locale.ROOT)
        return when {
            lowerMime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "svg") -> FileCategory.IMAGES
            lowerMime.startsWith("video/") || ext in setOf("mp4", "mkv", "mov", "avi", "webm", "3gp") -> FileCategory.VIDEOS
            lowerMime.startsWith("audio/") || ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus") -> FileCategory.AUDIO
            ext == "apk" || lowerMime == "application/vnd.android.package-archive" -> FileCategory.APKS
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || lowerMime.contains("zip") || lowerMime.contains("archive") -> FileCategory.ARCHIVES
            else -> {
                if (path.contains("/Download", ignoreCase = true) && lowerMime == "application/octet-stream") {
                    FileCategory.DOWNLOADS
                } else {
                    FileCategory.DOCUMENTS
                }
            }
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var unitIndex = 0
            while (value >= 1024.0 && unitIndex < units.lastIndex) {
                value /= 1024.0
                unitIndex++
            }
            return if (unitIndex == 0) {
                "${bytes} B"
            } else {
                String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
            }
        }

        fun isTextPreviewable(file: FileItem): Boolean {
            val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return file.mimeType.startsWith("text/") || ext in setOf(
                "txt", "md", "json", "xml", "csv", "log", "sh", "kt", "kts", "java", "py", "js", "ts", "html", "css", "yaml", "yml", "ini", "conf", "properties"
            )
        }
    }
}
