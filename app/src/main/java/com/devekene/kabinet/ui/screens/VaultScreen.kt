package com.devekene.kabinet.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.devekene.kabinet.data.FileRepository
import com.devekene.kabinet.model.FileCategory
import com.devekene.kabinet.model.FileItem

@Composable
fun VaultScreen(
    vaultFiles: List<FileItem>,
    isVaultUnlocked: Boolean,
    onUnlockVault: (Boolean) -> Unit,
    onHideFilesFromDevice: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onRestoreFile: (FileItem) -> Unit,
    onDeleteVaultFile: (FileItem) -> Unit,
    onPropertiesClick: (FileItem) -> Unit
) {
    val context = LocalContext.current
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isVaultUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Kabinet Secure Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hidden videos, photos, music, and documents are stored privately inside Kabinet. Authenticate with biometrics to unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                authErrorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        authErrorMessage = null
                        val activity = context as? FragmentActivity
                        if (activity == null) {
                            authErrorMessage = "Biometric prompt requires an active window."
                            return@Button
                        }

                        val biometricManager = BiometricManager.from(context)
                        val authenticatorsWithCredential =
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        val biometricOnly = BiometricManager.Authenticators.BIOMETRIC_WEAK

                        val canWithCredential = biometricManager.canAuthenticate(authenticatorsWithCredential)
                        val canBiometricOnly = biometricManager.canAuthenticate(biometricOnly)

                        val executor = ContextCompat.getMainExecutor(context)
                        val biometricPrompt = BiometricPrompt(
                            activity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    onUnlockVault(true)
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    ) {
                                        authErrorMessage = errString.toString()
                                    }
                                }
                            }
                        )

                        when {
                            canWithCredential == BiometricManager.BIOMETRIC_SUCCESS -> {
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Kabinet Vault")
                                    .setSubtitle("Verify your fingerprint, face, or device screen lock")
                                    .setAllowedAuthenticators(authenticatorsWithCredential)
                                    .build()
                                biometricPrompt.authenticate(promptInfo)
                            }
                            canBiometricOnly == BiometricManager.BIOMETRIC_SUCCESS -> {
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Kabinet Vault")
                                    .setSubtitle("Authenticate using your biometric credential")
                                    .setNegativeButtonText("Cancel")
                                    .build()
                                biometricPrompt.authenticate(promptInfo)
                            }
                            else -> {
                                authErrorMessage = "No fingerprint or screen lock is enrolled on this device yet. Set up biometrics in device Settings to protect the Vault."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_vault_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlocked", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secure Vault (${vaultFiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { onUnlockVault(false) },
                        modifier = Modifier.testTag("lock_vault_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action banner to pick & hide videos, photos, music, or any file directly into the Vault
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Files in Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Move videos, photos, music, or documents out of public storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onHideFilesFromDevice,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("vault_hide_files_button")
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hide")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (vaultFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderSpecial,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No hidden files in Secure Vault",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap Hide above or use the menu on any file to move it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vaultFiles, key = { it.id }) { file ->
                            VaultFileRow(
                                file = file,
                                onClick = { onFileClick(file) },
                                onRestoreClick = { onRestoreFile(file) },
                                onPropertiesClick = { onPropertiesClick(file) },
                                onDeleteClick = { onDeleteVaultFile(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultFileRow(
    file: FileItem,
    onClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onPropertiesClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                    maxLines = 1
                )
                Text(
                    text = "${FileRepository.formatBytes(file.size)} • Hidden in Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Vault File Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Unhide & Restore") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                        onClick = { showMenu = false; onRestoreClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = { showMenu = false; onPropertiesClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanently") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}
