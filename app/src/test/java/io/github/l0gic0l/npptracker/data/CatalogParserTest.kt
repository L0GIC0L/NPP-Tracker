package io.github.l0gic0l.npptracker.data

import io.github.l0gic0l.npptracker.ui.components.formatPartialDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled JSON files, so a bad hand edit fails the build instead of the app. */
class CatalogParserTest {
    private fun asset(name: String) = File("src/main/assets/$name").readText()

    @Test
    fun bundledReactorsParse() {
        val reactors = CatalogParser.reactors(asset("reactors.json"))

        assertEquals(95, reactors.size)
        assertEquals(reactors.size, reactors.map { it.name }.toSet().size)
        assertTrue(reactors.all { it.type == "PWR" || it.type == "BWR" })
        assertTrue(reactors.all { it.region in 1..4 })
    }

    @Test
    fun bundledRestartsParseAndAreConsistent() {
        val data = CatalogParser.restarts(asset("restarts.json"))
        val reactorNames = CatalogParser.reactors(asset("reactors.json")).map { it.name }.toSet()

        assertTrue(data.projects.isNotEmpty())
        assertEquals(data.projects.size, data.projects.map { it.id }.toSet().size)
        for (project in data.projects) {
            assertTrue(project.id, project.milestones.isNotEmpty())
            // A linked unit must match a name in the NRC daily report.
            project.nrcUnit?.let { assertTrue("${project.id}: unknown unit $it", it in reactorNames) }
            for (m in project.milestones) {
                assertTrue("${project.id}: empty milestone", m.text.isNotBlank())
                // Numeric dates must be real yyyy-MM-dd or yyyy-MM dates (phrases like "Early 2029" are fine).
                if (m.date.matches(Regex("""\d{4}-.*"""))) {
                    assertNotEquals("${project.id}: bad date ${m.date}", m.date, formatPartialDate(m.date))
                }
            }
            // Finished milestones come before unfinished ones.
            val firstUpcoming = project.milestones.indexOfFirst { !it.done }
            if (firstUpcoming >= 0) {
                assertTrue(project.id, project.milestones.drop(firstUpcoming).none { it.done })
            }
        }
    }

    @Test
    fun palisadesIsLinkedToItsNrcUnit() {
        val palisades = CatalogParser.restarts(asset("restarts.json")).projects.firstOrNull { it.id == "palisades" }

        assertNotNull(palisades)
        assertEquals("Palisades", palisades!!.nrcUnit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownStageIsRejected() {
        CatalogParser.restarts(
            """
            {"updated": "2026-10-27", "projects": [{"id": "x", "name": "X", "owner": "O", "location": "L",
             "state": "ST", "type": "PWR", "shutdown": "2020", "stage": "sleeping", "target": "?",
             "summary": "S", "milestones": []}]}
            """.trimIndent()
        )
    }
}
