package com.waycairn.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local-timezone day math. "Today" is always recomputed from the current clock/zone so a
 * mid-session day rollover (or a device timezone change) is picked up on the next read rather
 * than caching a stale day. See BUILD_PLAN.md §4 / Phase 8 edge cases.
 */
object DayRange {

    /** A single calendar day and its epoch-millis bounds; [startMillis, endMillis) is half-open. */
    data class Day(
        val date: LocalDate,
        val startMillis: Long,
        val endMillis: Long
    )

    fun localDateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** First epoch milli of the day containing [millis]. */
    fun startOfDay(millis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long =
        localDateOf(millis, zone).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Exclusive end of the day containing [millis] (== start of the next day). */
    fun endOfDay(millis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long =
        localDateOf(millis, zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun dayOf(millis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Day {
        val date = localDateOf(millis, zone)
        return Day(
            date = date,
            startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }

    fun today(zone: ZoneId = ZoneId.systemDefault()): Day = dayOf(System.currentTimeMillis(), zone)

    fun dayFor(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Day = Day(
        date = date,
        startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    )

    /**
     * Iterate each [Day] from the day containing [startMillis] through the day containing
     * [endMillis], inclusive, ascending. Useful for building calendar/heatmap ranges.
     */
    fun daysInRange(
        startMillis: Long,
        endMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Day> {
        val first = localDateOf(startMillis, zone)
        val last = localDateOf(endMillis, zone)
        if (last.isBefore(first)) return emptyList()
        val out = ArrayList<Day>()
        var cursor = first
        while (!cursor.isAfter(last)) {
            out.add(dayFor(cursor, zone))
            cursor = cursor.plusDays(1)
        }
        return out
    }
}
