package com.example.arkin.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import com.example.arkin.model.AppScreen
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.ui.components.AppHeader
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueText
import com.example.ui.theme.AccentLockerBg
import com.example.ui.theme.AccentLockerText
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeText
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentPurpleText
import com.example.ui.theme.ArkinPrimary

@Composable
fun ProfileScreen(viewModel: ArkinViewModel) {
    val profile by viewModel.studentProfile.collectAsState()
    var isEditProfileDialogOpen by remember { mutableStateOf(false) }
    var isChangePinDialogOpen by remember { mutableStateOf(false) }

    val name = profile?.name ?: "Rohan Sharma"
    val major = profile?.major ?: "Computer Science, 2nd Year"
    val isDark = profile?.isDarkMode ?: false

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .testTag("profile_screen_root")
    ) {
        AppHeader(
            title = "Profile & Settings",
            onBackClick = { viewModel.goBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = ArkinPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(92.dp)
                    .testTag("profile_avatar_circle")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = major,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            val collegeLabel = profile?.collegeName ?: "School of Engineering & Technology"
            val semLabel = profile?.semester ?: "Semester 4"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentBlueBg,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Academic",
                        tint = AccentBlueText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$collegeLabel • $semLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlueText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Menu Items Group
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Account & Academic Details
                    SettingsRow(
                        title = "Student & Academic Profile",
                        icon = Icons.Default.Person,
                        bgColor = AccentBlueBg,
                        iconColor = AccentBlueText,
                        onClick = { isEditProfileDialogOpen = true }
                    )

                    // Cloud Backup & Restore
                    SettingsRow(
                        title = "Cloud Backup & Restore",
                        icon = Icons.Default.CloudUpload,
                        bgColor = AccentPurpleBg,
                        iconColor = AccentPurpleText,
                        onClick = { viewModel.navigateTo(AppScreen.BACKUP_RESTORE) }
                    )

                    // AI Document & Timetable Organizer
                    SettingsRow(
                        title = "AI Document & Timetable Parser",
                        icon = Icons.Default.AutoAwesome,
                        bgColor = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF9333EA),
                        onClick = { viewModel.navigateTo(AppScreen.AI_DOC_ORGANIZER) }
                    )

                    // Change Locker PIN
                    SettingsRow(
                        title = "Change Locker PIN",
                        icon = Icons.Default.Lock,
                        bgColor = AccentLockerBg,
                        iconColor = AccentLockerText,
                        onClick = { isChangePinDialogOpen = true }
                    )

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentPurpleBg,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = "Dark Mode",
                                        tint = AccentPurpleText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = ArkinPrimary)
                        )
                    }

                    // Reset Demo Data
                    SettingsRow(
                        title = "Reload Sample Data",
                        icon = Icons.Default.Refresh,
                        bgColor = AccentOrangeBg,
                        iconColor = AccentOrangeText,
                        onClick = { viewModel.resetToDemoData() }
                    )

                    // Logout
                    SettingsRow(
                        title = "Log Out",
                        icon = Icons.Default.ExitToApp,
                        bgColor = Color(0xFFFEE2E2),
                        iconColor = Color(0xFFDC2626),
                        titleColor = Color(0xFFDC2626),
                        onClick = { viewModel.logout() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Arkin v1.0 • Built with Gemini AI & Room",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (isEditProfileDialogOpen) {
        EditProfileDialog(
            currentName = name,
            currentMajor = major,
            currentEmail = profile?.email ?: "rohan.sharma@campus.edu",
            currentCollege = profile?.collegeName ?: "School of Engineering & Technology",
            currentCourse = profile?.course ?: "B.Tech Computer Science & Engineering",
            currentSemester = profile?.semester ?: "Semester 4",
            currentAcademicYear = profile?.academicYear ?: "2025-2026",
            onDismiss = { isEditProfileDialogOpen = false },
            onSave = { newName, newMajor, newEmail, newCollege, newCourse, newSem, newYear ->
                viewModel.updateProfile(newName, newMajor, newEmail, newCollege, newCourse, newSem, newYear)
                isEditProfileDialogOpen = false
            }
        )
    }

    if (isChangePinDialogOpen) {
        ChangePinDialog(
            currentPin = profile?.lockerPin ?: "1234",
            onDismiss = { isChangePinDialogOpen = false },
            onSave = { newPin ->
                viewModel.changeLockerPin(newPin)
                isChangePinDialogOpen = false
            }
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                modifier = Modifier.size(40.dp)
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
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentMajor: String,
    currentEmail: String,
    currentCollege: String,
    currentCourse: String,
    currentSemester: String,
    currentAcademicYear: String,
    onDismiss: () -> Unit,
    onSave: (name: String, major: String, email: String, college: String, course: String, semester: String, academicYear: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var major by remember { mutableStateOf(currentMajor) }
    var email by remember { mutableStateOf(currentEmail) }
    var college by remember { mutableStateOf(currentCollege) }
    var course by remember { mutableStateOf(currentCourse) }
    var semester by remember { mutableStateOf(currentSemester) }
    var academicYear by remember { mutableStateOf(currentAcademicYear) }

    val dialogScrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(dialogScrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Student Profile 👤",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Student Email") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = college,
                    onValueChange = { college = it },
                    label = { Text("College / University") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Degree / Program") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("Semester") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("Academic Year") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 10.dp)
                    )
                }

                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("Department / Specialization") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onSave(name, major, email, college, course, semester, academicYear) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSave: (newPin: String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Locker PIN 🔒",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Current PIN: $currentPin. Enter a new 4-digit numeric passcode to secure your private notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    placeholder = { Text("e.g. 5678") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (newPin.length == 4) {
                                onSave(newPin)
                            }
                        },
                        enabled = newPin.length == 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                    ) {
                        Text("Update PIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
