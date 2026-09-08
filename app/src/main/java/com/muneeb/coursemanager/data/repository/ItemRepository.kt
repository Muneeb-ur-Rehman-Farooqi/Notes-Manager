package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.ItemDao
import com.muneeb.coursemanager.data.entities.Item
import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao
) {
    fun getItemsForCategory(categoryId: Long): Flow<List<Item>> =
        itemDao.getItemsForCategory(categoryId)

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun update(item: Item) = itemDao.update(item)

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun updateLastOpened(itemId: Long, timestamp: Long) =
        itemDao.updateLastOpened(itemId, timestamp)
}