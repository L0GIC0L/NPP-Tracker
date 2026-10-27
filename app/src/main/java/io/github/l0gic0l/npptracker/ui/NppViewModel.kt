package io.github.l0gic0l.npptracker.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.l0gic0l.npptracker.data.NrcRepository
import io.github.l0gic0l.npptracker.data.PowerReport
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.data.Reactor
import io.github.l0gic0l.npptracker.data.ReactorUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class UnitFilter { ALL, FULL, REDUCED, OFFLINE, STARRED }

enum class UnitSort { NAME, POWER_HIGH, POWER_LOW, CHANGE, STATE }

data class NppUiState(
    val units: List<ReactorUnit> = emptyList(),
    val reportDate: LocalDate? = null,
    val fetchedAt: Instant? = null,
    /** True until the first data (cached or downloaded) is available. */
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** A refresh failure to show once. */
    val errorMessage: String? = null,
    val starred: Set<String> = emptySet(),
    val query: String = "",
    val filter: UnitFilter = UnitFilter.ALL,
    val sort: UnitSort = UnitSort.NAME,
) {
    fun unit(name: String?): ReactorUnit? = units.firstOrNull { it.name == name }

    fun count(filter: UnitFilter): Int = units.count { it.matches(filter, starred) }

    val visibleUnits: List<ReactorUnit>
        get() {
            val q = query.trim()
            return units
                .filter { it.matches(filter, starred) }
                .filter { q.isEmpty() || it.matches(q) }
                .sortedWith(
                    when (sort) {
                        UnitSort.NAME -> compareBy { it.name }
                        UnitSort.POWER_HIGH -> compareByDescending<ReactorUnit> { it.power }.thenBy { it.name }
                        UnitSort.POWER_LOW -> compareBy<ReactorUnit> { it.power }.thenBy { it.name }
                        UnitSort.CHANGE -> compareByDescending<ReactorUnit> { kotlin.math.abs(it.change) }.thenBy { it.name }
                        UnitSort.STATE -> compareBy<ReactorUnit> { it.reactor?.state ?: "~" }.thenBy { it.name }
                    }
                )
        }
}

private fun ReactorUnit.matches(filter: UnitFilter, starred: Set<String>) = when (filter) {
    UnitFilter.ALL -> true
    UnitFilter.FULL -> status == PowerStatus.FULL
    UnitFilter.REDUCED -> status == PowerStatus.REDUCED
    UnitFilter.OFFLINE -> status == PowerStatus.OFFLINE
    UnitFilter.STARRED -> name in starred
}

private fun ReactorUnit.matches(query: String): Boolean {
    val r = reactor
    return listOfNotNull(name, r?.fullName, r?.state, r?.location, r?.operator, r?.type, r?.vendor)
        .any { it.contains(query, ignoreCase = true) }
}

class NppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NrcRepository(application)
    private val prefs = application.getSharedPreferences("npp_tracker", Context.MODE_PRIVATE)
    private var reactors: Map<String, Reactor> = emptyMap()

    private val _state = MutableStateFlow(
        NppUiState(starred = prefs.getStringSet(KEY_STARRED, emptySet()).orEmpty().toSet())
    )
    val state: StateFlow<NppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            reactors = repository.loadReactors()
            val cached = runCatching { repository.loadCachedReport() }.getOrNull()
            if (cached != null) show(cached)
            val stale = cached == null || Duration.between(cached.fetchedAt, Instant.now()) > MAX_CACHE_AGE
            if (stale) refresh()
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                show(repository.downloadReport())
            } catch (e: Exception) {
                _state.update {
                    it.copy(errorMessage = e.message ?: e.javaClass.simpleName, isLoading = false)
                }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun errorShown() = _state.update { it.copy(errorMessage = null) }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setFilter(filter: UnitFilter) = _state.update { it.copy(filter = filter) }

    fun setSort(sort: UnitSort) = _state.update { it.copy(sort = sort) }

    fun toggleStar(name: String) {
        _state.update {
            val starred = if (name in it.starred) it.starred - name else it.starred + name
            prefs.edit { putStringSet(KEY_STARRED, starred) }
            it.copy(starred = starred)
        }
    }

    private fun show(report: PowerReport) {
        // Every unit in the report is shown, even one that is missing from the bundled details.
        val units = report.history
            .map { (name, history) -> ReactorUnit(name, reactors[name], history) }
            .sortedBy { it.name }
        _state.update {
            it.copy(
                units = units,
                reportDate = units.mapNotNull { unit -> unit.latest?.date }.maxOrNull(),
                fetchedAt = report.fetchedAt,
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private companion object {
        const val KEY_STARRED = "starred"
        val MAX_CACHE_AGE: Duration = Duration.ofHours(1)
    }
}
