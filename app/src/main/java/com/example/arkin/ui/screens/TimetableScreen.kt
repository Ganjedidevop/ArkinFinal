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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.ui.components.TimetableCard
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.ArkinPrimary

@Composable
fun TimetableScreen(viewModel: ArkinViewModel) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val classes by viewModel.classesForDay.collectAsState()
    var isAddDialogOpen by remember { mutableStateOf(false) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("timetable_screen_root")
    ) {
        AppHeader(
            title = "Timetable",
            onBackClick = { viewModel.goBack() },
            trailingAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            viewModel.setUploadDocType(com.example.arkin.model.DocUploadType.TIMETABLE)
                            viewModel.navigateTo(com.example.arkin.model.AppScreen.AI_DOC_ORGANIZER)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3E8FF),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("timetable_ai_scan_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text("🪄 AI Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                        }
                    }

                    Surface(
                        onClick = { isAddDialogOpen = true },
                        shape = CircleShape,
                        color = Color(0xFFE0E7FF),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("timetable_add_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Class",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        )

        // Day of week selector
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(days) { day ->
                val isSelected = selectedDay.equals(day, ignoreCase = true)
                Surface(
                    onClick = { viewModel.selectDay(day) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 4.dp else 1.dp,
                    modifier = Modifier
                        .size(width = 54.dp, height = 66.dp)
                        .testTag("day_selector_$day")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (day) {
                                "Mon" -> "27"
                                "Tue" -> "28"
                                "Wed" -> "29"
                                "Thu" -> "30"
                                "Fri" -> "31"
                                else -> "1"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Classes for selected day
        if (classes.isEmpty()) {
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
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "No Classes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No classes scheduled for $selectedDay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enjoy your free study time or add your lecture schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = { isAddDialogOpen = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Add Class")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(classes, key = { it.id }) { item ->
                    TimetableCard(
                        timetable = item,
                        onDelete = { viewModel.deleteClass(item) }
                    )
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddClassDialog(
            day = selectedDay,
            onDismiss = { isAddDialogOpen = false },
            onAdd = { subject, timeSlot, room, colorKey ->
                viewModel.addClass(subject, timeSlot, room, colorKey)
                isAddDialogOpen = false
            }
        )
    }
}

@Composable
private fun AddClassDialog(
    day: String,
    onDismiss: () -> Unit,
    onAdd: (subject: String, timeSlot: String, room: String, colorKey: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("10:00 AM - 11:30 AM") }
    var room by remember { mutableStateOf("Room 302") }
    var colorKey by remember { mutableStateOf("purple") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_class_dialog_surface")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Class ($day) 🗓️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Course / Subject") },
                    placeholder = { Text("e.g. Operating Systems") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("class_subject_input")
                )

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot") },
                    placeholder = { Text("e.g. 10:00 AM - 11:30 AM") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("class_timeslot_input")
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Classroom / Lab") },
                    placeholder = { Text("e.g. Lab 2, Hall 401") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .testTag("class_room_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (subject.isNotBlank()) {
                                onAdd(subject, timeSlot, room, colorKey)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                        modifier = Modifier.testTag("submit_class_button")
                    ) {
                        Text("Add to Timetable", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
