package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.Page
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert
    suspend fun insert(page: Page): Long

    @Update
    suspend fun update(page: Page)

    @Delete
    suspend fun delete(page: Page)

    // Get all Pages belonging to a specific PHOTO_GROUP Item, ordered by pageNumber.
    // The spec explicitly says "ordered by sortOrder (or pageNumber for PageDao)".
    @Query("SELECT * FROM pages WHERE itemId = :itemId ORDER BY pageNumber ASC")
    fun getPagesForItem(itemId: Long): Flow<List<Page>>
}