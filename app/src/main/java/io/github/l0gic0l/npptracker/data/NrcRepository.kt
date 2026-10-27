package io.github.l0gic0l.npptracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant

data class PowerReport(
    val history: Map<String, List<DailyPower>>,
    /** When this copy of the report was downloaded. */
    val fetchedAt: Instant,
)

/**
 * Loads the bundled unit details and the NRC daily power report. The last downloaded
 * report is kept on disk so the app still works offline.
 */
class NrcRepository(private val context: Context) {
    private val cacheFile = File(context.filesDir, "power_reactor_status.txt")

    suspend fun loadReactors(): Map<String, Reactor> = withContext(Dispatchers.IO) {
        val json = context.assets.open("reactors.json").bufferedReader().use { it.readText() }
        CatalogParser.reactors(json).associateBy { it.name }
    }

    suspend fun loadCachedReport(): PowerReport? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        val history = PowerReportParser.parse(cacheFile.readText())
        if (history.isEmpty()) null else PowerReport(history, Instant.ofEpochMilli(cacheFile.lastModified()))
    }

    suspend fun downloadReport(): PowerReport = withContext(Dispatchers.IO) {
        val text = httpGet(STATUS_URL)
        val history = PowerReportParser.parse(text)
        if (history.isEmpty()) throw IOException("The NRC report could not be read")
        writeAtomically(cacheFile, text)
        PowerReport(history, Instant.now())
    }

    companion object {
        const val STATUS_URL = "https://www.nrc.gov/documents-reports/document-collections/" +
            "events-reports-associated-with/power-reactor-status-reports/PowerReactorStatusForLast365Days.txt"
        const val STATUS_PAGE_URL = "https://www.nrc.gov/documents-reports/document-collections/" +
            "events-reports-associated-with/power-reactor-status-reports/ps"
    }
}
