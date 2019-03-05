package no.nav.helse.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

private val datatypeFactory = DatatypeFactory.newInstance()

fun XMLGregorianCalendar.toLocalDate() = LocalDate.of(year, month, if (day == DatatypeConstants.FIELD_UNDEFINED) 1 else day)

fun LocalDate.toXmlGregorianCalendar() = this.let {
    val gcal = GregorianCalendar.from(this.atStartOfDay(ZoneOffset.UTC))
    datatypeFactory.newXMLGregorianCalendar(gcal)
}

fun YearMonth.toXmlGregorianCalendar() = atDay(1).toXmlGregorianCalendar()
