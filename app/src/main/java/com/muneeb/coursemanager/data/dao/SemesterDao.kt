package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {

    @Insert
    suspend fun insert(semester: Semester): Long

    @Update
    suspend fun update(semester: Semester)

    @Delete
    suspend fun delete(semester: Semester)

    @Query("SELECT * FROM semesters ORDER BY sortOrder ASC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters WHERE semesterId = :semesterId")
    fun getSemesterById(semesterId: Long): Flow<Semester?>
}