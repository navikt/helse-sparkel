package no.nav.helse.ws.arbeidsforhold

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.xml.datatype.DatatypeFactory

class GregorianToISOTests {

    @Test
    fun `the easy case`() {
        val calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar()
        calendar.year = 2018
        calendar.month = 11
        calendar.day = 20
        assertEquals("2018-11-20", calendar.iso8601())
    }

    @Test
    fun `should zero-pad every field`() {
        val calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar()
        calendar.year = 18
        calendar.month = 1
        calendar.day = 2
        assertEquals("0018-01-02", calendar.iso8601())
    }

}