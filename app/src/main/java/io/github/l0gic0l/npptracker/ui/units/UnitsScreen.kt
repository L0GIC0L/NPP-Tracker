package io.github.l0gic0l.npptracker.ui.units

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.l0gic0l.npptracker.ui.NppUiState
import io.github.l0gic0l.npptracker.ui.UnitFilter
import io.github.l0gic0l.npptracker.ui.UnitSort
import kotlinx.coroutines.launch

/**
 * The unit list and the selected unit's details. On phones the two are shown one at a time;
 * on tablets, unfolded foldables and landscape windows they sit side by side, and the divider
 * between them can be dragged to resize the panes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UnitsScreen(
    state: NppUiState,
    navigator: ThreePaneScaffoldNavigator<String>,
    pendingUnit: String?,
    onPendingUnitShown: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (UnitFilter) -> Unit,
    onSortChange: (UnitSort) -> Unit,
    onToggleStar: (String) -> Unit,
    onOpenRestarts: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val showUnit: (String) -> Unit = { name ->
        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, name) }
    }

    // A unit picked on another screen (e.g. the overview) is opened once this screen is shown.
    LaunchedEffect(pendingUnit) {
        if (pendingUnit != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, pendingUnit)
            onPendingUnitShown()
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                UnitListPane(
                    state = state,
                    selectedName = navigator.currentDestination?.contentKey,
                    onUnitClick = showUnit,
                    onRefresh = onRefresh,
                    onQueryChange = onQueryChange,
                    onFilterChange = onFilterChange,
                    onSortChange = onSortChange,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val unit = state.unit(navigator.currentDestination?.contentKey)
                if (unit == null) {
                    NoUnitSelected()
                } else {
                    key(unit.name) {
                        UnitDetailPane(
                            unit = unit,
                            siteUnits = remember(state.units, unit) {
                                state.units.filter { it.siteKey != null && it.siteKey == unit.siteKey && it.name != unit.name }
                            },
                            reportDate = state.reportDate,
                            starred = unit.name in state.starred,
                            showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                            restart = state.restartFor(unit.name),
                            onBack = { scope.launch { navigator.navigateBack() } },
                            onToggleStar = { onToggleStar(unit.name) },
                            onUnitClick = showUnit,
                            onOpenRestarts = onOpenRestarts,
                        )
                    }
                }
            }
        },
        paneExpansionState = rememberPaneExpansionState(keyProvider = navigator.scaffoldValue),
        paneExpansionDragHandle = { expansionState ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier = Modifier.paneExpansionDraggable(
                    expansionState,
                    LocalMinimumInteractiveComponentSize.current,
                    interactionSource,
                ),
                interactionSource = interactionSource,
            )
        },
    )
}
