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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.model.DocUploadType
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.ArkinPrimary

@Composable
fun AiDocumentOrganizerScreen(viewModel: ArkinViewModel) {
    val selectedDocType by viewModel.uploadedDocType.collectAsState()
    val uploadedDocName by viewModel.uploadedDocName.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingDoc.collectAsState()
    val analysisResult by viewModel.analyzedDocResult.collectAsState()

    var customNotes by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = "Document_Scan_${System.currentTimeMillis() % 10000}.jpg"
            viewModel.uploadedDocName.value = name
            viewModel.showToast("Uploaded photo selected: $name")
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = "Student_File_${System.currentTimeMillis() % 10000}.pdf"
            viewModel.uploadedDocName.value = name
            viewModel.showToast("Document selected: $name")
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
                text = "Select Document Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Document Type Tabs
            DocumentTypeSelectorRow(
                selectedDocType = selectedDocType,
                onSelectType = { viewModel.setUploadDocType(it) }
            )

            // File Upload Card
            FileUploadCard(
                uploadedDocName = uploadedDocName,
                customNotes = customNotes,
                onCustomNotesChange = { customNotes = it },
                isAnalyzing = isAnalyzing,
                selectedDocType = selectedDocType,
                onPhotoScan = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFile = { docPickerLauncher.launch("*/*") },
                onAnalyze = {
                    val filename = uploadedDocName ?: "Uploaded_Document.pdf"
                    viewModel.analyzeDocument(filename, customNotes)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // AI Analysis Results View
            if (analysisResult != null) {
                AnalysisResultCard(
                    analysisResult = analysisResult!!,
                    selectedDocType = selectedDocType,
                    onApplyTimetable = { viewModel.applyParsedTimetableToSchedule() },
                    onApplyHomework = { viewModel.applyParsedHomework() },
                    onApplyNotes = { viewModel.applyParsedQuestionsToNotes() }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
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
                    text = "Smart Document Parser",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Upload your university timetable, question papers, or assignment sheets. Arkin AI extracts and organizes everything automatically!",
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        DocUploadType.values().forEach { type ->
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
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FileUploadCard(
    uploadedDocName: String?,
    customNotes: String,
    onCustomNotesChange: (String) -> Unit,
    isAnalyzing: Boolean,
    selectedDocType: DocUploadType,
    onPhotoScan: () -> Unit,
    onPickFile: () -> Unit,
    onAnalyze: () -> Unit
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(2.dp, ArkinPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onPhotoScan() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = AccentBlueBg,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                tint = AccentBlueText,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to upload file or photo scan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArkinPrimary
                    )
                    Text(
                        text = "Supports PDF, JPG, PNG, DOCX",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPhotoScan,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 Scan Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onPickFile,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📁 Pick File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "File",
                    tint = ArkinPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uploadedDocName ?: "No file selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Ready",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = customNotes,
                onValueChange = onCustomNotesChange,
                placeholder = { Text("Add notes for AI (e.g. 'Solve questions 1-4' or 'Parse Mon-Fri only')") },
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAnalyze,
                enabled = !isAnalyzing,
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
                    Text("Arkin AI is organizing your document...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedDocType) {
                            DocUploadType.TIMETABLE -> "Analyze & Organize Timetable 🪄"
                            DocUploadType.QUESTION_PAPER -> "Analyze & Solve Question Paper 📝"
                            DocUploadType.ASSIGNMENT -> "Organize Assignment Tasks 📚"
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
private fun AnalysisResultCard(
    analysisResult: String,
    selectedDocType: DocUploadType,
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
                    text = "AI Organization Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = analysisResult,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedDocType) {
                DocUploadType.TIMETABLE -> {
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
