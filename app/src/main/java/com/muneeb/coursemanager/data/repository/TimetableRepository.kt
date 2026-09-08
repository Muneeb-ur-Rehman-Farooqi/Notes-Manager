package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.TimetableDao
import com.muneeb.coursemanager.data.entities.TimetableEntry
import kotlinx.coroutines.flow.Flow

class TimetableRepository(
    private val timetableDao: TimetableDao
) {
    fun getAll(): Flow<List<TimetableEntry>> = timetableDao.getAll()

    fun getById(entryId: Long): Flow<TimetableEntry?> = timetableDao.getById(entryId)

    suspend fun insert(entry: TimetableEntry): Long = timetableDao.insert(entry)

    suspend fun update(entry: TimetableEntry) = timetableDao.update(entry)

    suspend fun delete(entry: TimetableEntry) = timetableDao.delete(entry)

    suspend fun deleteAll() = timetableDao.deleteAll()
}