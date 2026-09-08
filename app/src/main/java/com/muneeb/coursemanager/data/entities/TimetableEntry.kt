package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val entryId: Long = 0,
    val dayOfWeek: Int, // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int, // 0-23
    val endMinute: Int, // 0-59
    val subjectName: String,
    val room: String? = null,
    val teacher: String? = null
)