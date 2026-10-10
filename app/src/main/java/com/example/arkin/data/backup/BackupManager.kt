package com.example.arkin.data.backup

import android.content.Context
import com.example.arkin.data.ArkinDatabase
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class BackupPreview(
    val version: Int,
    val timestamp: Long,
    val notesCount: Int,
    val tasksCount: Int,
    val homeworkCount: Int,
    val timetableCount: Int,
    val examResultsCount: Int,
    val isValid: Boolean,
    val rawJson: String
)

object BackupManager {

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = ArkinDatabase.getDatabase(context)

        val notes = db.noteDao().getAllNotesSync()
        val tasks = db.taskDao().getAllTasksSync()
        val homework = db.homeworkDao().getAllHomeworkSync()
        val timetable = db.timetableDao().getAllClassesSync()
        val examResults = db.examResultDao().getAllExamResultsSync()
        val reminders = db.reminderDao().getAllRemindersSync()
        val profile = db.studentProfileDao().getProfileSync()

        val root = JSONObject().apply {
            put("app", "Arkin")
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("studentName", profile?.name ?: "Student")
            put("studentEmail", profile?.email ?: "")

            // Notes
            val notesArr = JSONArray()
            notes.forEach { note ->
                notesArr.put(JSONObject().apply {
                    put("title", note.title)
                    put("content", note.content)
                    put("subject", note.subject)
                    put("semester", note.semester)
                    put("category", note.category)
                    put("tag", note.tag)
                    put("colorKey", note.colorKey)
                    put("timestamp", note.timestamp)
                    put("isPinned", note.isPinned)
                    put("isFavorite", note.isFavorite)
                    put("isArchived", note.isArchived)
                })
            }
            put("notes", notesArr)

            // Tasks
            val tasksArr = JSONArray()
            tasks.forEach { task ->
                tasksArr.put(JSONObject().apply {
                    put("title", task.title)
                    put("description", task.description)
                    put("subject", task.subject)
                    put("isCompleted", task.isCompleted)
                    put("priority", task.priority)
                    put("dueDate", task.dueDate)
                    put("dueTime", task.dueTime)
                })
            }
            put("tasks", tasksArr)

            // Homework
            val hwArr = JSONArray()
            homework.forEach { hw ->
                hwArr.put(JSONObject().apply {
                    put("subject", hw.subject)
                    put("title", hw.title)
                    put("description", hw.description)
                    put("teacherName", hw.teacherName)
                    put("dueDate", hw.dueDate)
                    put("dueTime", hw.dueTime)
                    put("priority", hw.priority)
                    put("status", hw.status)
                    put("isDone", hw.isDone)
                    put("colorKey", hw.colorKey)
                    put("aiDraftAnswer", hw.aiDraftAnswer ?: "")
                })
            }
            put("homework", hwArr)

            // Timetable
            val ttArr = JSONArray()
            timetable.forEach { tt ->
                ttArr.put(JSONObject().apply {
                    put("dayOfWeek", tt.dayOfWeek)
                    put("subject", tt.subject)
                    put("timeSlot", tt.timeSlot)
                    put("startTime", tt.startTime)
                    put("endTime", tt.endTime)
                    put("room", tt.room)
                    put("facultyName", tt.facultyName)
                    put("requiredMaterials", tt.requiredMaterials)
                    put("isLab", tt.isLab)
                    put("colorKey", tt.colorKey)
                })
            }
            put("timetable", ttArr)

            // Exam Results
            val examArr = JSONArray()
            examResults.forEach { exam ->
                examArr.put(JSONObject().apply {
                    put("subject", exam.subject)
                    put("examName", exam.examName)
                    put("marksObtained", exam.marksObtained.toDouble())
                    put("totalMarks", exam.totalMarks.toDouble())
                    put("semester", exam.semester)
                })
            }
            put("examResults", examArr)

            // Reminders
            val remArr = JSONArray()
            reminders.forEach { rem ->
                remArr.put(JSONObject().apply {
                    put("title", rem.title)
                    put("dueText", rem.dueText)
                    put("iconKey", rem.iconKey)
                    put("colorKey", rem.colorKey)
                })
            }
            put("reminders", remArr)
        }

        // Add checksum
        val jsonString = root.toString(2)
        val md5 = MessageDigest.getInstance("MD5").digest(jsonString.toByteArray()).joinToString("") { "%02x".format(it) }
        root.put("checksum", md5)

        root.toString(2)
    }

    fun parsePreview(jsonString: String): BackupPreview {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val notesCount = root.optJSONArray("notes")?.length() ?: 0
            val tasksCount = root.optJSONArray("tasks")?.length() ?: 0
            val hwCount = root.optJSONArray("homework")?.length() ?: 0
            val ttCount = root.optJSONArray("timetable")?.length() ?: 0
            val examCount = root.optJSONArray("examResults")?.length() ?: 0

            BackupPreview(
                version = version,
                timestamp = timestamp,
                notesCount = notesCount,
                tasksCount = tasksCount,
                homeworkCount = hwCount,
                timetableCount = ttCount,
                examResultsCount = examCount,
                isValid = root.optString("app") == "Arkin" || notesCount > 0,
                rawJson = jsonString
            )
        } catch (e: Exception) {
            BackupPreview(0, 0L, 0, 0, 0, 0, 0, false, jsonString)
        }
    }

    suspend fun restoreBackup(context: Context, jsonString: String, replaceAll: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val db = ArkinDatabase.getDatabase(context)

            if (replaceAll) {
                db.noteDao().clearAllNotes()
                db.taskDao().clearAllTasks()
                db.homeworkDao().clearAllHomework()
                db.timetableDao().clearAllClasses()
                db.examResultDao().clearAllExamResults()
            }

            // Restore Notes
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                val notes = mutableListOf<NoteEntity>()
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    notes.add(
                        NoteEntity(
                            title = obj.optString("title", "Untitled Note"),
                            content = obj.optString("content", ""),
                            subject = obj.optString("subject", "General"),
                            semester = obj.optString("semester", "Semester 4"),
                            category = obj.optString("category", "Class Notes"),
                            tag = obj.optString("tag", "Study"),
                            colorKey = obj.optString("colorKey", "purple"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isPinned = obj.optBoolean("isPinned", false),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
                db.noteDao().insertNotes(notes)
            }

            // Restore Tasks
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                val tasks = mutableListOf<TaskEntity>()
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.getJSONObject(i)
                    tasks.add(
                        TaskEntity(
                            title = obj.optString("title", "Task"),
                            description = obj.optString("description", ""),
                            subject = obj.optString("subject", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            priority = obj.optString("priority", "Medium"),
                            dueDate = obj.optString("dueDate", "Today"),
                            dueTime = obj.optString("dueTime", "05:00 PM")
                        )
                    )
                }
                db.taskDao().insertTasks(tasks)
            }

            // Restore Homework
            val hwArr = root.optJSONArray("homework")
            if (hwArr != null) {
                val homework = mutableListOf<HomeworkEntity>()
                for (i in 0 until hwArr.length()) {
                    val obj = hwArr.getJSONObject(i)
                    homework.add(
                        HomeworkEntity(
                            subject = obj.optString("subject", "General"),
                            title = obj.optString("title", "Assignment"),
                            description = obj.optString("description", ""),
                            teacherName = obj.optString("teacherName", ""),
                            dueDate = obj.optString("dueDate", "Today"),
                            dueTime = obj.optString("dueTime", "11:59 PM"),
                            priority = obj.optString("priority", "Medium"),
                            status = obj.optString("status", "Not Started"),
                            isDone = obj.optBoolean("isDone", false),
                            colorKey = obj.optString("colorKey", "purple"),
                            aiDraftAnswer = if (obj.has("aiDraftAnswer") && !obj.isNull("aiDraftAnswer")) obj.getString("aiDraftAnswer") else null
                        )
                    )
                }
                db.homeworkDao().insertHomeworkList(homework)
            }

            // Restore Timetable
            val ttArr = root.optJSONArray("timetable")
            if (ttArr != null) {
                val classes = mutableListOf<TimetableEntity>()
                for (i in 0 until ttArr.length()) {
                    val obj = ttArr.getJSONObject(i)
                    classes.add(
                        TimetableEntity(
                            dayOfWeek = obj.optString("dayOfWeek", "Mon"),
                            subject = obj.optString("subject", "Class"),
                            timeSlot = obj.optString("timeSlot", "10:00 AM - 11:00 AM"),
                            startTime = obj.optString("startTime", "10:00 AM"),
                            endTime = obj.optString("endTime", "11:00 AM"),
                            room = obj.optString("room", "Room 101"),
                            facultyName = obj.optString("facultyName", ""),
                            requiredMaterials = obj.optString("requiredMaterials", ""),
                            isLab = obj.optBoolean("isLab", false),
                            colorKey = obj.optString("colorKey", "purple")
                        )
                    )
                }
                db.timetableDao().insertClasses(classes)
            }

            // Restore Exam Results
            val examArr = root.optJSONArray("examResults")
            if (examArr != null) {
                val exams = mutableListOf<ExamResultEntity>()
                for (i in 0 until examArr.length()) {
                    val obj = examArr.getJSONObject(i)
                    exams.add(
                        ExamResultEntity(
                            subject = obj.optString("subject", "General"),
                            examName = obj.optString("examName", "Midterm"),
                            marksObtained = obj.optDouble("marksObtained", 0.0).toFloat(),
                            totalMarks = obj.optDouble("totalMarks", 100.0).toFloat(),
                            semester = obj.optString("semester", "Semester 4")
                        )
                    )
                }
                db.examResultDao().insertExamResults(exams)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
