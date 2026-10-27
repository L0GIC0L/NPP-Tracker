package io.github.l0gic0l.npptracker.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PowerStatus { FULL, REDUCED, OFFLINE }

fun powerStatus(power: Int): PowerStatus = when {
    power <= 0 -> PowerStatus.OFFLINE
    power < 100 -> PowerStatus.REDUCED
    else -> PowerStatus.FULL
}

/** A run of consecutive days at 0% power. [end] is the last day offline. */
data class Outage(val start: LocalDate, val end: LocalDate, val ongoing: Boolean) {
    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

/** One reactor unit: its bundled details (if known) and its recent power history. */
data class ReactorUnit(
    val name: String,
    val reactor: Reactor?,
    /** Oldest day first. */
    val history: List<DailyPower>,
) {
    val latest: DailyPower? get() = history.lastOrNull()
    val power: Int get() = latest?.power ?: 0
    val status: PowerStatus get() = powerStatus(power)

    /** Change in power since the previous report, or 0 if there is no previous report. */
    val change: Int
        get() = if (history.size < 2) 0 else history[history.size - 1].power - history[history.size - 2].power

    /** Units at the same plant share a location. */
    val siteKey: String? get() = reactor?.location

    /** Estimated current thermal output in MWt. */
    val thermalOutputMwt: Double? get() = reactor?.licensedMwt?.let { it * power / 100.0 }

    /** Days the unit has been continuously online (power > 0) or offline, counting today. */
    val streakDays: Int
        get() {
            val online = power > 0
            var count = 0
            for (i in history.indices.reversed()) {
                if ((history[i].power > 0) != online) break
                count++
            }
            return count
        }

    /** True when [streakDays] covers the entire history, so the real streak may be longer. */
    val streakCoversHistory: Boolean get() = history.isNotEmpty() && streakDays == history.size

    fun since(date: LocalDate): List<DailyPower> = history.filter { !it.date.isBefore(date) }
}

/** Finds every run of 0% power in [days] (oldest first). */
fun outages(days: List<DailyPower>): List<Outage> {
    val result = mutableListOf<Outage>()
    var start: LocalDate? = null
    var previous: LocalDate? = null
    for (day in days) {
        if (day.power <= 0) {
            if (start == null) start = day.date
        } else if (start != null) {
            result += Outage(start, previous!!, ongoing = false)
            start = null
        }
        previous = day.date
    }
    if (start != null) result += Outage(start, previous!!, ongoing = true)
    return result
}

fun averagePower(days: List<DailyPower>): Double? =
    if (days.isEmpty()) null else days.sumOf { it.power }.toDouble() / days.size

/** Units without a known rating count as an average-sized unit in fleet totals. */
private const val DEFAULT_MWT = 3000.0

/** Current output of [units] as a percentage of their combined licensed thermal power. */
fun weightedPower(units: List<ReactorUnit>): Double {
    val licensed = units.sumOf { it.reactor?.licensedMwt?.toDouble() ?: DEFAULT_MWT }
    val output = units.sumOf { (it.reactor?.licensedMwt?.toDouble() ?: DEFAULT_MWT) * it.power / 100.0 }
    return if (licensed > 0) output / licensed * 100 else 0.0
}

/** Fleet-wide numbers for one report day. */
data class FleetDay(
    val date: LocalDate,
    val unitsReporting: Int,
    val unitsOnline: Int,
    /** Licensed-thermal-power weighted average power, percent. */
    val weightedPower: Double,
)

/**
 * Builds the fleet history: for each report date, how many units were online and the
 * output of the whole fleet as a percentage of its licensed thermal power.
 */
fun fleetHistory(units: List<ReactorUnit>): List<FleetDay> {
    data class Acc(var reporting: Int = 0, var online: Int = 0, var output: Double = 0.0, var licensed: Double = 0.0)

    val byDate = sortedMapOf<LocalDate, Acc>()
    for (unit in units) {
        val mwt = unit.reactor?.licensedMwt?.toDouble() ?: DEFAULT_MWT
        for (day in unit.history) {
            val acc = byDate.getOrPut(day.date) { Acc() }
            acc.reporting++
            if (day.power > 0) acc.online++
            acc.output += mwt * day.power / 100.0
            acc.licensed += mwt
        }
    }
    return byDate.map { (date, acc) ->
        FleetDay(date, acc.reporting, acc.online, if (acc.licensed > 0) acc.output / acc.licensed * 100 else 0.0)
    }
}
