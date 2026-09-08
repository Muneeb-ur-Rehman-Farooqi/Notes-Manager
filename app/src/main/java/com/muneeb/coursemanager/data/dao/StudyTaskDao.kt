package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.StudyTask
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {

    @Insert
    suspend fun insert(task: StudyTask): Long

    @Update
    suspend fun update(task: StudyTask)

    @Delete
    suspend fun delete(task: StudyTask)

    @Query("SELECT * FROM study_tasks ORDER BY dateMillis ASC, reminderHour ASC")
    fun getAll(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE taskId = :taskId")
    suspend fun getById(taskId: Long): StudyTask?
}