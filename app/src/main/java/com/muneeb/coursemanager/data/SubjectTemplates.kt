package com.muneeb.coursemanager.data

object SubjectTemplates {

    fun getDefaultCategories(
        subjectName: String,
        level: String,
        part: String?
    ): List<String> {
        if (level == "UNIVERSITY") {
            return listOf(
                "Study Material",
                "Slides",
                "Assignments",
                "Notes",
                "Mock and Past Papers"
            )
        }

        val base = mutableListOf<String>()
        // Base categories for all Matric/Inter subjects
        base.addAll(listOf("Notes", "MCQs", "Past Papers"))

        // Language vs non‑language
        val languageSubjects = setOf("Urdu", "English", "Tarjma-tul-Quran")
        if (subjectName in languageSubjects) {
            base.add("Grammar")
        } else {
            base.addAll(listOf("Short Questions", "Long Questions"))
        }

        // Practicals for science subjects
        val practicalSubjects = setOf("Computer", "Biology", "Chemistry", "Physics")
        if (subjectName in practicalSubjects) {
            val isPart2 = (level == "INTER" && part == "PART_2") ||
                    (level == "MATRIC" && part == "GRADE_10")
            if (isPart2) {
                base.add("Practicals")
            }
        }

        // Entry Test Questions (only Inter Part 2)
        val entryTestSubjects = setOf("English", "Computer", "Biology", "Maths", "Physics", "Chemistry")
        if (level == "INTER" && part == "PART_2" && subjectName in entryTestSubjects) {
            base.add("Entry Test Questions")
        }

        return base
    }

    // Existing subject-resolution code remains unchanged
    private val compulsorySubjects = mapOf(
        "INTER" to mapOf(
            "PART_1" to listOf("English", "Urdu", "Islamiat", "Tarjma-tul-Quran"),
            "PART_2" to listOf("English", "Urdu", "Pak Studies", "Tarjma-tul-Quran")
        ),
        "MATRIC" to mapOf(
            "GRADE_9" to listOf("Tarjma-tul-Quran", "Urdu", "English", "Islamiat"),
            "GRADE_10" to listOf("Tarjma-tul-Quran", "Urdu", "English", "Pak Studies")
        )
    )

    private val fixedGroupSubjects = mapOf(
        "INTER" to mapOf(
            "PRE_MEDICAL" to listOf("Physics", "Chemistry", "Biology"),
            "PRE_ENGINEERING" to listOf("Physics", "Maths", "Chemistry"),
            "ICS" to listOf("Computer Science", "Maths")
        ),
        "MATRIC" to mapOf(
            "SCIENCE" to listOf("Physics", "Chemistry", "Maths")
        )
    )

    private val electiveOptions = mapOf(
        "INTER" to mapOf(
            "ICS" to listOf("Physics", "Statistics", "Economics")
        ),
        "MATRIC" to mapOf(
            "SCIENCE" to listOf("Biology", "Computer")
        )
    )

    private val freeTextSlots = mapOf(
        "INTER" to mapOf(
            "ARTS" to FreeTextSlot(count = 3, fixedSubjects = emptyList())
        ),
        "MATRIC" to mapOf(
            "ARTS" to FreeTextSlot(count = 2, fixedSubjects = listOf("General Maths", "General Science"))
        )
    )

    private data class FreeTextSlot(
        val count: Int,
        val fixedSubjects: List<String>
    )

    fun resolveSubjectList(
        level: String,
        part: String?,
        group: String?,
        electiveChoice: String?,
        freeTexts: List<String>?
    ): List<String> {
        if (level == "UNIVERSITY") return emptyList()
        val partKey = part ?: return emptyList()
        val groupKey = group ?: return emptyList()

        val compulsory = compulsorySubjects[level]?.get(partKey) ?: emptyList()
        val fixed = fixedGroupSubjects[level]?.get(groupKey) ?: emptyList()
        val elective = when {
            electiveOptions[level]?.containsKey(groupKey) == true -> {
                val options = electiveOptions[level]!![groupKey]!!
                if (electiveChoice != null && options.contains(electiveChoice)) {
                    listOf(electiveChoice)
                } else emptyList()
            }
            else -> emptyList()
        }
        val freeSlot = freeTextSlots[level]?.get(groupKey)
        val freeList = if (freeSlot != null) {
            val fixedFree = freeSlot.fixedSubjects
            val userFree = freeTexts?.take(freeSlot.count) ?: emptyList()
            (fixedFree + userFree).take(freeSlot.count + fixedFree.size)
        } else emptyList()

        return compulsory + fixed + elective + freeList
    }

    fun getGroupsForLevel(level: String): List<String> = when (level) {
        "INTER" -> listOf("PRE_MEDICAL", "PRE_ENGINEERING", "ICS", "ARTS")
        "MATRIC" -> listOf("SCIENCE", "ARTS")
        else -> emptyList()
    }

    fun getElectiveOptions(level: String, group: String): List<String> =
        electiveOptions[level]?.get(group) ?: emptyList()

    fun getFreeTextSlotInfo(level: String, group: String): Pair<Int, List<String>>? {
        return freeTextSlots[level]?.get(group)?.let { it.count to it.fixedSubjects }
    }
}