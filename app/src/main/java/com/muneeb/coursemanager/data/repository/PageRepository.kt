package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.PageDao
import com.muneeb.coursemanager.data.entities.Page
import kotlinx.coroutines.flow.Flow

class PageRepository(
    private val pageDao: PageDao
) {
    fun getPagesForItem(itemId: Long): Flow<List<Page>> =
        pageDao.getPagesForItem(itemId)

    suspend fun insert(page: Page): Long = pageDao.insert(page)

    suspend fun update(page: Page) = pageDao.update(page)

    suspend fun delete(page: Page) = pageDao.delete(page)
}