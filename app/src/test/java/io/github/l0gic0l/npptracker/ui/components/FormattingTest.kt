package io.github.l0gic0l.npptracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class FormattingTest {
    @Test
    fun partialDatesFormatToTheirPrecision() {
        assertEquals(LocalDate.of(2026, 8, 30).formatMedium(), formatPartialDate("2026-08-30"))
        assertEquals(YearMonth.of(2026, 10).format(DateTimeFormatter.ofPattern("MMM yyyy")), formatPartialDate("2026-10"))
        assertEquals("2027", formatPartialDate("2027"))
        assertEquals("Early 2029", formatPartialDate("Early 2029"))
    }

    @Test
    fun invalidDatesAreShownAsWritten() {
        assertEquals("2026-13-40", formatPartialDate("2026-13-40"))
    }
}
