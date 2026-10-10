package com.example.arkin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val subject: String = "General",
    val semester: String = "Semester 4",
    val category: String = "Class Notes", // Important Notes, Class Notes, Personal Notes, Revision Notes, Scripts
    val tag: String = "Study", // Study, Personal, Project, Exam, Secret
    val colorKey: String = "purple", // purple, teal, orange, blue, pink
    val timestamp: Long = System.currentTimeMillis(),
    val isSecure: Boolean = false,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val attachmentUri: String? = null,
    val attachmentName: String? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // High, Medium, Low
    val dueDate: String = "Today",
    val dueTime: String = "5:00 PM",
    val startDate: String = "Today",
    val linkedAssignmentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "homework")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val title: String,
    val description: String = "",
    val teacherName: String = "",
    val receivedDate: String = "Recent",
    val dueDate: String = "Today",
    val dueTime: String = "11:59 PM",
    val priority: String = "Medium", // High, Medium, Low
    val status: String = "Not Started", // Not Started, In Progress, Submitted, Completed
    val isDone: Boolean = false,
    val colorKey: String = "purple",
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 1440, // default 1 day (1440 mins)
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: String? = null,
    val extractedQuestionsJson: String? = null,
    val aiDraftAnswer: String? = null
)

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: String, // Mon, Tue, Wed, Thu, Fri, Sat
    val subject: String,
    val timeSlot: String,
    val startTime: String = "",
    val endTime: String = "",
    val room: String,
    val facultyName: String = "",
    val requiredMaterials: String = "", // e.g. "Practical Lab Record", "Graph Sheet"
    val isLab: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val colorKey: String = "purple"
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dueText: String,
    val iconKey: String = "bell", // bell, book, currency, alert
    val colorKey: String = "yellow",
    val isCompleted: Boolean = false
)

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val examName: String, // Midterm Exam, Final Exam, Quiz, Practical Viva
    val marksObtained: Float,
    val totalMarks: Float,
    val semester: String = "Semester 4",
    val dateRecorded: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Private Note", // ID Document, Certificate, Private Note, Password Hint
    val content: String,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Rohan Sharma",
    val email: String = "rohan.sharma@campus.edu",
    val major: String = "Computer Science",
    val university: String = "National Institute of Technology",
    val collegeName: String = "School of Engineering & Technology",
    val course: String = "B.Tech Computer Science & Engineering",
    val semester: String = "Semester 4",
    val academicYear: String = "2025-2026",
    val lockerPin: String = "1234",
    val currentStreak: Int = 5,
    val isDarkMode: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
    val lastBackupDate: Long = 0L,
    val aiExplanationStyle: String = "Simple & Structured", // Simple & Structured, Detailed Academic, Exam-Oriented
    val aiAnswerLength: String = "Standard" // Concise, Standard, Comprehensive
)
