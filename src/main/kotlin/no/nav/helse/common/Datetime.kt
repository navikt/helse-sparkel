package no.nav.helse.common

import java.time.*
import java.util.*
import javax.xml.datatype.*

private val datatypeFactory = DatatypeFactory.newInstance()

fun XMLGregorianCalendar.toLocalDate() = LocalDate.of(year, month, day)

fun LocalDate.toXmlGregorianCalendar() = this.let {
    val gcal = GregorianCalendar.from(this.atStartOfDay(ZoneId.systemDefault()))
    datatypeFactory.newXMLGregorianCalendar(gcal)
}