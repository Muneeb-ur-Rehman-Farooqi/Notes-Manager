package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0,
    val title: String,
    val dateMillis: Long,
    val hasReminder: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isCompleted: Boolean = false
)