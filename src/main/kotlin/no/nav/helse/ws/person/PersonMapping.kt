package no.nav.helse.ws.person

import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.Kjønn.KVINNE
import no.nav.helse.ws.person.Kjønn.MANN
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import java.time.LocalDate

object PersonMapper {
    fun toPerson(tpsPerson: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Person {
        return Person(
                AktørId((tpsPerson.aktoer as AktoerId).aktoerId),
                tpsPerson.personnavn.fornavn,
                tpsPerson.personnavn.mellomnavn,
                tpsPerson.personnavn.etternavn,
                tpsPerson.foedselsdato.foedselsdato.toLocalDate(),
                if (tpsPerson.kjoenn.kjoenn.value == "M") MANN else KVINNE,
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
