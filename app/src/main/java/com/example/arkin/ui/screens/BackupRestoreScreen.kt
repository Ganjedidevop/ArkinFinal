package com.example.arkin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentGreenBg
import com.example.ui.theme.AccentGreenText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.ArkinPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(viewModel: ArkinViewModel) {
    val profile by viewModel.studentProfile.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val isRestoring by viewModel.isRestoringBackup.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val lastFirestoreSyncTime by viewModel.lastFirestoreSyncTime.collectAsState()

    var generatedBackupJson by remember { mutableStateOf<String?>(null) }
    var restoreInputJson by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val lastBackupText = if (profile?.lastBackupDate != null && profile!!.lastBackupDate > 0) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(profile!!.lastBackupDate))
    } else {
        "Never backed up"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .testTag("backup_restore_screen_root")
    ) {
        AppHeader(
            title = "Backup & Restore",
            onBackClick = { viewModel.goBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Firebase Firestore Cloud Backend Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Firebase Firestore Cloud",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (firebaseUser != null) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (firebaseUser != null) "CONNECTED" else "OFFLINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (firebaseUser != null) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Project: arkin-22b82",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ArkinPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "App ID: 1:605569501594:android:077f691f06780f24e19223",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (firebaseUser != null) "Account: ${firebaseUser?.email ?: firebaseUser?.displayName ?: firebaseUser?.uid}" else "Not authenticated in Firebase Auth",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncToFirestore() },
                            enabled = !isFirestoreSyncing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isFirestoreSyncing) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Sync to Cloud ☁️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncFromFirestore() },
                            enabled = !isFirestoreSyncing,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Cloud 📥", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Storage Status Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentBlueBg,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = AccentBlueText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Local & Cloud Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Last Backup: $lastBackupText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cloud Sync Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Automatic Cloud Synchronization",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (profile?.cloudSyncEnabled == true) "Active: Backed up to student account" else "Disabled: Stored locally only",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = profile?.cloudSyncEnabled ?: true,
                            onCheckedChange = { viewModel.toggleCloudSync() },
                            colors = SwitchDefaults.colors(checkedThumbColor = ArkinPrimary)
                        )
                    }
                }
            }

            // Create Backup Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = ArkinPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Export Manual Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Generates a validated JSON backup containing all your notes, tasks, assignments, timetable classes, exam results, and study settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.createBackupJson { json ->
                                generatedBackupJson = json
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Backup Now 📦", fontWeight = FontWeight.Bold)
                    }

                    if (generatedBackupJson != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Backup Ready (${generatedBackupJson!!.length} bytes)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedBackupJson!!))
                                            viewModel.showToast("Backup JSON copied to clipboard!")
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy JSON", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Restore Backup Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Restore Database from Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Paste a previously exported Arkin backup JSON below to preview and safely merge or restore your data.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = restoreInputJson,
                        onValueChange = {
                            restoreInputJson = it
                            if (it.isNotBlank()) {
                                viewModel.previewBackupJson(it)
                            }
                        },
                        label = { Text("Paste Backup JSON") },
                        placeholder = { Text("{\n  \"app\": \"Arkin\",\n  \"version\": 2 ...\n}") },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Backup Validation & Preview
                    if (backupPreview != null) {
                        val prev = backupPreview!!
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (prev.isValid) AccentGreenBg else Color(0xFFFEE2E2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (prev.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (prev.isValid) AccentGreenText else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (prev.isValid) "Valid Arkin Backup (Version ${prev.version})" else "Invalid or Corrupted Backup File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (prev.isValid) AccentGreenText else Color(0xFFDC2626)
                                    )
                                }

                                if (prev.isValid) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "• Notes: ${prev.notesCount}  • Tasks: ${prev.tasksCount}\n• Assignments: ${prev.homeworkCount}  • Classes: ${prev.timetableCount}\n• Exam Records: ${prev.examResultsCount}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (prev.isValid) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.restoreBackup(prev.rawJson, replaceAll = false) },
                                    enabled = !isRestoring,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Safe Merge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.restoreBackup(prev.rawJson, replaceAll = true) },
                                    enabled = !isRestoring,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isRestoring) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text("Full Restore", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
