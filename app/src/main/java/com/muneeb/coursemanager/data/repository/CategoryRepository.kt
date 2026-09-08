package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.CategoryDao
import com.muneeb.coursemanager.data.entities.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao
) {
    fun getCategoriesForCourse(courseId: Long): Flow<List<Category>> =
        categoryDao.getCategoriesForCourse(courseId)

    fun getCategoryById(categoryId: Long): Flow<Category?> =
        categoryDao.getCategoryById(categoryId)

    suspend fun insert(category: Category): Long = categoryDao.insert(category)

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun delete(category: Category) = categoryDao.delete(category)
}