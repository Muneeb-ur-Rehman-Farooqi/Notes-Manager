package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.BookmarkDao
import com.muneeb.coursemanager.data.entities.Bookmark
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao
) {
    fun getForItem(itemId: Long): Flow<List<Bookmark>> = bookmarkDao.getForItem(itemId)

    fun getForItemAndPage(itemId: Long, pageNumber: Int): Flow<Bookmark?> =
        bookmarkDao.getForItemAndPage(itemId, pageNumber)

    suspend fun insert(bookmark: Bookmark): Long = bookmarkDao.insert(bookmark)

    suspend fun delete(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    suspend fun deleteByItemAndPage(itemId: Long, pageNumber: Int) =
        bookmarkDao.deleteByItemAndPage(itemId, pageNumber)
}