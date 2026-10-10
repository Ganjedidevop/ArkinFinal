package com.example.arkin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.arkin.data.dao.ExamResultDao
import com.example.arkin.data.dao.HomeworkDao
import com.example.arkin.data.dao.NoteDao
import com.example.arkin.data.dao.ReminderDao
import com.example.arkin.data.dao.StudentProfileDao
import com.example.arkin.data.dao.TaskDao
import com.example.arkin.data.dao.TimetableDao
import com.example.arkin.data.dao.VaultItemDao
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.StudentProfileEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.example.arkin.data.model.VaultItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        HomeworkEntity::class,
        TimetableEntity::class,
        ReminderEntity::class,
        ExamResultEntity::class,
        VaultItemEntity::class,
        StudentProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ArkinDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun timetableDao(): TimetableDao
    abstract fun reminderDao(): ReminderDao
    abstract fun examResultDao(): ExamResultDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun studentProfileDao(): StudentProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ArkinDatabase? = null

        fun getDatabase(context: Context): ArkinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArkinDatabase::class.java,
                    "arkin_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: ArkinDatabase) {
            // Profile
            db.studentProfileDao().insertProfile(
                StudentProfileEntity(
                    id = 1,
                    name = "Rohan Sharma",
                    email = "rohan.sharma@campus.edu",
                    major = "Computer Science",
                    university = "National Institute of Technology",
                    collegeName = "School of Engineering & Technology",
                    course = "B.Tech Computer Science & Engineering",
                    semester = "Semester 4",
                    academicYear = "2025-2026",
                    lockerPin = "1234",
                    currentStreak = 5,
                    isDarkMode = false,
                    cloudSyncEnabled = true
                )
            )

            // Initial Notes
            db.noteDao().insertNote(
                NoteEntity(
                    title = "DBMS Chapter 4: ACID & Normalization",
                    content = "Key Concepts:\n• Atomicity, Consistency, Isolation, Durability\n• Normal Forms: 1NF (atomic attributes), 2NF (no partial dependencies), 3NF (no transitive dependencies), BCNF.\n• Concurrency control using Two-Phase Locking (2PL).",
                    subject = "Database Systems",
                    semester = "Semester 4",
                    category = "Class Notes",
                    tag = "Study",
                    colorKey = "purple",
                    isPinned = true
                )
            )
            db.noteDao().insertNote(
                NoteEntity(
                    title = "OS Process Synchronization & Deadlocks",
                    content = "Four Necessary Coffman Conditions for Deadlock:\n1. Mutual Exclusion\n2. Hold and Wait\n3. No Preemption\n4. Circular Wait\n\nBanker's Algorithm: Resource allocation graph & safety check.",
                    subject = "Operating Systems",
                    semester = "Semester 4",
                    category = "Revision Notes",
                    tag = "Study",
                    colorKey = "teal"
                )
            )
            db.noteDao().insertNote(
                NoteEntity(
                    title = "AI Capstone Project Blueprint",
                    content = "1. Student AI Study Assistant (Arkin Architecture)\n2. Local Room persistence + Gemini multimodal document reasoning\n3. OCR pipeline for timetable extraction & deadline parsing.",
                    subject = "Software Engineering",
                    semester = "Semester 4",
                    category = "Important Notes",
                    tag = "Project",
                    colorKey = "orange"
                )
            )

            // Initial Tasks
            db.taskDao().insertTask(
                TaskEntity(
                    title = "Solve DBMS Practice Queries (LeetCode Database)",
                    description = "Focus on Joins, Subqueries and Window Functions.",
                    subject = "Database Systems",
                    priority = "High",
                    dueDate = "Today",
                    dueTime = "06:00 PM"
                )
            )
            db.taskDao().insertTask(
                TaskEntity(
                    title = "Read OS Memory Management (Chapter 8)",
                    description = "Review Paging, TLB hit ratio and Page replacement algorithms.",
                    subject = "Operating Systems",
                    isCompleted = true,
                    priority = "Medium",
                    dueDate = "Yesterday"
                )
            )
            db.taskDao().insertTask(
                TaskEntity(
                    title = "Complete AI Assignment 2 Draft",
                    description = "Implement A* search algorithm and heuristic comparison.",
                    subject = "Artificial Intelligence",
                    priority = "High",
                    dueDate = "Tomorrow",
                    dueTime = "11:59 PM"
                )
            )

            // Initial Homework / Assignments
            db.homeworkDao().insertHomework(
                HomeworkEntity(
                    subject = "Database Systems",
                    title = "Assignment 4: Relational Algebra & SQL Views",
                    description = "Solve problem set from textbook chapter 4. Write queries for hotel booking schema.",
                    teacherName = "Prof. Verma",
                    receivedDate = "05 Oct 2026",
                    dueDate = "Tomorrow",
                    dueTime = "11:59 PM",
                    priority = "High",
                    status = "In Progress",
                    colorKey = "purple",
                    extractedQuestionsJson = "[\"Q1: Define 3NF and BCNF with examples.\", \"Q2: Write SQL for inner join vs full outer join on customer/orders table.\", \"Q3: Explain dirty read and phantom read phenomena.\"]",
                    aiDraftAnswer = "[AI DRAFT]: 3NF ensures every non-prime attribute is non-transitively dependent on candidate keys. BCNF is stricter where for every X -> Y, X must be a superkey."
                )
            )
            db.homeworkDao().insertHomework(
                HomeworkEntity(
                    subject = "Operating Systems",
                    title = "Lab Report: Process Scheduling Simulation",
                    description = "Submit verified output screenshots for FCFS, Round Robin (Quantum=2) and SJF.",
                    teacherName = "Dr. Ananya Ray",
                    receivedDate = "02 Oct 2026",
                    dueDate = "Friday",
                    dueTime = "05:00 PM",
                    priority = "Medium",
                    status = "Not Started",
                    colorKey = "orange"
                )
            )

            // Initial Timetable with faculty and materials
            val classes = listOf(
                TimetableEntity(dayOfWeek = "Mon", subject = "Operating Systems", timeSlot = "10:00 AM - 11:00 AM", startTime = "10:00 AM", endTime = "11:00 AM", room = "Room 402", facultyName = "Dr. Ananya Ray", requiredMaterials = "Lecture Notebook", isLab = false, colorKey = "orange"),
                TimetableEntity(dayOfWeek = "Mon", subject = "Database Systems", timeSlot = "11:30 AM - 01:00 PM", startTime = "11:30 AM", endTime = "01:00 PM", room = "Lab 2", facultyName = "Prof. Verma", requiredMaterials = "Lab Manual & Practical Record", isLab = true, colorKey = "purple"),
                TimetableEntity(dayOfWeek = "Mon", subject = "Machine Learning", timeSlot = "02:00 PM - 03:30 PM", startTime = "02:00 PM", endTime = "03:30 PM", room = "Room 305", facultyName = "Dr. S. K. Gupta", requiredMaterials = "Math Reference Book", isLab = false, colorKey = "teal"),

                TimetableEntity(dayOfWeek = "Tue", subject = "Computer Networks", timeSlot = "09:00 AM - 10:30 AM", startTime = "09:00 AM", endTime = "10:30 AM", room = "Room 201", facultyName = "Prof. Mehra", requiredMaterials = "Networking Notes", isLab = false, colorKey = "blue"),
                TimetableEntity(dayOfWeek = "Tue", subject = "Software Engineering", timeSlot = "11:00 AM - 12:30 PM", startTime = "11:00 AM", endTime = "12:30 PM", room = "Hall A", facultyName = "Dr. Preeti Singh", requiredMaterials = "Case Study Printout", isLab = false, colorKey = "purple"),

                TimetableEntity(dayOfWeek = "Wed", subject = "Operating Systems", timeSlot = "10:00 AM - 11:00 AM", startTime = "10:00 AM", endTime = "11:00 AM", room = "Room 402", facultyName = "Dr. Ananya Ray", requiredMaterials = "Lecture Notebook", isLab = false, colorKey = "orange"),
                TimetableEntity(dayOfWeek = "Wed", subject = "Algorithm Design Lab", timeSlot = "02:00 PM - 04:00 PM", startTime = "02:00 PM", endTime = "04:00 PM", room = "Computing Lab 1", facultyName = "Prof. Mehra", requiredMaterials = "Algorithm Record File", isLab = true, colorKey = "teal"),

                TimetableEntity(dayOfWeek = "Thu", subject = "Database Systems", timeSlot = "10:00 AM - 11:30 AM", startTime = "10:00 AM", endTime = "11:30 AM", room = "Room 303", facultyName = "Prof. Verma", requiredMaterials = "Lecture Notebook", isLab = false, colorKey = "purple"),
                TimetableEntity(dayOfWeek = "Thu", subject = "Artificial Intelligence", timeSlot = "01:30 PM - 03:00 PM", startTime = "01:30 PM", endTime = "03:00 PM", room = "AI Seminar Room", facultyName = "Dr. S. K. Gupta", requiredMaterials = "Laptop with Python", isLab = false, colorKey = "teal"),

                TimetableEntity(dayOfWeek = "Fri", subject = "Ethics in Computing", timeSlot = "11:00 AM - 12:00 PM", startTime = "11:00 AM", endTime = "12:00 PM", room = "Room 105", facultyName = "Dr. Preeti Singh", requiredMaterials = "Seminar Notes", isLab = false, colorKey = "pink"),
                TimetableEntity(dayOfWeek = "Fri", subject = "OS Practical Lab", timeSlot = "02:00 PM - 04:00 PM", startTime = "02:00 PM", endTime = "04:00 PM", room = "Systems Lab", facultyName = "Dr. Ananya Ray", requiredMaterials = "OS Practical Observation Notebook", isLab = true, colorKey = "blue")
            )
            classes.forEach { db.timetableDao().insertClass(it) }

            // Initial Exam Results for Study Analyser
            db.examResultDao().insertExamResult(
                ExamResultEntity(
                    subject = "Database Systems",
                    examName = "Midterm Exam",
                    marksObtained = 46f,
                    totalMarks = 50f,
                    semester = "Semester 4"
                )
            )
            db.examResultDao().insertExamResult(
                ExamResultEntity(
                    subject = "Operating Systems",
                    examName = "Midterm Exam",
                    marksObtained = 42f,
                    totalMarks = 50f,
                    semester = "Semester 4"
                )
            )
            db.examResultDao().insertExamResult(
                ExamResultEntity(
                    subject = "Computer Networks",
                    examName = "Internal Assessment 1",
                    marksObtained = 23f,
                    totalMarks = 25f,
                    semester = "Semester 4"
                )
            )
            db.examResultDao().insertExamResult(
                ExamResultEntity(
                    subject = "Discrete Mathematics",
                    examName = "Semester 3 Final",
                    marksObtained = 88f,
                    totalMarks = 100f,
                    semester = "Semester 3"
                )
            )

            // Initial Reminders
            db.reminderDao().insertReminder(
                ReminderEntity(
                    title = "Semester Examination Fee Submission",
                    dueText = "In 2 days (11 Oct 2026)",
                    iconKey = "currency",
                    colorKey = "yellow"
                )
            )
            db.reminderDao().insertReminder(
                ReminderEntity(
                    title = "Return Library Book: 'Database System Concepts (Silberschatz)'",
                    dueText = "Tomorrow, 04:00 PM",
                    iconKey = "book",
                    colorKey = "blue"
                )
            )

            // Initial Secure Vault Items
            db.vaultItemDao().insertVaultItem(
                VaultItemEntity(
                    title = "College ID & Roll Number Credentials",
                    category = "ID Document",
                    content = "Student ID: 22BCS094\nUniversity Registration: NIT/2022/CS/094\nCampus Wi-Fi Portal Username: 22bcs094@campus.edu"
                )
            )
            db.vaultItemDao().insertVaultItem(
                VaultItemEntity(
                    title = "Academic Portal Recovery Key",
                    category = "Password Hint",
                    content = "Primary Recovery Passphrase: quantum-matrix-scholar-2026\nPortal Pin: 9842"
                )
            )
        }
    }
}
