package io.github.l0gic0l.npptracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.data.Reactor
import io.github.l0gic0l.npptracker.data.RestartStage
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val shortDate = DateTimeFormatter.ofPattern("MMM d")
private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy")
private val fullDatePattern = Regex("""\d{4}-\d{2}-\d{2}""")
private val monthPattern = Regex("""\d{4}-\d{2}""")

fun LocalDate.formatMedium(): String = format(mediumDate)

fun LocalDate.formatShort(): String = format(shortDate)

/**
 * Formats a date that may be known only to the month or year: "2026-08-30" becomes
 * "Aug 30, 2026", "2026-10" becomes "Oct 2026", and anything else ("2027", "Early 2029")
 * is shown as written.
 */
fun formatPartialDate(date: String): String = when {
    date.matches(fullDatePattern) -> runCatching { LocalDate.parse(date).formatMedium() }.getOrDefault(date)
    date.matches(monthPattern) -> runCatching { YearMonth.parse(date).format(monthYear) }.getOrDefault(date)
    else -> date
}

fun formatNumber(value: Number): String = NumberFormat.getIntegerInstance().format(value)

@StringRes
fun PowerStatus.labelRes(): Int = when (this) {
    PowerStatus.FULL -> R.string.status_full
    PowerStatus.REDUCED -> R.string.status_reduced
    PowerStatus.OFFLINE -> R.string.status_offline
}

@StringRes
fun RestartStage.labelRes(): Int = when (this) {
    RestartStage.ANNOUNCED -> R.string.stage_announced
    RestartStage.NRC_REVIEW -> R.string.stage_nrc_review
    RestartStage.APPROVED -> R.string.stage_approved
    RestartStage.FUEL_LOAD -> R.string.stage_fuel_load
    RestartStage.STARTUP -> R.string.stage_startup
    RestartStage.OPERATING -> R.string.stage_operating
}

private val romanNumerals = listOf("I", "II", "III", "IV")

@Composable
fun regionName(region: Int): String =
    stringResource(R.string.region_name, romanNumerals.getOrElse(region - 1) { region.toString() })

@Composable
fun reactorTypeName(type: String): String = when (type) {
    "PWR" -> stringResource(R.string.type_pwr)
    "BWR" -> stringResource(R.string.type_bwr)
    else -> type
}

/** "Reduced power · PWR · IL", the one-line summary shown in lists. */
@Composable
fun unitSupportingText(status: PowerStatus, reactor: Reactor?, restarting: Boolean = false): String {
    // An offline unit with a restart project is shown as restarting rather than just offline.
    val label = stringResource(if (restarting && status == PowerStatus.OFFLINE) R.string.status_restarting else status.labelRes())
    return if (reactor == null) label else stringResource(R.string.unit_supporting, label, reactor.type, reactor.state)
}
