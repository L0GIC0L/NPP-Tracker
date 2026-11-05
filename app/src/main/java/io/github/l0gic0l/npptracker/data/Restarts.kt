package io.github.l0gic0l.npptracker.data

import java.time.LocalDate

/** Steps a permanently shut-down reactor goes through to return to service, in order. */
enum class RestartStage {
    ANNOUNCED,
    NRC_REVIEW,
    APPROVED,
    FUEL_LOAD,
    STARTUP,
    OPERATING,
}

/**
 * One milestone in a restart project. [date] is "yyyy-MM-dd", "yyyy-MM", a year, free text
 * such as "Early 2029", or empty when no date is known.
 */
data class Milestone(val date: String, val text: String, val done: Boolean)

data class RestartSource(val title: String, val url: String)

/** A shut-down plant that is being returned to service. */
data class RestartProject(
    val id: String,
    val name: String,
    val formerName: String?,
    /** Unit name in the NRC daily status report, if the unit is listed there. */
    val nrcUnit: String?,
    val owner: String,
    val location: String,
    val state: String,
    val type: String,
    val design: String?,
    val capacityMwe: Int?,
    val shutdown: String,
    val stage: RestartStage,
    val target: String,
    val buyer: String?,
    val financing: String?,
    val summary: String,
    val milestones: List<Milestone>,
    val nrcUrl: String?,
    val sources: List<RestartSource>,
)

data class RestartData(
    /** When the restart information was last reviewed. */
    val updated: LocalDate,
    val projects: List<RestartProject>,
)
