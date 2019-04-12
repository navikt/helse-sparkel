package no.nav.helse.ws.person

import no.nav.helse.ws.person.domain.Person
import no.nav.helse.ws.person.dto.PersonDTO

object PersonDtoMapper {

    fun toDto(person: Person) =
            PersonDTO(
                    aktørId = person.id.aktor,
                    fornavn = person.fornavn,
                    mellomnavn = person.mellomnavn,
                    etternavn = person.etternavn,
                    kjønn = person.kjønn,
                    fdato = person.fdato,
                    statsborgerskap = person.statsborgerskap,
                    status = person.status,
                    bostedsland = person.bostedsland,
                    diskresjonskode = person.diskresjonskode
            )
}
