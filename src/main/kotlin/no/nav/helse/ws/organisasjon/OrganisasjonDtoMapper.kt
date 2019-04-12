package no.nav.helse.ws.organisasjon

import no.nav.helse.ws.organisasjon.domain.Organisasjon
import no.nav.helse.ws.organisasjon.dto.OrganisasjonDTO

object OrganisasjonDtoMapper {

    fun toDto(organisasjon: Organisasjon) = OrganisasjonDTO(organisasjon.orgnr.value, organisasjon.navn, organisasjon.type())
}
