package com.muneeb.coursemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: Course): Long

    @Update
    suspend fun update(course: Course)

    @Delete
    suspend fun delete(course: Course)

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY sortOrder ASC")
    fun getCoursesForSemester(semesterId: Long): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    fun getCourseById(courseId: Long): Flow<Course?>

    @Insert
    suspend fun insertCategories(categories: List<Category>): List<Long>
}