package com.muneeb.coursemanager.navigation

object Routes {
    // Onboarding
    const val EDUCATION_LEVEL = "education_level"
    const val PART_GRADE_SELECTION = "part_grade_selection"
    const val GROUP_SELECTION = "group_selection"
    const val ELECTIVE_CHOICE = "elective_choice"
    const val FREE_TEXT_ELECTIVE = "free_text_elective"

    // Main browsing
    const val SEMESTER_LIST = "semester_list"
    const val COURSE_LIST = "course_list/{semesterId}"
    const val CATEGORY_LIST = "category_list/{courseId}"
    const val ITEM_LIST = "item_list/{categoryId}"
    const val NOTE_EDITOR = "note_editor/{categoryId}"

    // Quick Notes
    const val NOTES_LIST = "notes_list"
    const val QUICK_NOTE_EDITOR = "quick_note_editor?noteId={noteId}"

    // Timetable
    const val TIMETABLE_LIST = "timetable_list"
    const val TIMETABLE_EDITOR = "timetable_editor?entryId={entryId}"

    // Study Tasks
    const val STUDY_TASK_LIST = "study_task_list"
    const val STUDY_TASK_EDITOR = "study_task_editor?taskId={taskId}"

    // Helper functions
    fun courseList(semesterId: Long) = "course_list/$semesterId"
    fun categoryList(courseId: Long) = "category_list/$courseId"
    fun itemList(categoryId: Long) = "item_list/$categoryId"
    fun noteEditor(categoryId: Long) = "note_editor/$categoryId"
    fun quickNoteEditor(noteId: Long = -1L) = "quick_note_editor?noteId=$noteId"
    fun timetableEditor(entryId: Long = -1L) = "timetable_editor?entryId=$entryId"
    fun studyTaskEditor(taskId: Long = -1L) = "study_task_editor?taskId=$taskId"
}