package io.github.l0gic0l.npptracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.data.Reactor
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val shortDate = DateTimeFormatter.ofPattern("MMM d")

fun LocalDate.formatMedium(): String = format(mediumDate)

fun LocalDate.formatShort(): String = format(shortDate)

fun formatNumber(value: Number): String = NumberFormat.getIntegerInstance().format(value)

@StringRes
fun PowerStatus.labelRes(): Int = when (this) {
    PowerStatus.FULL -> R.string.status_full
    PowerStatus.REDUCED -> R.string.status_reduced
    PowerStatus.OFFLINE -> R.string.status_offline
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
fun unitSupportingText(status: PowerStatus, reactor: Reactor?): String =
    if (reactor == null) {
        stringResource(status.labelRes())
    } else {
        stringResource(R.string.unit_supporting, stringResource(status.labelRes()), reactor.type, reactor.state)
    }
