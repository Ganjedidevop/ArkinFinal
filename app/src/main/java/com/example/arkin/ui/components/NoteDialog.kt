package com.example.arkin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.arkin.data.ai.GroqHelper
import com.example.arkin.data.model.NoteEntity
import com.example.ui.theme.ArkinPrimary
import kotlinx.coroutines.launch

@Composable
fun NoteDialog(
    note: NoteEntity?,
    isSecureMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tag: String, colorKey: String, isSecure: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedTag by remember { mutableStateOf(note?.tag ?: if (isSecureMode) "Secret" else "Study") }
    var selectedColor by remember { mutableStateOf(note?.colorKey ?: if (isSecureMode) "locker" else "purple") }
    var isAiEnhancing by remember { mutableStateOf(false) }
    var isAiSummarizing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val tags = if (isSecureMode) listOf("Secret", "Credentials", "Exam Keys") else listOf("Study", "Personal", "Project", "Exam")
    val colors = listOf("purple", "teal", "orange", "blue", "pink")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("note_dialog_surface")
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note != null) (if (isSecureMode) "Edit Secure Note" else "Edit Note")
                        else (if (isSecureMode) "New Secure Note 🔒" else "New Note 📝"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Chapter 5 Summary") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    placeholder = { Text("Type key concepts, formulas, ideas...") },
                    minLines = 5,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Study Assistant Tool Row
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // AI Summarize Button (Groq study summarizer)
                    Surface(
                        onClick = {
                            if (content.isNotBlank() && !isAiSummarizing) {
                                isAiSummarizing = true
                                coroutineScope.launch {
                                    val summary = GroqHelper.askAssistant(
                                        "Summarize the following study notes into structured, highly concise bullet points suitable for quick exam revision. Input text: $content"
                                    )
                                    content = if (content.contains("--- 📌 AI Exam Revision Summary ---")) {
                                        content + "\n\n" + summary
                                    } else {
                                        content + "\n\n--- 📌 AI Exam Revision Summary ---\n" + summary
                                    }
                                    isAiSummarizing = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("note_ai_summarize_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isAiSummarizing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Summarizing...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI Summarize",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Summarize",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    // AI Enhance & Format
                    Surface(
                        onClick = {
                            if (content.isNotBlank() && !isAiEnhancing) {
                                isAiEnhancing = true
                                coroutineScope.launch {
                                    val improved = GroqHelper.askAssistant(
                                        "Expand and structure this student note neatly with bullet points and bold key terms:\nTitle: $title\nContent:\n$content"
                                    )
                                    content = improved
                                    isAiEnhancing = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3E8FF),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("ai_enhance_note_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isAiEnhancing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF9333EA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Formatting...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9333EA)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI Format",
                                    tint = Color(0xFF9333EA),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Format",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9333EA)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tag Chips
                Text(
                    text = "Category Tag",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tags) { tag ->
                        val isSelected = selectedTag == tag
                        Surface(
                            onClick = { selectedTag = tag },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("tag_chip_$tag")
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Choice
                Text(
                    text = "Theme Accent",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { cKey ->
                        val (bg, txt) = getCategoryColor(cKey)
                        val isChosen = selectedColor == cKey
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(txt)
                                .border(
                                    if (isChosen) 3.dp else 0.dp,
                                    if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = cKey }
                                .testTag("color_choice_$cKey")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), content.trim(), selectedTag, selectedColor, isSecureMode)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text(
                            text = "Save Note",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
