package io.github.l0gic0l.npptracker.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Parses the bundled JSON files (unit details and restart projects). */
object CatalogParser {
    fun reactors(json: String): List<Reactor> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Reactor(
                name = o.getString("name"),
                fullName = o.stringOrNull("fullName") ?: o.getString("name"),
                type = o.getString("type"),
                location = o.getString("location"),
                state = o.getString("state"),
                operator = o.getString("operator"),
                region = o.getInt("region"),
                docket = o.getString("docket"),
                license = o.getString("license"),
                licensedMwt = if (o.isNull("licensedMwt")) null else o.getInt("licensedMwt"),
                licenseIssued = o.stringOrNull("licenseIssued"),
                licenseRenewed = o.stringOrNull("licenseRenewed"),
                licenseExpires = o.stringOrNull("licenseExpires"),
                vendor = o.stringOrNull("vendor"),
                containment = o.stringOrNull("containment"),
                nrcUrl = o.stringOrNull("nrcUrl"),
            )
        }
    }

    /** Throws if the file is malformed, so a bad download is never used. */
    fun restarts(json: String): RestartData {
        val root = JSONObject(json)
        val projects = root.getJSONArray("projects")
        return RestartData(
            updated = LocalDate.parse(root.getString("updated")),
            projects = (0 until projects.length()).map { i ->
                val o = projects.getJSONObject(i)
                RestartProject(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    formerName = o.stringOrNull("formerName"),
                    nrcUnit = o.stringOrNull("nrcUnit"),
                    owner = o.getString("owner"),
                    location = o.getString("location"),
                    state = o.getString("state"),
                    type = o.getString("type"),
                    design = o.stringOrNull("design"),
                    capacityMwe = if (o.has("capacityMwe") && !o.isNull("capacityMwe")) o.getInt("capacityMwe") else null,
                    shutdown = o.getString("shutdown"),
                    stage = RestartStage.entries.firstOrNull { it.name.equals(o.getString("stage"), ignoreCase = true) }
                        ?: throw IllegalArgumentException("Unknown restart stage: " + o.getString("stage")),
                    target = o.getString("target"),
                    buyer = o.stringOrNull("buyer"),
                    financing = o.stringOrNull("financing"),
                    summary = o.getString("summary"),
                    milestones = o.getJSONArray("milestones").objects().map {
                        Milestone(it.optString("date"), it.getString("text"), it.getBoolean("done"))
                    },
                    nrcUrl = o.stringOrNull("nrcUrl"),
                    sources = o.optJSONArray("sources")?.objects()?.map {
                        RestartSource(it.getString("title"), it.getString("url"))
                    }.orEmpty(),
                )
            },
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).ifEmpty { null }
}
