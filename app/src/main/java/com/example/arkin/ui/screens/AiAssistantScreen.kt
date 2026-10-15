package com.example.arkin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.data.ai.GroqHelper
import com.example.arkin.model.ChatMessage
import com.example.arkin.ui.components.MarkdownText
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentPinkBg
import com.example.ui.theme.AccentPinkText
import com.example.ui.theme.ArkinPrimary

@Composable
fun AiAssistantScreen(viewModel: ArkinViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var lastSubmittedPrompt by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val suggestions = listOf(
        "Summarize my notes 📝",
        "Create an exam study plan 📅",
        "Explain ACID database properties 💡",
        "Generate practice questions 🎯",
        "Top productivity tips for students ⚡"
    )

    // Automatically scroll to the latest response when new messages arrive or loading state changes
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isKeyConfigured = remember { GroqHelper.getApiKey().isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("ai_assistant_screen_root")
    ) {
        // AI Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = AccentPinkBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = "Arkin AI",
                                tint = AccentPinkText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Arkin AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isKeyConfigured) Color(0xFF22C55E) else Color(0xFFEAB308))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isKeyConfigured) "Online (Groq Fast AI)" else "Offline Intelligence",
                                fontSize = 11.sp,
                                color = if (isKeyConfigured) Color(0xFF22C55E) else Color(0xFFCA8A04),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(onClick = { showClearConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Suggested Prompts Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestions) { prompt ->
                Surface(
                    onClick = {
                        if (!isLoading) {
                            lastSubmittedPrompt = prompt
                            viewModel.sendAiMessage(prompt)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.testTag("ai_suggestion_chip")
                ) {
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ArkinPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Chat Message History List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages, key = { it.id }) { msg ->
                val isFailed = msg.sender == "ai" && (
                    msg.text.contains("timed out", ignoreCase = true) ||
                    msg.text.contains("rate limit", ignoreCase = true) ||
                    msg.text.contains("error", ignoreCase = true) && msg.text.length < 150
                )

                ChatBubble(
                    message = msg,
                    isFailed = isFailed,
                    onRetry = {
                        val retryQuery = lastSubmittedPrompt ?: messages.lastOrNull { it.sender == "user" }?.text
                        if (!retryQuery.isNullOrBlank() && !isLoading) {
                            viewModel.sendAiMessage(retryQuery)
                        }
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.text))
                        viewModel.showToast("Copied to clipboard")
                    },
                    onSaveAsNote = {
                        viewModel.saveNote(
                            title = "AI Study Note: ${msg.text.take(30)}...",
                            content = msg.text,
                            tag = "Study",
                            colorKey = "purple",
                            isSecure = false
                        )
                        viewModel.showToast("Saved to My Notes! 📝")
                    }
                )
            }

            // Loading / Thinking state
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentPinkBg,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = "AI",
                                    tint = AccentPinkText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = ArkinPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Arkin is researching & structuring answer...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("Ask Arkin about your courses, homework, or concepts...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArkinPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input_field")
                )

                Surface(
                    onClick = {
                        val query = inputQuery.trim()
                        if (query.isNotBlank() && !isLoading) {
                            lastSubmittedPrompt = query
                            viewModel.sendAiMessage(query)
                            inputQuery = ""
                        }
                    },
                    shape = CircleShape,
                    color = if (inputQuery.isNotBlank() && !isLoading) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (inputQuery.isNotBlank()) 4.dp else 0.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_chat_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputQuery.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("Are you sure you want to clear your conversation history with Arkin AI?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isFailed: Boolean = false,
    onRetry: () -> Unit = {},
    onCopy: () -> Unit,
    onSaveAsNote: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = AccentPinkBg,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "AI",
                        tint = AccentPinkText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.92f)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 20.dp
                ),
                color = if (isUser) ArkinPrimary else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isUser) 2.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (isUser) {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = Color.White
                        )
                    } else {
                        // Render using rich Compose Markdown
                        MarkdownText(
                            markdown = message.text,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            isUser = false
                        )
                    }
                }
            }

            // Action row below AI responses
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onSaveAsNote,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "Save to Notes",
                            tint = ArkinPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isFailed) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ArkinPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArkinPrimary)
                        }
                    }
                }
            }
        }
    }
}
