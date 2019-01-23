package no.nav.helse.common

import java.time.*
import javax.xml.datatype.*

private val datatypeFactory = DatatypeFactory.newInstance()

fun XMLGregorianCalendar.toLocalDate() = LocalDate.of(year, month, day)

fun LocalDate.toXmlGregorianCalendar() = this.let { localDate ->
    datatypeFactory.newXMLGregorianCalendar().apply {
        year = localDate.year
        month = localDate.monthValue
        day = localDate.dayOfMonth
    }
}