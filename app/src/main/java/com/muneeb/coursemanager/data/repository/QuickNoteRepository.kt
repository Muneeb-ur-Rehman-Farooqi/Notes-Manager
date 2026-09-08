package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.QuickNoteDao
import com.muneeb.coursemanager.data.entities.QuickNote
import kotlinx.coroutines.flow.Flow

class QuickNoteRepository(
    private val quickNoteDao: QuickNoteDao
) {
    fun getAll(): Flow<List<QuickNote>> = quickNoteDao.getAll()

    fun getById(noteId: Long): Flow<QuickNote?> = quickNoteDao.getById(noteId)

    suspend fun insert(note: QuickNote): Long = quickNoteDao.insert(note)

    suspend fun update(note: QuickNote) = quickNoteDao.update(note)

    suspend fun delete(note: QuickNote) = quickNoteDao.delete(note)
}