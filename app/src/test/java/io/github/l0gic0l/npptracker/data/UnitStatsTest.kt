package io.github.l0gic0l.npptracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UnitStatsTest {
    private val start = LocalDate.of(2026, 1, 1)

    private fun days(vararg power: Int) = power.mapIndexed { i, p -> DailyPower(start.plusDays(i.toLong()), p) }

    private fun reactor(mwt: Int?) = Reactor(
        name = "Test 1", fullName = "Test, Unit 1", type = "PWR", location = "Somewhere, TX", state = "TX",
        operator = "Operator", region = 4, docket = "05000000", license = "NPF-0", licensedMwt = mwt,
        licenseIssued = null, licenseRenewed = null, licenseExpires = null, vendor = null, containment = null,
        nrcUrl = null,
    )

    @Test
    fun statusThresholds() {
        assertEquals(PowerStatus.OFFLINE, powerStatus(0))
        assertEquals(PowerStatus.REDUCED, powerStatus(1))
        assertEquals(PowerStatus.REDUCED, powerStatus(99))
        assertEquals(PowerStatus.FULL, powerStatus(100))
    }

    @Test
    fun changeAndStreak() {
        val unit = ReactorUnit("Test 1", null, days(100, 0, 0, 20, 60))

        assertEquals(60, unit.power)
        assertEquals(40, unit.change)
        assertEquals(2, unit.streakDays)
        assertFalse(unit.streakCoversHistory)
    }

    @Test
    fun streakCoveringWholeHistoryIsFlagged() {
        val unit = ReactorUnit("Test 1", null, days(0, 0, 0))

        assertEquals(3, unit.streakDays)
        assertTrue(unit.streakCoversHistory)
    }

    @Test
    fun findsEndedAndOngoingOutages() {
        val result = outages(days(100, 0, 0, 100, 100, 0))

        assertEquals(
            listOf(
                Outage(start.plusDays(1), start.plusDays(2), ongoing = false),
                Outage(start.plusDays(5), start.plusDays(5), ongoing = true),
            ),
            result,
        )
        assertEquals(2, result[0].days)
    }

    @Test
    fun weightedPowerUsesLicensedThermalPower() {
        val big = ReactorUnit("Big", reactor(3000), days(100))
        val small = ReactorUnit("Small", reactor(1000), days(0))

        assertEquals(75.0, weightedPower(listOf(big, small)), 0.001)
        assertEquals(3000.0, big.thermalOutputMwt!!, 0.001)
    }

    @Test
    fun fleetHistoryCountsUnitsOnlinePerDay() {
        val a = ReactorUnit("A", reactor(1000), days(100, 50))
        val b = ReactorUnit("B", reactor(1000), days(0, 100))

        val fleet = fleetHistory(listOf(a, b))

        assertEquals(2, fleet.size)
        assertEquals(1, fleet[0].unitsOnline)
        assertEquals(50.0, fleet[0].weightedPower, 0.001)
        assertEquals(2, fleet[1].unitsOnline)
        assertEquals(75.0, fleet[1].weightedPower, 0.001)
    }
}
