package no.nav.helse.domene.ytelse

import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.helse.domene.ytelse.dto.YtelseDto

object YtelseDtoMapper {

    fun toDto(ytelse: Ytelse) =
            YtelseDto(
                    kilde = ytelse.kilde.type(),
                    tema = ytelse.tema,
                    fom = ytelse.fom,
                    tom = ytelse.tom
            )
}
