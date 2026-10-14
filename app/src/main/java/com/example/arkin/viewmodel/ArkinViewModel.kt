package com.example.arkin.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.ai.GeminiHelper
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
import com.example.arkin.model.AppScreen
import com.example.arkin.model.BottomNavTab
import com.example.arkin.model.ChatMessage
import com.example.arkin.model.DocUploadType
import com.example.arkin.model.ParsedTimetableClass
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val uploadedDocName = MutableStateFlow<String?>("Engineering_Weekly_Timetable.pdf")
    val uploadedDocType = MutableStateFlow(DocUploadType.TIMETABLE)
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
    val isFirestoreSyncing = MutableStateFlow(false)
    val lastFirestoreSyncTime = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            val prof = profileDao.getProfileSync()
            if (prof == null) {
                ArkinDatabase.populateInitialData(db)
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
        category: String = "Class Notes"
    ) {
        viewModelScope.launch {
            val existing = editingNote.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    content = content,
                    tag = tag,
                    colorKey = colorKey,
                    subject = subject,
                    category = category,
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
        extractedQuestions: String? = null
    ) {
        if (title.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
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
                extractedQuestionsJson = extractedQuestions
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
            val answer = GeminiHelper.solveAssignmentQuestion(hw.title, question)
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
                    attachmentName = attachmentName
                )
            )
            showToast("Item securely saved in Vault 🔒")
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
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
                val correctPin = studentProfile.value?.lockerPin ?: "1234"
                if (newPin == correctPin) {
                    _isVaultUnlocked.value = true
                    _enteredPin.value = ""
                    showToast("Vault unlocked 🔓")
                } else {
                    _isPinError.value = true
                    showToast("Incorrect PIN")
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(400)
                        _enteredPin.value = ""
                        _isPinError.value = false
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
            viewModelScope.launch {
                val current = studentProfile.value ?: StudentProfileEntity()
                profileDao.updateProfile(current.copy(lockerPin = newPin))
                showToast("Locker PIN updated to $newPin")
            }
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
            val aiResponse = GeminiHelper.askAssistant(trimmed, notesSummary, history)
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
        analyzedDocResult.value = null
        parsedClasses.value = emptyList()
        parsedAssignments.value = emptyList()
        parsedExamQuestions.value = emptyList()
        uploadedDocName.value = when (type) {
            DocUploadType.TIMETABLE -> "Semester_Schedule_2026.pdf"
            DocUploadType.QUESTION_PAPER -> "DBMS_Midterm_Paper.pdf"
            DocUploadType.ASSIGNMENT -> "OS_Lab_Assignment_4.docx"
            DocUploadType.LECTURE_NOTE -> "Chapter_5_Distributed_Systems.png"
        }
    }

    fun analyzeDocument(filename: String, customPrompt: String = "") {
        uploadedDocName.value = filename
        isAnalyzingDoc.value = true
        analyzedDocResult.value = null

        viewModelScope.launch {
            val type = uploadedDocType.value
            val prompt = when (type) {
                DocUploadType.TIMETABLE -> {
                    "Analyze this student class timetable document '$filename'. " +
                            "Extract each day's lectures, start/end time, subject names, classrooms, and faculty. " +
                            "Organize into a clean weekly schedule with recommendations for study gaps."
                }
                DocUploadType.QUESTION_PAPER -> {
                    "Analyze this university examination question paper '$filename'. " +
                            "Break down key topics tested, marks distribution, step-by-step model solutions for the hardest questions, and high-yield revision tips."
                }
                DocUploadType.ASSIGNMENT -> {
                    "Analyze this student assignment sheet '$filename'. " +
                            "Identify the course, assignment title, due date, deliverable checklist, and recommended problem-solving steps."
                }
                DocUploadType.LECTURE_NOTE -> {
                    "Analyze and summarize this lecture note document '$filename' into structured exam revision bullet points, formulas, and flashcards."
                }
            } + if (customPrompt.isNotBlank()) "\nStudent notes: $customPrompt" else ""

            val result = GeminiHelper.askAssistant(prompt)
            analyzedDocResult.value = result
            isAnalyzingDoc.value = false

            when (type) {
                DocUploadType.TIMETABLE -> {
                    parsedClasses.value = listOf(
                        ParsedTimetableClass("Mon", "Advanced DBMS", "09:00 AM - 10:30 AM", "Lab 3", "Prof. Verma", "purple"),
                        ParsedTimetableClass("Mon", "Distributed Computing", "11:00 AM - 12:30 PM", "Hall 102", "Dr. S. K. Gupta", "teal"),
                        ParsedTimetableClass("Wed", "AI & Machine Learning", "10:00 AM - 11:30 AM", "Room 405", "Dr. Preeti Singh", "blue"),
                        ParsedTimetableClass("Thu", "Computer Architecture", "01:30 PM - 03:00 PM", "Room 201", "Prof. Mehra", "orange"),
                        ParsedTimetableClass("Fri", "Cloud Systems Lab", "02:00 PM - 04:30 PM", "AI Lab 1", "Dr. Ananya Ray", "pink")
                    )
                }
                DocUploadType.ASSIGNMENT -> {
                    parsedAssignments.value = listOf(
                        Triple("Operating Systems", "Lab Assignment 4: Process Synchronization", "Next Monday, 11:59 PM"),
                        Triple("Distributed Systems", "RPC & RMI Implementation Exercise", "Friday, 5:00 PM")
                    )
                }
                DocUploadType.QUESTION_PAPER -> {
                    parsedExamQuestions.value = listOf(
                        "Explain 2-Phase Locking protocol (2PL) and prove how it guarantees serializability.",
                        "Compare Paging vs Segmentation in virtual memory architectures.",
                        "Solve the Deadlock Banker's Algorithm with given allocation and max matrices."
                    )
                }
                else -> {}
            }
            showToast("Document analyzed & organized by Arkin AI ✨")
        }
    }

    fun applyParsedTimetableToSchedule() {
        val classes = parsedClasses.value
        if (classes.isEmpty()) return
        viewModelScope.launch {
            classes.forEach {
                timetableDao.insertClass(
                    TimetableEntity(
                        dayOfWeek = it.day,
                        subject = it.subject,
                        timeSlot = it.timeSlot,
                        room = it.room,
                        facultyName = it.faculty,
                        colorKey = it.colorKey
                    )
                )
            }
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
            showToast("Assignments added to Homework & Planner! 📚")
            navigateTo(AppScreen.HOMEWORK)
        }
    }

    fun applyParsedQuestionsToNotes() {
        val content = analyzedDocResult.value ?: return
        val docName = uploadedDocName.value ?: "Question Paper Analysis"
        viewModelScope.launch {
            noteDao.insertNote(
                NoteEntity(
                    title = "Exam Prep: $docName",
                    content = content,
                    subject = "Exam Prep",
                    category = "Revision Notes",
                    tag = "Exam",
                    colorKey = "purple",
                    timestamp = System.currentTimeMillis()
                )
            )
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

    fun resetToDemoData() {
        viewModelScope.launch {
            ArkinDatabase.populateInitialData(db)
            showToast("Demo data reloaded")
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
                    name = fullName.ifBlank { "Student" },
                    email = email.trim(),
                    major = if (course.isNotBlank()) "$course, $semester" else "Computer Science",
                    collegeName = college.ifBlank { "National Institute of Technology" },
                    course = course.ifBlank { "B.Tech Computer Science" },
                    semester = semester.ifBlank { "Semester 4" },
                    academicYear = academicYear.ifBlank { "2025-2026" },
                    lastBackupDate = System.currentTimeMillis()
                )
                profileDao.updateProfile(newProfile)
                FirestoreManager.saveProfile(newProfile, user.uid)
                // Upload sample student productivity data to new Firebase user
                FirestoreManager.syncLocalToFirestore(db)
                showToast("Account created successfully! Welcome to Arkin, $fullName 🎉")
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
                showToast("Signed in with Google! Welcome, ${user.displayName ?: user.email} 🎉")
                navigateTo(AppScreen.DASHBOARD)
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
