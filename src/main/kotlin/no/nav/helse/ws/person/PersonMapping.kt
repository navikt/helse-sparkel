package no.nav.helse.ws.person

import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.domain.Kjønn.KVINNE
import no.nav.helse.ws.person.domain.Kjønn.MANN
import no.nav.helse.ws.person.domain.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId

object PersonMapper {
    fun toPerson(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) =
            Person(
                    AktørId((person.aktoer as AktoerId).aktoerId),
                    person.personnavn.fornavn,
                    person.personnavn.mellomnavn,
                    person.personnavn.etternavn,
                    person.foedselsdato.foedselsdato.toLocalDate(),
                    if (person.kjoenn.kjoenn.value == "M") MANN else KVINNE,
                    person.statsborgerskap?.land?.value,
                    person.personstatus?.personstatus?.value,
                    person.bostedsadresse?.strukturertAdresse?.landkode?.value,
                    person.diskresjonskode?.value
            )
}
