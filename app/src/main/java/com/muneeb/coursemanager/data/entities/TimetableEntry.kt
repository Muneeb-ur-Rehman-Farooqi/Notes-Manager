package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val entryId: Long = 0,
    val groupId: String,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val subjectName: String,
    val room: String? = null,
    val teacher: String? = null,
    val meetingLink: String? = null
)