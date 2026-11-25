package app.formsmanager.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun formatRelativeTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    val instant = runCatching { Instant.parse(isoString) }.getOrNull()
        ?: return isoString.take(10)

    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.isNegative || duration.seconds < 45 -> "just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        duration.toDays() < 30 -> "${duration.toDays() / 7}w ago"
        duration.toDays() < 365 -> "${duration.toDays() / 30}mo ago"
        else -> "${duration.toDays() / 365}y ago"
    }
}

fun formatDateTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    val instant = runCatching { Instant.parse(isoString) }.getOrNull()
        ?: return isoString.take(16).replace('T', ' ')

    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
