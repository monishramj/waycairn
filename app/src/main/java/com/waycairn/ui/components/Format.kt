package com.waycairn.ui.components

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** minutes-since-midnight -> "6:00 PM"; null -> "any time today". */
fun deadlineLabel(deadlineMinutes: Int?): String {
    if (deadlineMinutes == null) return "any time today"
    val safe = deadlineMinutes.coerceIn(0, 24 * 60 - 1)
    return LocalTime.of(safe / 60, safe % 60).format(timeFormatter)
}
