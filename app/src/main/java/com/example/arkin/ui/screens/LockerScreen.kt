package com.example.arkin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.arkin.data.model.VaultItemEntity
import com.example.arkin.data.security.VaultSecurityManager
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.ConfirmationDialog
import com.example.arkin.ui.components.PinKeypad
import com.example.arkin.ui.components.SecurePinChangeDialog
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentLockerBg
import com.example.ui.theme.AccentLockerText
import com.example.ui.theme.ArkinPrimary

@Composable
fun LockerScreen(viewModel: ArkinViewModel) {
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val enteredPin by viewModel.enteredPin.collectAsState()
    val isPinError by viewModel.isPinError.collectAsState()
    val allVaultItems by viewModel.allVaultItems.collectAsState()
    val context = LocalContext.current

    val profile by viewModel.studentProfile.collectAsState()
    var isChangePinDialogOpen by remember { mutableStateOf(false) }
    var isRecoveryDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("locker_screen_root")
    ) {
        AppHeader(
            title = "Private Locker",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isChangePinDialogOpen = true },
                        modifier = Modifier.testTag("locker_change_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "Change PIN",
                            tint = AccentLockerText
                        )
                    }
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = AccentLockerText
                            )
                        }
                    }
                }
            }
        )

        if (!isUnlocked) {
            VaultLockedView(
                context = context,
                enteredPin = enteredPin,
                isPinError = isPinError,
                onDigit = { viewModel.onPinDigit(it) },
                onClear = { viewModel.onPinClear() },
                onBackspace = { viewModel.onPinBackspace() },
                onForgotPinClick = { isRecoveryDialogOpen = true }
            )
        } else {
            VaultUnlockedView(
                viewModel = viewModel,
                vaultItems = allVaultItems,
                context = context
            )
        }
    }

    if (isChangePinDialogOpen) {
        SecurePinChangeDialog(
            onDismiss = { isChangePinDialogOpen = false },
            onPinSuccessfullyChanged = { newPin ->
                viewModel.changeLockerPin(newPin)
                isChangePinDialogOpen = false
            }
        )
    }

    if (isRecoveryDialogOpen) {
        VaultRecoveryDialog(
            context = context,
            profileEmail = profile?.email,
            onDismiss = { isRecoveryDialogOpen = false },
            onPinReset = { newPin ->
                viewModel.changeLockerPin(newPin)
                isRecoveryDialogOpen = false
                viewModel.onPinClear()
                viewModel.showToast("PIN reset successfully! Please unlock your vault.")
            }
        )
    }
}

@Composable
private fun VaultLockedView(
    context: Context,
    enteredPin: String,
    isPinError: Boolean,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onForgotPinClick: () -> Unit
) {
    val isConfigured = remember { VaultSecurityManager.isPinSet(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = AccentLockerBg,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Vault Security",
                    tint = AccentLockerText,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isConfigured) "Enter 4-Digit PIN" else "Set Your 4-Digit PIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isConfigured) {
                "Enter your secure PIN to decrypt and access confidential files."
            } else {
                "Create a 4-digit PIN to activate hardware AES-256 vault encryption."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
        )

        PinKeypad(
            enteredPin = enteredPin,
            isPinError = isPinError,
            onDigitClick = onDigit,
            onClearClick = onClear,
            onBackspaceClick = onBackspace
        )

        if (isConfigured) {
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = onForgotPinClick,
                modifier = Modifier.testTag("forgot_pin_button")
            ) {
                Text(
                    text = "Forgot PIN? Recover Vault 🔑",
                    color = ArkinPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun VaultUnlockedView(
    viewModel: ArkinViewModel,
    vaultItems: List<VaultItemEntity>,
    context: Context
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var pendingAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAttachmentType by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<VaultItemEntity?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingAttachmentUri = uri
            pendingAttachmentType = "IMAGE"
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingAttachmentUri = uri
            pendingAttachmentType = "DOCUMENT"
        }
    }

    val filteredItems = vaultItems.filter {
        when (selectedFilter) {
            "Notes" -> it.fileType == "TEXT"
            "Images" -> it.fileType == "IMAGE"
            "Documents" -> it.fileType == "DOCUMENT"
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Vault Unlocked Header Banner
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Unlocked",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vault Decrypted & Active",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "AES-256 hardware encryption protecting student documents & credentials.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row: Note, Photo, Document
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddNoteDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                modifier = Modifier.weight(1f).height(46.dp).testTag("add_vault_note_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                modifier = Modifier.weight(1f).height(46.dp).testTag("add_vault_photo_btn")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    documentPickerLauncher.launch(
                        arrayOf("application/pdf", "text/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                modifier = Modifier.weight(1f).height(46.dp).testTag("add_vault_doc_btn")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Doc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Notes", "Images", "Documents").forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Items List or Empty State
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No vault items stored",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Encrypted private files and notes will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    VaultItemCard(
                        item = item,
                        context = context,
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Add Text Item Dialog
    if (showAddNoteDialog) {
        AddTextItemDialog(
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, category, content ->
                viewModel.addVaultItem(title, category, content)
                showAddNoteDialog = false
            }
        )
    }

    // Add Attachment Dialog
    pendingAttachmentUri?.let { uri ->
        val fileType = pendingAttachmentType ?: "DOCUMENT"
        AddAttachmentDetailsDialog(
            fileType = fileType,
            onDismiss = {
                pendingAttachmentUri = null
                pendingAttachmentType = null
            },
            onSave = { title, category, customName ->
                viewModel.addVaultAttachment(title, category, uri, fileType, customName)
                pendingAttachmentUri = null
                pendingAttachmentType = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        ConfirmationDialog(
            title = "Delete Vault Item?",
            message = "Are you sure you want to permanently delete '${item.title}'? This action cannot be undone.",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteVaultItem(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItemEntity,
    context: Context,
    onDeleteClick: () -> Unit
) {
    val icon = when (item.fileType) {
        "IMAGE" -> Icons.Default.Image
        "DOCUMENT" -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.Note
    }

    val iconBg = when (item.fileType) {
        "IMAGE" -> Color(0xFFCCFBF1)
        "DOCUMENT" -> Color(0xFFEDE9FE)
        else -> Color(0xFFEFF6FF)
    }

    val iconTint = when (item.fileType) {
        "IMAGE" -> Color(0xFF0F766E)
        "DOCUMENT" -> Color(0xFF6D28D9)
        else -> Color(0xFF1D4ED8)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth().testTag("vault_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iconBg,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.category,
                                fontSize = 11.sp,
                                color = ArkinPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.fileSize > 0) {
                                Text(
                                    text = " • ${formatBytes(item.fileSize)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFDC2626)
                    )
                }
            }

            if (item.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.content,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!item.attachmentUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        openEncryptedAttachment(context, item)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Decrypt & View (${item.attachmentName ?: "File"})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun openEncryptedAttachment(context: Context, item: VaultItemEntity) {
    val relPath = item.attachmentUri ?: return
    val fileName = item.attachmentName ?: "attachment"
    val tempFile = VaultSecurityManager.decryptToTempFile(context, relPath, fileName) ?: return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType ?: "application/octet-stream")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open Vault File"))
    } catch (_: Exception) {
        // Fallback if no specific handler available
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "$bytes B"
    }
}

@Composable
private fun AddTextItemDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Private Note") }
    var content by remember { mutableStateOf("") }

    val categories = listOf("Private Note", "ID Document", "Password Hint", "Secret Key", "Certificate")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Private Note 🔒", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. WiFi Password / Backup Key") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selector
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (category == cat) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (category == cat) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Secret Content") },
                    placeholder = { Text("Enter confidential details...") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title, category, content) },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Save Note", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAttachmentDetailsDialog(
    fileType: String,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, customName: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (fileType == "IMAGE") "ID Document" else "Certificate") }

    val categories = if (fileType == "IMAGE") {
        listOf("ID Document", "Admit Card", "Certificate", "Private Photo")
    } else {
        listOf("Certificate", "Mark Sheet", "Admit Card", "Financial Doc", "Academic Record")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Secure $fileType 🔒", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Attachment Title") },
                    placeholder = { Text(if (fileType == "IMAGE") "e.g. Student ID Card" else "e.g. Semester 3 Grade Sheet") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (category == cat) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (category == cat) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title, category, null) },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Encrypt & Save 🔒", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultRecoveryDialog(
    context: Context,
    profileEmail: String?,
    onDismiss: () -> Unit,
    onPinReset: (String) -> Unit
) {
    val configuredQuestion = remember { VaultSecurityManager.getSecurityQuestion(context) }
    val configuredEmail = remember { VaultSecurityManager.getRecoveryEmail(context) }
    val effectiveEmail = configuredEmail ?: profileEmail

    var selectedMode by remember {
        mutableStateOf(if (!configuredQuestion.isNullOrBlank()) "question" else "email")
    }
    var answerInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ArkinPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = ArkinPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isVerified) "Create New PIN" else "Vault Recovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (!isVerified) {
                    // Mode Toggle (Question vs Email)
                    if (!configuredQuestion.isNullOrBlank() && !effectiveEmail.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    selectedMode = "question"
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedMode == "question") ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Security Question",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == "question") Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Surface(
                                onClick = {
                                    selectedMode = "email"
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedMode == "email") ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Recovery Email",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == "email") Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (selectedMode == "question" && !configuredQuestion.isNullOrBlank()) {
                        Text(
                            text = "Answer your secret question to verify vault ownership:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "❓ $configuredQuestion",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = {
                                answerInput = it
                                errorMessage = null
                            },
                            label = { Text("Your Answer") },
                            placeholder = { Text("Enter secret answer") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (VaultSecurityManager.verifySecurityAnswer(context, answerInput)) {
                                    isVerified = true
                                    errorMessage = null
                                } else {
                                    errorMessage = "Incorrect answer. Please try again."
                                }
                            },
                            enabled = answerInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Answer →", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Email Mode
                        Text(
                            text = "Enter your registered student or recovery email:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            label = { Text("Registered Email") },
                            placeholder = { Text("student@example.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (VaultSecurityManager.verifyRecoveryEmail(context, emailInput, profileEmail)) {
                                    isVerified = true
                                    errorMessage = null
                                } else {
                                    errorMessage = "Email does not match our records. Please verify spelling."
                                }
                            },
                            enabled = emailInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Email →", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // User is verified! Set New PIN
                    Text(
                        text = "Identity Verified! Set your new 4-digit PIN:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                newPinInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("New 4-Digit PIN") },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                confirmPinInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Confirm New PIN") },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newPinInput.length != 4) {
                                errorMessage = "PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (newPinInput != confirmPinInput) {
                                errorMessage = "PINs do not match."
                                return@Button
                            }
                            val success = VaultSecurityManager.resetPinWithRecovery(context, newPinInput)
                            if (success) {
                                onPinReset(newPinInput)
                            } else {
                                errorMessage = "Failed to save new PIN."
                            }
                        },
                        enabled = newPinInput.length == 4 && confirmPinInput.length == 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset & Unlock Vault 🔓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
