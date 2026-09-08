package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.QuickNote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {

    @Insert
    suspend fun insert(note: QuickNote): Long

    @Update
    suspend fun update(note: QuickNote)

    @Delete
    suspend fun delete(note: QuickNote)

    @Query("SELECT * FROM quick_notes ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<QuickNote>>

    @Query("SELECT * FROM quick_notes WHERE noteId = :noteId")
    fun getById(noteId: Long): Flow<QuickNote?>
}