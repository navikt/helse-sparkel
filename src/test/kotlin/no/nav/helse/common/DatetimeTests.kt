package no.nav.helse.common

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*
import java.util.*
import javax.xml.datatype.*

class DatetimeTests {

    @Test
    fun toCalendar() {
        val expected = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
            year = 2019
            month = 1
            day = 23
            hour = 0
            minute = 0
            second = 0
            millisecond = 0
            timezone = TimeZone.getDefault().rawOffset / 1000 / 60
        }
        val actual = LocalDate.of(2019, 1, 23).toXmlGregorianCalendar()
        assertEquals(expected, actual)
    }

    @Test
    fun toLocalDate() {
        val expected = LocalDate.of(2019, 1, 23)
        val actual = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
            year = 2019
            month = 1
            day = 23
        }.toLocalDate()
        assertEquals(expected, actual)
    }

}
