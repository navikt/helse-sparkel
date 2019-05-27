package no.nav.helse.domene.person

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.person.domain.Barn
import no.nav.helse.domene.person.domain.Kjønn.KVINNE
import no.nav.helse.domene.person.domain.Kjønn.MANN
import no.nav.helse.domene.person.domain.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId

object PersonMapper {

    fun toPerson(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) =
            Person(
                    person.mapAktørId(),
                    person.personnavn.fornavn,
                    person.personnavn.mellomnavn,
                    person.personnavn.etternavn,
                    person.mapFødselsdato(),
                    person.mapKjønn(),
                    person.statsborgerskap.land.value,
                    person.mapPersonstatus(),
                    person.bostedsadresse?.strukturertAdresse?.landkode?.value,
                    person.diskresjonskode?.value
            )

    fun toBarn(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) =
            Barn(
                    person.mapAktørId(),
                    person.personnavn.fornavn,
                    person.personnavn.mellomnavn,
                    person.personnavn.etternavn,
                    person.mapFødselsdato(),
                    person.mapKjønn(),
                    person.statsborgerskap.land.value,
                    person.mapPersonstatus(),
                    person.diskresjonskode?.value
            )
}

private fun no.nav.tjeneste.virksomhet.person.v3.informasjon.Person.mapKjønn() = if (kjoenn.kjoenn.value == "M") MANN else KVINNE
private fun no.nav.tjeneste.virksomhet.person.v3.informasjon.Person.mapAktørId() = AktørId((aktoer as AktoerId).aktoerId)
private fun no.nav.tjeneste.virksomhet.person.v3.informasjon.Person.mapFødselsdato() = foedselsdato.foedselsdato.toLocalDate()
private fun no.nav.tjeneste.virksomhet.person.v3.informasjon.Person.mapPersonstatus() = if (personstatus.personstatus.value == "DØDD") "DØD" else personstatus.personstatus.value