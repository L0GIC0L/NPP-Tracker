package io.github.l0gic0l.npptracker.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.NrcRepository
import io.github.l0gic0l.npptracker.ui.overview.OverviewScreen
import io.github.l0gic0l.npptracker.ui.units.UnitsScreen
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class AppDestination(@StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    UNITS(R.string.nav_units, Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    OVERVIEW(R.string.nav_overview, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
}

/**
 * App shell. The navigation suite shows a bottom bar on phones and a navigation rail on
 * tablets, unfolded foldables and other wide windows.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NppApp(viewModel: NppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.UNITS) }
    var pendingUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    // Two panes from medium width up, so unfolded foldables and portrait tablets show the
    // list and the details together. The panes are kept clear of a separating hinge.
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        scaffoldDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfoV2()),
    )
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh failures are shown as a snackbar when there is still data on screen.
    val refreshFailed = state.errorMessage?.let { stringResource(R.string.refresh_failed, it) }
    LaunchedEffect(refreshFailed) {
        if (refreshFailed != null && state.units.isNotEmpty()) {
            snackbarHostState.showSnackbar(refreshFailed)
            viewModel.errorShown()
        }
    }

    BackHandler(enabled = destination != AppDestination.UNITS) { destination = AppDestination.UNITS }

    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationItems = {
                for (item in AppDestination.entries) {
                    val selected = item == destination
                    NavigationSuiteItem(
                        selected = selected,
                        onClick = { destination = item },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            },
        ) {
            when (destination) {
                AppDestination.UNITS -> UnitsScreen(
                    state = state,
                    navigator = navigator,
                    pendingUnit = pendingUnit,
                    onPendingUnitShown = { pendingUnit = null },
                    onRefresh = viewModel::refresh,
                    onQueryChange = viewModel::setQuery,
                    onFilterChange = viewModel::setFilter,
                    onSortChange = viewModel::setSort,
                    onToggleStar = viewModel::toggleStar,
                )
                AppDestination.OVERVIEW -> OverviewScreen(
                    state = state,
                    onUnitClick = { name ->
                        pendingUnit = name
                        destination = AppDestination.UNITS
                    },
                    onRefresh = viewModel::refresh,
                    onAbout = { showAbout = true },
                )
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    if (showAbout) AboutDialog(state, onDismiss = { showAbout = false })
}

@Composable
private fun AboutDialog(state: NppUiState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            val downloaded = state.fetchedAt?.let {
                "\n\n" + stringResource(
                    R.string.about_downloaded,
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault())
                        .format(it),
                )
            }.orEmpty()
            Text(stringResource(R.string.about_body) + downloaded)
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri(NrcRepository.STATUS_PAGE_URL) } }) {
                Text(stringResource(R.string.open_nrc_report))
            }
        },
    )
}
