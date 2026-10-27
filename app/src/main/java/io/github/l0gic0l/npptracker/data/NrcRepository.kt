package io.github.l0gic0l.npptracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
        val array = JSONArray(json)
        (0 until array.length()).map { parseReactor(array.getJSONObject(it)) }.associateBy { it.name }
    }

    suspend fun loadCachedReport(): PowerReport? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        val history = PowerReportParser.parse(cacheFile.readText())
        if (history.isEmpty()) null else PowerReport(history, Instant.ofEpochMilli(cacheFile.lastModified()))
    }

    suspend fun downloadReport(): PowerReport = withContext(Dispatchers.IO) {
        val text = download(STATUS_URL)
        val history = PowerReportParser.parse(text)
        if (history.isEmpty()) throw IOException("The NRC report could not be read")
        // Write to a temporary file first so a failed write never corrupts the cache.
        val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        temp.writeText(text)
        if (!temp.renameTo(cacheFile)) {
            temp.copyTo(cacheFile, overwrite = true)
            temp.delete()
        }
        PowerReport(history, Instant.now())
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "NPP-Tracker Android app")
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("NRC server returned HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseReactor(o: JSONObject) = Reactor(
        name = o.getString("name"),
        fullName = o.optStringOrNull("fullName") ?: o.getString("name"),
        type = o.getString("type"),
        location = o.getString("location"),
        state = o.getString("state"),
        operator = o.getString("operator"),
        region = o.getInt("region"),
        docket = o.getString("docket"),
        license = o.getString("license"),
        licensedMwt = if (o.isNull("licensedMwt")) null else o.getInt("licensedMwt"),
        licenseIssued = o.optStringOrNull("licenseIssued"),
        licenseRenewed = o.optStringOrNull("licenseRenewed"),
        licenseExpires = o.optStringOrNull("licenseExpires"),
        vendor = o.optStringOrNull("vendor"),
        containment = o.optStringOrNull("containment"),
        nrcUrl = o.optStringOrNull("nrcUrl"),
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifEmpty { null }

    companion object {
        const val STATUS_URL = "https://www.nrc.gov/documents-reports/document-collections/" +
            "events-reports-associated-with/power-reactor-status-reports/PowerReactorStatusForLast365Days.txt"
        const val STATUS_PAGE_URL = "https://www.nrc.gov/documents-reports/document-collections/" +
            "events-reports-associated-with/power-reactor-status-reports/ps"
    }
}
