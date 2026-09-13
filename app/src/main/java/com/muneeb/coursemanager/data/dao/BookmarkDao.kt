package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.muneeb.coursemanager.data.entities.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE itemId = :itemId AND pageNumber = :pageNumber")
    suspend fun deleteByItemAndPage(itemId: Long, pageNumber: Int)

    @Query("SELECT * FROM bookmarks WHERE itemId = :itemId ORDER BY pageNumber ASC")
    fun getForItem(itemId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE itemId = :itemId AND pageNumber = :pageNumber LIMIT 1")
    fun getForItemAndPage(itemId: Long, pageNumber: Int): Flow<Bookmark?>
}