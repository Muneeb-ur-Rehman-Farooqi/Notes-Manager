package com.muneeb.coursemanager.data

fun formatStageLabel(level: String?, part: String?): String {
    val levelLabel = when (level) {
        "MATRIC" -> "Matric"
        "INTER" -> "Inter"
        "UNIVERSITY" -> "University"
        else -> level ?: "Stage"
    }
    val partLabel = when (part) {
        "GRADE_9" -> "Grade 9"
        "GRADE_10" -> "Grade 10"
        "PART_1" -> "Part 1"
        "PART_2" -> "Part 2"
        else -> null
    }
    return if (partLabel != null) "$levelLabel — $partLabel" else levelLabel
}