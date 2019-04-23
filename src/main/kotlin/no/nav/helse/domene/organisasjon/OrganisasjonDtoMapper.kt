package no.nav.helse.domene.organisasjon

import no.nav.helse.domene.organisasjon.domain.Organisasjon
import no.nav.helse.domene.organisasjon.dto.OrganisasjonDTO

object OrganisasjonDtoMapper {

    fun toDto(organisasjon: Organisasjon) = OrganisasjonDTO(organisasjon.orgnr.value, organisasjon.navn, organisasjon.type())
}
