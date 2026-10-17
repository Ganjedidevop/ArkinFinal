package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arkin.model.AppScreen
import com.example.arkin.ui.components.ArkinBottomNavBar
import com.example.arkin.ui.components.NoteDialog
import com.example.arkin.ui.screens.AiAssistantScreen
import com.example.arkin.ui.screens.AnalyticsScreen
import com.example.arkin.ui.screens.AuthScreen
import com.example.arkin.ui.screens.AssignmentDetailScreen

import com.example.arkin.ui.screens.DashboardScreen
import com.example.arkin.ui.screens.HomeworkScreen
import com.example.arkin.ui.screens.LockerScreen
import com.example.arkin.ui.screens.NotesScreen
import com.example.arkin.ui.screens.ProfileScreen
import com.example.arkin.ui.screens.RemindersScreen
import com.example.arkin.ui.screens.SplashScreen
import com.example.arkin.ui.screens.TimetableScreen
import com.example.arkin.ui.screens.TodoScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
import com.google.firebase.FirebaseApp
import com.example.arkin.util.NotificationHelper
import com.example.arkin.viewmodel.ArkinViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed: ${e.message}")
        }
        NotificationHelper.createNotificationChannels(this)
        requestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            val viewModel: ArkinViewModel = viewModel()
            val profile by viewModel.studentProfile.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = profile?.isDarkMode ?: systemDark

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ArkinViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentBottomTab by viewModel.currentBottomNav.collectAsState()
    val isNoteDialogOpen by viewModel.isNoteDialogOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val isSecureNoteDialog by viewModel.isSecureNoteDialog.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Android Hardware Back Button Handling
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD && currentScreen != AppScreen.AUTH) {
        viewModel.goBack()
    }

    // Bottom Bar is visible on main student workflow screens (excluding full-screen chat to prevent keyboard squeeze)
    val shouldShowBottomBar = currentScreen in listOf(
        AppScreen.DASHBOARD,
        AppScreen.NOTES,
        AppScreen.TODO,
        AppScreen.PROFILE
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (shouldShowBottomBar) {
                ArkinBottomNavBar(
                    currentTab = currentBottomTab,
                    onTabSelected = { viewModel.selectBottomNav(it) },
                    onFabClick = { viewModel.openAddNoteDialog(isSecure = false) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() }
                    )) togetherWith (fadeOut(animationSpec = tween(220)) + slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() }
                    ))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.SPLASH -> SplashScreen(viewModel = viewModel)
                    AppScreen.AUTH -> AuthScreen(viewModel = viewModel)
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppScreen.NOTES -> NotesScreen(viewModel = viewModel)
                    AppScreen.TODO -> TodoScreen(viewModel = viewModel)
                    AppScreen.HOMEWORK -> HomeworkScreen(viewModel = viewModel)
                    AppScreen.TIMETABLE -> TimetableScreen(viewModel = viewModel)
                    AppScreen.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel)
                    AppScreen.AI_DOC_ORGANIZER -> com.example.arkin.ui.screens.AiDocumentOrganizerScreen(viewModel = viewModel)
                    AppScreen.LOCKER -> LockerScreen(viewModel = viewModel)
                    AppScreen.REMINDERS -> RemindersScreen(viewModel = viewModel)
                    AppScreen.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                    AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)

                    AppScreen.ASSIGNMENT_DETAIL -> AssignmentDetailScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Global Add / Edit Note Dialog
    if (isNoteDialogOpen) {
        NoteDialog(
            note = editingNote,
            isSecureMode = isSecureNoteDialog,
            onDismiss = { viewModel.closeNoteDialog() },
            onSave = { title, content, tag, colorKey, isSecure, attUri, attName ->
                viewModel.saveNote(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    isSecure = isSecure,
                    attachmentUri = attUri,
                    attachmentName = attName
                )
            }
        )
    }
}
