package no.nav.helse.ws.person

import no.nav.helse.ws.*
import no.nav.helse.ws.person.Kjønn.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import java.time.*
import javax.xml.datatype.*

private val datatypeFactory = DatatypeFactory.newInstance()

object PersonMapper {
    fun toPerson(response: HentPersonResponse): Person {
        val tpsPerson = response.person

        return Person(
                AktørId((tpsPerson.aktoer as AktoerId).aktoerId),
                tpsPerson.personnavn.fornavn,
                tpsPerson.personnavn.mellomnavn,
                tpsPerson.personnavn.etternavn,
                tpsPerson.foedselsdato.foedselsdato.toLocalDate(),
                if (response.person.kjoenn.kjoenn.value == "M") MANN else KVINNE
        )
    }
}

enum class Kjønn {
    MANN, KVINNE
}

data class Person(
        val id: AktørId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn
)

fun XMLGregorianCalendar.toLocalDate() = LocalDate.of(year, month, day)

fun LocalDate.toXmlGregorianCalendar() = this.let { localDate ->
    datatypeFactory.newXMLGregorianCalendar().apply {
        year = localDate.year
        month = localDate.monthValue
        day = localDate.dayOfMonth
    }
}