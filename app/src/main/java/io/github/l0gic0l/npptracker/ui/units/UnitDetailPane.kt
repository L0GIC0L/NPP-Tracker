package io.github.l0gic0l.npptracker.ui.units

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.data.Reactor
import io.github.l0gic0l.npptracker.data.ReactorUnit
import io.github.l0gic0l.npptracker.data.RestartProject
import io.github.l0gic0l.npptracker.data.averagePower
import io.github.l0gic0l.npptracker.data.outages
import io.github.l0gic0l.npptracker.ui.components.ChartPoint
import io.github.l0gic0l.npptracker.ui.components.ChartRange
import io.github.l0gic0l.npptracker.ui.components.LabeledValue
import io.github.l0gic0l.npptracker.ui.components.PowerChart
import io.github.l0gic0l.npptracker.ui.components.PowerRing
import io.github.l0gic0l.npptracker.ui.components.RangeSelector
import io.github.l0gic0l.npptracker.ui.components.SectionCard
import io.github.l0gic0l.npptracker.ui.components.StageProgress
import io.github.l0gic0l.npptracker.ui.components.StatTile
import io.github.l0gic0l.npptracker.ui.components.formatMedium
import io.github.l0gic0l.npptracker.ui.components.formatNumber
import io.github.l0gic0l.npptracker.ui.components.labelRes
import io.github.l0gic0l.npptracker.ui.components.reactorTypeName
import io.github.l0gic0l.npptracker.ui.components.regionName
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailPane(
    unit: ReactorUnit,
    siteUnits: List<ReactorUnit>,
    reportDate: LocalDate?,
    starred: Boolean,
    showBack: Boolean,
    restart: RestartProject?,
    onBack: () -> Unit,
    onToggleStar: () -> Unit,
    onUnitClick: (String) -> Unit,
    onOpenRestarts: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(unit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleStar) {
                        Icon(
                            imageVector = if (starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(if (starred) R.string.unstar else R.string.star),
                            tint = if (starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusCard(unit)
                if (restart != null) RestartProjectCard(restart, onOpenRestarts)
                HistoryCard(unit, reportDate)
                OutagesCard(unit)
                DetailsCard(unit.reactor)
                if (siteUnits.isNotEmpty()) SiteCard(siteUnits, onUnitClick)
                LinkButtons(
                    reactor = unit.reactor,
                    onOpen = { url -> runCatching { uriHandler.openUri(url) } },
                )
            }
        }
    }
}

@Composable
private fun StatusCard(unit: ReactorUnit) {
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) = when (unit.status) {
        PowerStatus.FULL -> colors.primaryContainer to colors.onPrimaryContainer
        PowerStatus.REDUCED -> colors.tertiaryContainer to colors.onTertiaryContainer
        PowerStatus.OFFLINE -> colors.errorContainer to colors.onErrorContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
    ) {
        // The ring and status sit on one row and the details below, so the card reads well
        // in a narrow pane (e.g. a foldable split in two) as well as a wide one.
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                PowerRing(
                    power = unit.power,
                    status = unit.status,
                    size = 80.dp,
                    strokeWidth = 8.dp,
                    textStyle = MaterialTheme.typography.headlineSmall,
                )
                Column {
                    Text(stringResource(unit.status.labelRes()), style = MaterialTheme.typography.titleLarge)
                    unit.latest?.let {
                        Text(
                            text = stringResource(R.string.as_of, it.date.formatMedium()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            val history = unit.history
            if (history.size >= 2) {
                val previous = history[history.size - 2].date.formatMedium()
                Text(
                    text = if (unit.change == 0) {
                        stringResource(R.string.no_change_since, previous)
                    } else {
                        stringResource(R.string.change_since, unit.change, previous)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            val streak = unit.streakDays
            if (streak > 0) {
                val online = unit.power > 0
                val plural = when {
                    online && unit.streakCoversHistory -> R.plurals.online_for_at_least
                    online -> R.plurals.online_for
                    unit.streakCoversHistory -> R.plurals.offline_for_at_least
                    else -> R.plurals.offline_for
                }
                Text(pluralStringResource(plural, streak, streak), style = MaterialTheme.typography.bodyLarge)
            }
            val output = unit.thermalOutputMwt
            val licensed = unit.reactor?.licensedMwt
            if (output != null && licensed != null) {
                Text(
                    text = stringResource(R.string.thermal_output, formatNumber(output), formatNumber(licensed)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun RestartProjectCard(project: RestartProject, onOpenRestarts: () -> Unit) {
    SectionCard(title = stringResource(R.string.restart_project)) {
        StageProgress(project.stage)
        LabeledValue(stringResource(R.string.restart_target), project.target)
        FilledTonalButton(onClick = onOpenRestarts) { Text(stringResource(R.string.restart_details_button)) }
    }
}

@Composable
private fun HistoryCard(unit: ReactorUnit, reportDate: LocalDate?) {
    var range by rememberSaveable { mutableStateOf(ChartRange.YEAR) }
    val end = reportDate ?: unit.latest?.date ?: return
    val days = remember(unit.history, range, end) { unit.since(end.minusDays(range.days - 1)) }
    if (days.isEmpty()) return
    val average = averagePower(days) ?: 0.0
    val averageText = stringResource(R.string.power_percent, average.toInt())
    SectionCard(title = stringResource(R.string.power_history)) {
        RangeSelector(range, { range = it }, Modifier.fillMaxWidth())
        PowerChart(
            points = remember(days) { days.map { ChartPoint(it.date, it.power.toFloat()) } },
            contentDescription = stringResource(
                R.string.chart_description,
                days.first().date.formatMedium(),
                days.last().date.formatMedium(),
                averageText,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.average_power), averageText, Modifier.weight(1f))
            StatTile(stringResource(R.string.days_offline), days.count { it.power <= 0 }.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.outage_count), outages(days).size.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun OutagesCard(unit: ReactorUnit) {
    val all = remember(unit.history) { outages(unit.history).reversed() }
    val firstDate = unit.history.firstOrNull()?.date ?: return
    SectionCard(title = stringResource(R.string.outages_title)) {
        if (all.isEmpty()) {
            Text(
                text = stringResource(R.string.no_outages, firstDate.formatMedium()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        all.forEachIndexed { index, outage ->
            if (index > 0) HorizontalDivider()
            val startText = if (outage.start == firstDate) {
                stringResource(R.string.outage_before, outage.start.formatMedium())
            } else {
                outage.start.formatMedium()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (outage.ongoing) {
                        stringResource(R.string.outage_ongoing, startText)
                    } else {
                        stringResource(R.string.outage_range, startText, outage.end.formatMedium())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(R.plurals.days, outage.days, outage.days),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (outage.ongoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsCard(reactor: Reactor?) {
    SectionCard(title = stringResource(R.string.unit_details)) {
        if (reactor == null) {
            Text(stringResource(R.string.no_unit_details), style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        val rows = listOfNotNull(
            R.string.detail_full_name to reactor.fullName,
            R.string.detail_type to reactorTypeName(reactor.type),
            reactor.vendor?.let { R.string.detail_vendor to it },
            reactor.containment?.let { R.string.detail_containment to it },
            reactor.licensedMwt?.let { R.string.detail_licensed_power to stringResource(R.string.mwt_value, formatNumber(it)) },
            R.string.detail_operator to reactor.operator,
            R.string.detail_location to reactor.location,
            R.string.detail_region to regionName(reactor.region),
            R.string.detail_docket to reactor.docket,
            R.string.detail_license to reactor.license,
            reactor.licenseIssued?.let { R.string.detail_license_issued to it },
            reactor.licenseRenewed?.let { R.string.detail_license_renewed to it },
            reactor.licenseExpires?.let { R.string.detail_license_expires to it },
        )
        // Two columns when the pane is wide enough, one otherwise.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            for ((label, value) in rows) {
                LabeledValue(
                    label = stringResource(label),
                    value = value,
                    modifier = Modifier
                        .widthIn(min = 240.dp)
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SiteCard(siteUnits: List<ReactorUnit>, onUnitClick: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.site_units)) {
        Column {
            for (unit in siteUnits) {
                ListItem(
                    headlineContent = { Text(unit.name) },
                    supportingContent = { Text(stringResource(unit.status.labelRes())) },
                    leadingContent = { PowerRing(unit.power, unit.status, size = 40.dp, textStyle = MaterialTheme.typography.labelMedium) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onUnitClick(unit.name) },
                )
            }
        }
    }
}

@Composable
private fun LinkButtons(reactor: Reactor?, onOpen: (String) -> Unit) {
    if (reactor == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reactor.nrcUrl?.let { url ->
            OutlinedButton(onClick = { onOpen(url) }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.nrc_page))
            }
        }
        OutlinedButton(
            onClick = {
                val plant = reactor.fullName.substringBefore(", Unit")
                onOpen("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("$plant, ${reactor.state}"))
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(Icons.Default.Map, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.map))
        }
    }
}

/** Shown in the detail pane on large screens before a unit is picked. */
@Composable
fun NoUnitSelected() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Factory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.select_unit),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
