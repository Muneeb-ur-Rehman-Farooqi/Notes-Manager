package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.TimetableEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Insert
    suspend fun insert(entry: TimetableEntry): Long

    @Update
    suspend fun update(entry: TimetableEntry)

    @Delete
    suspend fun delete(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAll(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries")
    suspend fun getAllOnce(): List<TimetableEntry>

    @Query("SELECT * FROM timetable_entries WHERE entryId = :entryId")
    fun getById(entryId: Long): Flow<TimetableEntry?>
}