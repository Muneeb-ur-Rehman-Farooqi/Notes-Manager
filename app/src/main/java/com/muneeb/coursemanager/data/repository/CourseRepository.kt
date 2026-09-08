package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.dao.CourseDao
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(
    private val courseDao: CourseDao
) {
    fun getCoursesForSemester(semesterId: Long): Flow<List<Course>> =
        courseDao.getCoursesForSemester(semesterId)

    fun getCourseById(courseId: Long): Flow<Course?> =
        courseDao.getCourseById(courseId)

    suspend fun insert(course: Course): Long = courseDao.insert(course)

    suspend fun update(course: Course) = courseDao.update(course)

    suspend fun delete(course: Course) = courseDao.delete(course)

    suspend fun insertCategories(categories: List<Category>): List<Long> =
        courseDao.insertCategories(categories)
}