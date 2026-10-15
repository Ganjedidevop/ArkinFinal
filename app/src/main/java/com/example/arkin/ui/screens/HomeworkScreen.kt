package com.example.arkin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.model.AppScreen
import com.example.arkin.model.DocUploadType
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.getCategoryColor
import com.example.arkin.util.DocumentTextExtractor
import com.example.arkin.util.FileViewerHelper
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.ArkinPrimary

@Composable
fun HomeworkScreen(viewModel: ArkinViewModel) {
    val context = LocalContext.current
    val homeworkList by viewModel.allHomework.collectAsState()
    var isManualAddDialogOpen by remember { mutableStateOf(false) }
    var initialAttachmentUri by remember { mutableStateOf<Uri?>(null) }

    // File Picker for Upload Assignment Button - Attaches file to homework item directly without switching screens
    val assignmentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            initialAttachmentUri = uri
            isManualAddDialogOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("homework_screen_root")
    ) {
        AppHeader(
            title = "Homework & Assignments",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Explicit AI Document Scanner Option
                    Surface(
                        onClick = {
                            viewModel.setUploadDocType(DocUploadType.ASSIGNMENT)
                            viewModel.navigateTo(AppScreen.AI_DOC_ORGANIZER)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFEDD5),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("homework_ai_scan_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text("🪄 AI Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                    }

                    Surface(
                        onClick = {
                            initialAttachmentUri = null
                            isManualAddDialogOpen = true
                        },
                        shape = CircleShape,
                        color = Color(0xFFFFEDD5),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("homework_manual_add_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manual Add",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        )

        // Action Banner: Direct File Attachment OR Manual Entry (Never navigates unexpectedly)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        assignmentFilePicker.launch(
                            arrayOf("application/pdf", "image/*", "*/*")
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("upload_assignment_file_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Attach File 📄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        initialAttachmentUri = null
                        isManualAddDialogOpen = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("create_assignment_manual_button")
                ) {
                    Text("+ Manual ✏️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (homeworkList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Book,
                                contentDescription = "No Homework",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No pending homework!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Attach an assignment file or create an assignment entry manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                assignmentFilePicker.launch(
                                    arrayOf("application/pdf", "image/*", "*/*")
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                        ) {
                            Text("Attach File")
                        }
                        OutlinedButton(
                            onClick = {
                                initialAttachmentUri = null
                                isManualAddDialogOpen = true
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Manual Entry")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(homeworkList, key = { it.id }) { hw ->
                    InteractiveHomeworkCard(
                        hw = hw,
                        onClick = { viewModel.openAssignmentDetail(hw) },
                        onToggle = { viewModel.toggleHomework(hw) },
                        onOpenAttachment = {
                            FileViewerHelper.openDocument(context, hw.attachmentUri, hw.attachmentName)
                        }
                    )
                }
            }
        }
    }

    if (isManualAddDialogOpen) {
        ManualAddAssignmentDialog(
            initialUri = initialAttachmentUri,
            onDismiss = {
                isManualAddDialogOpen = false
                initialAttachmentUri = null
            },
            onAdd = { subject, title, dueDate, dueTime, description, teacher, priority, attUri, attName, attSize ->
                viewModel.addHomework(
                    subject = subject,
                    title = title,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    description = description,
                    teacherName = teacher,
                    priority = priority,
                    attachmentUri = attUri,
                    attachmentName = attName,
                    attachmentSize = attSize
                )
                isManualAddDialogOpen = false
                initialAttachmentUri = null
            }
        )
    }
}

@Composable
private fun InteractiveHomeworkCard(
    hw: HomeworkEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onOpenAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColor(hw.colorKey)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("homework_card_${hw.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bgColor
                    ) {
                        Text(
                            text = hw.subject,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = hw.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = hw.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hw.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (hw.isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                // Attachment preview pill if document is attached
                if (!hw.attachmentName.isNullOrBlank()) {
                    Surface(
                        onClick = onOpenAttachment,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3E8FF),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = "Attachment",
                                tint = ArkinPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hw.attachmentName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ArkinPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Due",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Due: ${hw.dueDate} (${hw.dueTime})",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!hw.aiDraftAnswer.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✨ AI Solved",
                            fontSize = 10.sp,
                            color = ArkinPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (hw.isDone) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("homework_check_${hw.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = if (hw.isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualAddAssignmentDialog(
    initialUri: Uri? = null,
    onDismiss: () -> Unit,
    onAdd: (
        subject: String,
        title: String,
        dueDate: String,
        dueTime: String,
        description: String,
        teacher: String,
        priority: String,
        attachmentUri: String?,
        attachmentName: String?,
        attachmentSize: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("Tomorrow") }
    var dueTime by remember { mutableStateOf("11:59 PM") }
    var priority by remember { mutableStateOf("Medium") }

    var attachedUri by remember { mutableStateOf(initialUri) }
    var attachedFileName by remember {
        mutableStateOf(
            if (initialUri != null) DocumentTextExtractor.getFileMetadata(context, initialUri).first else null
        )
    }
    var attachedFileSize by remember {
        mutableStateOf(
            if (initialUri != null) {
                val size = DocumentTextExtractor.getFileMetadata(context, initialUri).second
                DocumentTextExtractor.formatFileSize(size)
            } else null
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedUri = uri
            val (name, size) = DocumentTextExtractor.getFileMetadata(context, uri)
            attachedFileName = name
            attachedFileSize = DocumentTextExtractor.formatFileSize(size)
            if (title.isBlank()) {
                title = name.substringBeforeLast('.')
            }
        }
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .testTag("manual_assignment_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Assignment 📝",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Attached Document Section
                if (attachedFileName != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = "Attachment",
                                tint = ArkinPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachedFileName ?: "Attached File",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = attachedFileSize ?: "",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    attachedUri = null
                                    attachedFileName = null
                                    attachedFileSize = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "*/*")) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Attach Assignment Document / PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Course / Subject *") },
                    placeholder = { Text("e.g. Database Systems, OS, Math") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g. Relational Algebra Problem Set 3") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Instructions") },
                    placeholder = { Text("Type questions or notes given by professor...") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher / Faculty (Optional)") },
                    placeholder = { Text("e.g. Prof. Verma") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date *") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Due Time *") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && title.isNotBlank()) {
                                onAdd(
                                    subject,
                                    title,
                                    dueDate,
                                    dueTime,
                                    description,
                                    teacher,
                                    priority,
                                    attachedUri?.toString(),
                                    attachedFileName,
                                    attachedFileSize
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Save Assignment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
