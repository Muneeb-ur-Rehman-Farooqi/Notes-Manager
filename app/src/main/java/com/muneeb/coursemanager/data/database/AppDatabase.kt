package com.muneeb.coursemanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.muneeb.coursemanager.data.dao.CategoryDao
import com.muneeb.coursemanager.data.dao.CourseDao
import com.muneeb.coursemanager.data.dao.ItemDao
import com.muneeb.coursemanager.data.dao.PageDao
import com.muneeb.coursemanager.data.dao.QuickNoteDao
import com.muneeb.coursemanager.data.dao.SemesterDao
import com.muneeb.coursemanager.data.dao.StudyTaskDao
import com.muneeb.coursemanager.data.dao.TimetableDao
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Converters
import com.muneeb.coursemanager.data.entities.Course
import com.muneeb.coursemanager.data.entities.Item
import com.muneeb.coursemanager.data.entities.Page
import com.muneeb.coursemanager.data.entities.QuickNote
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.entities.StudyTask
import com.muneeb.coursemanager.data.entities.TimetableEntry

@Database(
    entities = [
        Semester::class,
        Course::class,
        Category::class,
        Item::class,
        Page::class,
        QuickNote::class,
        TimetableEntry::class,
        StudyTask::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun pageDao(): PageDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun timetableDao(): TimetableDao
    abstract fun studyTaskDao(): StudyTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_manager_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}