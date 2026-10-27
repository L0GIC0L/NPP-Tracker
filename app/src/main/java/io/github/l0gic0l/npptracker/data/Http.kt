package io.github.l0gic0l.npptracker.data

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads [url] as text. Call off the main thread. */
internal fun httpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    connection.setRequestProperty("User-Agent", "NPP-Tracker Android app")
    try {
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) throw IOException("Server returned HTTP $code")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/** Writes to a temporary file first so a failed write never corrupts [file]. */
internal fun writeAtomically(file: File, text: String) {
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeText(text)
    if (!temp.renameTo(file)) {
        temp.copyTo(file, overwrite = true)
        temp.delete()
    }
}
