package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val semesterId: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val educationLevel: String? = null,
    val partGrade: String? = null
)