package io.github.l0gic0l.npptracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PowerReportParserTest {
    @Test
    fun parsesUnitsAndSortsHistoryOldestFirst() {
        val text = """
            ReportDt|Unit|Power
            10/26/2026 12:00:00 AM|Byron 2|48
            10/26/2026 12:00:00 AM|Callaway|0
            10/25/2026 12:00:00 AM|Byron 2|0
            10/25/2026 12:00:00 AM|Callaway|0
        """.trimIndent()

        val history = PowerReportParser.parse(text)

        assertEquals(setOf("Byron 2", "Callaway"), history.keys)
        assertEquals(
            listOf(DailyPower(LocalDate.of(2026, 10, 25), 0), DailyPower(LocalDate.of(2026, 10, 26), 48)),
            history.getValue("Byron 2"),
        )
    }

    @Test
    fun skipsHeaderAndMalformedLines() {
        val text = "ReportDt|Unit|Power\r\nnot a line\r\n1/2/2026 12:00:00 AM|Ginna|abc\r\n1/2/2026 12:00:00 AM|Ginna|100\r\n"

        val history = PowerReportParser.parse(text)

        assertEquals(listOf(DailyPower(LocalDate.of(2026, 1, 2), 100)), history.getValue("Ginna"))
    }

    @Test
    fun returnsEmptyMapForNonReportText() {
        assertTrue(PowerReportParser.parse("<html><body>Page not found</body></html>").isEmpty())
    }
}
