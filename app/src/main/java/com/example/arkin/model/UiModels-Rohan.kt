package com.example.arkin.model

enum class AppScreen {
    SPLASH,
    AUTH,
    DASHBOARD,
    NOTES,
    TODO,
    HOMEWORK,
    TIMETABLE,
    AI_ASSISTANT,
    AI_DOC_ORGANIZER,
    LOCKER,
    REMINDERS,
    ANALYTICS,
    PROFILE,
    BACKUP_RESTORE,
    ASSIGNMENT_DETAIL
}

enum class BottomNavTab {
    HOME,
    NOTES,
    PLANNER,
    AI_ASSISTANT,
    PROFILE
}

enum class DocUploadType {
    TIMETABLE,
    QUESTION_PAPER,
    ASSIGNMENT,
    LECTURE_NOTE
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DayScheduleItem(
    val day: String,
    val dateNumber: String,
    val isToday: Boolean = false
)

enum class DocProcessingState {
    IDLE,
    SELECTING,
    READING_DOCUMENT,
    EXTRACTING_TEXT,
    ANALYZING_AI,
    REVIEWING,
    SAVING,
    COMPLETED,
    FAILED
}

data class ParsedTimetableClass(
    val day: String,
    val subject: String,
    val timeSlot: String,
    val room: String,
    val faculty: String = "",
    val colorKey: String = "purple",
    val startTime: String = "",
    val endTime: String = "",
    val isLab: Boolean = false
)
