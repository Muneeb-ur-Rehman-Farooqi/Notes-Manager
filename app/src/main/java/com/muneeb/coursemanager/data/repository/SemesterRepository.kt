package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.SemesterDao
import com.muneeb.coursemanager.data.entities.Semester
import kotlinx.coroutines.flow.Flow

class SemesterRepository(
    private val semesterDao: SemesterDao
) {
    fun getAllSemesters(): Flow<List<Semester>> = semesterDao.getAllSemesters()

    fun getSemesterById(semesterId: Long): Flow<Semester?> =
        semesterDao.getSemesterById(semesterId)

    suspend fun insert(semester: Semester): Long = semesterDao.insert(semester)

    suspend fun update(semester: Semester) = semesterDao.update(semester)

    suspend fun delete(semester: Semester) = semesterDao.delete(semester)
}