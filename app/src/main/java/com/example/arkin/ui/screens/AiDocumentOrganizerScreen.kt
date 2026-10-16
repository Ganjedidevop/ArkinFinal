package com.example.arkin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.model.DocProcessingState
import com.example.arkin.model.DocUploadType
import com.example.arkin.model.ParsedTimetableClass
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.MarkdownText
import com.example.arkin.util.DocumentTextExtractor
import com.example.arkin.util.FileViewerHelper
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.ArkinPrimary

@Composable
fun AiDocumentOrganizerScreen(viewModel: ArkinViewModel) {
    val context = LocalContext.current
    val selectedDocType by viewModel.uploadedDocType.collectAsState()
    val uploadedDocName by viewModel.uploadedDocName.collectAsState()
    val selectedDocUri by viewModel.selectedDocUri.collectAsState()
    val docFileSize by viewModel.docFileSize.collectAsState()
    val docProcessingState by viewModel.docProcessingState.collectAsState()
    val docProcessingStatusText by viewModel.docProcessingStatusText.collectAsState()
    val docProcessingError by viewModel.docProcessingError.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingDoc.collectAsState()
    val analysisResult by viewModel.analyzedDocResult.collectAsState()
    val parsedClasses by viewModel.parsedClasses.collectAsState()

    var customNotes by remember { mutableStateOf("") }
    var classBeingEdited by remember { mutableStateOf<Pair<Int, ParsedTimetableClass>?>(null) }
    var isAddingClass by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Activity Result Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onDocumentSelected(uri)
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onDocumentSelected(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .testTag("ai_doc_organizer_root")
    ) {
        AppHeader(
            title = "AI File & Doc Organizer",
            onBackClick = { viewModel.goBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Introductory AI Card
            IntroductoryAiBanner()

            Text(
                text = "Select Document Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Document Type Selector (Horizontally scrollable, non-wrapping)
            DocumentTypeSelectorRow(
                selectedDocType = selectedDocType,
                onSelectType = { viewModel.setUploadDocType(it) }
            )

            // File Upload & Selection Section
            FileUploadCard(
                uploadedDocName = uploadedDocName,
                uploadedDocSize = docFileSize,
                selectedDocUri = selectedDocUri,
                customNotes = customNotes,
                onCustomNotesChange = { customNotes = it },
                isAnalyzing = isAnalyzing,
                docProcessingState = docProcessingState,
                docProcessingStatusText = docProcessingStatusText,
                selectedDocType = selectedDocType,
                onPhotoScan = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFile = {
                    docPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/*",
                            "text/*",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "*/*"
                        )
                    )
                },
                onRemoveFile = { viewModel.removeSelectedDocument() },
                onViewFile = {
                    selectedDocUri?.let { FileViewerHelper.openDocument(context, it.toString(), uploadedDocName) }
                },
                onAnalyze = { viewModel.startDocumentAnalysis(customNotes) },
                onCancel = { viewModel.cancelDocProcessing() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Processing Status Indicator (when analyzing or saving)
            if (isAnalyzing || docProcessingState == DocProcessingState.READING_DOCUMENT ||
                docProcessingState == DocProcessingState.EXTRACTING_TEXT ||
                docProcessingState == DocProcessingState.ANALYZING_AI ||
                docProcessingState == DocProcessingState.SAVING
            ) {
                ProcessingStatusCard(
                    statusText = docProcessingStatusText,
                    onCancel = { viewModel.cancelDocProcessing() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error Card (if extraction or analysis failed)
            if (docProcessingState == DocProcessingState.FAILED && docProcessingError != null) {
                ProcessingErrorCard(
                    errorMessage = docProcessingError ?: "Document processing failed",
                    onRetry = { viewModel.startDocumentAnalysis(customNotes) },
                    onDismiss = { viewModel.cancelDocProcessing() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Timetable Review Section (Allows editing entries before committing to database)
            if (selectedDocType == DocUploadType.TIMETABLE && parsedClasses.isNotEmpty()) {
                TimetableReviewSection(
                    classes = parsedClasses,
                    onEditClass = { index, cls -> classBeingEdited = Pair(index, cls) },
                    onDeleteClass = { index -> viewModel.deleteParsedClass(index) },
                    onAddClass = { isAddingClass = true },
                    onApplySchedule = { viewModel.applyParsedTimetableToSchedule() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // AI Analysis Results View
            if (analysisResult != null) {
                AnalysisResultCard(
                    analysisResult = analysisResult!!,
                    selectedDocType = selectedDocType,
                    hasExtractedClasses = parsedClasses.isNotEmpty(),
                    onApplyTimetable = { viewModel.applyParsedTimetableToSchedule() },
                    onApplyHomework = { viewModel.applyParsedHomework() },
                    onApplyNotes = { viewModel.applyParsedQuestionsToNotes() }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Edit Class Dialog
    classBeingEdited?.let { (index, cls) ->
        EditClassDialog(
            initialClass = cls,
            title = "Edit Schedule Entry",
            onDismiss = { classBeingEdited = null },
            onConfirm = { updated ->
                viewModel.updateParsedClass(index, updated)
                classBeingEdited = null
            }
        )
    }

    // Add Class Dialog
    if (isAddingClass) {
        EditClassDialog(
            initialClass = ParsedTimetableClass(
                day = "Mon",
                subject = "",
                timeSlot = "09:00 AM - 10:30 AM",
                room = "Room 101"
            ),
            title = "Add Class Entry",
            onDismiss = { isAddingClass = false },
            onConfirm = { newClass ->
                viewModel.addParsedClass(newClass)
                isAddingClass = false
            }
        )
    }
}

@Composable
private fun IntroductoryAiBanner() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AccentPurpleBg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = AccentPurpleText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart Document Parser & OCR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Upload PDF or image. On-device OCR reads the full text and Arkin AI organizes timetable, assignments, or revision sheets.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DocumentTypeSelectorRow(
    selectedDocType: DocUploadType,
    onSelectType: (DocUploadType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        items(DocUploadType.values()) { type ->
            DocumentTypeChip(
                type = type,
                isSelected = selectedDocType == type,
                onClick = { onSelectType(type) }
            )
        }
    }
}

@Composable
private fun DocumentTypeChip(
    type: DocUploadType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (label, icon) = when (type) {
        DocUploadType.TIMETABLE -> "Timetable" to Icons.Default.CalendarMonth
        DocUploadType.QUESTION_PAPER -> "Question Paper" to Icons.Default.Quiz
        DocUploadType.ASSIGNMENT -> "Assignment" to Icons.AutoMirrored.Filled.Assignment
        DocUploadType.LECTURE_NOTE -> "Lecture Notes" to Icons.AutoMirrored.Filled.MenuBook
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier.testTag("upload_type_${type.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else ArkinPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FileUploadCard(
    uploadedDocName: String?,
    uploadedDocSize: Long,
    selectedDocUri: Uri?,
    customNotes: String,
    onCustomNotesChange: (String) -> Unit,
    isAnalyzing: Boolean,
    docProcessingState: DocProcessingState,
    docProcessingStatusText: String,
    selectedDocType: DocUploadType,
    onPhotoScan: () -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onViewFile: () -> Unit,
    onAnalyze: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_upload_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag / Tap drop zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(2.dp, ArkinPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onPickFile() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = AccentBlueBg,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                tint = AccentBlueText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uploadedDocName == null) "Tap to select document (PDF / Images)" else "Change selected file",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArkinPrimary
                    )
                    Text(
                        text = "PDF, JPG, PNG, DOCX supported • Max 20MB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual source buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPhotoScan,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 Photo / Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onPickFile,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📁 Browse PDF / Doc", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File metadata row (shows real file name, size, and clear action)
            if (uploadedDocName != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = "File",
                            tint = ArkinPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uploadedDocName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = DocumentTextExtractor.formatFileSize(uploadedDocSize),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // View file button
                        IconButton(
                            onClick = onViewFile,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "View",
                                tint = ArkinPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Remove file button
                        IconButton(
                            onClick = onRemoveFile,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Custom Instructions for AI
            OutlinedTextField(
                value = customNotes,
                onValueChange = onCustomNotesChange,
                placeholder = {
                    Text(
                        when (selectedDocType) {
                            DocUploadType.TIMETABLE -> "Optional: e.g. 'Monday to Friday only' or 'Section B classes'"
                            DocUploadType.ASSIGNMENT -> "Optional: e.g. 'Solve questions 1 through 5 only'"
                            DocUploadType.QUESTION_PAPER -> "Optional: e.g. 'Focus on Module 3 & 4 answers'"
                            DocUploadType.LECTURE_NOTE -> "Optional: e.g. 'Generate revision flashcards'"
                        }
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Button
            Button(
                onClick = onAnalyze,
                enabled = !isAnalyzing && selectedDocUri != null,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("analyze_document_submit_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(docProcessingStatusText, fontSize = 13.sp, maxLines = 1)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedDocType) {
                            DocUploadType.TIMETABLE -> "Extract & Organize Timetable 🪄"
                            DocUploadType.QUESTION_PAPER -> "Analyze & Solve Questions 📝"
                            DocUploadType.ASSIGNMENT -> "Extract Assignment Deliverables 📚"
                            DocUploadType.LECTURE_NOTE -> "Summarize Lecture Note ⚡"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingStatusCard(
    statusText: String,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF5F3FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = ArkinPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArkinPrimary
                    )
                }

                TextButton(onClick = onCancel) {
                    Text("Cancel", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                color = ArkinPrimary,
                trackColor = Color(0xFFDDD6FE),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProcessingErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Document Processing Issue",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = Color(0xFF6B7280), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Analysis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimetableReviewSection(
    classes: List<ParsedTimetableClass>,
    onEditClass: (Int, ParsedTimetableClass) -> Unit,
    onDeleteClass: (Int) -> Unit,
    onAddClass: () -> Unit,
    onApplySchedule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Extracted Classes for Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${classes.size} classes recognized. Review & correct before saving.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onAddClass,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            classes.forEachIndexed { index, cls ->
                ClassReviewItem(
                    index = index,
                    item = cls,
                    onEdit = { onEditClass(index, cls) },
                    onDelete = { onDeleteClass(index) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onApplySchedule,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_timetable_button")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Apply ${classes.size} Classes to Timetable 🗓️", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClassReviewItem(
    index: Int,
    item: ParsedTimetableClass,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ArkinPrimary,
                modifier = Modifier.size(width = 44.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.day,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.timeSlot} • ${item.room}${if (item.faculty.isNotBlank()) " • " + item.faculty else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = ArkinPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EditClassDialog(
    initialClass: ParsedTimetableClass,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (ParsedTimetableClass) -> Unit
) {
    var day by remember { mutableStateOf(initialClass.day) }
    var subject by remember { mutableStateOf(initialClass.subject) }
    var timeSlot by remember { mutableStateOf(initialClass.timeSlot) }
    var room by remember { mutableStateOf(initialClass.room) }
    var faculty by remember { mutableStateOf(initialClass.faculty) }
    var isLab by remember { mutableStateOf(initialClass.isLab) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Day selector chips
                Text("Day of Week", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    items(days) { d ->
                        val isSel = day.equals(d, ignoreCase = true)
                        Surface(
                            onClick = { day = d },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = d,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Course Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot (e.g. 09:00 AM - 10:30 AM)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Classroom / Lab") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty / Professor") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Is this a Laboratory session?", fontSize = 13.sp)
                    Switch(checked = isLab, onCheckedChange = { isLab = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        onConfirm(
                            initialClass.copy(
                                day = day,
                                subject = subject.trim(),
                                timeSlot = timeSlot.trim(),
                                room = room.trim(),
                                faculty = faculty.trim(),
                                isLab = isLab
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AnalysisResultCard(
    analysisResult: String,
    selectedDocType: DocUploadType,
    hasExtractedClasses: Boolean,
    onApplyTimetable: () -> Unit,
    onApplyHomework: () -> Unit,
    onApplyNotes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 30.dp)
            .testTag("analysis_result_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Organization & Analysis Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Rich Markdown rendering of AI Analysis
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    MarkdownText(
                        markdown = analysisResult,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedDocType) {
                DocUploadType.TIMETABLE -> {
                    if (!hasExtractedClasses) {
                        Button(
                            onClick = onApplyTimetable,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_timetable_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Schedule to My Timetable 🗓️", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                DocUploadType.ASSIGNMENT -> {
                    Button(
                        onClick = onApplyHomework,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("apply_assignment_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Deliverables to Homework & To-Do 📚", fontWeight = FontWeight.Bold)
                    }
                }
                DocUploadType.QUESTION_PAPER, DocUploadType.LECTURE_NOTE -> {
                    Button(
                        onClick = onApplyNotes,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("apply_notes_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Analysis to Study Notes 📝", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
