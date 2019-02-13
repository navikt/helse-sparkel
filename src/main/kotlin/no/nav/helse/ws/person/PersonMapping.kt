package no.nav.helse.ws.person

import no.nav.helse.common.*
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
                if (response.person.kjoenn.kjoenn.value == "M") MANN else KVINNE,
                tpsPerson.bostedsadresse.strukturertAdresse.landkode.value
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
        val kjønn: Kjønn,
        val bostedsland: String
)
