package io.github.l0gic0l.npptracker.ui.restarts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.Milestone
import io.github.l0gic0l.npptracker.data.ReactorUnit
import io.github.l0gic0l.npptracker.data.RestartProject
import io.github.l0gic0l.npptracker.data.RestartStage
import io.github.l0gic0l.npptracker.ui.NppUiState
import io.github.l0gic0l.npptracker.ui.components.LabeledValue
import io.github.l0gic0l.npptracker.ui.components.PowerRing
import io.github.l0gic0l.npptracker.ui.components.StageProgress
import io.github.l0gic0l.npptracker.ui.components.formatMedium
import io.github.l0gic0l.npptracker.ui.components.formatNumber
import io.github.l0gic0l.npptracker.ui.components.formatPartialDate
import io.github.l0gic0l.npptracker.ui.components.labelRes
import io.github.l0gic0l.npptracker.ui.screens.AppTopBar
import io.github.l0gic0l.npptracker.ui.screens.RefreshableContent

/** Shut-down plants that are being returned to service, with each project's progress. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestartsScreen(
    state: NppUiState,
    onUnitClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val data = state.restarts
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.restarts_title),
                subtitle = data?.let { stringResource(R.string.restarts_reviewed, it.updated.formatMedium()) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        if (data == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        RefreshableContent(state.isRefreshing, onRefresh, Modifier.padding(padding)) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 360.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
            ) {
                item(key = "summary", span = StaggeredGridItemSpan.FullLine) { RestartSummaryCard(data.projects) }
                items(data.projects, key = { it.id }) { project ->
                    RestartCard(
                        project = project,
                        unit = project.nrcUnit?.let { state.unit(it) },
                        onUnitClick = onUnitClick,
                    )
                }
                item(key = "disclaimer", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = stringResource(R.string.restarts_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RestartSummaryCard(projects: List<RestartProject>) {
    val colors = MaterialTheme.colorScheme
    val capacity = projects.sumOf { it.capacityMwe ?: 0 }
    val generating = projects.count { it.stage == RestartStage.OPERATING }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = pluralStringResource(R.plurals.restarts_count, projects.size, projects.size),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    R.string.restarts_capacity,
                    formatNumber(capacity),
                    if (generating == 0) {
                        stringResource(R.string.restarts_none_generating)
                    } else {
                        stringResource(R.string.restarts_generating, generating)
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = stringResource(R.string.restarts_explainer), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestartCard(project: RestartProject, unit: ReactorUnit?, onUnitClick: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(project.name, style = MaterialTheme.typography.titleLarge)
                project.formerName?.let {
                    Text(
                        text = stringResource(R.string.formerly, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.restart_owner_location, project.owner, project.location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StageProgress(project.stage)
            Text(project.summary, style = MaterialTheme.typography.bodyMedium)

            if (unit != null) LiveStatus(unit, onUnitClick)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2,
            ) {
                val facts = listOfNotNull(
                    R.string.restart_target to project.target,
                    project.capacityMwe?.let { R.string.restart_capacity to stringResource(R.string.mwe_value, formatNumber(it)) },
                    project.design?.let { R.string.restart_design to it },
                    R.string.restart_shutdown to formatPartialDate(project.shutdown),
                    project.buyer?.let { R.string.restart_buyer to it },
                    project.financing?.let { R.string.restart_financing to it },
                )
                for ((label, value) in facts) {
                    LabeledValue(
                        label = stringResource(label),
                        value = value,
                        modifier = Modifier
                            .widthIn(min = 140.dp)
                            .weight(1f),
                    )
                }
            }

            Text(stringResource(R.string.restart_timeline), style = MaterialTheme.typography.titleMedium)
            Timeline(project.milestones)

            if (project.sources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.restart_sources),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    for (source in project.sources) {
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { runCatching { uriHandler.openUri(source.url) } }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
            project.nrcUrl?.let { url ->
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(url) } },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.nrc_page))
                }
            }
        }
    }
}

/** The unit's latest line in the NRC daily report, for restarts that already appear there. */
@Composable
private fun LiveStatus(unit: ReactorUnit, onUnitClick: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PowerRing(unit.power, unit.status, size = 40.dp, textStyle = MaterialTheme.typography.labelMedium)
            Text(
                text = stringResource(
                    R.string.live_nrc_status,
                    stringResource(unit.status.labelRes()),
                    unit.latest?.date?.formatMedium().orEmpty(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = { onUnitClick(unit.name) }) {
                Text(stringResource(R.string.unit_details_button))
            }
        }
    }
}

/** A vertical timeline: filled dots for completed milestones, hollow dots for upcoming ones. */
@Composable
private fun Timeline(milestones: List<Milestone>) {
    val colors = MaterialTheme.colorScheme
    val doneText = stringResource(R.string.milestone_done)
    val upcomingText = stringResource(R.string.milestone_upcoming)
    Column {
        milestones.forEachIndexed { index, milestone ->
            val date = milestone.date.ifEmpty { null }?.let { formatPartialDate(it) }
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .semantics(mergeDescendants = true) {
                        contentDescription = listOfNotNull(
                            if (milestone.done) doneText else upcomingText,
                            date,
                            milestone.text,
                        ).joinToString(", ")
                    },
            ) {
                // Dot column, with a connecting line to the next milestone.
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (index < milestones.lastIndex) {
                        // The line is solid up to the last completed milestone.
                        val lineDone = milestone.done && milestones[index + 1].done
                        Box(
                            Modifier
                                .padding(top = 14.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(if (lineDone) colors.primary else colors.outlineVariant),
                        )
                    }
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .then(
                                if (milestone.done) {
                                    Modifier.background(colors.primary)
                                } else {
                                    Modifier.border(2.dp, colors.outline, CircleShape)
                                }
                            ),
                    )
                }
                Column(
                    Modifier
                        .padding(start = 8.dp, bottom = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = date ?: stringResource(R.string.milestone_undated),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (milestone.done) colors.onSurfaceVariant else colors.tertiary,
                    )
                    Text(
                        text = milestone.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (milestone.done) colors.onSurface else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
