package com.interviewprep.core.domain

/** Days are epoch days (LocalDate.toEpochDay()) so this stays platform-independent. */
data class StreakInfo(val current: Int, val longest: Int, val studiedToday: Boolean)

object Streak {
    fun compute(activeDays: Collection<Long>, today: Long): StreakInfo {
        val days = activeDays.toSortedSet()
        if (days.isEmpty()) return StreakInfo(0, 0, false)
        // longest run
        var longest = 1
        var run = 1
        var prev: Long? = null
        for (d in days) {
            if (prev != null) {
                run = if (d == prev + 1) run + 1 else 1
                if (run > longest) longest = run
            }
            prev = d
        }
        val studiedToday = today in days
        // current streak counts back from today, or from yesterday if today isn't done yet
        var cursor = if (studiedToday) today else today - 1
        var current = 0
        while (cursor in days) {
            current++
            cursor--
        }
        return StreakInfo(current, maxOf(longest, current), studiedToday)
    }
}
