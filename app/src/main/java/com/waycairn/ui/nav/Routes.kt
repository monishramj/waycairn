package com.waycairn.ui.nav

/** Central place for NavHost route strings and their argument keys. */
object Routes {
    const val ARG_HABIT_ID = "habitId"

    // Bottom-nav destinations
    const val HABITS = "habits"
    const val CALENDAR = "calendar"
    const val APPS = "apps"
    const val SETTINGS = "settings"

    // Pushed screens
    const val HABIT_DETAIL = "habit/{$ARG_HABIT_ID}"
    fun habitDetail(id: Long): String = "habit/$id"

    // habitId <= 0 means "create new"
    const val HABIT_EDIT = "edit?$ARG_HABIT_ID={$ARG_HABIT_ID}"
    fun habitEdit(id: Long? = null): String = "edit?$ARG_HABIT_ID=${id ?: -1L}"

    val bottomNav: Set<String> = setOf(HABITS, CALENDAR, APPS, SETTINGS)
}
