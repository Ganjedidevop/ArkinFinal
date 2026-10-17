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
    version = 3,
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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
