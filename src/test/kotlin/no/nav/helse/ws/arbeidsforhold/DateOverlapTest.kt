package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.ws.arbeidsforhold.dateOverlap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateOverlapTest {

    val january1 = LocalDate.of(2019, 1, 1)
    val january31 = LocalDate.of(2019, 1, 31)

    @Test
    fun `older interval should not overlap`() {
        Assertions.assertFalse(dateOverlap(LocalDate.of(2018, 12, 1), LocalDate.of(2018, 12, 31), january1, january31))
    }

    @Test
    fun `newer interval should not overlap`() {
        Assertions.assertFalse(dateOverlap(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28), january1, january31))
    }

    @Test
    fun `should overlap on first day`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1), january1, january31))
    }

    @Test
    fun `should overlap on last day`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2019, 1, 31), LocalDate.of(2019, 1, 31), january1, january31))
    }

    @Test
    fun `partly older interval should overlap`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2018, 12, 1), LocalDate.of(2019, 1, 14), january1, january31))
    }

    @Test
    fun `partly newer interval should overlap`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2018, 1, 14), LocalDate.of(2019, 2, 28), january1, january31))
    }

    @Test
    fun `smaller interval should overlap`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 14), january1, january31))
    }

    @Test
    fun `bigger interval should overlap`() {
        Assertions.assertTrue(dateOverlap(LocalDate.of(2018, 12, 1), LocalDate.of(2019, 2, 28), january1, january31))
    }

    @Test
    fun `invalid interval should not overlap`() {
        val december1 = LocalDate.of(2018, 12, 1)
        val december31 = LocalDate.of(2018, 12, 31)

        Assertions.assertFalse(dateOverlap(december31, december1, january1, january31))
        Assertions.assertFalse(dateOverlap(january1, january31, january31, january1))
        Assertions.assertFalse(dateOverlap(january31, december1, january1, january31))
    }
}