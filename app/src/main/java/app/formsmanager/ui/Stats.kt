package app.formsmanager.ui

import app.formsmanager.FormData
import app.formsmanager.api.FormResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

fun FormResponse.instant(): Instant? = runCatching { Instant.parse(submittedAt) }.getOrNull()

private fun FormResponse.date(): LocalDate? = instant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

/** A person-friendly label: the first short answer, else the email name, else "Anonymous". */
fun FormResponse.displayName(): String {
    val first = answers.firstOrNull()?.values?.firstOrNull()?.trim().orEmpty()
    return when {
        first.isNotEmpty() && first.length <= 40 -> first
        respondentEmail != null -> respondentEmail.substringBefore('@')
        else -> "Anonymous"
    }
}

fun FormResponse.agoShort(now: Instant = Instant.now()): String {
    val then = instant() ?: return ""
    val minutes = Duration.between(then, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)}d"
        else -> "${minutes / (60 * 24 * 30)}mo"
    }
}

/** Response count, with a plus sign when Google holds more than the 500 we load. */
fun FormData.countLabel() = "${responses.size}${if (hasMore) "+" else ""}"

fun greeting(hour: Int) = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

fun countToday(responses: List<FormResponse>, today: LocalDate = LocalDate.now()) =
    responses.count { it.date() == today }

class RangeStats(
    val total: Int,
    /** Count for the period right before this one, or null for "all time". */
    val previous: Int?,
    val chart: List<Int>,
    val labels: List<String>,
    val dailyLabels: Boolean,
)

/** Counts responses over the last [days] days, or since the first response when [days] is null. */
fun rangeStats(responses: List<FormResponse>, days: Int?, today: LocalDate = LocalDate.now()): RangeStats {
    val dates = responses.mapNotNull { it.date() }
    val span = days ?: (dates.minOrNull()?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 } ?: 7).coerceIn(7, 365)
    val start = today.minusDays(span - 1L)
    val inRange = dates.filter { !it.isBefore(start) && !it.isAfter(today) }
    val previous = if (days == null) null else dates.count {
        !it.isBefore(start.minusDays(span.toLong())) && it.isBefore(start)
    }

    val buckets = if (span <= 7) span else 7
    val counts = IntArray(buckets)
    for (date in inRange) {
        val offset = ChronoUnit.DAYS.between(start, date).toInt()
        counts[(offset * buckets / span).coerceIn(0, buckets - 1)]++
    }
    val labels = if (span <= 7) {
        (0 until span).map { start.plusDays(it.toLong()).dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()) }
    } else {
        listOf(shortDate(start), shortDate(today))
    }
    return RangeStats(inRange.size, previous, counts.toList(), labels, span <= 7)
}

private fun shortDate(date: LocalDate) =
    "${date.dayOfMonth} ${date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())}"
