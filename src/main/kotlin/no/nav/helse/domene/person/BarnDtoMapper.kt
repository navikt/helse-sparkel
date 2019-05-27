package no.nav.helse.domene.person

import no.nav.helse.domene.person.domain.Barn
import no.nav.helse.domene.person.dto.BarnDTO

object BarnDtoMapper {
    private fun toDto(barn : Barn) = no.nav.helse.domene.person.dto.Barn(
            aktørId = barn.id.aktor,
            fornavn = barn.fornavn,
            mellomnavn = barn.mellomnavn,
            etternavn = barn.etternavn,
            fdato = barn.fdato,
            kjønn = barn.kjønn,
            statsborgerskap = barn.statsborgerskap,
            status = barn.status,
            diskresjonskode = barn.diskresjonskode
    )

    internal fun toDto(barn : List<Barn>) = BarnDTO(barn.map { toDto(it) })

}