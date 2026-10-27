package io.github.l0gic0l.npptracker.ui.units

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.ReactorUnit
import io.github.l0gic0l.npptracker.ui.NppUiState
import io.github.l0gic0l.npptracker.ui.UnitFilter
import io.github.l0gic0l.npptracker.ui.UnitSort
import io.github.l0gic0l.npptracker.ui.components.ChangeIndicator
import io.github.l0gic0l.npptracker.ui.components.PowerRing
import io.github.l0gic0l.npptracker.ui.components.unitSupportingText
import io.github.l0gic0l.npptracker.ui.screens.AppTopBar
import io.github.l0gic0l.npptracker.ui.screens.LoadingOrError
import io.github.l0gic0l.npptracker.ui.screens.RefreshableContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitListPane(
    state: NppUiState,
    selectedName: String?,
    onUnitClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (UnitFilter) -> Unit,
    onSortChange: (UnitSort) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                reportDate = state.reportDate,
                scrollBehavior = scrollBehavior,
                actions = {
                    SortMenu(state.sort, onSortChange)
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        LoadingOrError(state, onRetry = onRefresh, modifier = Modifier.padding(padding)) {
            RefreshableContent(state.isRefreshing, onRefresh, Modifier.padding(padding)) {
                val visible = remember(state.units, state.query, state.filter, state.sort, state.starred) {
                    state.visibleUnits
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    item(key = "search") { UnitSearchField(state.query, onQueryChange) }
                    item(key = "filters") { FilterRow(state, onFilterChange) }
                    items(visible, key = { it.name }) { unit ->
                        UnitRow(
                            unit = unit,
                            starred = unit.name in state.starred,
                            selected = unit.name == selectedName,
                            onClick = { onUnitClick(unit.name) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (visible.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.no_matching_units),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { keyboard?.hide() },
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.search_units)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {}
}

@Composable
private fun FilterRow(state: NppUiState, onFilterChange: (UnitFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (filter in UnitFilter.entries) {
            val count = state.count(filter)
            val selected = state.filter == filter
            // The starred chip only appears once something is starred (or while it is selected).
            if (filter == UnitFilter.STARRED && count == 0 && !selected) continue
            val label = stringResource(
                when (filter) {
                    UnitFilter.ALL -> R.string.filter_all
                    UnitFilter.FULL -> R.string.status_full
                    UnitFilter.REDUCED -> R.string.status_reduced
                    UnitFilter.OFFLINE -> R.string.status_offline
                    UnitFilter.STARRED -> R.string.filter_starred
                }
            )
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(R.string.filter_with_count, label, count)) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun SortMenu(sort: UnitSort, onSortChange: (UnitSort) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in UnitSort.entries) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (option) {
                                    UnitSort.NAME -> R.string.sort_name
                                    UnitSort.POWER_HIGH -> R.string.sort_power_high
                                    UnitSort.POWER_LOW -> R.string.sort_power_low
                                    UnitSort.CHANGE -> R.string.sort_change
                                    UnitSort.STATE -> R.string.sort_state
                                }
                            )
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                    trailingIcon = if (option == sort) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
fun UnitRow(
    unit: ReactorUnit,
    starred: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(unit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(unitSupportingText(unit.status, unit.reactor), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { PowerRing(unit.power, unit.status) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChangeIndicator(unit.change)
                if (starred) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(R.string.starred),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        ),
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    )
}
