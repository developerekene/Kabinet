package com.example.data

import com.example.model.FileCategory
import com.example.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class FileRepository {

    private val initialFiles = listOf(
        FileItem(
            id = "1",
            name = "Vacation_Sunset_2026.jpg",
            path = "/storage/emulated/0/DCIM/Camera/Vacation_Sunset_2026.jpg",
            size = 4250000,
            dateModified = System.currentTimeMillis() - 3600000 * 2,
            mimeType = "image/jpeg",
            category = FileCategory.IMAGES,
            isFavorite = true,
            isCloudSynced = true,
            checksum = "SHA256: 4a2f8c9e1b3d5a7f..."
        ),
        FileItem(
            id = "2",
            name = "Project_Architecture_Design.pdf",
            path = "/storage/emulated/0/Documents/Project_Architecture_Design.pdf",
            size = 12400000,
            dateModified = System.currentTimeMillis() - 3600000 * 24,
            mimeType = "application/pdf",
            category = FileCategory.DOCUMENTS,
            isFavorite = true,
            isCloudSynced = true,
            contentSnippet = "Kabinet File Explorer Architecture v2.0\n1. Overview\n2. Security & Biometrics\n3. Cloud Sync Protocol"
        ),
        FileItem(
            id = "3",
            name = "Roadtrip_Cinematic_4K.mp4",
            path = "/storage/emulated/0/Movies/Roadtrip_Cinematic_4K.mp4",
            size = 345000000,
            dateModified = System.currentTimeMillis() - 3600000 * 48,
            mimeType = "video/mp4",
            category = FileCategory.VIDEOS,
            isFavorite = false,
            isCloudSynced = false
        ),
        FileItem(
            id = "4",
            name = "LoFi_Chill_Study_Beat.mp3",
            path = "/storage/emulated/0/Music/LoFi_Chill_Study_Beat.mp3",
            size = 8900000,
            dateModified = System.currentTimeMillis() - 3600000 * 12,
            mimeType = "audio/mpeg",
            category = FileCategory.AUDIO,
            isFavorite = true,
            isCloudSynced = true
        ),
        FileItem(
            id = "5",
            name = "Kabinet_v1.2.3_release.apk",
            path = "/storage/emulated/0/Download/Kabinet_v1.2.3_release.apk",
            size = 28400000,
            dateModified = System.currentTimeMillis() - 3600000 * 5,
            mimeType = "application/vnd.android.package-archive",
            category = FileCategory.APKS,
            isFavorite = false,
            isCloudSynced = true
        ),
        FileItem(
            id = "6",
            name = "System_Backup_Config.zip",
            path = "/storage/emulated/0/Download/System_Backup_Config.zip",
            size = 54200000,
            dateModified = System.currentTimeMillis() - 3600000 * 72,
            mimeType = "application/zip",
            category = FileCategory.ARCHIVES,
            isFavorite = false,
            isCloudSynced = false
        ),
        FileItem(
            id = "7",
            name = "Passport_Scan_Confidential.pdf",
            path = "/storage/emulated/0/Documents/Secure/Passport_Scan_Confidential.pdf",
            size = 2100000,
            dateModified = System.currentTimeMillis() - 3600000 * 100,
            mimeType = "application/pdf",
            category = FileCategory.VAULT,
            isFavorite = false,
            isLocked = true,
            isCloudSynced = false
        ),
        FileItem(
            id = "8",
            name = "Financial_Ledger_2026.xlsx",
            path = "/storage/emulated/0/Documents/Financial_Ledger_2026.xlsx",
            size = 1500000,
            dateModified = System.currentTimeMillis() - 3600000 * 10,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            category = FileCategory.DOCUMENTS,
            isFavorite = true,
            isLocked = true,
            isCloudSynced = true
        ),
        FileItem(
            id = "9",
            name = "Server_Setup_Script.sh",
            path = "/storage/emulated/0/Download/Server_Setup_Script.sh",
            size = 4200,
            dateModified = System.currentTimeMillis() - 3600000 * 3,
            mimeType = "text/x-shellscript",
            category = FileCategory.DOCUMENTS,
            isFavorite = false,
            isCloudSynced = true,
            contentSnippet = "#!/bin/bash\necho 'Initializing Kabinet Secure Cloud Sync...'\nservice mongod start\necho 'Ready.'"
        ),
        FileItem(
            id = "10",
            name = "Family_Reunion_2025.png",
            path = "/storage/emulated/0/Pictures/Family_Reunion_2025.png",
            size = 6100000,
            dateModified = System.currentTimeMillis() - 3600000 * 200,
            mimeType = "image/png",
            category = FileCategory.IMAGES,
            isFavorite = false,
            isCloudSynced = true
        )
    )

    private val _files = MutableStateFlow<List<FileItem>>(initialFiles)
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    fun deleteFile(id: String) {
        _files.value = _files.value.filter { it.id != id }
    }

    fun toggleFavorite(id: String) {
        _files.value = _files.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun toggleLock(id: String) {
        _files.value = _files.value.map {
            if (it.id == id) it.copy(isLocked = !it.isLocked) else it
        }
    }

    fun toggleCloudSync(id: String) {
        _files.value = _files.value.map {
            if (it.id == id) it.copy(isCloudSynced = !it.isCloudSynced) else it
        }
    }

    fun syncAllToCloud() {
        _files.value = _files.value.map { it.copy(isCloudSynced = true) }
    }

    fun addFile(item: FileItem) {
        _files.value = listOf(item) + _files.value
    }
}
