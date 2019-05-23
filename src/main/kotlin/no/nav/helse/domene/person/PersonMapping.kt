package no.nav.helse.domene.person

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.person.domain.Kjønn.KVINNE
import no.nav.helse.domene.person.domain.Kjønn.MANN
import no.nav.helse.domene.person.domain.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus

object PersonMapper {
    private fun status(personstatus: Personstatus) = if ("DØDD" == personstatus.personstatus.value) "DØD" else personstatus.personstatus.value

    fun toPerson(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) =
            Person(
                    AktørId((person.aktoer as AktoerId).aktoerId),
                    person.personnavn.fornavn,
                    person.personnavn.mellomnavn,
                    person.personnavn.etternavn,
                    person.foedselsdato.foedselsdato.toLocalDate(),
                    if (person.kjoenn.kjoenn.value == "M") MANN else KVINNE,
                    person.statsborgerskap.land.value,
                    status(person.personstatus),
                    person.bostedsadresse?.strukturertAdresse?.landkode?.value,
                    person.diskresjonskode?.value
            )
}
