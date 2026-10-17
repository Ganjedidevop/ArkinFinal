package com.example.arkin.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.ai.GroqHelper
import com.example.arkin.data.backup.BackupManager
import com.example.arkin.data.backup.BackupPreview
import com.example.arkin.data.firebase.FirebaseAuthHelper
import com.example.arkin.data.firebase.FirestoreManager
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.StudentProfileEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.example.arkin.data.model.VaultItemEntity
import com.example.arkin.data.security.PinVerificationResult
import com.example.arkin.data.security.VaultSecurityManager
import android.net.Uri
import com.example.arkin.model.AppScreen
import com.example.arkin.model.BottomNavTab
import com.example.arkin.model.ChatMessage
import com.example.arkin.model.DocProcessingState
import com.example.arkin.model.DocUploadType
import com.example.arkin.model.ParsedTimetableClass
import com.example.arkin.util.DocumentTextExtractor
import com.example.arkin.util.ExtractionResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ArkinViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ArkinDatabase.getDatabase(application)
    private val noteDao = db.noteDao()
    private val taskDao = db.taskDao()
    private val homeworkDao = db.homeworkDao()
    private val timetableDao = db.timetableDao()
    private val reminderDao = db.reminderDao()
    private val examResultDao = db.examResultDao()
    private val vaultItemDao = db.vaultItemDao()
    private val profileDao = db.studentProfileDao()

    // Screen Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationStack = mutableListOf<AppScreen>()

    private val _currentBottomNav = MutableStateFlow(BottomNavTab.HOME)
    val currentBottomNav: StateFlow<BottomNavTab> = _currentBottomNav.asStateFlow()

    // Search and Filters for Notes
    val noteSearchQuery = MutableStateFlow("")
    val selectedNoteTag = MutableStateFlow("All")
    val selectedNoteCategory = MutableStateFlow("All")
    val selectedNoteSubject = MutableStateFlow("All")

    // Notes Flows
    private val allPublicNotesFlow = noteDao.getAllPublicNotes()
    val filteredPublicNotes: StateFlow<List<NoteEntity>> = combine(
        allPublicNotesFlow,
        noteSearchQuery,
        selectedNoteTag,
        selectedNoteCategory,
        selectedNoteSubject
    ) { notes, query, tag, category, subject ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.subject.contains(query, ignoreCase = true)
            val matchesTag = tag == "All" || note.tag.equals(tag, ignoreCase = true)
            val matchesCat = category == "All" || note.category.equals(category, ignoreCase = true)
            val matchesSub = subject == "All" || note.subject.equals(subject, ignoreCase = true)
            matchesQuery && matchesTag && matchesCat && matchesSub
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secureNotes: StateFlow<List<NoteEntity>> = noteDao.getNotes(secure = true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks Flow & Planner Filters
    val taskFilter = MutableStateFlow("All") // All, Today, Upcoming, Completed
    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        taskFilter
    ) { tasks, filter ->
        tasks.filter {
            when (filter) {
                "Today" -> !it.isCompleted && (it.dueDate.equals("Today", ignoreCase = true))
                "Upcoming" -> !it.isCompleted
                "Completed" -> it.isCompleted
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPlannerCalendarView = MutableStateFlow(false)

    // Homework Flow & Selected Assignment
    val allHomework: StateFlow<List<HomeworkEntity>> = homeworkDao.getAllHomework()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAssignment = MutableStateFlow<HomeworkEntity?>(null)

    // Timetable
    val selectedDay = MutableStateFlow("Mon")
    val classesForDay: StateFlow<List<TimetableEntity>> = combine(
        timetableDao.getAllClasses(),
        selectedDay
    ) { classes, day ->
        classes.filter { it.dayOfWeek.equals(day, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exam Results for Study Analyser
    val allExamResults: StateFlow<List<ExamResultEntity>> = examResultDao.getAllExamResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Items
    val allVaultItems: StateFlow<List<VaultItemEntity>> = vaultItemDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile
    val studentProfile: StateFlow<StudentProfileEntity?> = profileDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI Assistant Chat
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Hello! 👋 I'm Arkin, your AI study assistant. I can explain complex academic concepts, solve assignment questions step-by-step, organize your timetable, or generate revision flashcards."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Private Locker PIN & Security
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _isPinError = MutableStateFlow(false)
    val isPinError: StateFlow<Boolean> = _isPinError.asStateFlow()

    // Note Modal
    val isNoteDialogOpen = MutableStateFlow(false)
    val editingNote = MutableStateFlow<NoteEntity?>(null)
    val isSecureNoteDialog = MutableStateFlow(false)

    // Feedback Toast/Snackbar
    val toastMessage = MutableStateFlow<String?>(null)

    // AI Document Upload & Organizer
    val uploadedDocName = MutableStateFlow<String?>(null)
    val uploadedDocType = MutableStateFlow(DocUploadType.TIMETABLE)
    val selectedDocUri = MutableStateFlow<Uri?>(null)
    val docFileSize = MutableStateFlow<Long>(0L)
    val docProcessingState = MutableStateFlow(DocProcessingState.IDLE)
    val docProcessingStatusText = MutableStateFlow("Ready to analyze")
    val docProcessingError = MutableStateFlow<String?>(null)
    val docExtractedRawText = MutableStateFlow<String?>(null)
    val isAnalyzingDoc = MutableStateFlow(false)
    val analyzedDocResult = MutableStateFlow<String?>(null)
    val parsedClasses = MutableStateFlow<List<ParsedTimetableClass>>(emptyList())
    val parsedAssignments = MutableStateFlow<List<Triple<String, String, String>>>(emptyList())
    val parsedExamQuestions = MutableStateFlow<List<String>>(emptyList())

    // Backup & Restore
    val backupPreview = MutableStateFlow<BackupPreview?>(null)
    val isRestoringBackup = MutableStateFlow(false)

    // Firebase Authentication & Cloud State
    val firebaseUser: StateFlow<FirebaseUser?> = FirebaseAuthHelper.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FirebaseAuthHelper.currentUser)
    val isAuthLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val needsProfileSetup = MutableStateFlow(false)
    val isFirestoreSyncing = MutableStateFlow(false)
    val lastFirestoreSyncTime = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            val prof = profileDao.getProfileSync()
            if (prof == null) {
                profileDao.insertProfile(StudentProfileEntity())
            }
            // If user is already authenticated with Firebase, pull latest cloud data
            if (FirebaseAuthHelper.isUserLoggedIn) {
                val user = FirebaseAuthHelper.currentUser
                if (user != null) {
                    val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                    if (remoteProfile != null) {
                        profileDao.updateProfile(remoteProfile)
                    }
                    FirestoreManager.syncFirestoreToLocal(db, merge = true)
                    lastFirestoreSyncTime.value = System.currentTimeMillis()
                }
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: AppScreen, addToStack: Boolean = true) {
        if (addToStack && _currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen

        when (screen) {
            AppScreen.DASHBOARD -> _currentBottomNav.value = BottomNavTab.HOME
            AppScreen.NOTES -> _currentBottomNav.value = BottomNavTab.NOTES
            AppScreen.TODO -> _currentBottomNav.value = BottomNavTab.PLANNER
            AppScreen.AI_ASSISTANT -> _currentBottomNav.value = BottomNavTab.AI_ASSISTANT
            AppScreen.PROFILE -> _currentBottomNav.value = BottomNavTab.PROFILE
            else -> {}
        }

        if (screen != AppScreen.LOCKER && _isVaultUnlocked.value) {
            lockVault()
        }
    }

    fun goBack(): Boolean {
        if (_navigationStack.isNotEmpty()) {
            val prev = _navigationStack.removeAt(_navigationStack.size - 1)
            navigateTo(prev, addToStack = false)
            return true
        } else if (_currentScreen.value != AppScreen.DASHBOARD && _currentScreen.value != AppScreen.SPLASH && _currentScreen.value != AppScreen.AUTH) {
            navigateTo(AppScreen.DASHBOARD, addToStack = false)
            return true
        }
        return false
    }

    fun selectBottomNav(tab: BottomNavTab) {
        _currentBottomNav.value = tab
        when (tab) {
            BottomNavTab.HOME -> navigateTo(AppScreen.DASHBOARD)
            BottomNavTab.NOTES -> navigateTo(AppScreen.NOTES)
            BottomNavTab.PLANNER -> navigateTo(AppScreen.TODO)
            BottomNavTab.AI_ASSISTANT -> navigateTo(AppScreen.AI_ASSISTANT)
            BottomNavTab.PROFILE -> navigateTo(AppScreen.PROFILE)
        }
    }

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    // Notes Actions
    fun openAddNoteDialog(isSecure: Boolean = false) {
        editingNote.value = null
        isSecureNoteDialog.value = isSecure
        isNoteDialogOpen.value = true
    }

    fun openEditNoteDialog(note: NoteEntity) {
        editingNote.value = note
        isSecureNoteDialog.value = note.isSecure
        isNoteDialogOpen.value = true
    }

    fun closeNoteDialog() {
        isNoteDialogOpen.value = false
        editingNote.value = null
    }

    fun saveNote(
        title: String,
        content: String,
        tag: String,
        colorKey: String,
        isSecure: Boolean,
        subject: String = "General",
        category: String = "Class Notes",
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        viewModelScope.launch {
            val resolvedUri = if (!attachmentUri.isNullOrBlank() && attachmentUri.startsWith("content://")) {
                val (persisted, _) = com.example.arkin.util.FileViewerHelper.persistAttachment(
                    getApplication(),
                    android.net.Uri.parse(attachmentUri)
                )
                persisted
            } else {
                attachmentUri
            }

            val existing = editingNote.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    subject = subject,
                    category = category,
                    attachmentUri = resolvedUri ?: existing.attachmentUri,
                    attachmentName = attachmentName ?: existing.attachmentName,
                    timestamp = System.currentTimeMillis()
                )
                noteDao.updateNote(updated)
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.saveNote(updated)
                }
                showToast("Note updated successfully")
            } else {
                val newNote = NoteEntity(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    subject = subject,
                    category = category,
                    attachmentUri = resolvedUri,
                    attachmentName = attachmentName,
                    timestamp = System.currentTimeMillis(),
                    isSecure = isSecure
                )
                val id = noteDao.insertNote(newNote)
                if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                    FirestoreManager.saveNote(newNote.copy(id = id))
                }
                showToast(if (isSecure) "Saved to Secure Locker" else "Note created")
            }
            closeNoteDialog()
        }
    }

    fun updateNoteAttachmentUri(note: NoteEntity, newPath: String) {
        viewModelScope.launch {
            val updated = note.copy(attachmentUri = newPath)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
        }
    }

    fun toggleFavoriteNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
        }
    }

    fun toggleArchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isArchived = !note.isArchived)
            noteDao.updateNote(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(updated)
            }
            showToast(if (!note.isArchived) "Note archived" else "Note unarchived")
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteNote(note.id)
            }
            showToast("Note deleted")
        }
    }

    // Tasks Actions
    fun addTask(title: String, priority: String = "Medium", description: String = "", subject: String = "", dueDate: String = "Today") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim(),
                priority = priority,
                dueDate = dueDate
            )
            val id = taskDao.insertTask(task)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTask(task.copy(id = id))
            }
            showToast("Task added")
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updated)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTask(updated)
            }
            if (updated.isCompleted) {
                showToast("Task completed! 🎉")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteTask(task.id)
            }
            showToast("Task removed")
        }
    }

    // Homework Actions
    fun addHomework(
        subject: String,
        title: String,
        dueDate: String,
        dueTime: String = "11:59 PM",
        description: String = "",
        teacherName: String = "",
        priority: String = "Medium",
        colorKey: String = "purple",
        extractedQuestions: String? = null,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null
    ) {
        if (title.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
            val resolvedUri = if (!attachmentUri.isNullOrBlank() && attachmentUri.startsWith("content://")) {
                val (persisted, _) = com.example.arkin.util.FileViewerHelper.persistAttachment(
                    getApplication(),
                    android.net.Uri.parse(attachmentUri)
                )
                persisted
            } else {
                attachmentUri
            }

            val hw = HomeworkEntity(
                subject = subject.trim(),
                title = title.trim(),
                description = description.trim(),
                teacherName = teacherName.trim(),
                dueDate = dueDate.trim(),
                dueTime = dueTime.trim(),
                priority = priority,
                status = "Not Started",
                colorKey = colorKey,
                extractedQuestionsJson = extractedQuestions,
                attachmentUri = resolvedUri,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize
            )
            val id = homeworkDao.insertHomework(hw)
            val plannerTask = TaskEntity(
                title = "Assignment: $title ($subject)",
                description = description,
                subject = subject,
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                linkedAssignmentId = id
            )
            val taskId = taskDao.insertTask(plannerTask)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(hw.copy(id = id))
                FirestoreManager.saveTask(plannerTask.copy(id = taskId))
            }
            showToast("Homework added and synced to study planner")
        }
    }

    fun attachFileToHomework(hw: HomeworkEntity, uri: Uri) {
        viewModelScope.launch {
            val (savedPath, name) = com.example.arkin.util.FileViewerHelper.persistAttachment(getApplication(), uri)
            val file = java.io.File(savedPath)
            val sizeStr = DocumentTextExtractor.formatFileSize(if (file.exists()) file.length() else 0L)
            val updated = hw.copy(
                attachmentUri = savedPath,
                attachmentName = name,
                attachmentSize = sizeStr
            )
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Attached: $name")
        }
    }

    fun removeHomeworkAttachment(hw: HomeworkEntity) {
        viewModelScope.launch {
            val updated = hw.copy(attachmentUri = null, attachmentName = null, attachmentSize = null)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Attachment removed")
        }
    }

    fun openAssignmentDetail(hw: HomeworkEntity) {
        selectedAssignment.value = hw
        navigateTo(AppScreen.ASSIGNMENT_DETAIL)
    }

    fun updateAssignmentStatus(hw: HomeworkEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = hw.copy(status = newStatus, isDone = (newStatus == "Completed" || newStatus == "Submitted"))
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Status updated to $newStatus")
        }
    }

    fun solveAssignmentQuestionWithAi(hw: HomeworkEntity, question: String) {
        _isAiLoading.value = true
        viewModelScope.launch {
            val answer = GroqHelper.solveAssignmentQuestion(hw.title, question)
            val updated = hw.copy(aiDraftAnswer = answer)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            _isAiLoading.value = false
            showToast("AI Solution generated!")
        }
    }

    fun saveAssignmentAnswerDraft(hw: HomeworkEntity, draft: String) {
        viewModelScope.launch {
            val updated = hw.copy(aiDraftAnswer = draft)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast("Draft saved")
        }
    }

    fun toggleHomework(hw: HomeworkEntity) {
        viewModelScope.launch {
            val updated = hw.copy(
                isDone = !hw.isDone,
                status = if (!hw.isDone) "Completed" else "In Progress"
            )
            homeworkDao.updateHomework(updated)
            if (selectedAssignment.value?.id == hw.id) {
                selectedAssignment.value = updated
            }
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveHomework(updated)
            }
            showToast(if (updated.isDone) "Marked as done!" else "Marked as pending")
        }
    }

    fun deleteHomework(hw: HomeworkEntity) {
        viewModelScope.launch {
            homeworkDao.deleteHomework(hw)
            if (selectedAssignment.value?.id == hw.id) {
                goBack()
            }
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteHomework(hw.id)
            }
            showToast("Homework removed")
        }
    }

    // Timetable Actions
    fun selectDay(day: String) {
        selectedDay.value = day
    }

    fun addClass(
        subject: String,
        timeSlot: String,
        room: String,
        facultyName: String = "",
        requiredMaterials: String = "",
        isLab: Boolean = false,
        colorKey: String = "purple"
    ) {
        if (subject.isBlank() || timeSlot.isBlank()) return
        viewModelScope.launch {
            val entry = TimetableEntity(
                dayOfWeek = selectedDay.value,
                subject = subject.trim(),
                timeSlot = timeSlot.trim(),
                room = room.trim(),
                facultyName = facultyName.trim(),
                requiredMaterials = requiredMaterials.trim(),
                isLab = isLab,
                colorKey = colorKey
            )
            val id = timetableDao.insertClass(entry)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveTimetableClass(entry.copy(id = id))
            }
            showToast("Class added to timetable")
        }
    }

    fun deleteClass(entry: TimetableEntity) {
        viewModelScope.launch {
            timetableDao.deleteClass(entry)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteTimetableClass(entry.id)
            }
            showToast("Class removed")
        }
    }

    fun clearTimetable() {
        viewModelScope.launch {
            timetableDao.clearAllClasses()
            showToast("Timetable cleared")
        }
    }

    // Reminder Actions
    fun addReminder(title: String, dueText: String, iconKey: String = "bell", colorKey: String = "yellow") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title.trim(),
                dueText = dueText.trim(),
                iconKey = iconKey,
                colorKey = colorKey
            )
            val id = reminderDao.insertReminder(reminder)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveReminder(reminder.copy(id = id))
            }
            showToast("Reminder created")
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteReminder(reminder.id)
            }
            showToast("Reminder dismissed")
        }
    }

    // Exam Results (Study Analyser)
    fun addExamResult(subject: String, examName: String, marksObtained: Float, totalMarks: Float, semester: String = "Semester 4") {
        if (subject.isBlank() || examName.isBlank() || totalMarks <= 0f) return
        viewModelScope.launch {
            val exam = ExamResultEntity(
                subject = subject.trim(),
                examName = examName.trim(),
                marksObtained = marksObtained,
                totalMarks = totalMarks,
                semester = semester.trim()
            )
            val id = examResultDao.insertExamResult(exam)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveExamResult(exam.copy(id = id))
            }
            showToast("Exam marks recorded!")
        }
    }

    fun deleteExamResult(result: ExamResultEntity) {
        viewModelScope.launch {
            examResultDao.deleteExamResult(result)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.deleteExamResult(result.id)
            }
            showToast("Exam record deleted")
        }
    }

    // Vault Items Actions
    fun addVaultItem(title: String, category: String, content: String, attachmentName: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            vaultItemDao.insertVaultItem(
                VaultItemEntity(
                    title = title.trim(),
                    category = category.trim(),
                    content = content.trim(),
                    attachmentName = attachmentName,
                    fileType = "TEXT"
                )
            )
            showToast("Item securely saved in Vault 🔒")
        }
    }

    fun addVaultAttachment(title: String, category: String, uri: Uri, fileType: String, customName: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val encryptResult = VaultSecurityManager.encryptAndStoreAttachment(context, uri, customName)
            encryptResult.onSuccess { enc ->
                vaultItemDao.insertVaultItem(
                    VaultItemEntity(
                        title = title.trim(),
                        category = category.trim(),
                        content = "",
                        attachmentUri = enc.encryptedRelativePath,
                        attachmentName = enc.originalFileName,
                        fileType = fileType,
                        fileSize = enc.fileSize,
                        mimeType = enc.mimeType
                    )
                )
                showToast("File encrypted & secured in Vault 🔒")
            }.onFailure { err ->
                showToast("Encryption failed: ${err.message}")
            }
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            if (!item.attachmentUri.isNullOrBlank()) {
                VaultSecurityManager.deleteEncryptedFile(context, item.attachmentUri)
            }
            vaultItemDao.deleteVaultItem(item)
            showToast("Vault item deleted")
        }
    }

    // Locker PIN Actions
    fun onPinDigit(digit: String) {
        if (_enteredPin.value.length < 4) {
            val newPin = _enteredPin.value + digit
            _enteredPin.value = newPin
            _isPinError.value = false

            if (newPin.length == 4) {
                val context = getApplication<Application>().applicationContext
                if (!VaultSecurityManager.isPinSet(context)) {
                    VaultSecurityManager.setPin(context, newPin)
                    _isVaultUnlocked.value = true
                    _enteredPin.value = ""
                    showToast("PIN created & Vault unlocked 🔓")
                    return
                }

                when (val result = VaultSecurityManager.verifyCurrentPin(context, newPin)) {
                    is PinVerificationResult.Success -> {
                        _isVaultUnlocked.value = true
                        _enteredPin.value = ""
                        showToast("Vault unlocked 🔓")
                    }
                    is PinVerificationResult.Failure -> {
                        _isPinError.value = true
                        showToast("Incorrect PIN (${result.attemptsRemaining} left)")
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            _enteredPin.value = ""
                            _isPinError.value = false
                        }
                    }
                    is PinVerificationResult.LockedOut -> {
                        _isPinError.value = true
                        showToast("Vault locked. Try again in ${result.remainingSeconds}s")
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            _enteredPin.value = ""
                            _isPinError.value = false
                        }
                    }
                }
            }
        }
    }

    fun onPinBackspace() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
            _isPinError.value = false
        }
    }

    fun onPinClear() {
        _enteredPin.value = ""
        _isPinError.value = false
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _enteredPin.value = ""
        _isPinError.value = false
        showToast("Vault locked 🔒")
    }

    fun changeLockerPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            val context = getApplication<Application>().applicationContext
            VaultSecurityManager.setPin(context, newPin)
            showToast("Locker PIN updated successfully 🔒")
        } else {
            showToast("PIN must be 4 digits")
        }
    }

    // AI Assistant Actions
    fun sendAiMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _isAiLoading.value) return

        val userMsg = ChatMessage(sender = "user", text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val history = _chatMessages.value.map { it.sender to it.text }
            val notesSummary = filteredPublicNotes.value.take(3).joinToString("\n---\n") {
                "${it.title}: ${it.content}"
            }
            val aiResponse = GroqHelper.askAssistant(trimmed, notesSummary, history)
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = aiResponse)
            _isAiLoading.value = false
        }
    }

    fun summarizeNoteWithAi(note: NoteEntity) {
        navigateTo(AppScreen.AI_ASSISTANT)
        sendAiMessage("Please summarize this student note into structured revision bullet points and key formulas:\n\"${note.title}\"\n\nContent:\n${note.content}")
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "ai",
                text = "Chat history cleared. How else can I assist with your studies today?"
            )
        )
    }

    // AI Document Upload & Organizer
    fun setUploadDocType(type: DocUploadType) {
        uploadedDocType.value = type
        selectedDocUri.value = null
        uploadedDocName.value = null
        docFileSize.value = 0L
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Ready to analyze"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
    }

    fun onDocumentSelected(uri: Uri) {
        selectedDocUri.value = uri
        val (name, size) = DocumentTextExtractor.getFileMetadata(getApplication(), uri)
        uploadedDocName.value = name
        docFileSize.value = size
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Document ready for analysis"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
        showToast("Selected: $name")
    }

    fun removeSelectedDocument() {
        selectedDocUri.value = null
        uploadedDocName.value = null
        docFileSize.value = 0L
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Ready to analyze"
        docProcessingError.value = null
        docExtractedRawText.value = null
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
    }

    fun cancelDocProcessing() {
        docProcessingState.value = DocProcessingState.IDLE
        docProcessingStatusText.value = "Cancelled"
        isAnalyzingDoc.value = false
    }

    fun updateParsedClass(index: Int, updated: ParsedTimetableClass) {
        val current = parsedClasses.value.toMutableList()
        if (index in 0 until current.size) {
            current[index] = updated
            parsedClasses.value = current
        }
    }

    fun deleteParsedClass(index: Int) {
        val current = parsedClasses.value.toMutableList()
        if (index in 0 until current.size) {
            current.removeAt(index)
            parsedClasses.value = current
            showToast("Class removed from schedule review")
        }
    }

    fun addParsedClass(entry: ParsedTimetableClass) {
        parsedClasses.value = parsedClasses.value + entry
    }

    fun startDocumentAnalysis(customPrompt: String = "") {
        val uri = selectedDocUri.value
        val docName = uploadedDocName.value ?: "Document.pdf"

        if (uri == null) {
            showToast("Please select a document or scan a photo first.")
            return
        }

        docProcessingState.value = DocProcessingState.READING_DOCUMENT
        docProcessingStatusText.value = "Reading document..."
        docProcessingError.value = null
        isAnalyzingDoc.value = true

        viewModelScope.launch {
            // Step 1: Document reading & On-device OCR
            docProcessingState.value = DocProcessingState.EXTRACTING_TEXT
            val extraction = DocumentTextExtractor.extractText(getApplication(), uri) { step, _ ->
                docProcessingStatusText.value = step
            }

            val extractedDoc = when (extraction) {
                is ExtractionResult.Success -> extraction.document
                is ExtractionResult.Failure -> {
                    docProcessingState.value = DocProcessingState.FAILED
                    docProcessingError.value = extraction.errorMessage
                    docProcessingStatusText.value = "Text extraction failed"
                    isAnalyzingDoc.value = false
                    showToast(extraction.errorMessage)
                    return@launch
                }
            }

            val textContent = extractedDoc.text
            docExtractedRawText.value = textContent

            // Step 2: Send extracted text to AI for structured timetable/document analysis
            docProcessingState.value = DocProcessingState.ANALYZING_AI
            docProcessingStatusText.value = "Arkin AI is analyzing timetable & schedule..."

            val type = uploadedDocType.value
            when (type) {
                DocUploadType.TIMETABLE -> {
                    val prompt = """
                        You are Arkin AI, an expert academic schedule and timetable assistant for college students.
                        Analyze the following extracted text from a class timetable document ("${extractedDoc.fileName}").
                        Parse each lecture or lab period into structured data.
                        
                        Required timetable fields for each class:
                        - "day": must be standard 3-letter day: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
                        - "subject": course name (e.g. "Computer Networks", "Database Systems")
                        - "timeSlot": time interval string (e.g. "09:00 AM - 10:30 AM")
                        - "startTime": start time (e.g. "09:00 AM")
                        - "endTime": end time (e.g. "10:30 AM")
                        - "room": classroom or lab (e.g. "Room 304", "Lab 2", "Hall B")
                        - "faculty": teacher/professor name if present
                        - "isLab": boolean (true if practical or lab)
                        - "colorKey": one of "purple", "blue", "teal", "orange", "pink"
                        
                        You MUST respond with valid JSON in this exact structure:
                        {
                          "summary": "Clear student-focused Markdown summary of the timetable schedule, total classes per week, and recommended study gaps.",
                          "classes": [
                             {
                               "day": "Mon",
                               "subject": "Distributed Systems",
                               "timeSlot": "09:00 AM - 10:30 AM",
                               "startTime": "09:00 AM",
                               "endTime": "10:30 AM",
                               "room": "Room 401",
                               "faculty": "Prof. Rao",
                               "isLab": false,
                               "colorKey": "purple"
                             }
                          ]
                        }
                        
                        Do not wrap with anything other than the JSON object.
                        ${if (customPrompt.isNotBlank()) "Student notes: $customPrompt" else ""}
                        
                        Extracted Document Text:
                        ---
                        $textContent
                        ---
                    """.trimIndent()

                    val rawResponse = GroqHelper.askAssistant(prompt)
                    val parsed = parseTimetableAiResponse(rawResponse)
                    analyzedDocResult.value = parsed.first
                    parsedClasses.value = parsed.second
                    docProcessingState.value = DocProcessingState.REVIEWING
                    docProcessingStatusText.value = "Analysis complete (${parsed.second.size} classes found)"
                    isAnalyzingDoc.value = false
                    showToast("Extracted ${parsed.second.size} classes! Review and confirm below 🗓️")
                }

                DocUploadType.ASSIGNMENT -> {
                    val prompt = """
                        You are Arkin AI. Analyze this assignment sheet ("${extractedDoc.fileName}").
                        Extract:
                        1. Course/Subject
                        2. Assignment Title & Due Date
                        3. List of specific questions/deliverables
                        4. Step-by-step model approach
                        
                        Return JSON:
                        {
                          "summary": "Detailed Markdown explanation of the assignment deliverables and steps.",
                          "assignments": [
                             {
                               "subject": "Course Name",
                               "title": "Assignment Title",
                               "dueDate": "Tomorrow"
                             }
                          ],
                          "questions": ["Question 1 text...", "Question 2 text..."]
                        }
                        
                        Document Text:
                        $textContent
                    """.trimIndent()

                    val rawResponse = GroqHelper.askAssistant(prompt)
                    val parsed = parseAssignmentAiResponse(rawResponse)
                    analyzedDocResult.value = parsed.first
                    parsedAssignments.value = parsed.second
                    parsedExamQuestions.value = parsed.third
                    docProcessingState.value = DocProcessingState.REVIEWING
                    docProcessingStatusText.value = "Assignment organized"
                    isAnalyzingDoc.value = false
                    showToast("Assignment deliverables organized! 📚")
                }

                DocUploadType.QUESTION_PAPER, DocUploadType.LECTURE_NOTE -> {
                    val prompt = """
                        You are Arkin AI. Analyze this university examination paper / lecture note ("${extractedDoc.fileName}").
                        Provide a comprehensive, high-yield revision breakdown with:
                        - Key Topics & Exam Weightage
                        - Detailed Solutions & Answers for hardest questions
                        - Core Formulas and Definitions
                        - Flashcard-style summary bullets
                        
                        Document Text:
                        $textContent
                    """.trimIndent()

                    val rawResponse = GroqHelper.askAssistant(prompt)
                    analyzedDocResult.value = rawResponse
                    docProcessingState.value = DocProcessingState.REVIEWING
                    docProcessingStatusText.value = "Analysis complete"
                    isAnalyzingDoc.value = false
                    showToast("Document analyzed! 📝")
                }
            }
        }
    }

    private fun parseTimetableAiResponse(rawResponse: String): Pair<String, List<ParsedTimetableClass>> {
        val cleanJson = rawResponse.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val classesList = mutableListOf<ParsedTimetableClass>()
        var summaryText = ""

        try {
            val json = JSONObject(cleanJson)
            summaryText = json.optString("summary", "")

            val arr = json.optJSONArray("classes")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val dayRaw = obj.optString("day", "Mon")
                    val dayFormatted = when (dayRaw.lowercase().take(3)) {
                        "mon" -> "Mon"
                        "tue" -> "Tue"
                        "wed" -> "Wed"
                        "thu" -> "Thu"
                        "fri" -> "Fri"
                        "sat" -> "Sat"
                        "sun" -> "Sun"
                        else -> "Mon"
                    }
                    val subject = obj.optString("subject", "Class ${i + 1}")
                    val timeSlot = obj.optString("timeSlot", "10:00 AM - 11:30 AM")
                    val startTime = obj.optString("startTime", "")
                    val endTime = obj.optString("endTime", "")
                    val room = obj.optString("room", "Classroom")
                    val faculty = obj.optString("faculty", "")
                    val isLab = obj.optBoolean("isLab", false)
                    val colorKey = obj.optString("colorKey", if (isLab) "teal" else "purple")

                    classesList.add(
                        ParsedTimetableClass(
                            day = dayFormatted,
                            subject = subject,
                            timeSlot = timeSlot,
                            room = room,
                            faculty = faculty,
                            colorKey = colorKey,
                            startTime = startTime,
                            endTime = endTime,
                            isLab = isLab
                        )
                    )
                }
            }
        } catch (e: Exception) {
            summaryText = rawResponse
        }

        return Pair(summaryText.ifBlank { rawResponse }, classesList)
    }

    private fun parseAssignmentAiResponse(rawResponse: String): Triple<String, List<Triple<String, String, String>>, List<String>> {
        val cleanJson = rawResponse.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val assignments = mutableListOf<Triple<String, String, String>>()
        val questions = mutableListOf<String>()
        var summaryText = ""

        try {
            val json = JSONObject(cleanJson)
            summaryText = json.optString("summary", "")

            val arr = json.optJSONArray("assignments")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val sub = obj.optString("subject", "General")
                    val title = obj.optString("title", "Assignment Task")
                    val due = obj.optString("dueDate", "Tomorrow")
                    assignments.add(Triple(sub, title, due))
                }
            }

            val qArr = json.optJSONArray("questions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    questions.add(qArr.getString(i))
                }
            }
        } catch (e: Exception) {
            summaryText = rawResponse
        }

        return Triple(summaryText.ifBlank { rawResponse }, assignments, questions)
    }

    fun applyParsedTimetableToSchedule() {
        val classes = parsedClasses.value
        if (classes.isEmpty()) {
            showToast("No classes found to import.")
            return
        }

        docProcessingState.value = DocProcessingState.SAVING
        docProcessingStatusText.value = "Saving classes to timetable..."

        viewModelScope.launch {
            val entities = classes.map {
                TimetableEntity(
                    dayOfWeek = it.day,
                    subject = it.subject,
                    timeSlot = it.timeSlot,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    room = it.room,
                    facultyName = it.faculty,
                    isLab = it.isLab,
                    colorKey = it.colorKey
                )
            }
            timetableDao.insertClasses(entities)

            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                entities.forEach { FirestoreManager.saveTimetableClass(it) }
            }

            docProcessingState.value = DocProcessingState.COMPLETED
            docProcessingStatusText.value = "Imported ${classes.size} classes"
            showToast("All ${classes.size} classes imported into your Timetable! 🗓️")
            navigateTo(AppScreen.TIMETABLE)
        }
    }

    fun applyParsedHomework() {
        val hwItems = parsedAssignments.value
        if (hwItems.isEmpty()) return
        viewModelScope.launch {
            hwItems.forEach { (sub, title, due) ->
                addHomework(
                    subject = sub,
                    title = title,
                    dueDate = due,
                    priority = "High"
                )
            }
            docProcessingState.value = DocProcessingState.COMPLETED
            showToast("Assignments added to Homework & Planner! 📚")
            navigateTo(AppScreen.HOMEWORK)
        }
    }

    fun applyParsedQuestionsToNotes() {
        val content = analyzedDocResult.value ?: return
        val docName = uploadedDocName.value ?: "Question Paper Analysis"
        viewModelScope.launch {
            val note = NoteEntity(
                title = "Exam Prep: $docName",
                content = content,
                subject = "Exam Prep",
                category = "Revision Notes",
                tag = "Exam",
                colorKey = "purple",
                timestamp = System.currentTimeMillis()
            )
            val id = noteDao.insertNote(note)
            if (studentProfile.value?.cloudSyncEnabled == true && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveNote(note.copy(id = id))
            }
            docProcessingState.value = DocProcessingState.COMPLETED
            showToast("Saved to your Study Notes! 📝")
            navigateTo(AppScreen.NOTES)
        }
    }

    // Backup & Restore Actions
    fun createBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupManager.createBackupJson(getApplication())
            val current = studentProfile.value ?: StudentProfileEntity()
            profileDao.updateProfile(current.copy(lastBackupDate = System.currentTimeMillis()))
            onReady(json)
            showToast("Backup created successfully")
        }
    }

    fun previewBackupJson(json: String) {
        val preview = BackupManager.parsePreview(json)
        backupPreview.value = preview
    }

    fun restoreBackup(json: String, replaceAll: Boolean) {
        isRestoringBackup.value = true
        viewModelScope.launch {
            val success = BackupManager.restoreBackup(getApplication(), json, replaceAll)
            isRestoringBackup.value = false
            if (success) {
                showToast("Database restored successfully! 🎉")
                backupPreview.value = null
                navigateTo(AppScreen.DASHBOARD)
            } else {
                showToast("Failed to restore backup: Invalid format")
            }
        }
    }

    // Profile & Settings Actions
    fun updateProfile(name: String, major: String, email: String, college: String = "", course: String = "", semester: String = "", academicYear: String = "") {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(
                name = name.trim(),
                major = major.trim(),
                email = email.trim(),
                collegeName = if (college.isNotBlank()) college.trim() else current.collegeName,
                course = if (course.isNotBlank()) course.trim() else current.course,
                semester = if (semester.isNotBlank()) semester.trim() else current.semester,
                academicYear = if (academicYear.isNotBlank()) academicYear.trim() else current.academicYear
            )
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast("Profile updated")
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(isDarkMode = !current.isDarkMode)
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast(if (updated.isDarkMode) "Dark mode enabled" else "Light mode enabled")
        }
    }

    fun toggleCloudSync() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(cloudSyncEnabled = !current.cloudSyncEnabled)
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast(if (updated.cloudSyncEnabled) "Cloud synchronization enabled" else "Cloud sync paused (Local mode)")
        }
    }

    fun updateAiPreferences(style: String, length: String) {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(
                aiExplanationStyle = style,
                aiAnswerLength = length
            )
            profileDao.updateProfile(updated)
            if (updated.cloudSyncEnabled && FirebaseAuthHelper.isUserLoggedIn) {
                FirestoreManager.saveProfile(updated)
            }
            showToast("AI preferences saved")
        }
    }


    // ==========================================
    // Firebase Authentication Actions
    // ==========================================

    fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
        college: String = "",
        course: String = "",
        semester: String = "",
        academicYear: String = ""
    ) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signUpWithEmail(email, password, fullName)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val newProfile = StudentProfileEntity(
                    id = 1,
                    name = fullName.trim().ifBlank { "Student" },
                    email = email.trim(),
                    major = if (course.isNotBlank() && semester.isNotBlank()) "$course, $semester" else course.ifBlank { semester },
                    collegeName = college.trim(),
                    course = course.trim(),
                    semester = semester.trim(),
                    academicYear = academicYear.trim(),
                    lastBackupDate = System.currentTimeMillis()
                )
                profileDao.updateProfile(newProfile)
                FirestoreManager.saveProfile(newProfile, user.uid)
                FirestoreManager.syncLocalToFirestore(db)
                showToast("Account created successfully! Welcome to Arkin, ${newProfile.name} 🎉")
                navigateTo(AppScreen.DASHBOARD)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Sign up failed"
                authError.value = err
                showToast(err)
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signInWithEmail(email, password)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                if (remoteProfile != null) {
                    profileDao.updateProfile(remoteProfile)
                } else {
                    val current = studentProfile.value ?: StudentProfileEntity()
                    val updated = current.copy(
                        name = user.displayName ?: current.name,
                        email = user.email ?: current.email
                    )
                    profileDao.updateProfile(updated)
                    FirestoreManager.saveProfile(updated, user.uid)
                }
                FirestoreManager.syncFirestoreToLocal(db, merge = true)
                showToast("Welcome back, ${user.displayName ?: user.email}! 👋")
                navigateTo(AppScreen.DASHBOARD)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Login failed"
                authError.value = err
                showToast(err)
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signInWithGoogle(context)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                try {
                    val remoteProfile = FirestoreManager.fetchProfile(user.uid)
                    if (remoteProfile != null && remoteProfile.collegeName.isNotBlank() && remoteProfile.course.isNotBlank()) {
                        profileDao.updateProfile(remoteProfile)
                        FirestoreManager.syncFirestoreToLocal(db, merge = true)
                        showToast("Welcome back, ${user.displayName ?: user.email}! 🎉")
                        navigateTo(AppScreen.DASHBOARD)
                    } else {
                        val current = studentProfile.value ?: StudentProfileEntity()
                        val updated = current.copy(
                            name = user.displayName?.takeIf { it.isNotBlank() } ?: current.name,
                            email = user.email?.takeIf { it.isNotBlank() } ?: current.email
                        )
                        profileDao.updateProfile(updated)
                        needsProfileSetup.value = true
                    }
                } catch (e: Exception) {
                    val current = studentProfile.value ?: StudentProfileEntity()
                    val updated = current.copy(
                        name = user.displayName?.takeIf { it.isNotBlank() } ?: current.name,
                        email = user.email?.takeIf { it.isNotBlank() } ?: current.email
                    )
                    profileDao.updateProfile(updated)
                    needsProfileSetup.value = true
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex !is java.util.concurrent.CancellationException) {
                    val err = ex?.message ?: "Google Sign-In failed"
                    authError.value = err
                    showToast(err)
                }
            }
        }
    }

    fun completeGoogleProfileSetup(
        fullName: String,
        college: String,
        course: String,
        semester: String,
        academicYear: String
    ) {
        isAuthLoading.value = true
        viewModelScope.launch {
            val user = FirebaseAuthHelper.currentUser
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(
                name = fullName.trim().ifBlank { current.name.ifBlank { "Student" } },
                email = user?.email ?: current.email,
                major = if (course.isNotBlank() && semester.isNotBlank()) "$course, $semester" else course.ifBlank { semester },
                collegeName = college.trim(),
                course = course.trim(),
                semester = semester.trim(),
                academicYear = academicYear.trim(),
                lastBackupDate = System.currentTimeMillis()
            )
            profileDao.updateProfile(updated)
            if (user != null) {
                try {
                    FirestoreManager.saveProfile(updated, user.uid)
                } catch (e: Exception) {
                    // Ignore non-fatal sync error
                }
            }
            isAuthLoading.value = false
            needsProfileSetup.value = false
            showToast("Profile completed! Welcome to Arkin, ${updated.name} 🎉")
            navigateTo(AppScreen.DASHBOARD)
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            showToast("Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            val result = FirebaseAuthHelper.sendPasswordReset(email)
            if (result.isSuccess) {
                showToast("Password reset link sent to $email! Please check your inbox.")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to send reset link"
                showToast(err)
            }
        }
    }

    fun syncToFirestore() {
        if (!FirebaseAuthHelper.isUserLoggedIn) {
            showToast("Please log in to sync with Firebase Firestore")
            return
        }
        isFirestoreSyncing.value = true
        viewModelScope.launch {
            val result = FirestoreManager.syncLocalToFirestore(db)
            isFirestoreSyncing.value = false
            if (result.isSuccess) {
                lastFirestoreSyncTime.value = System.currentTimeMillis()
                showToast("Successfully backed up ${result.getOrNull()} items to Firestore! ☁️")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Sync failed"
                showToast("Firestore sync failed: $err")
            }
        }
    }

    fun syncFromFirestore() {
        if (!FirebaseAuthHelper.isUserLoggedIn) {
            showToast("Please log in to restore from Firebase Firestore")
            return
        }
        isFirestoreSyncing.value = true
        viewModelScope.launch {
            val result = FirestoreManager.syncFirestoreToLocal(db, merge = true)
            isFirestoreSyncing.value = false
            if (result.isSuccess) {
                lastFirestoreSyncTime.value = System.currentTimeMillis()
                showToast("Restored ${result.getOrNull()} items from Firestore! ☁️")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Restore failed"
                showToast("Firestore restore failed: $err")
            }
        }
    }

    fun logout() {
        FirebaseAuthHelper.signOut()
        navigateTo(AppScreen.AUTH, addToStack = false)
        showToast("Signed out of Firebase")
    }
}
