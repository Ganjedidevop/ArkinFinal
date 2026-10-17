package com.example.arkin.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.arkin.data.model.ExamResultEntity
import com.example.arkin.data.model.HomeworkEntity
import com.example.arkin.data.model.NoteEntity
import com.example.arkin.data.model.ReminderEntity
import com.example.arkin.data.model.StudentProfileEntity
import com.example.arkin.data.model.TaskEntity
import com.example.arkin.data.model.TimetableEntity
import com.example.arkin.data.model.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isSecure = :secure AND isArchived = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getNotes(secure: Boolean): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isSecure = 0 AND isArchived = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllPublicNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework ORDER BY isDone ASC, id ASC")
    fun getAllHomework(): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework WHERE id = :id")
    suspend fun getHomeworkById(id: Long): HomeworkEntity?

    @Query("SELECT * FROM homework")
    suspend fun getAllHomeworkSync(): List<HomeworkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(hw: HomeworkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkList(list: List<HomeworkEntity>)

    @Update
    suspend fun updateHomework(hw: HomeworkEntity)

    @Delete
    suspend fun deleteHomework(hw: HomeworkEntity)

    @Query("DELETE FROM homework")
    suspend fun clearAllHomework()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY id ASC")
    fun getClassesForDay(day: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable ORDER BY id ASC")
    fun getAllClasses(): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable")
    suspend fun getAllClassesSync(): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(entry: TimetableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<TimetableEntity>)

    @Delete
    suspend fun deleteClass(entry: TimetableEntity)

    @Query("DELETE FROM timetable")
    suspend fun clearAllClasses()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, id ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersSync(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
}

@Dao
interface ExamResultDao {
    @Query("SELECT * FROM exam_results ORDER BY dateRecorded DESC")
    fun getAllExamResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results")
    suspend fun getAllExamResultsSync(): List<ExamResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResult(result: ExamResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResults(results: List<ExamResultEntity>)

    @Delete
    suspend fun deleteExamResult(result: ExamResultEntity)

    @Query("DELETE FROM exam_results")
    suspend fun clearAllExamResults()
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items")
    suspend fun getAllVaultItemsSync(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity): Long

    @Delete
    suspend fun deleteVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items")
    suspend fun clearAllVaultItems()
}

@Dao
interface StudentProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getProfile(): Flow<StudentProfileEntity?>

    @Query("SELECT * FROM student_profile WHERE id = 1")
    suspend fun getProfileSync(): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StudentProfileEntity)

    @Update
    suspend fun updateProfile(profile: StudentProfileEntity)

    @Query("DELETE FROM student_profile")
    suspend fun clearProfile()
}
