package com.example.arkin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.ai.GeminiHelper
import com.example.arkin.data.backup.BackupManager
import com.example.arkin.data.backup.BackupPreview
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

    init {
        viewModelScope.launch {
            val prof = studentProfile.value
            if (prof == null) {
                ArkinDatabase.populateInitialData(db)
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
                noteDao.updateNote(
                    existing.copy(
                        title = title,
                        content = content,
                        tag = tag,
                        colorKey = colorKey,
                        subject = subject,
                        category = category,
                        timestamp = System.currentTimeMillis()
                    )
                )
                showToast("Note updated successfully")
            } else {
                noteDao.insertNote(
                    NoteEntity(
                        title = title,
                        content = content,
                        tag = tag,
                        colorKey = colorKey,
                        subject = subject,
                        category = category,
                        timestamp = System.currentTimeMillis(),
                        isSecure = isSecure
                    )
                )
                showToast(if (isSecure) "Saved to Secure Locker" else "Note created")
            }
            closeNoteDialog()
        }
    }

    fun toggleFavoriteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun toggleArchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isArchived = !note.isArchived))
            showToast(if (!note.isArchived) "Note archived" else "Note unarchived")
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            showToast("Note deleted")
        }
    }

    // Tasks Actions
    fun addTask(title: String, priority: String = "Medium", description: String = "", subject: String = "", dueDate: String = "Today") {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskDao.insertTask(
                TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    priority = priority,
                    dueDate = dueDate
                )
            )
            showToast("Task added")
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updated)
            if (updated.isCompleted) {
                showToast("Task completed! 🎉")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
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
            val id = homeworkDao.insertHomework(
                HomeworkEntity(
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
            )
            // Automatically add as a planner task
            taskDao.insertTask(
                TaskEntity(
                    title = "Assignment: $title ($subject)",
                    description = description,
                    subject = subject,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    linkedAssignmentId = id
                )
            )
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
            _isAiLoading.value = false
            showToast("AI Solution generated!")
        }
    }

    fun saveAssignmentAnswerDraft(hw: HomeworkEntity, draft: String) {
        viewModelScope.launch {
            val updated = hw.copy(aiDraftAnswer = draft)
            homeworkDao.updateHomework(updated)
            selectedAssignment.value = updated
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
            showToast(if (updated.isDone) "Marked as done!" else "Marked as pending")
        }
    }

    fun deleteHomework(hw: HomeworkEntity) {
        viewModelScope.launch {
            homeworkDao.deleteHomework(hw)
            if (selectedAssignment.value?.id == hw.id) {
                goBack()
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
            timetableDao.insertClass(
                TimetableEntity(
                    dayOfWeek = selectedDay.value,
                    subject = subject.trim(),
                    timeSlot = timeSlot.trim(),
                    room = room.trim(),
                    facultyName = facultyName.trim(),
                    requiredMaterials = requiredMaterials.trim(),
                    isLab = isLab,
                    colorKey = colorKey
                )
            )
            showToast("Class added to timetable")
        }
    }

    fun deleteClass(entry: TimetableEntity) {
        viewModelScope.launch {
            timetableDao.deleteClass(entry)
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
            reminderDao.insertReminder(
                ReminderEntity(
                    title = title.trim(),
                    dueText = dueText.trim(),
                    iconKey = iconKey,
                    colorKey = colorKey
                )
            )
            showToast("Reminder created")
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            showToast("Reminder dismissed")
        }
    }

    // Exam Results (Study Analyser)
    fun addExamResult(subject: String, examName: String, marksObtained: Float, totalMarks: Float, semester: String = "Semester 4") {
        if (subject.isBlank() || examName.isBlank() || totalMarks <= 0f) return
        viewModelScope.launch {
            examResultDao.insertExamResult(
                ExamResultEntity(
                    subject = subject.trim(),
                    examName = examName.trim(),
                    marksObtained = marksObtained,
                    totalMarks = totalMarks,
                    semester = semester.trim()
                )
            )
            showToast("Exam marks recorded!")
        }
    }

    fun deleteExamResult(result: ExamResultEntity) {
        viewModelScope.launch {
            examResultDao.deleteExamResult(result)
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
            profileDao.updateProfile(
                current.copy(
                    name = name.trim(),
                    major = major.trim(),
                    email = email.trim(),
                    collegeName = if (college.isNotBlank()) college.trim() else current.collegeName,
                    course = if (course.isNotBlank()) course.trim() else current.course,
                    semester = if (semester.isNotBlank()) semester.trim() else current.semester,
                    academicYear = if (academicYear.isNotBlank()) academicYear.trim() else current.academicYear
                )
            )
            showToast("Profile updated")
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(isDarkMode = !current.isDarkMode)
            profileDao.updateProfile(updated)
            showToast(if (updated.isDarkMode) "Dark mode enabled" else "Light mode enabled")
        }
    }

    fun toggleCloudSync() {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            val updated = current.copy(cloudSyncEnabled = !current.cloudSyncEnabled)
            profileDao.updateProfile(updated)
            showToast(if (updated.cloudSyncEnabled) "Cloud synchronization enabled" else "Cloud sync paused (Local mode)")
        }
    }

    fun updateAiPreferences(style: String, length: String) {
        viewModelScope.launch {
            val current = studentProfile.value ?: StudentProfileEntity()
            profileDao.updateProfile(
                current.copy(
                    aiExplanationStyle = style,
                    aiAnswerLength = length
                )
            )
            showToast("AI preferences saved")
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            ArkinDatabase.populateInitialData(db)
            showToast("Demo data reloaded")
        }
    }

    fun logout() {
        navigateTo(AppScreen.AUTH, addToStack = false)
        showToast("Logged out")
    }
}
