package io.github.l0gic0l.npptracker.data

/** Static details for one licensed power reactor unit, bundled from the NRC website. */
data class Reactor(
    /** Unit name exactly as it appears in the NRC daily power reactor status report. */
    val name: String,
    val fullName: String,
    /** "PWR" or "BWR". */
    val type: String,
    val location: String,
    val state: String,
    val operator: String,
    /** NRC region 1-4. */
    val region: Int,
    val docket: String,
    val license: String,
    val licensedMwt: Int?,
    val licenseIssued: String?,
    val licenseRenewed: String?,
    val licenseExpires: String?,
    val vendor: String?,
    val containment: String?,
    val nrcUrl: String?,
)
