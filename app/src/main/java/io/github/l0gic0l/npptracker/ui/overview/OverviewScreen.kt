package io.github.l0gic0l.npptracker.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.data.ReactorUnit
import io.github.l0gic0l.npptracker.data.RestartProject
import io.github.l0gic0l.npptracker.data.fleetHistory
import io.github.l0gic0l.npptracker.data.weightedPower
import io.github.l0gic0l.npptracker.ui.NppUiState
import io.github.l0gic0l.npptracker.ui.components.ChangeIndicator
import io.github.l0gic0l.npptracker.ui.components.ChartPoint
import io.github.l0gic0l.npptracker.ui.components.ChartRange
import io.github.l0gic0l.npptracker.ui.components.PowerChart
import io.github.l0gic0l.npptracker.ui.components.PowerRing
import io.github.l0gic0l.npptracker.ui.components.RangeSelector
import io.github.l0gic0l.npptracker.ui.components.SectionCard
import io.github.l0gic0l.npptracker.ui.components.formatMedium
import io.github.l0gic0l.npptracker.ui.components.formatNumber
import io.github.l0gic0l.npptracker.ui.components.labelRes
import io.github.l0gic0l.npptracker.ui.components.regionName
import io.github.l0gic0l.npptracker.ui.components.unitSupportingText
import io.github.l0gic0l.npptracker.ui.screens.AppTopBar
import io.github.l0gic0l.npptracker.ui.screens.LoadingOrError
import io.github.l0gic0l.npptracker.ui.screens.RefreshableContent
import io.github.l0gic0l.npptracker.ui.screens.reportSubtitle
import io.github.l0gic0l.npptracker.ui.theme.statusColor

/** Fleet-wide dashboard. Cards flow into as many columns as the window has room for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: NppUiState,
    onUnitClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onAbout: () -> Unit,
    onOpenRestarts: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.fleet_overview),
                subtitle = reportSubtitle(state.reportDate),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about))
                    }
                },
            )
        },
    ) { padding ->
        LoadingOrError(state, onRetry = onRefresh, modifier = Modifier.padding(padding)) {
            RefreshableContent(state.isRefreshing, onRefresh, Modifier.padding(padding)) {
                val units = state.units
                val offline = remember(units) {
                    units.filter { it.status == PowerStatus.OFFLINE }.sortedByDescending { it.streakDays }
                }
                val reduced = remember(units) {
                    units.filter { it.status == PowerStatus.REDUCED }.sortedBy { it.power }
                }
                val changed = remember(units) {
                    units.filter { it.change != 0 }.sortedByDescending { kotlin.math.abs(it.change) }
                }
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    item(key = "status", span = StaggeredGridItemSpan.FullLine) { FleetStatusCard(units) }
                    item(key = "trend", span = StaggeredGridItemSpan.FullLine) { FleetTrendCard(units) }
                    item(key = "offline") {
                        UnitListCard(
                            title = stringResource(R.string.offline_units) + " · ${offline.size}",
                            units = offline,
                            emptyText = stringResource(R.string.none_offline),
                            supporting = { unit ->
                                val days = unit.streakDays
                                val offlineFor = pluralStringResource(
                                    if (unit.streakCoversHistory) R.plurals.offline_for_at_least else R.plurals.offline_for,
                                    days,
                                    days,
                                )
                                if (state.restartFor(unit.name) != null) {
                                    stringResource(R.string.restarting_offline_for, offlineFor)
                                } else {
                                    offlineFor
                                }
                            },
                            onUnitClick = onUnitClick,
                        )
                    }
                    item(key = "reduced") {
                        UnitListCard(
                            title = stringResource(R.string.reduced_units) + " · ${reduced.size}",
                            units = reduced,
                            emptyText = stringResource(R.string.none_reduced),
                            supporting = { unitSupportingText(it.status, it.reactor) },
                            onUnitClick = onUnitClick,
                        )
                    }
                    item(key = "changes") {
                        val previous = units.firstNotNullOfOrNull { unit ->
                            unit.history.getOrNull(unit.history.size - 2)?.date
                        }
                        UnitListCard(
                            title = stringResource(R.string.changes_title, previous?.formatMedium() ?: "—"),
                            units = changed,
                            emptyText = stringResource(R.string.no_changes),
                            supporting = { unit ->
                                stringResource(R.string.change_from_to, unit.power - unit.change, unit.power)
                            },
                            trailing = { ChangeIndicator(it.change) },
                            onUnitClick = onUnitClick,
                        )
                    }
                    state.restarts?.let { restarts ->
                        item(key = "restarts") { RestartsCard(restarts.projects, onOpenRestarts) }
                    }
                    item(key = "regions") { RegionsCard(units) }
                    item(key = "types") { TypesCard(units) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FleetStatusCard(units: List<ReactorUnit>) {
    val colors = MaterialTheme.colorScheme
    val counts = PowerStatus.entries.associateWith { status -> units.count { it.status == status } }
    val online = units.count { it.power > 0 }
    val output = remember(units) { weightedPower(units) }
    val outputGwt = units.sumOf { it.thermalOutputMwt ?: 0.0 } / 1000
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.fleet_status), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(online.toString(), style = MaterialTheme.typography.displayMedium)
                Text(
                    text = stringResource(R.string.units_generating, units.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = stringResource(
                    R.string.fleet_output,
                    stringResource(R.string.power_percent_decimal, output),
                    formatNumber(outputGwt),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            // Stacked bar: one segment per status, sized by unit count.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (status in PowerStatus.entries) {
                    val count = counts.getValue(status)
                    if (count > 0) {
                        Box(
                            Modifier
                                .weight(count.toFloat())
                                .fillMaxSize()
                                .background(colors.statusColor(status)),
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (status in PowerStatus.entries) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.statusColor(status)),
                        )
                        Text(
                            text = stringResource(R.string.filter_with_count, stringResource(status.labelRes()), counts.getValue(status)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetTrendCard(units: List<ReactorUnit>) {
    var range by rememberSaveable { mutableStateOf(ChartRange.YEAR) }
    val history = remember(units) { fleetHistory(units) }
    val end = history.lastOrNull()?.date ?: return
    val days = remember(history, range) { history.filter { !it.date.isBefore(end.minusDays(range.days - 1)) } }
    val average = days.map { it.weightedPower }.average()
    val averageText = stringResource(R.string.power_percent_decimal, average)
    SectionCard(title = stringResource(R.string.fleet_trend)) {
        RangeSelector(range, { range = it }, Modifier.fillMaxWidth())
        PowerChart(
            points = remember(days) { days.map { ChartPoint(it.date, it.weightedPower.toFloat()) } },
            contentDescription = stringResource(
                R.string.fleet_chart_description,
                days.first().date.formatMedium(),
                days.last().date.formatMedium(),
                averageText,
            ),
        )
        Text(
            text = stringResource(R.string.fleet_trend_note, averageText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UnitListCard(
    title: String,
    units: List<ReactorUnit>,
    emptyText: String,
    supporting: @Composable (ReactorUnit) -> String,
    onUnitClick: (String) -> Unit,
    trailing: (@Composable (ReactorUnit) -> Unit)? = null,
) {
    SectionCard(title = title) {
        if (units.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            for (unit in units) {
                ListItem(
                    headlineContent = { Text(unit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(supporting(unit), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = {
                        PowerRing(unit.power, unit.status, size = 40.dp, textStyle = MaterialTheme.typography.labelMedium)
                    },
                    trailingContent = trailing?.let { { it(unit) } },
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
private fun RestartsCard(projects: List<RestartProject>, onOpenRestarts: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.restarts_title) + " · ${projects.size}",
        modifier = Modifier
            .clip(CardDefaults.shape)
            .clickable(onClick = onOpenRestarts),
    ) {
        for (project in projects) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        R.string.restart_owner_location,
                        stringResource(project.stage.labelRes()),
                        stringResource(R.string.restart_target) + ": " + project.target,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onOpenRestarts) { Text(stringResource(R.string.restart_details_button)) }
    }
}

@Composable
private fun RegionsCard(units: List<ReactorUnit>) {
    val areas = listOf(R.string.region_1_area, R.string.region_2_area, R.string.region_3_area, R.string.region_4_area)
    SectionCard(title = stringResource(R.string.by_region)) {
        for (region in 1..4) {
            GroupRow(
                title = regionName(region),
                subtitle = stringResource(areas[region - 1]),
                units = units.filter { it.reactor?.region == region },
            )
        }
    }
}

@Composable
private fun TypesCard(units: List<ReactorUnit>) {
    SectionCard(title = stringResource(R.string.by_type)) {
        GroupRow("PWR", stringResource(R.string.pwr_short), units.filter { it.reactor?.type == "PWR" })
        GroupRow("BWR", stringResource(R.string.bwr_short), units.filter { it.reactor?.type == "BWR" })
    }
}

/** A group of units with its size, how many are generating and its combined output. */
@Composable
private fun GroupRow(title: String, subtitle: String, units: List<ReactorUnit>) {
    if (units.isEmpty()) return
    val output = weightedPower(units)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$title · $subtitle", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.group_summary, units.size, units.count { it.power > 0 }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.power_percent_decimal, output), style = MaterialTheme.typography.titleMedium)
        }
        LinearProgressIndicator(
            progress = { (output / 100).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
