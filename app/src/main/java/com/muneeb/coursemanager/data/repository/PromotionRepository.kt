package com.muneeb.coursemanager.data.repository

import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.preferences.UserPreferences
import kotlinx.coroutines.flow.firstOrNull

sealed class PromotionResult {
    data class SameLevelPromoted(
        val newSemesterId: Long,
        val newStageName: String,
        val newPart: String
    ) : PromotionResult()
    data class NeedsGroupSelection(
        val newLevel: String,
        val newPart: String
    ) : PromotionResult()
    data object PromotedToUniversity : PromotionResult()
    data object NotPromotable : PromotionResult()
}

class PromotionRepository(
    private val semesterRepository: SemesterRepository,
    private val onboardingRepository: OnboardingRepository,
    private val userPreferences: UserPreferences
) {
    suspend fun promote(currentSemesterId: Long): PromotionResult {
        val old = semesterRepository.getSemesterById(currentSemesterId).firstOrNull()
            ?: return PromotionResult.NotPromotable
        val level = old.educationLevel ?: return PromotionResult.NotPromotable
        val part = old.partGrade

        return when {
            level == "MATRIC" && part == "GRADE_9" ->
                promoteWithinLevel(old, "GRADE_10", "Grade 10")
            level == "INTER" && part == "PART_1" ->
                promoteWithinLevel(old, "PART_2", "Part 2")
            level == "MATRIC" && part == "GRADE_10" ->
                PromotionResult.NeedsGroupSelection("INTER", "PART_1")
            level == "INTER" && part == "PART_2" ->
                PromotionResult.PromotedToUniversity
            else -> PromotionResult.NotPromotable
        }
    }

    private suspend fun promoteWithinLevel(
        old: Semester,
        newPart: String,
        newStageName: String
    ): PromotionResult.SameLevelPromoted {
        val newSemester = Semester(
            name = "${old.educationLevel} $newPart",
            sortOrder = 0,
            educationLevel = old.educationLevel,
            partGrade = newPart,
            electiveChoice = old.electiveChoice,
            freeTextElectives = old.freeTextElectives
        )
        val newId = semesterRepository.insert(newSemester)
        val group = userPreferences.selectedGroup.firstOrNull()
        onboardingRepository.applyEducationTemplate(
            semesterId = newId,
            level = old.educationLevel ?: "MATRIC",
            part = newPart,
            group = group,
            electiveChoice = old.electiveChoice,
            freeTexts = old.freeTextElectives?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
        return PromotionResult.SameLevelPromoted(
            newSemesterId = newId,
            newStageName = newStageName,
            newPart = newPart
        )
    }
}