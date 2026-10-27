package io.github.l0gic0l.npptracker.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Reactor power level for one day, in percent of licensed thermal power. */
data class DailyPower(val date: LocalDate, val power: Int)

/**
 * Parses the NRC "Power Reactor Status for Last 365 Days" text file.
 *
 * The file is pipe-delimited, one line per unit per day:
 * ```
 * ReportDt|Unit|Power
 * 10/26/2026 12:00:00 AM|Arkansas Nuclear 1|100
 * ```
 */
object PowerReportParser {
    private val dateFormat = DateTimeFormatter.ofPattern("M/d/yyyy")

    /** Returns each unit's history sorted oldest day first. Malformed lines are skipped. */
    fun parse(text: String): Map<String, List<DailyPower>> {
        val byUnit = HashMap<String, MutableMap<LocalDate, Int>>()
        for (line in text.lineSequence()) {
            val parts = line.split('|')
            if (parts.size < 3) continue
            val date = try {
                LocalDate.parse(parts[0].trim().substringBefore(' '), dateFormat)
            } catch (_: DateTimeParseException) {
                continue
            }
            val power = parts[2].trim().toIntOrNull() ?: continue
            val unit = parts[1].trim().ifEmpty { continue }
            byUnit.getOrPut(unit) { HashMap() }[date] = power
        }
        return byUnit.mapValues { (_, days) ->
            days.entries.sortedBy { it.key }.map { DailyPower(it.key, it.value) }
        }
    }
}
