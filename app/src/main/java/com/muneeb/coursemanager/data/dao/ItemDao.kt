package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun getItemsForCategory(categoryId: Long): Flow<List<Item>>

    @Query("UPDATE items SET lastOpenedAt = :timestamp WHERE itemId = :itemId")
    suspend fun updateLastOpened(itemId: Long, timestamp: Long)
}