package no.nav.helse.domene.aiy.organisasjon

import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon
import no.nav.helse.domene.aiy.organisasjon.dto.OrganisasjonDTO

object OrganisasjonDtoMapper {

    fun toDto(organisasjon: Organisasjon) = OrganisasjonDTO(organisasjon.orgnr.value, organisasjon.navn, organisasjon.type())
}
