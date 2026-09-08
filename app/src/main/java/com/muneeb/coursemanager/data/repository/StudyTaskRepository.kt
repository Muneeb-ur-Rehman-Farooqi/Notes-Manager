package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.StudyTaskDao
import com.muneeb.coursemanager.data.entities.StudyTask
import kotlinx.coroutines.flow.Flow

class StudyTaskRepository(
    private val studyTaskDao: StudyTaskDao
) {
    fun getAll(): Flow<List<StudyTask>> = studyTaskDao.getAll()

    suspend fun getById(taskId: Long): StudyTask? = studyTaskDao.getById(taskId)

    suspend fun insert(task: StudyTask): Long = studyTaskDao.insert(task)

    suspend fun update(task: StudyTask) = studyTaskDao.update(task)

    suspend fun delete(task: StudyTask) = studyTaskDao.delete(task)
}