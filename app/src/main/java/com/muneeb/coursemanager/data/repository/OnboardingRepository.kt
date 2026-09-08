package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.SubjectTemplates
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Course
import com.muneeb.coursemanager.data.entities.Semester

class OnboardingRepository(
    private val courseRepository: CourseRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun createCourseWithDefaultCategories(
        semesterId: Long,
        courseName: String,
        courseCode: String?,
        categories: List<String>
    ): Long {
        val course = Course(
            semesterId = semesterId,
            name = courseName,
            courseCode = courseCode,
            sortOrder = 0
        )
        val courseId = courseRepository.insert(course)

        val categoryEntities = categories.mapIndexed { index, categoryName ->
            Category(
                courseId = courseId,
                name = categoryName,
                sortOrder = index
            )
        }
        courseRepository.insertCategories(categoryEntities)
        return courseId
    }

    suspend fun applyEducationTemplate(
        semesterId: Long,
        level: String,
        part: String?,
        group: String?,
        electiveChoice: String?,
        freeTexts: List<String>?
    ) {
        if (level == "UNIVERSITY") return

        val subjects = SubjectTemplates.resolveSubjectList(
            level = level,
            part = part,
            group = group,
            electiveChoice = electiveChoice,
            freeTexts = freeTexts
        )

        for (subjectName in subjects) {
            val categories = SubjectTemplates.getDefaultCategories(
                subjectName = subjectName,
                level = level,
                part = part
            )
            createCourseWithDefaultCategories(
                semesterId = semesterId,
                courseName = subjectName,
                courseCode = null,
                categories = categories
            )
        }
    }
}