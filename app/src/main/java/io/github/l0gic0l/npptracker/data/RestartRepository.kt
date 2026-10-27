package io.github.l0gic0l.npptracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Restart projects are curated by hand in `app/src/main/assets/restarts.json`. The app also
 * downloads the latest copy of that file from the repository, so edits on GitHub reach
 * installed apps without a new release. Whichever copy was reviewed more recently is used.
 */
class RestartRepository(private val context: Context) {
    private val cacheFile = File(context.filesDir, "restarts.json")

    suspend fun load(): RestartData = withContext(Dispatchers.IO) {
        val bundled = CatalogParser.restarts(context.assets.open("restarts.json").bufferedReader().use { it.readText() })
        val cached = runCatching { CatalogParser.restarts(cacheFile.readText()) }.getOrNull()
        if (cached != null && !cached.updated.isBefore(bundled.updated)) cached else bundled
    }

    /** Downloads the latest restart file. Returns null if it could not be fetched or read. */
    suspend fun download(): RestartData? = withContext(Dispatchers.IO) {
        runCatching {
            val text = httpGet(REMOTE_URL)
            CatalogParser.restarts(text).also { writeAtomically(cacheFile, text) }
        }.getOrNull()
    }

    private companion object {
        const val REMOTE_URL =
            "https://raw.githubusercontent.com/L0GIC0L/NPP-Tracker/main/app/src/main/assets/restarts.json"
    }
}
