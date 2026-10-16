package com.example.arkin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentGreenBg
import com.example.ui.theme.AccentGreenText
import com.example.ui.theme.AccentLockerBg
import com.example.ui.theme.AccentLockerText
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeText
import com.example.ui.theme.AccentPinkBg
import com.example.ui.theme.AccentPinkText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealText
import com.example.ui.theme.AccentYellowBg
import com.example.ui.theme.AccentYellowText
import com.example.ui.theme.ArkinPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCategoryColor(colorKey: String): Pair<Color, Color> {
    return when (colorKey.lowercase()) {
        "purple" -> AccentPurpleBg to AccentPurpleText
        "teal" -> AccentTealBg to AccentTealText
        "orange" -> AccentOrangeBg to AccentOrangeText
        "blue" -> AccentBlueBg to AccentBlueText
        "pink" -> AccentPinkBg to AccentPinkText
        "locker" -> AccentLockerBg to AccentLockerText
        "yellow" -> AccentYellowBg to AccentYellowText
        "green" -> AccentGreenBg to AccentGreenText
        else -> AccentPurpleBg to AccentPurpleText
    }
}

@Composable
fun AppHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBackClick != null) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("header_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun HeroBanner(
    completedTasks: Int,
    totalTasks: Int,
    currentDateText: String,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_banner_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6466F1), Color(0xFF8B5CF6))
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentDateText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Plan your day and achieve your academic goals.",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF472B6),
                    trackColor = Color.Black.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$completedTasks / $totalTasks Tasks Completed",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun QuickAccessButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("quick_access_${title.lowercase()}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bgColor,
                shadowElevation = 2.dp,
                modifier = Modifier.size(62.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatCard(
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bgColor.copy(alpha = 0.45f),
        modifier = modifier
            .width(140.dp)
            .testTag("stat_card_${title.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = count.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onNoteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAiSummarizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColor(note.colorKey)
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    val dateString = sdf.format(Date(note.timestamp))

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNoteClick)
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = note.tag.uppercase(Locale.getDefault()),
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = onAiSummarizeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = "AI Summarize",
                            tint = ArkinPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (!note.attachmentName.isNullOrBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = {
                        com.example.arkin.util.FileViewerHelper.openDocument(context, note.attachmentUri, note.attachmentName)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3E8FF)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("📎 ", fontSize = 11.sp)
                        Text(
                            text = note.attachmentName ?: "Attachment",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArkinPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_row_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ArkinPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.priority == "High") {
                        Text(
                            text = "Priority: High",
                            fontSize = 10.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HomeworkCard(
    hw: HomeworkEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColor(hw.colorKey)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = hw.subject,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = hw.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hw.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (hw.isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Due",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Due ${hw.dueDate}",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (hw.isDone) ArkinPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
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
fun TimetableCard(
    timetable: TimetableEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColor(timetable.colorKey)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(70.dp)
        ) {
            val startTime = timetable.timeSlot.split(" - ").firstOrNull() ?: timetable.timeSlot
            Text(
                text = startTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(textColor)
                .border(2.dp, bgColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .testTag("timetable_item_${timetable.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = timetable.subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 ${timetable.room}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Class",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderRow(
    reminder: ReminderEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColor(reminder.colorKey)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (reminder.iconKey) {
                                "currency" -> "💳"
                                "book" -> "📖"
                                "alert" -> "⚠️"
                                else -> "🔔"
                            },
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = reminder.dueText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss Reminder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PinKeypad(
    enteredPin: String,
    isPinError: Boolean,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            for (i in 0 until 4) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPinError -> Color(0xFFEF4444)
                                isFilled -> ArkinPrimary
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            2.dp,
                            if (isPinError) Color(0xFFEF4444) else if (isFilled) ArkinPrimary else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
            }
        }

        // 3x4 Keypad Grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "<")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { key ->
                    Surface(
                        onClick = {
                            when (key) {
                                "C" -> onClearClick()
                                "<" -> onBackspaceClick()
                                else -> onDigitClick(key)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(64.dp)
                            .testTag("pin_key_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == "<") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = if (key == "C") 14.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (key == "C") ArkinPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
